package org.markeb.gateway.route.strategy;

import org.markeb.gateway.session.GatewaySession;
import org.markeb.service.registry.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 一致性哈希路由策略
 * 根据玩家ID进行哈希，确保同一玩家的请求总是路由到同一节点
 * 适用于需要会话亲和性的场景
 */
@Component
public class ConsistentHashRouteStrategy extends AbstractRouteStrategy {

    private final RoundRobinRouteStrategy fallbackStrategy;

    @Autowired
    public ConsistentHashRouteStrategy(RoundRobinRouteStrategy fallbackStrategy) {
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public Type getType() {
        return Type.CONSISTENT_HASH;
    }

    @Override
    protected ServiceInstance doSelect(List<ServiceInstance> nodes, GatewaySession session) {
        Long playerId = session.getPlayerId();
        if (playerId == null) {
            // 玩家ID为空时降级为轮询策略
            return fallbackStrategy.doSelect(nodes, session);
        }
        int index = calculateIndex(playerId, nodes.size());
        return nodes.get(index);
    }

    @Override
    protected String doSelectFromNodeIds(List<String> nodeIds, GatewaySession session) {
        Long playerId = session.getPlayerId();
        if (playerId == null) {
            // 玩家ID为空时返回第一个节点
            return nodeIds.get(0);
        }
        int index = calculateIndex(playerId, nodeIds.size());
        return nodeIds.get(index);
    }

    /**
     * 根据玩家ID计算节点索引
     */
    private int calculateIndex(long playerId, int size) {
        int hash = Math.abs(Long.hashCode(playerId));
        return hash % size;
    }
}

