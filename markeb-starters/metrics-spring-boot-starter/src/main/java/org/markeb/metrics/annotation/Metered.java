package org.markeb.metrics.annotation;

import java.lang.annotation.*;

/**
 * 方法监控注解
 * <p>
 * 标记在方法上，自动记录方法调用次数和耗时。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Metered(name = "player.login", description = "玩家登录")
 * public void login(long playerId) {
 *     // 登录逻辑
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Metered {

    /**
     * 指标名称
     */
    String name();

    /**
     * 指标描述
     */
    String description() default "";

    /**
     * 是否记录耗时
     */
    boolean timed() default true;

    /**
     * 是否记录调用次数
     */
    boolean counted() default true;

    /**
     * 额外标签（key=value 格式）
     */
    String[] tags() default {};
}

