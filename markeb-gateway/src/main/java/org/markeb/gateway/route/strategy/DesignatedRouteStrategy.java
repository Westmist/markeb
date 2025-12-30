package org.markeb.gateway.route.strategy;

import org.markeb.gateway.session.GatewaySession;
import org.markeb.service.registry.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 指定节点路由策略
 * 始终选择列表中的第一个节点
 * 适用于测试或需要固定路由的场景
 */
@Component
public class DesignatedRouteStrategy extends AbstractRouteStrategy {

    @Override
    public Type getType() {
        return Type.DESIGNATED;
    }

    @Override
    protected ServiceInstance doSelect(List<ServiceInstance> nodes, GatewaySession session) {
        return nodes.get(0);
    }

    @Override
    protected String doSelectFromNodeIds(List<String> nodeIds, GatewaySession session) {
        return nodeIds.get(0);
    }
}

