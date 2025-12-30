package org.markeb.gateway.backend;

import org.markeb.gateway.session.GatewaySession;
import org.markeb.gateway.session.SessionManager;
import org.markeb.net.gateway.GatewayPacket;
import org.markeb.net.gateway.codec.GatewayDecoder;
import org.markeb.net.gateway.codec.GatewayEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 后端连接管理器
 * 管理网关到游戏节点的连接
 */
@Component
public class BackendChannelManager {

    private static final Logger log = LoggerFactory.getLogger(BackendChannelManager.class);

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(4);

    /**
     * nodeAddress -> Channel（到后端节点的连接）
     */
    private final Map<String, Channel> backendChannels = new ConcurrentHashMap<>();

    /**
     * pendingKey (sessionId:seq) -> sessionId
     * 用于后端响应时找到对应的前端会话
     */
    private final Map<String, Integer> pendingRequests = new ConcurrentHashMap<>();

    @Autowired
    private SessionManager sessionManager;

    /**
     * 转发请求到后端节点
     *
     * @param nodeAddress 节点地址 (host:port)
     * @param session     网关会话
     * @param packet      协议包
     */
    public CompletableFuture<Void> forward(String nodeAddress, GatewaySession session, GatewayPacket packet) {
        Channel backend = getOrCreateChannel(nodeAddress);
        if (backend == null || !backend.isActive()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Backend not available: " + nodeAddress));
        }

        // 构建内部协议包（带 sessionId）
        GatewayPacket internalPacket = new GatewayPacket(
                session.getSessionId(),
                packet.getMsgId(),
                packet.getSeq(),
                packet.getBody()
        );

        // 记录 pending 请求
        String pendingKey = internalPacket.getPendingKey();
        pendingRequests.put(pendingKey, session.getSessionId());

        CompletableFuture<Void> future = new CompletableFuture<>();
        backend.writeAndFlush(internalPacket).addListener(f -> {
            if (f.isSuccess()) {
                session.updateActiveTime();
                future.complete(null);
            } else {
                pendingRequests.remove(pendingKey);
                future.completeExceptionally(f.cause());
            }
        });

        return future;
    }

    /**
     * 处理后端响应
     */
    public void handleResponse(String nodeAddress, GatewayPacket packet) {
        String pendingKey = packet.getPendingKey();
        Integer sessionId = pendingRequests.remove(pendingKey);

        if (sessionId == null) {
            log.warn("No pending request for key: {}", pendingKey);
            return;
        }

        sessionManager.getSession(sessionId).ifPresentOrElse(
                session -> {
                    if (session.isActive()) {
                        // 回给客户端（不带 sessionId）
                        GatewayPacket clientPacket = new GatewayPacket(
                                0, packet.getMsgId(), packet.getSeq(), packet.getBody());
                        session.send(clientPacket);
                    } else {
                        log.warn("Session {} is not active, dropping response", sessionId);
                    }
                },
                () -> log.warn("Session {} not found for response", sessionId)
        );
    }

    /**
     * 获取或创建到后端的连接
     */
    private Channel getOrCreateChannel(String nodeAddress) {
        return backendChannels.computeIfAbsent(nodeAddress, this::createChannel);
    }

    /**
     * 创建到后端的连接
     */
    private Channel createChannel(String nodeAddress) {
        String[] parts = nodeAddress.split(":");
        if (parts.length != 2) {
            log.error("Invalid node address: {}", nodeAddress);
            return null;
        }

        String host = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            log.error("Invalid port in address: {}", nodeAddress);
            return null;
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 读空闲60秒检测连接存活，写空闲30秒发送心跳
                                .addLast(new IdleStateHandler(60, 30, 0, TimeUnit.SECONDS))
                                .addLast(new GatewayDecoder(false))
                                .addLast(new GatewayEncoder(false))
                                .addLast(new BackendChannelHandler(BackendChannelManager.this, nodeAddress));
                    }
                });

        try {
            ChannelFuture cf = bootstrap.connect(new InetSocketAddress(host, port)).sync();
            Channel channel = cf.channel();

            // 监听连接关闭
            channel.closeFuture().addListener(f -> {
                log.info("Backend channel closed: {}", nodeAddress);
                backendChannels.remove(nodeAddress);
            });

            log.info("Connected to backend node: {}", nodeAddress);
            return channel;
        } catch (Exception e) {
            log.error("Failed to connect to backend: {}", nodeAddress, e);
            return null;
        }
    }

    /**
     * 清理会话相关的 pending 请求
     */
    public void cleanupSession(int sessionId) {
        String prefix = sessionId + ":";
        pendingRequests.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }

    /**
     * 关闭指定节点的连接
     */
    public void closeChannel(String nodeAddress) {
        Channel channel = backendChannels.remove(nodeAddress);
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down backend channel manager...");
        backendChannels.values().forEach(ch -> {
            try {
                ch.close().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        workerGroup.shutdownGracefully();
    }
}

