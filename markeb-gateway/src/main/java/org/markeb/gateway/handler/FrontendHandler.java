package org.markeb.gateway.handler;

import org.markeb.gateway.backend.BackendChannelManager;
import org.markeb.gateway.route.NodeRouter;
import org.markeb.gateway.session.GatewaySession;
import org.markeb.gateway.session.SessionManager;
import org.markeb.net.gateway.GatewayPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 前端连接处理器
 * 处理客户端到网关的连接和消息
 */
public class FrontendHandler extends SimpleChannelInboundHandler<GatewayPacket> {

    private static final Logger log = LoggerFactory.getLogger(FrontendHandler.class);

    /**
     * 登录消息ID（需要根据实际协议定义）
     */
    private static final int MSG_ID_LOGIN = 11000;

    private final SessionManager sessionManager;
    private final BackendChannelManager backendChannelManager;
    private final NodeRouter nodeRouter;

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

        session.updateActiveTime();

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
     * 处理登录请求
     */
    private void handleLogin(GatewayPacket packet) {
        session.setState(GatewaySession.SessionState.AUTHENTICATING);

        // 选择节点
        nodeRouter.selectNode(session, NodeRouter.RouteStrategy.ROUND_ROBIN)
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
                log.info("Client idle timeout, closing session: {}",
                        session != null ? session.getSessionId() : "null");
                ctx.close();
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

