package org.markeb.game.netty;

import com.google.protobuf.Message;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.markeb.net.msg.IMessagePool;
import org.markeb.net.netty.NettyProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final IMessagePool<Message> messagePool;
    private final NettyProperties nettyProperties;

    public ServerChannelInitializer(IMessagePool<Message> messagePool, NettyProperties nettyProperties) {
        this.messagePool = messagePool;
        this.nettyProperties = nettyProperties;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast("idleStateHandler",
                new IdleStateHandler(nettyProperties.getReaderIdleTime(),
                        nettyProperties.getWriterIdleTime(),
                        nettyProperties.getAllIdleTime(),
                        TimeUnit.SECONDS));

        ch.pipeline().addLast(messagePool.decoder());
        ch.pipeline().addLast(messagePool.encoder());

        ch.pipeline().addLast(new ServerHandler(messagePool));
    }
}
