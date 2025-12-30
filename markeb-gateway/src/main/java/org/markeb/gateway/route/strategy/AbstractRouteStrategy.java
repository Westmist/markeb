package org.markeb.gateway.route.strategy;

import org.markeb.gateway.session.GatewaySession;
import org.markeb.service.registry.ServiceInstance;

import java.util.List;

/**
 * 路由策略抽象基类
 * 提供通用的模板方法和工具方法
 */
public abstract class AbstractRouteStrategy implements RouteStrategy {

    @Override
    public ServiceInstance select(List<ServiceInstance> nodes, GatewaySession session) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return doSelect(nodes, session);
    }

    @Override
    public String selectFromNodeIds(List<String> nodeIds, GatewaySession session) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return null;
        }
        if (nodeIds.size() == 1) {
            return nodeIds.get(0);
        }
        return doSelectFromNodeIds(nodeIds, session);
    }

    /**
     * 实际选择逻辑（由子类实现）
     *
     * @param nodes   可用节点列表（保证非空且size > 1）
     * @param session 网关会话
     * @return 选中的服务实例
     */
    protected abstract ServiceInstance doSelect(List<ServiceInstance> nodes, GatewaySession session);

    /**
     * 实际从节点ID列表选择逻辑（由子类实现）
     *
     * @param nodeIds 节点ID列表（保证非空且size > 1）
     * @param session 网关会话
     * @return 选中的节点ID
     */
    protected abstract String doSelectFromNodeIds(List<String> nodeIds, GatewaySession session);
}

