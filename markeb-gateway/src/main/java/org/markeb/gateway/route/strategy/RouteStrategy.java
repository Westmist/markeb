package org.markeb.gateway.route.strategy;

import org.markeb.gateway.session.GatewaySession;
import org.markeb.service.registry.ServiceInstance;

import java.util.List;

/**
 * 路由策略接口
 * 定义节点选择的策略行为
 */
public interface RouteStrategy {

    /**
     * 策略类型枚举
     */
    enum Type {
        /**
         * 轮询
         */
        ROUND_ROBIN,

        /**
         * 随机
         */
        RANDOM,

        /**
         * 一致性哈希（根据玩家ID）
         */
        CONSISTENT_HASH,

        /**
         * 指定节点
         */
        DESIGNATED
    }

    /**
     * 获取策略类型
     *
     * @return 策略类型
     */
    Type getType();

    /**
     * 从服务实例列表中选择一个节点
     *
     * @param nodes   可用节点列表
     * @param session 网关会话（可用于获取玩家ID等信息）
     * @return 选中的服务实例
     */
    ServiceInstance select(List<ServiceInstance> nodes, GatewaySession session);

    /**
     * 从节点ID列表中选择一个节点（用于静态配置场景）
     *
     * @param nodeIds 节点ID列表
     * @param session 网关会话
     * @return 选中的节点ID
     */
    String selectFromNodeIds(List<String> nodeIds, GatewaySession session);
}

