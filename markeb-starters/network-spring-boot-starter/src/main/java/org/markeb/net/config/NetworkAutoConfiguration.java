package org.markeb.net.config;

import org.markeb.net.INetworkServer;
import org.markeb.net.handler.MessageDispatcher;
import org.markeb.net.handler.PacketHandler;
import org.markeb.net.heartbeat.HeartbeatHandler;
import org.markeb.net.heartbeat.HeartbeatMessageFactory;
import org.markeb.net.heartbeat.PacketHeartbeatFactory;
import org.markeb.net.protocol.ProtocolType;
import org.markeb.net.protocol.codec.PacketDecoder;
import org.markeb.net.protocol.codec.PacketEncoder;
import org.markeb.net.serialization.CodecType;
import org.markeb.net.serialization.MessageCodec;
import org.markeb.net.serialization.MessageRegistry;
import org.markeb.net.serialization.json.JsonCodec;
import org.markeb.net.serialization.protobuf.ProtobufCodec;
import org.markeb.net.serialization.protostuff.ProtostuffCodec;
import org.markeb.net.transport.TransportServer;
import org.markeb.net.transport.TransportType;
import org.markeb.net.transport.kcp.KcpTransportServer;
import org.markeb.net.transport.tcp.TcpTransportServer;
import org.markeb.net.transport.websocket.WebSocketServerInitializer;
import org.markeb.net.transport.websocket.WebSocketTransportServer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 网络模块自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(NetworkProperties.class)
@ConditionalOnProperty(prefix = "markeb.network", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NetworkAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NetworkAutoConfiguration.class);

    /**
     * 消息注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageRegistry messageRegistry() {
        return new MessageRegistry();
    }

    /**
     * 消息编解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageCodec messageCodec(NetworkProperties properties, MessageRegistry registry) {
        CodecType codecType = properties.getCodec();
        log.info("Creating MessageCodec with type: {}", codecType);

        return switch (codecType) {
            case PROTOBUF -> new ProtobufCodec(registry);
            case PROTOSTUFF -> new ProtostuffCodec(registry);
            case JSON -> new JsonCodec(registry);
        };
    }

    /**
     * 消息分发器
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageDispatcher messageDispatcher(MessageCodec messageCodec) {
        return new MessageDispatcher(messageCodec);
    }

    /**
     * 心跳消息工厂
     * 根据协议类型自动选择对应的心跳实现
     */
    @Bean
    @ConditionalOnMissingBean
    public HeartbeatMessageFactory heartbeatMessageFactory(NetworkProperties properties) {
        return new PacketHeartbeatFactory(properties.getProtocol());
    }

    /**
     * Channel 初始化器
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelInitializer<SocketChannel> channelInitializer(
            NetworkProperties properties,
            MessageDispatcher messageDispatcher,
            HeartbeatMessageFactory heartbeatMessageFactory) {

        ProtocolType protocolType = properties.getProtocol();
        NetworkProperties.NettyConfig nettyConfig = properties.getNetty();
        NetworkProperties.HeartbeatConfig heartbeatConfig = properties.getHeartbeat();

        log.info("Creating ChannelInitializer with protocol: {}, codec: {}, heartbeat: {}",
                protocolType, properties.getCodec(), heartbeatConfig.isEnabled());

        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                // 空闲检测
                ch.pipeline().addLast("idleStateHandler",
                        new IdleStateHandler(
                                nettyConfig.getReaderIdleTime(),
                                nettyConfig.getWriterIdleTime(),
                                nettyConfig.getAllIdleTime(),
                                TimeUnit.SECONDS));

                // 协议编解码
                ch.pipeline().addLast("decoder",
                        new PacketDecoder(protocolType, properties.getMaxFrameLength()));
                ch.pipeline().addLast("encoder", new PacketEncoder());

                // 心跳处理（在业务处理器之前）
                if (heartbeatConfig.isEnabled()) {
                    ch.pipeline().addLast("heartbeat",
                            new HeartbeatHandler(heartbeatMessageFactory, heartbeatConfig.getMaxMissedHeartbeats()));
                }

                // 消息处理
                ch.pipeline().addLast("handler", new PacketHandler(messageDispatcher));
            }
        };
    }

    /**
     * 传输服务器
     * 只有当没有 INetworkServer (如 NettyServer) 时才创建
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean({TransportServer.class, INetworkServer.class})
    public TransportServer transportServer(
            NetworkProperties properties,
            ChannelInitializer<SocketChannel> channelInitializer,
            MessageDispatcher messageDispatcher) {

        TransportType transportType = properties.getTransport();
        int port = properties.getPort();
        NetworkProperties.NettyConfig nettyConfig = properties.getNetty();

        log.info("Creating TransportServer with type: {}, port: {}", transportType, port);

        return switch (transportType) {
            case TCP -> new TcpTransportServer(
                    port,
                    nettyConfig.getBossThreads(),
                    nettyConfig.getWorkerThreads(),
                    channelInitializer);
            case KCP -> new KcpTransportServer(
                    port,
                    nettyConfig.getWorkerThreads(),
                    channelInitializer);
            case WEBSOCKET -> createWebSocketServer(properties, messageDispatcher);
        };
    }

    /**
     * 创建 WebSocket 服务器
     */
    private TransportServer createWebSocketServer(NetworkProperties properties, MessageDispatcher messageDispatcher) {
        NetworkProperties.WebSocketConfig wsConfig = properties.getWebsocket();
        NetworkProperties.NettyConfig nettyConfig = properties.getNetty();

        // 构建 SSL 上下文（如果启用）
        SslContext sslContext = null;
        if (wsConfig.isSslEnabled()) {
            sslContext = buildSslContext(wsConfig);
        }

        // 构建 WebSocket 初始化器
        WebSocketServerInitializer initializer = WebSocketServerInitializer.builder()
                .sslContext(sslContext)
                .websocketPath(wsConfig.getPath())
                .maxFrameSize(wsConfig.getMaxFrameSize())
                .enableCompression(wsConfig.isEnableCompression())
                .readerIdleTime(nettyConfig.getReaderIdleTime())
                .writerIdleTime(nettyConfig.getWriterIdleTime())
                .allIdleTime(nettyConfig.getAllIdleTime())
                .protocolType(properties.getProtocol())
                .maxFrameLength(properties.getMaxFrameLength())
                .messageDispatcher(messageDispatcher)
                .build();

        return new WebSocketTransportServer(
                properties.getPort(),
                nettyConfig.getBossThreads(),
                nettyConfig.getWorkerThreads(),
                initializer);
    }

    /**
     * 构建 SSL 上下文
     */
    private SslContext buildSslContext(NetworkProperties.WebSocketConfig wsConfig) {
        try {
            if (!StringUtils.hasText(wsConfig.getSslCertPath()) || !StringUtils.hasText(wsConfig.getSslKeyPath())) {
                log.warn("SSL enabled but certificate or key path not configured, SSL will be disabled");
                return null;
            }

            File certFile = new File(wsConfig.getSslCertPath());
            File keyFile = new File(wsConfig.getSslKeyPath());

            if (!certFile.exists() || !keyFile.exists()) {
                log.warn("SSL certificate or key file not found, SSL will be disabled");
                return null;
            }

            SslContextBuilder builder = SslContextBuilder.forServer(certFile, keyFile);
            if (StringUtils.hasText(wsConfig.getSslKeyPassword())) {
                builder = SslContextBuilder.forServer(certFile, keyFile, wsConfig.getSslKeyPassword());
            }

            log.info("SSL context created for WebSocket server");
            return builder.build();
        } catch (Exception e) {
            log.error("Failed to create SSL context", e);
            return null;
        }
    }
}

