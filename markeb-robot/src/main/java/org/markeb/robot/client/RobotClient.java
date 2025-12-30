package org.markeb.robot.client;

import com.google.protobuf.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.markeb.robot.codec.RobotDecoder;
import org.markeb.robot.codec.RobotEncoder;
import org.markeb.robot.handler.RobotClientHandler;
import org.markeb.robot.message.RobotMessageParser;
import org.markeb.robot.protocol.RobotPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Robot 客户端
 * <p>
 * 用于连接网关服务器，发送协议消息，维持 TCP 连接
 */
public class RobotClient {

    private static final Logger log = LoggerFactory.getLogger(RobotClient.class);

    private final String host;
    private final int port;
    private final String robotId;
    private final RobotMessageParser messageParser;

    private EventLoopGroup workerGroup;
    private Channel channel;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger seqGenerator = new AtomicInteger(0);

    /**
     * 消息处理器映射 (msgId -> handler)
     */
    private final Map<Integer, Consumer<Message>> messageHandlers = new ConcurrentHashMap<>();

    /**
     * 请求-响应映射 (seq -> future)
     */
    private final Map<Integer, CompletableFuture<Message>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 重连配置
     */
    private boolean autoReconnect = true;
    private int reconnectDelaySeconds = 5;
    private int maxReconnectAttempts = 10;
    private int reconnectAttempts = 0;

    /**
     * 心跳配置（秒）
     */
    private int readerIdleTime = 60;
    private int writerIdleTime = 30;

    public RobotClient(String robotId, String host, int port, RobotMessageParser messageParser) {
        this.robotId = robotId;
        this.host = host;
        this.port = port;
        this.messageParser = messageParser;
    }

    /**
     * 连接到网关服务器
     */
    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        workerGroup = new NioEventLoopGroup(1);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 空闲检测：读空闲用于检测连接是否存活，写空闲用于触发心跳
                                .addLast(new IdleStateHandler(readerIdleTime, writerIdleTime, 0, TimeUnit.SECONDS))
                                .addLast(new RobotDecoder())
                                .addLast(new RobotEncoder(messageParser))
                                .addLast(new RobotClientHandler(RobotClient.this));
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                connected.set(true);
                reconnectAttempts = 0;
                log.info("[{}] Connected to gateway {}:{}", robotId, host, port);
                future.complete(null);
            } else {
                log.error("[{}] Failed to connect to gateway {}:{}", robotId, host, port, f.cause());
                future.completeExceptionally(f.cause());
                scheduleReconnect();
            }
        });

        return future;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        autoReconnect = false;
        if (channel != null && channel.isActive()) {
            channel.close().syncUninterruptibly();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        connected.set(false);
        log.info("[{}] Disconnected from gateway", robotId);
    }

    /**
     * 发送消息（不等待响应）
     */
    public void send(Message message) {
        if (!isConnected()) {
            log.warn("[{}] Not connected, cannot send message", robotId);
            return;
        }

        int seq = seqGenerator.incrementAndGet();
        int msgId = messageParser.getMsgId(message.getClass());

        RobotPacket packet = new RobotPacket(msgId, seq, message.toByteArray());
        channel.writeAndFlush(packet).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                log.debug("[{}] Sent message: msgId={}, seq={}", robotId, msgId, seq);
            } else {
                log.error("[{}] Failed to send message: msgId={}, seq={}", robotId, msgId, seq, f.cause());
            }
        });
    }

    /**
     * 发送请求并等待响应
     */
    public <T extends Message> CompletableFuture<T> request(Message message, long timeoutMs) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected"));
        }

        int seq = seqGenerator.incrementAndGet();
        int msgId = messageParser.getMsgId(message.getClass());

        CompletableFuture<Message> responseFuture = new CompletableFuture<>();
        pendingRequests.put(seq, responseFuture);

        // 设置超时
        workerGroup.schedule(() -> {
            CompletableFuture<Message> pending = pendingRequests.remove(seq);
            if (pending != null && !pending.isDone()) {
                pending.completeExceptionally(new java.util.concurrent.TimeoutException(
                        "Request timeout: msgId=" + msgId + ", seq=" + seq));
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);

        RobotPacket packet = new RobotPacket(msgId, seq, message.toByteArray());
        channel.writeAndFlush(packet).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                pendingRequests.remove(seq);
                responseFuture.completeExceptionally(f.cause());
            }
        });

        @SuppressWarnings("unchecked")
        CompletableFuture<T> result = (CompletableFuture<T>) responseFuture;
        return result;
    }

    /**
     * 注册消息处理器
     */
    public void registerHandler(int msgId, Consumer<Message> handler) {
        messageHandlers.put(msgId, handler);
    }

    /**
     * 注册消息处理器（通过消息类型）
     */
    public <T extends Message> void registerHandler(Class<T> messageClass, Consumer<T> handler) {
        int msgId = messageParser.getMsgId(messageClass);
        messageHandlers.put(msgId, msg -> {
            @SuppressWarnings("unchecked")
            T typedMsg = (T) msg;
            handler.accept(typedMsg);
        });
    }

    /**
     * 处理接收到的消息
     */
    public void handleMessage(RobotPacket packet) {
        int msgId = packet.getMsgId();
        int seq = packet.getSeq();

        try {
            Message message = messageParser.parse(msgId, packet.getBody());

            // 检查是否是请求的响应
            CompletableFuture<Message> pendingFuture = pendingRequests.remove(seq);
            if (pendingFuture != null) {
                pendingFuture.complete(message);
                return;
            }

            // 调用注册的处理器
            Consumer<Message> handler = messageHandlers.get(msgId);
            if (handler != null) {
                handler.accept(message);
            } else {
                log.debug("[{}] No handler for msgId: {}", robotId, msgId);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to handle message: msgId={}, seq={}", robotId, msgId, seq, e);
        }
    }

    /**
     * 处理连接断开
     */
    public void onDisconnected() {
        connected.set(false);
        log.warn("[{}] Connection lost", robotId);

        // 清理 pending 请求
        pendingRequests.forEach((seq, future) -> {
            future.completeExceptionally(new IllegalStateException("Connection lost"));
        });
        pendingRequests.clear();

        scheduleReconnect();
    }

    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        if (!autoReconnect) {
            return;
        }

        if (reconnectAttempts >= maxReconnectAttempts) {
            log.error("[{}] Max reconnect attempts reached, giving up", robotId);
            return;
        }

        reconnectAttempts++;
        log.info("[{}] Scheduling reconnect in {} seconds (attempt {}/{})",
                robotId, reconnectDelaySeconds, reconnectAttempts, maxReconnectAttempts);

        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.schedule(this::reconnect, reconnectDelaySeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 重连
     */
    private void reconnect() {
        if (connected.get()) {
            return;
        }

        log.info("[{}] Attempting to reconnect...", robotId);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 空闲检测：读空闲用于检测连接是否存活，写空闲用于触发心跳
                                .addLast(new IdleStateHandler(readerIdleTime, writerIdleTime, 0, TimeUnit.SECONDS))
                                .addLast(new RobotDecoder())
                                .addLast(new RobotEncoder(messageParser))
                                .addLast(new RobotClientHandler(RobotClient.this));
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                connected.set(true);
                reconnectAttempts = 0;
                log.info("[{}] Reconnected to gateway {}:{}", robotId, host, port);
            } else {
                log.error("[{}] Reconnect failed", robotId, f.cause());
                scheduleReconnect();
            }
        });
    }

    public boolean isConnected() {
        return connected.get() && channel != null && channel.isActive();
    }

    public String getRobotId() {
        return robotId;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public void setReconnectDelaySeconds(int reconnectDelaySeconds) {
        this.reconnectDelaySeconds = reconnectDelaySeconds;
    }

    public void setMaxReconnectAttempts(int maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
    }

    public void setReaderIdleTime(int readerIdleTime) {
        this.readerIdleTime = readerIdleTime;
    }

    public void setWriterIdleTime(int writerIdleTime) {
        this.writerIdleTime = writerIdleTime;
    }

    /**
     * 发送心跳包（框架内部使用）
     */
    public void sendHeartbeat() {
        if (!isConnected()) {
            return;
        }
        // 使用保留的消息ID 0 作为心跳请求
        RobotPacket heartbeat = RobotPacket.createHeartbeatRequest();
        channel.writeAndFlush(heartbeat).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                log.debug("[{}] Heartbeat sent", robotId);
            } else {
                log.warn("[{}] Failed to send heartbeat", robotId, f.cause());
            }
        });
    }
}

