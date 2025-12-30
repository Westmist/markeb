package org.markeb.gateway.route.strategy;

import org.markeb.gateway.session.GatewaySession;
import org.markeb.service.registry.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询路由策略
 * 按顺序循环选择节点，实现请求的均匀分配
 */
@Component
public class RoundRobinRouteStrategy extends AbstractRouteStrategy {

    /**
     * 轮询计数器
     */
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Type getType() {
        return Type.ROUND_ROBIN;
    }

    @Override
    protected ServiceInstance doSelect(List<ServiceInstance> nodes, GatewaySession session) {
        int index = getNextIndex(nodes.size());
        return nodes.get(index);
    }

    @Override
    protected String doSelectFromNodeIds(List<String> nodeIds, GatewaySession session) {
        int index = getNextIndex(nodeIds.size());
        return nodeIds.get(index);
    }

    /**
     * 获取下一个索引
     */
    private int getNextIndex(int size) {
        return Math.abs(counter.getAndIncrement() % size);
    }
}

