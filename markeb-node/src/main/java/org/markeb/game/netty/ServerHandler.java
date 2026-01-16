package org.markeb.game.netty;

import org.markeb.game.actor.Player;
import org.markeb.game.manager.PlayerManager;
import org.markeb.net.msg.IMessagePool;
import org.markeb.net.register.IContextHandle;
import org.markeb.proto.notice.Forward.ForwardNotice;
import org.markeb.proto.notice.Session.SessionBindNotice;
import org.markeb.proto.notice.Session.SessionUnbindNotice;
import com.google.protobuf.Message;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {

    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);

    private final IMessagePool<Message> messagePool;

    public ServerHandler(IMessagePool<Message> messagePool) {
        this.messagePool = messagePool;
    }

    /**
     * 接收消息
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg instanceof ForwardNotice) {
            handleForwardNotice(ctx, (ForwardNotice) msg);
        } else if (msg instanceof SessionBindNotice) {
            handleSessionBindNotice(ctx, (SessionBindNotice) msg);
        } else if (msg instanceof SessionUnbindNotice) {
            handleSessionUnbindNotice(ctx, (SessionUnbindNotice) msg);
        } else {
            // 处理其他系统级消息
            log.info("Received system message: {} from {}", msg.getClass().getSimpleName(),
                    ctx.channel().remoteAddress());
        }
    }

    private void handleForwardNotice(ChannelHandlerContext ctx, ForwardNotice notice) {
        String playerId = notice.getPlayerId();
        Player player = PlayerManager.getInstance().getPlayer(playerId);

        if (player != null) {
            try {
                Message innerMsg = messagePool.messageParser().parseFrom(notice.getMsgId(),
                        notice.getPayload().toByteArray());

                @SuppressWarnings("unchecked")
                IContextHandle<Player, Message> handler = (IContextHandle<Player, Message>) messagePool
                        .getHandler(innerMsg);
                if (handler != null) {
                    Message rep = handler.invoke(player, innerMsg);
                    if (rep != null) {
                        player.send(rep);
                    }
                } else {
                    log.warn("Message handler not found for message: {}", innerMsg.getClass().getSimpleName());
                }
            } catch (Throwable e) {
                log.error("Error handling message for player {}", playerId, e);
            }
        } else {
            log.warn("Player {} not found on this node", playerId);
        }
    }

    private void handleSessionBindNotice(ChannelHandlerContext ctx, SessionBindNotice notice) {
        String playerId = notice.getPlayerId();
        // 绑定时创建玩家对象，持有 Gateway Channel
        Player player = new Player(playerId, ctx.channel(), messagePool.messageParser());
        PlayerManager.getInstance().addPlayer(player);
        log.info("Player bound: {} from {}", playerId, ctx.channel().remoteAddress());
    }

    private void handleSessionUnbindNotice(ChannelHandlerContext ctx, SessionUnbindNotice notice) {
        String playerId = notice.getPlayerId();
        PlayerManager.getInstance().removePlayer(playerId);
        log.info("Player unbound: {}", playerId);
    }

    /**
     * 建立新连接
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Gateway connected: {}", ctx.channel().remoteAddress());
    }

    /**
     * 断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Gateway disconnected: {}", ctx.channel().remoteAddress());
        // 当网关断开时，可能需要清理该网关下的所有玩家，这里暂且保留
    }

    /**
     * 有异常发生
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof TooLongFrameException) {
            log.warn("Invalid protocol from {}: {} (possibly HTTP request to game port)",
                    ctx.channel().remoteAddress(), cause.getMessage());
            ctx.close();
            return;
        }
        log.error("Connection exception: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    /**
     * 通道可写状态改变
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        log.info("Channel writability changed: {} isWritable: {}",
                ctx.channel().remoteAddress(), ctx.channel().isWritable());
    }

    /**
     * 心跳检测
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log.info("User event triggered: {} event: {}",
                ctx.channel().remoteAddress(), evt.getClass().getSimpleName());
    }

}
