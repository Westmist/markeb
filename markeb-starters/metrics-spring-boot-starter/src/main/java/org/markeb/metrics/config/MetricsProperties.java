package org.markeb.metrics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 监控指标配置属性
 */
@Data
@ConfigurationProperties(prefix = "markeb.metrics")
public class MetricsProperties {

    /**
     * 是否启用监控
     */
    private boolean enabled = true;

    /**
     * 游戏指标配置
     */
    private GameMetricsConfig game = new GameMetricsConfig();

    /**
     * Actor 指标配置
     */
    private ActorMetricsConfig actor = new ActorMetricsConfig();

    /**
     * 网络指标配置
     */
    private NetworkMetricsConfig network = new NetworkMetricsConfig();

    /**
     * JVM 扩展指标配置
     */
    private JvmMetricsConfig jvm = new JvmMetricsConfig();

    /**
     * 通用标签配置
     */
    private CommonTagsConfig commonTags = new CommonTagsConfig();

    @Data
    public static class GameMetricsConfig {
        /**
         * 是否启用游戏指标
         */
        private boolean enabled = true;
    }

    @Data
    public static class ActorMetricsConfig {
        /**
         * 是否启用 Actor 指标
         */
        private boolean enabled = true;

        /**
         * 是否绑定 ActorSystem 指标
         */
        private boolean bindActorSystem = true;
    }

    @Data
    public static class NetworkMetricsConfig {
        /**
         * 是否启用网络指标
         */
        private boolean enabled = true;
    }

    @Data
    public static class JvmMetricsConfig {
        /**
         * 是否启用 JVM 扩展指标
         */
        private boolean extendedEnabled = true;
    }

    @Data
    public static class CommonTagsConfig {
        /**
         * 应用名称标签
         */
        private String application;

        /**
         * 环境标签
         */
        private String environment;

        /**
         * 服务器 ID 标签
         */
        private String serverId;
    }
}

