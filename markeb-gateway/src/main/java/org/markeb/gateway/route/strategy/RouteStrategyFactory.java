package org.markeb.gateway.route.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 路由策略工厂
 * 负责管理和获取路由策略实例
 */
@Component
public class RouteStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(RouteStrategyFactory.class);

    /**
     * 策略映射表
     */
    private final Map<RouteStrategy.Type, RouteStrategy> strategyMap;

    /**
     * 默认策略
     */
    private final RouteStrategy defaultStrategy;

    @Autowired
    public RouteStrategyFactory(List<RouteStrategy> strategies) {
        this.strategyMap = new EnumMap<>(RouteStrategy.Type.class);

        // 注册所有策略
        for (RouteStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
            log.info("Registered route strategy: {}", strategy.getType());
        }

        // 设置默认策略为轮询
        this.defaultStrategy = strategyMap.getOrDefault(
                RouteStrategy.Type.ROUND_ROBIN,
                strategies.isEmpty() ? null : strategies.get(0)
        );

        log.info("Route strategy factory initialized with {} strategies, default: {}",
                strategyMap.size(),
                defaultStrategy != null ? defaultStrategy.getType() : "none");
    }

    /**
     * 获取指定类型的策略
     *
     * @param type 策略类型
     * @return 策略实例，如果不存在则返回默认策略
     */
    public RouteStrategy getStrategy(RouteStrategy.Type type) {
        if (type == null) {
            return defaultStrategy;
        }
        return strategyMap.getOrDefault(type, defaultStrategy);
    }

    /**
     * 获取默认策略
     *
     * @return 默认策略实例
     */
    public RouteStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    /**
     * 检查是否存在指定类型的策略
     *
     * @param type 策略类型
     * @return 是否存在
     */
    public boolean hasStrategy(RouteStrategy.Type type) {
        return strategyMap.containsKey(type);
    }
}

