package org.markeb.metrics.binder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.markeb.actor.ActorSystem;

/**
 * Actor 系统指标绑定器
 * <p>
 * 自动绑定 ActorSystem 的指标到 Micrometer。
 * 当 ActorSystem 存在时自动注册。
 * </p>
 */
@Slf4j
public class ActorSystemMetricsBinder implements MeterBinder {

    private final ActorSystem actorSystem;

    public ActorSystemMetricsBinder(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // Actor 数量
        Gauge.builder("actor.system.actors.count", actorSystem, ActorSystem::actorCount)
                .description("ActorSystem 中的 Actor 数量")
                .register(registry);

        // 执行器类型（作为标签）
        Gauge.builder("actor.system.info", () -> 1)
                .tag("executor_type", actorSystem.getExecutorType().name())
                .description("ActorSystem 信息")
                .register(registry);

        log.info("ActorSystem metrics bound to registry");
    }
}

