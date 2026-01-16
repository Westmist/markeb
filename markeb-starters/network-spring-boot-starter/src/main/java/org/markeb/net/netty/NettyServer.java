package org.markeb.net.netty;

import org.markeb.common.event.NetworkStartedEvent;
import org.markeb.net.INetworkServer;
import org.markeb.net.config.NetworkProperties;
import org.springframework.context.ApplicationEventPublisher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer implements INetworkServer {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    private final NetworkProperties networkProperties;

    private final NettyProperties nettyProperties;

    private final ChannelInitializer<SocketChannel> channelInitializer;

    private final ApplicationEventPublisher publisher;

    private Channel serverChannel;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    public NettyServer(NetworkProperties networkProperties,
                       NettyProperties nettyProperties,
                       ChannelInitializer<SocketChannel> channelInitializer,
                       ApplicationEventPublisher publisher) {
        this.networkProperties = networkProperties;
        this.nettyProperties = nettyProperties;
        this.channelInitializer = channelInitializer;
        this.publisher = publisher;
    }

    private WriteBufferWaterMark writeBufferWaterMark() {
        return new WriteBufferWaterMark(256 * 1024, 512 * 1024);
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(nettyProperties.getBossThreads());
        workerGroup = new NioEventLoopGroup(nettyProperties.getWorkerThreads());
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, writeBufferWaterMark());

            bootstrap.childHandler(channelInitializer);

            ChannelFuture channelFuture = bootstrap.bind(networkProperties.getPort()).sync();
            this.serverChannel = channelFuture.channel();
            log.info("Netty started at port {}", networkProperties.getPort());
            publisher.publishEvent(new NetworkStartedEvent(this));
        } catch (Exception e) {
            log.error("Netty server start failed on port {}", networkProperties.getPort(), e);
            stop();
            throw e;
        }
    }

    public void stop() {
        log.info("Shutting down Netty server...");
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
                log.info("Server channel closed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully().syncUninterruptibly();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().syncUninterruptibly();
            }
            log.info("Netty server shutdown complete");
        }
    }

}
