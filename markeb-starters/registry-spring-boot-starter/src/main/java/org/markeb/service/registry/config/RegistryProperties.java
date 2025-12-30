package org.markeb.service.registry.config;

import org.markeb.service.registry.RegistryType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务注册配置属性
 * <p>
 * 遵循约定大于配置原则：
 * <ul>
 *   <li>serviceName 默认从 spring.application.name 获取</li>
 *   <li>port 默认从 network.port 或 server.port 获取</li>
 *   <li>host 默认自动获取本机 IP</li>
 * </ul>
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "markeb.registry")
public class RegistryProperties {

    /**
     * 是否启用服务注册
     * <p>默认：true</p>
     */
    private boolean enabled = true;

    /**
     * 是否自动注册服务到注册中心
     * <p>设置为 false 时，只启用服务发现功能，不自动注册自身</p>
     * <p>默认：true</p>
     */
    private boolean autoRegister = true;

    /**
     * 注册中心类型
     * <p>默认：NACOS</p>
     */
    private RegistryType type = RegistryType.NACOS;

    /**
     * 服务名称
     * <p>默认：null（自动从 spring.application.name 获取）</p>
     */
    private String serviceName;

    /**
     * 服务实例ID
     * <p>默认：null（自动生成为 serviceName-host-port）</p>
     * <p>建议配置为稳定的值，避免服务重启后 session 绑定失效</p>
     */
    private String instanceId;

    /**
     * 服务主机地址
     * <p>默认：null（自动获取本机 IP）</p>
     */
    private String host;

    /**
     * 服务端口
     * <p>默认：0（自动从 network.port 或 server.port 获取）</p>
     */
    private int port;

    /**
     * 服务权重
     */
    private double weight = 1.0;

    /**
     * 服务元数据
     */
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Nacos 配置
     */
    private NacosConfig nacos = new NacosConfig();

    /**
     * Etcd 配置
     */
    private EtcdConfig etcd = new EtcdConfig();

    /**
     * Consul 配置
     */
    private ConsulConfig consul = new ConsulConfig();

    @Data
    public static class NacosConfig {
        /**
         * Nacos 服务地址
         */
        private String serverAddr = "127.0.0.1:8848";

        /**
         * 命名空间
         */
        private String namespace = "public";

        /**
         * 分组
         */
        private String group = "DEFAULT_GROUP";

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;
    }

    @Data
    public static class EtcdConfig {
        /**
         * Etcd 端点地址列表
         */
        private String[] endpoints = {"http://127.0.0.1:2379"};

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * 服务注册 TTL（秒）
         */
        private long ttl = 30;
    }

    @Data
    public static class ConsulConfig {
        /**
         * Consul 主机地址
         */
        private String host = "127.0.0.1";

        /**
         * Consul 端口
         */
        private int port = 8500;

        /**
         * ACL Token
         */
        private String aclToken;

        /**
         * 健康检查间隔
         */
        private String healthCheckInterval = "10s";

        /**
         * 健康检查超时
         */
        private String healthCheckTimeout = "5s";
    }
}

