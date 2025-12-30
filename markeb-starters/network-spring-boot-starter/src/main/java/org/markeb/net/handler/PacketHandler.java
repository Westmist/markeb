package org.markeb.net.handler;

import org.markeb.net.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据包处理器
 * 接收 Packet 并交给 MessageDispatcher 处理
 */
public class PacketHandler extends SimpleChannelInboundHandler<Packet> {

    private static final Logger log = LoggerFactory.getLogger(PacketHandler.class);

    private final MessageDispatcher messageDispatcher;

    public PacketHandler(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel active: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        log.debug("Received packet: messageId={}, seq={}", packet.getMessageId(), packet.getSeq());
        messageDispatcher.dispatch(ctx, packet);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel inactive: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 空闲事件由 HeartbeatHandler 处理
        // 如果没有配置心跳处理器，这里作为兜底处理
        if (evt instanceof IdleStateEvent idleEvent) {
            log.warn("Channel idle (no heartbeat handler): {}, state: {}",
                    ctx.channel().remoteAddress(), idleEvent.state());
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}

