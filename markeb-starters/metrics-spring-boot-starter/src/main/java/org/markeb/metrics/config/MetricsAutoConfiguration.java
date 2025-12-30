package org.markeb.metrics.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.markeb.actor.ActorSystem;
import org.markeb.metrics.ActorMetrics;
import org.markeb.metrics.GameMetrics;
import org.markeb.metrics.NetworkMetrics;
import org.markeb.metrics.binder.ActorSystemMetricsBinder;
import org.markeb.metrics.binder.JvmExtendedMetricsBinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 监控指标自动配置
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "markeb.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MetricsProperties.class)
public class MetricsAutoConfiguration {

    /**
     * 通用标签配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            MetricsProperties properties,
            @Value("${spring.application.name:unknown}") String applicationName) {

        return registry -> {
            MetricsProperties.CommonTagsConfig tags = properties.getCommonTags();

            // 应用名称
            String app = StringUtils.hasText(tags.getApplication()) ? tags.getApplication() : applicationName;
            registry.config().commonTags("application", app);

            // 环境
            if (StringUtils.hasText(tags.getEnvironment())) {
                registry.config().commonTags("environment", tags.getEnvironment());
            }

            // 服务器 ID
            if (StringUtils.hasText(tags.getServerId())) {
                registry.config().commonTags("server_id", tags.getServerId());
            }

            log.info("Metrics common tags configured: application={}", app);
        };
    }

    /**
     * 游戏核心指标配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "markeb.metrics.game", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class GameMetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public GameMetrics gameMetrics(MeterRegistry registry) {
            log.info("Creating GameMetrics bean");
            return new GameMetrics(registry);
        }
    }

    /**
     * Actor 指标配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "markeb.metrics.actor", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ActorMetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ActorMetrics actorMetrics(MeterRegistry registry) {
            log.info("Creating ActorMetrics bean");
            return new ActorMetrics(registry);
        }

        @Bean
        @ConditionalOnBean(ActorSystem.class)
        @ConditionalOnProperty(prefix = "markeb.metrics.actor", name = "bind-actor-system", havingValue = "true", matchIfMissing = true)
        public ActorSystemMetricsBinder actorSystemMetricsBinder(ActorSystem actorSystem) {
            log.info("Creating ActorSystemMetricsBinder bean");
            return new ActorSystemMetricsBinder(actorSystem);
        }
    }

    /**
     * 网络指标配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "markeb.metrics.network", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class NetworkMetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public NetworkMetrics networkMetrics(MeterRegistry registry) {
            log.info("Creating NetworkMetrics bean");
            return new NetworkMetrics(registry);
        }
    }

    /**
     * JVM 扩展指标配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "markeb.metrics.jvm", name = "extended-enabled", havingValue = "true", matchIfMissing = true)
    static class JvmMetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public JvmExtendedMetricsBinder jvmExtendedMetricsBinder() {
            log.info("Creating JvmExtendedMetricsBinder bean");
            return new JvmExtendedMetricsBinder();
        }
    }
}

