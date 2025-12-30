package org.markeb.robot.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.markeb.robot.client.RobotClient;
import org.markeb.robot.protocol.RobotPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Robot 客户端消息处理器
 */
public class RobotClientHandler extends SimpleChannelInboundHandler<RobotPacket> {

    private static final Logger log = LoggerFactory.getLogger(RobotClientHandler.class);

    private final RobotClient robotClient;

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
        log.debug("[{}] Received message: {}", robotClient.getRobotId(), packet);
        robotClient.handleMessage(packet);
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

