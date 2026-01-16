package org.markeb.net.gateway.config;

import org.markeb.net.gateway.backend.BackendConnectionManager;
import org.markeb.net.gateway.handler.GatewayChannelInitializer;
import org.markeb.net.netty.NettyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关模式自动配置。
 */
@Configuration
@EnableConfigurationProperties(GatewayBackendProperties.class)
@ConditionalOnProperty(value = "network.gateway.enabled", havingValue = "true")
public class GatewayAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    public BackendConnectionManager backendConnectionManager(GatewayBackendProperties props) {
        return new BackendConnectionManager(props);
    }

    @Bean
    @ConditionalOnMissingBean(GatewayChannelInitializer.class)
    public GatewayChannelInitializer gatewayChannelInitializer(BackendConnectionManager manager,
                                                                NettyProperties nettyProperties) {
        return new GatewayChannelInitializer(manager, nettyProperties);
    }
}

