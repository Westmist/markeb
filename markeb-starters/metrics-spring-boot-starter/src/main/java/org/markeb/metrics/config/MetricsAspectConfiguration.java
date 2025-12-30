package org.markeb.metrics.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.markeb.metrics.aspect.MeteredAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控切面配置
 * <p>
 * 配置 Micrometer 的注解支持切面。
 * </p>
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
public class MetricsAspectConfiguration {

    /**
     * Micrometer @Timed 注解支持
     */
    @Bean
    @ConditionalOnMissingBean
    public TimedAspect timedAspect(MeterRegistry registry) {
        log.info("Creating TimedAspect bean");
        return new TimedAspect(registry);
    }

    /**
     * Micrometer @Counted 注解支持
     */
    @Bean
    @ConditionalOnMissingBean
    public CountedAspect countedAspect(MeterRegistry registry) {
        log.info("Creating CountedAspect bean");
        return new CountedAspect(registry);
    }

    /**
     * 自定义 @Metered 注解支持
     */
    @Bean
    @ConditionalOnMissingBean
    public MeteredAspect meteredAspect(MeterRegistry registry) {
        log.info("Creating MeteredAspect bean");
        return new MeteredAspect(registry);
    }
}

