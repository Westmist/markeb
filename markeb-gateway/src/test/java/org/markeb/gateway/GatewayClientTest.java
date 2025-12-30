package org.markeb.gateway;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.junit.jupiter.api.Test;
import org.markeb.proto.message.Login.ReqLoginMessage;
import org.markeb.proto.message.Login.ResLoginMessage;
import org.markeb.proto.message.Test.ReqTestMessage;
import org.markeb.proto.message.Test.ResTestMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 网关客户端测试：测试连接网关并发送/接收消息。
 * <p>
 * 支持多种协议格式测试：
 * <ul>
 *   <li>Protobuf - Google Protocol Buffers</li>
 *   <li>JSON - Jackson JSON</li>
 * </ul>
 * <p>
 * 客户端 -> 网关协议头： length(4) + msgId(4) + seq(4) + body(n)
 * <p>
 * 使用前确保网关服务已启动在 7000 端口。
 */
class GatewayClientTest {

    private static final String GATEWAY_HOST = "127.0.0.1";
    private static final int GATEWAY_PORT = 7000;

    // ======================== Protobuf 协议测试 ========================

    /**
     * 测试连接网关并发送登录请求（Protobuf）
     */
    @Test
    void testProtobuf_LoginMessage() throws InterruptedException {
        TestResult result = sendProtobufMessage(
            11000,  // ReqLoginMessage msgId
            ReqLoginMessage.newBuilder()
                .setOpenId("test_user_001")
                .setToken("test_token_abc123")
                .build()
                .toByteArray(),
            11001,  // 期望响应的 msgId
            body -> {
                try {
                    ResLoginMessage response = ResLoginMessage.parseFrom(body);
                    System.out.println("[Protobuf] 登录响应: openId=" + response.getOpenId() + ", success=" + response.getSuccess());
                    return response.getSuccess();
                } catch (Exception e) {
                    System.err.println("[Protobuf] 解析登录响应失败: " + e.getMessage());
                    return false;
                }
            }
        );

        assertTrue(result.connected, "未成功连接到网关");
        assertTrue(result.messageSent, "消息未发送");
        System.out.println("[Protobuf] 登录测试完成，收到响应: " + result.responseReceived);
    }

    /**
     * 测试发送测试消息（Protobuf）
     */
    @Test
    void testProtobuf_TestMessage() throws InterruptedException {
        TestResult result = sendProtobufMessage(
            10001,  // ReqTestMessage msgId
            ReqTestMessage.newBuilder()
                .setId(12345)
                .setName("测试消息")
                .build()
                .toByteArray(),
            10002,  // 期望响应的 msgId
            body -> {
                try {
                    ResTestMessage response = ResTestMessage.parseFrom(body);
                    System.out.println("[Protobuf] 测试响应: result=" + response.getResult());
                    return true;
                } catch (Exception e) {
                    System.err.println("[Protobuf] 解析测试响应失败: " + e.getMessage());
                    return false;
                }
            }
        );

        assertTrue(result.connected, "未成功连接到网关");
        assertTrue(result.messageSent, "消息未发送");
    }

    /**
     * 测试发送多条消息（Protobuf）
     */
    @Test
    void testProtobuf_MultipleMessages() throws InterruptedException {
        int messageCount = 5;
        AtomicInteger receivedCount = new AtomicInteger(0);
        
        TestResult result = sendMultipleMessages(
            messageCount,
            seq -> {
                ReqTestMessage msg = ReqTestMessage.newBuilder()
                    .setId(seq)
                    .setName("批量消息-" + seq)
                    .build();
                return new ClientPacket(10001, seq, msg.toByteArray());
            },
            (msgId, seq, body) -> {
                System.out.println("[Protobuf] 收到批量响应: msgId=" + msgId + ", seq=" + seq);
                receivedCount.incrementAndGet();
            }
        );

        assertTrue(result.connected, "未成功连接到网关");
        System.out.println("[Protobuf] 批量测试完成，发送: " + messageCount + ", 收到: " + receivedCount.get());
    }

    // ======================== JSON 协议测试 ========================

    /**
     * 测试发送 JSON 消息
     * <p>
     * 注意：需要服务端配置支持 JSON 编解码
     */
    @Test
    void testJson_SimpleMessage() throws InterruptedException {
        // JSON 消息体
        String jsonBody = """
            {
                "action": "ping",
                "timestamp": %d,
                "data": {
                    "message": "Hello from client"
                }
            }
            """.formatted(System.currentTimeMillis());

        TestResult result = sendRawMessage(
            20001,  // 假设的 JSON 消息 ID
            jsonBody.getBytes(StandardCharsets.UTF_8),
            (msgId, seq, body) -> {
                String response = new String(body, StandardCharsets.UTF_8);
                System.out.println("[JSON] 收到响应: msgId=" + msgId + ", body=" + response);
            }
        );

        assertTrue(result.connected, "未成功连接到网关");
        assertTrue(result.messageSent, "JSON 消息未发送");
    }

    // ======================== 协议压力测试 ========================

    /**
     * 测试高频发送消息
     */
    @Test
    void testHighFrequencyMessages() throws InterruptedException {
        int messageCount = 100;
        AtomicInteger sentCount = new AtomicInteger(0);
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(messageCount);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = createBootstrap(group, new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) {
                    System.out.println("[压力测试] 开始发送 " + messageCount + " 条消息");
                    
                    // 高频发送消息
                    for (int i = 1; i <= messageCount; i++) {
                        ReqTestMessage msg = ReqTestMessage.newBuilder()
                            .setId(i)
                            .setName("高频消息-" + i)
                            .build();
                        
                        ClientPacket packet = new ClientPacket(10001, i, msg.toByteArray());
                        ctx.write(packet);
                        sentCount.incrementAndGet();
                    }
                    ctx.flush();
                    System.out.println("[压力测试] 已发送 " + sentCount.get() + " 条消息");
                }

                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                    receivedCount.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    System.err.println("[压力测试] 异常: " + cause.getMessage());
                    ctx.close();
                }
            });

            ChannelFuture cf = bootstrap.connect(GATEWAY_HOST, GATEWAY_PORT).sync();
            
            // 等待所有响应或超时
            boolean allReceived = latch.await(30, TimeUnit.SECONDS);
            
            cf.channel().close();
            cf.channel().closeFuture().sync();

            System.out.println("[压力测试] 完成: 发送=" + sentCount.get() + ", 收到=" + receivedCount.get() + ", 全部收到=" + allReceived);
            
        } finally {
            group.shutdownGracefully().sync();
        }

        assertEquals(messageCount, sentCount.get(), "发送数量不匹配");
    }

    // ======================== 协议兼容性测试 ========================

    /**
     * 测试发送空消息体
     */
    @Test
    void testEmptyBodyMessage() throws InterruptedException {
        TestResult result = sendRawMessage(
            10003,  // ReqTestVoidMessage msgId (空消息)
            new byte[0],
            (msgId, seq, body) -> {
                System.out.println("[空消息] 收到响应: msgId=" + msgId + ", bodyLen=" + body.length);
            }
        );

        assertTrue(result.connected, "未成功连接到网关");
        assertTrue(result.messageSent, "空消息未发送");
    }

    /**
     * 测试发送大消息体
     */
    @Test
    void testLargeBodyMessage() throws InterruptedException {
        // 构建一个较大的消息（100KB）
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("测试数据-").append(i).append(";");
        }
        
        ReqTestMessage largeMsg = ReqTestMessage.newBuilder()
            .setId(99999)
            .setName(largeContent.toString())
            .build();

        System.out.println("[大消息] 消息体大小: " + largeMsg.toByteArray().length + " bytes");

        TestResult result = sendProtobufMessage(
            10001,
            largeMsg.toByteArray(),
            10002,
            body -> {
                System.out.println("[大消息] 收到响应，bodyLen=" + body.length);
                return true;
            }
        );

        assertTrue(result.connected, "未成功连接到网关");
        assertTrue(result.messageSent, "大消息未发送");
    }

    // ======================== 辅助方法 ========================

    /**
     * 发送 Protobuf 消息并等待响应
     */
    private TestResult sendProtobufMessage(int msgId, byte[] body, int expectedResponseMsgId, 
                                           ResponseParser responseParser) throws InterruptedException {
        TestResult result = new TestResult();
        CountDownLatch latch = new CountDownLatch(1);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = createBootstrap(group, new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) {
                    result.connected = true;
                    System.out.println("[客户端] 已连接到网关: " + ctx.channel().remoteAddress());

                    ClientPacket packet = new ClientPacket(msgId, 1, body);
                    ctx.writeAndFlush(packet);
                    result.messageSent = true;
                    System.out.println("[客户端] 已发送消息: msgId=" + msgId + ", bodyLen=" + body.length);
                }

                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                    int respMsgId = msg.readInt();
                    int seq = msg.readInt();
                    byte[] respBody = new byte[msg.readableBytes()];
                    msg.readBytes(respBody);

                    System.out.println("[客户端] 收到响应: msgId=" + respMsgId + ", seq=" + seq + ", bodyLen=" + respBody.length);

                    if (respMsgId == expectedResponseMsgId) {
                        result.responseReceived = true;
                        result.parseSuccess = responseParser.parse(respBody);
                    }
                    latch.countDown();
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    System.err.println("[客户端] 异常: " + cause.getMessage());
                    cause.printStackTrace();
                    ctx.close();
                    latch.countDown();
                }
            });

            ChannelFuture cf = bootstrap.connect(GATEWAY_HOST, GATEWAY_PORT).sync();
            latch.await(5, TimeUnit.SECONDS);
            cf.channel().close();
            cf.channel().closeFuture().sync();

        } finally {
            group.shutdownGracefully().sync();
        }

        return result;
    }

    /**
     * 发送原始消息（支持任意协议）
     */
    private TestResult sendRawMessage(int msgId, byte[] body, 
                                      ResponseHandler responseHandler) throws InterruptedException {
        TestResult result = new TestResult();
        CountDownLatch latch = new CountDownLatch(1);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = createBootstrap(group, new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) {
                    result.connected = true;
                    ClientPacket packet = new ClientPacket(msgId, 1, body);
                    ctx.writeAndFlush(packet);
                    result.messageSent = true;
                    System.out.println("[客户端] 已发送原始消息: msgId=" + msgId);
                }

                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                    int respMsgId = msg.readInt();
                    int seq = msg.readInt();
                    byte[] respBody = new byte[msg.readableBytes()];
                    msg.readBytes(respBody);

                    result.responseReceived = true;
                    responseHandler.handle(respMsgId, seq, respBody);
                    latch.countDown();
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    System.err.println("[客户端] 异常: " + cause.getMessage());
                    ctx.close();
                    latch.countDown();
                }
            });

            ChannelFuture cf = bootstrap.connect(GATEWAY_HOST, GATEWAY_PORT).sync();
            latch.await(5, TimeUnit.SECONDS);
            cf.channel().close();
            cf.channel().closeFuture().sync();

        } finally {
            group.shutdownGracefully().sync();
        }

        return result;
    }

    /**
     * 发送多条消息
     */
    private TestResult sendMultipleMessages(int count, PacketBuilder packetBuilder,
                                            ResponseHandler responseHandler) throws InterruptedException {
        TestResult result = new TestResult();
        CountDownLatch latch = new CountDownLatch(count);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = createBootstrap(group, new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) {
                    result.connected = true;
                    System.out.println("[客户端] 开始发送 " + count + " 条消息");

                    for (int i = 1; i <= count; i++) {
                        ClientPacket packet = packetBuilder.build(i);
                        ctx.write(packet);
                    }
                    ctx.flush();
                    result.messageSent = true;
                }

                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                    int respMsgId = msg.readInt();
                    int seq = msg.readInt();
                    byte[] respBody = new byte[msg.readableBytes()];
                    msg.readBytes(respBody);

                    responseHandler.handle(respMsgId, seq, respBody);
                    latch.countDown();
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    System.err.println("[客户端] 异常: " + cause.getMessage());
                    ctx.close();
                }
            });

            ChannelFuture cf = bootstrap.connect(GATEWAY_HOST, GATEWAY_PORT).sync();
            latch.await(10, TimeUnit.SECONDS);
            cf.channel().close();
            cf.channel().closeFuture().sync();

        } finally {
            group.shutdownGracefully().sync();
        }

        return result;
    }

    /**
     * 创建 Bootstrap
     */
    private Bootstrap createBootstrap(EventLoopGroup group, ChannelHandler handler) {
        return new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4))
                        .addLast(new ClientEncoder())
                        .addLast(handler);
                }
            });
    }

    // ======================== 内部类 ========================

    /**
     * 测试结果
     */
    private static class TestResult {
        boolean connected = false;
        boolean messageSent = false;
        boolean responseReceived = false;
        boolean parseSuccess = false;
    }

    /**
     * 客户端数据包
     */
    private static class ClientPacket {
        final int msgId;
        final int seq;
        final byte[] body;

        ClientPacket(int msgId, int seq, byte[] body) {
            this.msgId = msgId;
            this.seq = seq;
            this.body = body;
        }
    }

    /**
     * 客户端编码器
     * 协议格式：length(4) + msgId(4) + seq(4) + body(n)
     */
    private static class ClientEncoder extends MessageToByteEncoder<ClientPacket> {
        @Override
        protected void encode(ChannelHandlerContext ctx, ClientPacket packet, ByteBuf out) {
            int bodyLen = packet.body == null ? 0 : packet.body.length;
            out.writeInt(8 + bodyLen);  // length = msgId(4) + seq(4) + body
            out.writeInt(packet.msgId);
            out.writeInt(packet.seq);
            if (bodyLen > 0) {
                out.writeBytes(packet.body);
            }
        }
    }

    /**
     * 响应解析器
     */
    @FunctionalInterface
    private interface ResponseParser {
        boolean parse(byte[] body);
    }

    /**
     * 响应处理器
     */
    @FunctionalInterface
    private interface ResponseHandler {
        void handle(int msgId, int seq, byte[] body);
    }

    /**
     * 数据包构建器
     */
    @FunctionalInterface
    private interface PacketBuilder {
        ClientPacket build(int seq);
    }
}
