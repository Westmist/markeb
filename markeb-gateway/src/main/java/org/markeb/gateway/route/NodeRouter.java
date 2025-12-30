package org.markeb.gateway.route;

import org.markeb.gateway.route.strategy.RouteStrategy;
import org.markeb.gateway.route.strategy.RouteStrategyFactory;
import org.markeb.gateway.session.GatewaySession;
import org.markeb.service.registry.ServiceInstance;
import org.markeb.service.registry.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点路由器
 * 负责将请求路由到合适的游戏节点
 * 使用策略模式支持多种负载均衡策略
 */
@Component
public class NodeRouter {

    private static final Logger log = LoggerFactory.getLogger(NodeRouter.class);

    private static final String NODE_SERVICE_NAME = "markeb-node";

    @Autowired(required = false)
    private ServiceRegistry serviceRegistry;

    @Autowired
    private RouteStrategyFactory strategyFactory;

    /**
     * 静态节点配置（当没有服务注册时使用）
     */
    private final Map<String, String> staticNodes = new ConcurrentHashMap<>();

    /**
     * 为会话选择节点
     *
     * @param session      网关会话
     * @param strategyType 路由策略类型
     * @return 节点地址 (host:port)
     */
    public Optional<String> selectNode(GatewaySession session, RouteStrategy.Type strategyType) {
        // 如果会话已绑定节点，直接返回
        if (session.getNodeId() != null) {
            return getNodeAddress(session.getNodeId());
        }

        // 获取路由策略
        RouteStrategy strategy = strategyFactory.getStrategy(strategyType);
        if (strategy == null) {
            log.error("No route strategy available for type: {}", strategyType);
            return Optional.empty();
        }

        // 获取可用节点列表
        List<ServiceInstance> nodes = getAvailableNodes();
        if (nodes.isEmpty()) {
            // 尝试使用静态配置
            if (!staticNodes.isEmpty()) {
                return selectFromStaticNodes(session, strategy);
            }
            log.warn("No available nodes for routing");
            return Optional.empty();
        }

        // 使用策略选择节点
        ServiceInstance selected = strategy.select(nodes, session);
        if (selected != null) {
            String nodeId = selected.getInstanceId();
            session.setNodeId(nodeId);
            log.debug("Selected node {} using strategy {} for session {}",
                    nodeId, strategy.getType(), session.getSessionId());
            return Optional.of(selected.getHost() + ":" + selected.getPort());
        }

        return Optional.empty();
    }

    /**
     * 获取指定节点的地址
     */
    public Optional<String> getNodeAddress(String nodeId) {
        // 先从服务注册中心查找
        if (serviceRegistry != null) {
            List<ServiceInstance> instances = serviceRegistry.getInstances(NODE_SERVICE_NAME);
            for (ServiceInstance instance : instances) {
                if (nodeId.equals(instance.getInstanceId())) {
                    return Optional.of(instance.getHost() + ":" + instance.getPort());
                }
            }
        }

        // 从静态配置查找
        String address = staticNodes.get(nodeId);
        return Optional.ofNullable(address);
    }

    /**
     * 获取可用节点列表
     */
    private List<ServiceInstance> getAvailableNodes() {
        if (serviceRegistry == null) {
            return List.of();
        }
        return serviceRegistry.getInstances(NODE_SERVICE_NAME);
    }

    /**
     * 从静态配置选择节点
     */
    private Optional<String> selectFromStaticNodes(GatewaySession session, RouteStrategy strategy) {
        if (staticNodes.isEmpty()) {
            return Optional.empty();
        }

        List<String> nodeIds = List.copyOf(staticNodes.keySet());
        String selectedId = strategy.selectFromNodeIds(nodeIds, session);

        if (selectedId != null) {
            session.setNodeId(selectedId);
            log.debug("Selected static node {} using strategy {} for session {}",
                    selectedId, strategy.getType(), session.getSessionId());
            return Optional.ofNullable(staticNodes.get(selectedId));
        }

        return Optional.empty();
    }

    /**
     * 添加静态节点配置
     */
    public void addStaticNode(String nodeId, String address) {
        staticNodes.put(nodeId, address);
        log.info("Added static node: {} -> {}", nodeId, address);
    }

    /**
     * 移除静态节点配置
     */
    public void removeStaticNode(String nodeId) {
        staticNodes.remove(nodeId);
        log.info("Removed static node: {}", nodeId);
    }

    /**
     * 获取所有静态节点
     */
    public Map<String, String> getStaticNodes() {
        return Map.copyOf(staticNodes);
    }
}
