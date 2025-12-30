package org.markeb.metrics.binder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * JVM 扩展指标绑定器
 * <p>
 * 提供额外的 JVM 和系统指标，补充 Micrometer 默认的 JVM 指标。
 * </p>
 */
@Slf4j
public class JvmExtendedMetricsBinder implements MeterBinder {

    @Override
    public void bindTo(MeterRegistry registry) {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

        // JVM 启动时间
        Gauge.builder("jvm.start.time", runtimeMXBean, RuntimeMXBean::getStartTime)
                .description("JVM 启动时间戳（毫秒）")
                .baseUnit("milliseconds")
                .register(registry);

        // JVM 运行时间
        Gauge.builder("jvm.uptime", runtimeMXBean, RuntimeMXBean::getUptime)
                .description("JVM 运行时间（毫秒）")
                .baseUnit("milliseconds")
                .register(registry);

        // 系统负载
        Gauge.builder("system.load.average", osMXBean, OperatingSystemMXBean::getSystemLoadAverage)
                .description("系统负载平均值")
                .register(registry);

        // 可用处理器数
        Gauge.builder("system.cpu.count", osMXBean, OperatingSystemMXBean::getAvailableProcessors)
                .description("可用处理器数量")
                .register(registry);

        // 虚拟线程相关指标（Java 21+）
        try {
            // 获取虚拟线程数量（如果可用）
            Gauge.builder("jvm.threads.virtual.count", () -> {
                        try {
                            return Thread.getAllStackTraces().keySet().stream()
                                    .filter(Thread::isVirtual)
                                    .count();
                        } catch (Exception e) {
                            return 0L;
                        }
                    })
                    .description("虚拟线程数量")
                    .register(registry);
        } catch (Exception e) {
            log.debug("Virtual thread metrics not available: {}", e.getMessage());
        }

        log.info("JVM extended metrics bound to registry");
    }
}

