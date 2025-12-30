package org.markeb.gateway.backend;

import org.markeb.net.gateway.GatewayPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 后端连接处理器
 * 处理游戏节点的响应和心跳保活
 */
public class BackendChannelHandler extends SimpleChannelInboundHandler<GatewayPacket> {

    private static final Logger log = LoggerFactory.getLogger(BackendChannelHandler.class);

    /**
     * 心跳消息ID（保留，框架内部使用）
     */
    private static final int HEARTBEAT_REQUEST_ID = 0;
    private static final int HEARTBEAT_RESPONSE_ID = 1;

    /**
     * 最大丢失心跳次数
     */
    private static final int MAX_MISSED_HEARTBEATS = 3;

    private final BackendChannelManager channelManager;
    private final String nodeAddress;
    private final AtomicInteger missedHeartbeats = new AtomicInteger(0);

    public BackendChannelHandler(BackendChannelManager channelManager, String nodeAddress) {
        this.channelManager = channelManager;
        this.nodeAddress = nodeAddress;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GatewayPacket msg) {
        // 收到任何消息都重置丢失计数
        missedHeartbeats.set(0);

        // 心跳响应不传递给业务层
        if (msg.getMsgId() == HEARTBEAT_RESPONSE_ID) {
            long latency = calculateHeartbeatLatency(msg);
            if (latency >= 0) {
                log.debug("Heartbeat response from backend {}, latency: {}ms", nodeAddress, latency);
            }
            return;
        }

        // 收到后端响应，转发给对应的前端会话
        channelManager.handleResponse(nodeAddress, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Backend channel active: {}", nodeAddress);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Backend channel inactive: {}", nodeAddress);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            switch (idleEvent.state()) {
                case WRITER_IDLE -> {
                    // 写空闲：发送心跳包保持连接
                    sendHeartbeat(ctx);
                }
                case READER_IDLE -> {
                    // 读空闲：检测连接是否存活
                    int missed = missedHeartbeats.incrementAndGet();
                    if (missed >= MAX_MISSED_HEARTBEATS) {
                        log.warn("Backend {} missed {} heartbeats, closing connection",
                                nodeAddress, missed);
                        ctx.close();
                    } else {
                        log.debug("Backend {} reader idle, missed heartbeats: {}", nodeAddress, missed);
                    }
                }
                case ALL_IDLE -> {
                    // 不处理
                }
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 发送心跳请求
     */
    private void sendHeartbeat(ChannelHandlerContext ctx) {
        byte[] body = new byte[8];
        ByteBuffer.wrap(body).putLong(System.currentTimeMillis());

        GatewayPacket heartbeat = new GatewayPacket(0, HEARTBEAT_REQUEST_ID, (short) 0, body);
        ctx.writeAndFlush(heartbeat).addListener(f -> {
            if (f.isSuccess()) {
                log.debug("Heartbeat sent to backend: {}", nodeAddress);
            } else {
                log.warn("Failed to send heartbeat to backend: {}", nodeAddress, f.cause());
            }
        });
    }

    /**
     * 计算心跳延迟
     */
    private long calculateHeartbeatLatency(GatewayPacket response) {
        byte[] body = response.getBody();
        if (body == null || body.length < 16) {
            return -1;
        }

        ByteBuffer buffer = ByteBuffer.wrap(body);
        buffer.getLong(); // skip serverTime
        long clientTime = buffer.getLong();

        if (clientTime > 0) {
            return System.currentTimeMillis() - clientTime;
        }
        return -1;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Backend channel error: {}", nodeAddress, cause);
        ctx.close();
    }
}

