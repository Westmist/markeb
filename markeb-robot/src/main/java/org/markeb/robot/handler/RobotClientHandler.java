package org.markeb.robot.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.markeb.robot.client.RobotClient;
import org.markeb.robot.protocol.RobotPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Robot 客户端消息处理器
 * <p>
 * 处理消息接收和心跳机制
 */
public class RobotClientHandler extends SimpleChannelInboundHandler<RobotPacket> {

    private static final Logger log = LoggerFactory.getLogger(RobotClientHandler.class);

    /**
     * 最大丢失心跳次数
     */
    private static final int MAX_MISSED_HEARTBEATS = 3;

    private final RobotClient robotClient;
    private final AtomicInteger missedHeartbeats = new AtomicInteger(0);

    public RobotClientHandler(RobotClient robotClient) {
        this.robotClient = robotClient;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("[{}] Channel active", robotClient.getRobotId());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RobotPacket packet) {
        // 收到任何消息都重置丢失计数
        missedHeartbeats.set(0);

        // 心跳响应不传递给业务层
        if (packet.isHeartbeatResponse()) {
            long latency = packet.calculateHeartbeatLatency();
            if (latency >= 0) {
                log.debug("[{}] Heartbeat response, latency: {}ms", robotClient.getRobotId(), latency);
            }
            return;
        }

        // 心跳请求自动响应（通常客户端不会收到请求，但以防万一）
        if (packet.isHeartbeatRequest()) {
            ctx.writeAndFlush(RobotPacket.createHeartbeatResponse(packet));
            return;
        }

        log.debug("[{}] Received message: msgId={}", robotClient.getRobotId(), packet.getMsgId());
        robotClient.handleMessage(packet);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            switch (idleEvent.state()) {
                case WRITER_IDLE -> {
                    // 写空闲：发送心跳
                    robotClient.sendHeartbeat();
                }
                case READER_IDLE -> {
                    // 读空闲：检测连接是否存活
                    int missed = missedHeartbeats.incrementAndGet();
                    if (missed >= MAX_MISSED_HEARTBEATS) {
                        log.warn("[{}] Missed {} heartbeats, closing connection",
                                robotClient.getRobotId(), missed);
                        ctx.close();
                    } else {
                        log.debug("[{}] Reader idle, missed heartbeats: {}",
                                robotClient.getRobotId(), missed);
                    }
                }
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("[{}] Channel inactive", robotClient.getRobotId());
        robotClient.onDisconnected();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[{}] Exception caught", robotClient.getRobotId(), cause);
        ctx.close();
    }
}

