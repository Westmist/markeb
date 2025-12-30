package org.markeb.service.registry.config;

import org.markeb.service.registry.ServiceInstance;
import org.markeb.service.registry.ServiceRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 服务自动注册器
 * <p>
 * 在应用启动时自动将服务注册到注册中心。
 * </p>
 * <p>
 * 遵循约定大于配置原则：
 * <ul>
 *   <li>serviceName: 优先使用 markeb.registry.service-name，否则使用 spring.application.name</li>
 *   <li>port: 优先使用 markeb.registry.port，否则使用 network.port，再否则使用 server.port</li>
 *   <li>host: 优先使用 markeb.registry.host，否则自动获取本机 IP</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "markeb.registry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServiceAutoRegistrar {

    private final ServiceRegistry serviceRegistry;
    private final RegistryProperties properties;
    private ServiceInstance registeredInstance;

    @Value("${spring.application.name:}")
    private String applicationName;

    @Value("${markeb.network.port:${network.port:0}}")
    private int networkPort;

    @Value("${markeb.gateway.port:0}")
    private int gatewayPort;

    @Value("${server.port:0}")
    private int serverPort;

    public ServiceAutoRegistrar(ServiceRegistry serviceRegistry, RegistryProperties properties) {
        this.serviceRegistry = serviceRegistry;
        this.properties = properties;
    }

    @PostConstruct
    public void register() {
        if (!properties.isEnabled()) {
            log.info("Service registration is disabled");
            return;
        }

        if (!properties.isAutoRegister()) {
            log.info("Auto registration is disabled, using registry for discovery only");
            return;
        }

        // 解析服务名称
        String serviceName = resolveServiceName();
        if (serviceName == null || serviceName.isEmpty()) {
            log.warn("Service name is not configured (set markeb.registry.service-name or spring.application.name), skipping registration");
            return;
        }

        // 解析端口
        int port = resolvePort();
        if (port <= 0) {
            log.warn("Service port is not configured (set markeb.registry.port, network.port or server.port), skipping registration");
            return;
        }

        try {
            // 解析主机地址
            String host = resolveHost();

            // 使用稳定的 instanceId，避免服务重启后 session 绑定失效
            // 如果配置了自定义 instanceId 则使用配置的，否则使用 serviceName-host-port
            String instanceId = properties.getInstanceId() != null && !properties.getInstanceId().isEmpty()
                    ? properties.getInstanceId()
                    : serviceName + "-" + host + "-" + port;

            registeredInstance = ServiceInstance.builder()
                    .instanceId(instanceId)
                    .serviceName(serviceName)
                    .host(host)
                    .port(port)
                    .weight(properties.getWeight())
                    .healthy(true)
                    .enabled(true)
                    .metadata(properties.getMetadata())
                    .build();

            serviceRegistry.register(registeredInstance);
            log.info("Service registered: {} -> {}:{}", serviceName, host, port);
        } catch (Exception e) {
            log.error("Failed to register service", e);
        }
    }

    @PreDestroy
    public void deregister() {
        if (registeredInstance != null) {
            try {
                serviceRegistry.deregister(registeredInstance);
                log.info("Service deregistered: {}", registeredInstance.getServiceName());
            } catch (Exception e) {
                log.error("Failed to deregister service", e);
            }
        }
    }

    /**
     * 解析服务名称
     * <p>优先级：markeb.registry.service-name > spring.application.name</p>
     */
    private String resolveServiceName() {
        String name = properties.getServiceName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return applicationName;
    }

    /**
     * 解析端口
     * <p>优先级：markeb.registry.port > markeb.network.port > markeb.gateway.port > server.port</p>
     */
    private int resolvePort() {
        int port = properties.getPort();
        if (port > 0) {
            return port;
        }
        if (networkPort > 0) {
            return networkPort;
        }
        if (gatewayPort > 0) {
            return gatewayPort;
        }
        return serverPort;
    }

    /**
     * 解析主机地址
     * <p>优先级：markeb.registry.host > 自动获取本机 IP</p>
     */
    private String resolveHost() {
        String host = properties.getHost();
        if (host != null && !host.isBlank()) {
            return host;
        }
        return getLocalHostAddress();
    }

    private String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Failed to get local host address, using 127.0.0.1", e);
            return "127.0.0.1";
        }
    }
}
