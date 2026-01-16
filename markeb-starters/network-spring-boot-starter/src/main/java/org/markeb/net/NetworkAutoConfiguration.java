package org.markeb.net;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.markeb.net.config.NetworkProperties;
import org.markeb.net.msg.IGameParser;
import org.markeb.net.msg.IMessagePool;
import org.markeb.net.msg.ProtoBuffGameMessagePool;
import org.markeb.net.msg.ProtoBuffParser;
import org.markeb.net.netty.NettyProperties;
import org.markeb.net.netty.NettyServer;
import org.markeb.net.register.MessageHandlerRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({NettyProperties.class, NetworkProperties.class})
public class NetworkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IGameParser.class)
    public IGameParser<?> gameParser() {
        return new ProtoBuffParser();
    }

    @Bean
    @ConditionalOnMissingBean(IMessagePool.class)
    public IMessagePool<?> messagePool(IGameParser<?> gameParser, NetworkProperties networkProperties) {
        // 根据协议类型选择编解码器
        boolean gatewayInternalMode = networkProperties.getProtocol() == org.markeb.net.protocol.ProtocolType.GATEWAY_INTERNAL;
        return new ProtoBuffGameMessagePool(gameParser, gatewayInternalMode);
    }

    @Bean
    public MessageHandlerRegistrar messageHandlerRegistrar(ApplicationContext applicationContext) {
        return new MessageHandlerRegistrar(applicationContext);
    }

    @ConditionalOnMissingBean(INetworkServer.class)
    @ConditionalOnBean(ChannelInitializer.class)
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(value = "markeb.network.enabled", matchIfMissing = true)
    public NettyServer nettyServer(
        NetworkProperties networkProperties,
        NettyProperties nettyProperties,
        ChannelInitializer<SocketChannel> initializer,
        ApplicationEventPublisher applicationEventPublisher) {
        return new NettyServer(networkProperties, nettyProperties, initializer, applicationEventPublisher);
    }

}
