package org.markeb.gateway.handler;

import org.markeb.gateway.backend.BackendChannelManager;
import org.markeb.gateway.route.NodeRouter;
import org.markeb.gateway.route.strategy.RouteStrategy;
import org.markeb.gateway.session.GatewaySession;
import org.markeb.gateway.session.SessionManager;
import org.markeb.net.gateway.GatewayPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 前端连接处理器
 * 处理客户端到网关的连接、消息和心跳
 */
public class FrontendHandler extends SimpleChannelInboundHandler<GatewayPacket> {

    private static final Logger log = LoggerFactory.getLogger(FrontendHandler.class);

    /**
     * 心跳消息ID（保留，框架内部使用）
     */
    private static final int HEARTBEAT_REQUEST_ID = 0;
    private static final int HEARTBEAT_RESPONSE_ID = 1;

    /**
     * 登录消息ID（需要根据实际协议定义）
     */
    private static final int MSG_ID_LOGIN = 11000;

    /**
     * 最大丢失心跳次数
     */
    private static final int MAX_MISSED_HEARTBEATS = 3;

    private final SessionManager sessionManager;
    private final BackendChannelManager backendChannelManager;
    private final NodeRouter nodeRouter;
    private final AtomicInteger missedHeartbeats = new AtomicInteger(0);

    private GatewaySession session;

    public FrontendHandler(SessionManager sessionManager,
                           BackendChannelManager backendChannelManager,
                           NodeRouter nodeRouter) {
        this.sessionManager = sessionManager;
        this.backendChannelManager = backendChannelManager;
        this.nodeRouter = nodeRouter;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 创建会话
        session = sessionManager.createSession(ctx.channel());
        log.info("Client connected, session: {}", session.getSessionId());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GatewayPacket msg) {
        if (session == null) {
            log.error("Session is null, dropping message");
            return;
        }

        // 收到任何消息都重置丢失计数
        missedHeartbeats.set(0);
        session.updateActiveTime();

        // 处理心跳请求（框架内部，业务层无感知）
        if (msg.getMsgId() == HEARTBEAT_REQUEST_ID) {
            handleHeartbeatRequest(ctx, msg);
            return;
        }

        // 心跳响应不应该从客户端收到，忽略
        if (msg.getMsgId() == HEARTBEAT_RESPONSE_ID) {
            return;
        }

        // 处理特殊消息（如登录）
        if (msg.getMsgId() == MSG_ID_LOGIN) {
            handleLogin(msg);
            return;
        }

        // 检查是否已认证
        if (!session.isAuthenticated()) {
            log.warn("Session {} not authenticated, dropping msgId {}",
                    session.getSessionId(), msg.getMsgId());
            return;
        }

        // 路由到后端节点
        routeToBackend(msg);
    }

    /**
     * 处理心跳请求
     */
    private void handleHeartbeatRequest(ChannelHandlerContext ctx, GatewayPacket request) {
        long clientTime = 0;
        byte[] requestBody = request.getBody();
        if (requestBody != null && requestBody.length >= 8) {
            clientTime = ByteBuffer.wrap(requestBody).getLong();
        }

        // 构建响应：serverTime (8 bytes) + clientTime (8 bytes)
        byte[] body = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(body);
        buffer.putLong(System.currentTimeMillis());
        buffer.putLong(clientTime);

        GatewayPacket response = new GatewayPacket(0, HEARTBEAT_RESPONSE_ID, request.getSeq(), body);
        ctx.writeAndFlush(response);
        log.debug("Heartbeat response sent to session: {}", session.getSessionId());
    }

    /**
     * 处理登录请求
     */
    private void handleLogin(GatewayPacket packet) {
        session.setState(GatewaySession.SessionState.AUTHENTICATING);

        // 选择节点
        nodeRouter.selectNode(session, RouteStrategy.Type.ROUND_ROBIN)
                .ifPresentOrElse(
                        nodeAddress -> {
                            // 转发登录请求到后端
                            backendChannelManager.forward(nodeAddress, session, packet)
                                    .whenComplete((v, ex) -> {
                                        if (ex != null) {
                                            log.error("Forward login failed for session {}",
                                                    session.getSessionId(), ex);
                                            // 可以发送错误响应给客户端
                                        }
                                    });
                        },
                        () -> {
                            log.error("No available node for login, session {}", session.getSessionId());
                            // 发送错误响应给客户端
                        }
                );
    }

    /**
     * 路由消息到后端节点
     */
    private void routeToBackend(GatewayPacket packet) {
        nodeRouter.getNodeAddress(session.getNodeId())
                .ifPresentOrElse(
                        nodeAddress -> {
                            backendChannelManager.forward(nodeAddress, session, packet)
                                    .exceptionally(ex -> {
                                        log.error("Forward failed for session {} msgId {}",
                                                session.getSessionId(), packet.getMsgId(), ex);
                                        return null;
                                    });
                        },
                        () -> {
                            log.error("Node {} not found for session {}",
                                    session.getNodeId(), session.getSessionId());
                        }
                );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (session != null) {
            log.info("Client disconnected, session: {}", session.getSessionId());

            // 清理 pending 请求
            backendChannelManager.cleanupSession(session.getSessionId());

            // 移除会话
            sessionManager.removeSession(session.getSessionId());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.READER_IDLE) {
                // 读空闲：客户端长时间没有发送任何消息（包括心跳）
                int missed = missedHeartbeats.incrementAndGet();
                if (missed >= MAX_MISSED_HEARTBEATS) {
                    log.info("Client idle timeout, closing session: {}",
                            session != null ? session.getSessionId() : "null");
                    ctx.close();
                } else {
                    log.debug("Client reader idle, session: {}, missed: {}",
                            session != null ? session.getSessionId() : "null", missed);
                }
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Frontend handler error, session: {}",
                session != null ? session.getSessionId() : "null", cause);
        ctx.close();
    }
}

