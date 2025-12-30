package org.markeb.gateway.route.strategy;

import org.markeb.gateway.session.GatewaySession;
import org.markeb.service.registry.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机路由策略
 * 随机选择一个节点，适用于节点性能相近的场景
 */
@Component
public class RandomRouteStrategy extends AbstractRouteStrategy {

    @Override
    public Type getType() {
        return Type.RANDOM;
    }

    @Override
    protected ServiceInstance doSelect(List<ServiceInstance> nodes, GatewaySession session) {
        int index = ThreadLocalRandom.current().nextInt(nodes.size());
        return nodes.get(index);
    }

    @Override
    protected String doSelectFromNodeIds(List<String> nodeIds, GatewaySession session) {
        int index = ThreadLocalRandom.current().nextInt(nodeIds.size());
        return nodeIds.get(index);
    }
}

