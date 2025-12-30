package org.markeb.metrics.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.markeb.metrics.annotation.Metered;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方法监控切面
 * <p>
 * 处理 @Metered 注解，自动记录方法调用次数和耗时。
 * </p>
 */
@Slf4j
@Aspect
public class MeteredAspect {

    private final MeterRegistry registry;
    private final Map<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();

    public MeteredAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(org.markeb.metrics.annotation.Metered)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Metered metered = method.getAnnotation(Metered.class);

        String metricName = metered.name();
        String description = metered.description();
        String[] tags = metered.tags();

        // 记录调用次数
        if (metered.counted()) {
            getOrCreateCounter(metricName, description, tags).increment();
        }

        // 记录耗时
        if (metered.timed()) {
            Timer.Sample sample = Timer.start(registry);
            try {
                return joinPoint.proceed();
            } finally {
                sample.stop(getOrCreateTimer(metricName, description, tags));
            }
        } else {
            return joinPoint.proceed();
        }
    }

    private Timer getOrCreateTimer(String name, String description, String[] tags) {
        String cacheKey = buildCacheKey(name + ".timer", tags);
        return timerCache.computeIfAbsent(cacheKey, k -> {
            Timer.Builder builder = Timer.builder(name + ".duration")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99);

            if (StringUtils.hasText(description)) {
                builder.description(description + " 耗时");
            }

            if (tags.length > 0) {
                builder.tags(parseTags(tags));
            }

            return builder.register(registry);
        });
    }

    private Counter getOrCreateCounter(String name, String description, String[] tags) {
        String cacheKey = buildCacheKey(name + ".counter", tags);
        return counterCache.computeIfAbsent(cacheKey, k -> {
            Counter.Builder builder = Counter.builder(name + ".count");

            if (StringUtils.hasText(description)) {
                builder.description(description + " 调用次数");
            }

            if (tags.length > 0) {
                builder.tags(parseTags(tags));
            }

            return builder.register(registry);
        });
    }

    private String buildCacheKey(String name, String[] tags) {
        if (tags.length == 0) {
            return name;
        }
        return name + ":" + String.join(",", tags);
    }

    private String[] parseTags(String[] tagPairs) {
        // 将 "key=value" 格式转换为 ["key", "value", ...] 格式
        String[] result = new String[tagPairs.length * 2];
        for (int i = 0; i < tagPairs.length; i++) {
            String[] parts = tagPairs[i].split("=", 2);
            result[i * 2] = parts[0];
            result[i * 2 + 1] = parts.length > 1 ? parts[1] : "";
        }
        return result;
    }
}

