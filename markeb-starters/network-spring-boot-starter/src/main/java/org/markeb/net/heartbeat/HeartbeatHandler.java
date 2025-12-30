package org.markeb.net.heartbeat;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 心跳处理器
 * <p>
 * 处理空闲检测事件，发送心跳包保持连接存活。
 * 支持配置最大丢失心跳次数，超过后断开连接。
 * </p>
 */
public class HeartbeatHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatHandler.class);

    private final HeartbeatMessageFactory messageFactory;
    private final int maxMissedHeartbeats;
    private final AtomicInteger missedHeartbeats = new AtomicInteger(0);

    /**
     * 创建心跳处理器
     *
     * @param messageFactory      心跳消息工厂
     * @param maxMissedHeartbeats 最大丢失心跳次数，超过后断开连接
     */
    public HeartbeatHandler(HeartbeatMessageFactory messageFactory, int maxMissedHeartbeats) {
        this.messageFactory = messageFactory;
        this.maxMissedHeartbeats = maxMissedHeartbeats;
    }

    public HeartbeatHandler(HeartbeatMessageFactory messageFactory) {
        this(messageFactory, 3);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            handleIdleEvent(ctx, idleEvent);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void handleIdleEvent(ChannelHandlerContext ctx, IdleStateEvent event) {
        IdleState state = event.state();
        switch (state) {
            case READER_IDLE -> {
                // 读空闲：没有收到对方数据，可能对方已断开
                int missed = missedHeartbeats.incrementAndGet();
                if (missed >= maxMissedHeartbeats) {
                    log.warn("Channel {} missed {} heartbeats, closing connection",
                            ctx.channel().remoteAddress(), missed);
                    ctx.close();
                } else {
                    log.debug("Channel {} reader idle, missed heartbeats: {}",
                            ctx.channel().remoteAddress(), missed);
                }
            }
            case WRITER_IDLE -> {
                // 写空闲：长时间没有发送数据，发送心跳包
                sendHeartbeat(ctx);
            }
            case ALL_IDLE -> {
                // 读写都空闲：发送心跳包
                sendHeartbeat(ctx);
            }
        }
    }

    private void sendHeartbeat(ChannelHandlerContext ctx) {
        if (messageFactory == null) {
            log.debug("No heartbeat message factory configured, skipping heartbeat");
            return;
        }

        Object heartbeatMessage = messageFactory.createHeartbeatRequest();
        if (heartbeatMessage != null) {
            ctx.writeAndFlush(heartbeatMessage).addListener(future -> {
                if (future.isSuccess()) {
                    log.debug("Heartbeat sent to {}", ctx.channel().remoteAddress());
                } else {
                    log.warn("Failed to send heartbeat to {}", ctx.channel().remoteAddress(), future.cause());
                }
            });
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 收到任何消息都重置丢失计数
        missedHeartbeats.set(0);

        // 检查是否是心跳请求，如果是则自动响应
        if (messageFactory != null && messageFactory.isHeartbeatRequest(msg)) {
            Object response = messageFactory.createHeartbeatResponse(msg);
            if (response != null) {
                ctx.writeAndFlush(response);
                log.debug("Heartbeat response sent to {}", ctx.channel().remoteAddress());
                return; // 心跳消息不继续传递
            }
        }

        // 如果是心跳响应，记录延迟但不继续传递
        if (messageFactory != null && messageFactory.isHeartbeatResponse(msg)) {
            long latency = messageFactory.calculateLatency(msg);
            if (latency >= 0) {
                log.debug("Heartbeat response from {}, latency: {}ms",
                        ctx.channel().remoteAddress(), latency);
            }
            return; // 心跳响应不继续传递
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // 发送任何消息都重置丢失计数（因为对方会收到我们的数据）
        missedHeartbeats.set(0);
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HeartbeatHandler error on channel {}", ctx.channel().remoteAddress(), cause);
        super.exceptionCaught(ctx, cause);
    }
}

