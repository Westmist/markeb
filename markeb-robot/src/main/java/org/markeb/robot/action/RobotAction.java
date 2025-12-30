package org.markeb.robot.action;

import java.lang.annotation.*;

/**
 * 机器人动作注解
 * <p>
 * 标注在方法上，定义一个可执行的机器人动作
 * 
 * <pre>
 * {@code
 * @RobotAction(name = "login", description = "登录请求")
 * public void login(RobotClient robot) {
 *     robot.send(ReqLoginMessage.newBuilder()...build());
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RobotAction {

    /**
     * 动作名称（唯一标识）
     */
    String name();

    /**
     * 动作描述
     */
    String description() default "";

    /**
     * 执行顺序（用于自动执行时的排序，数值越小越先执行）
     */
    int order() default 100;

    /**
     * 是否在启动时自动执行
     */
    boolean autoExecute() default false;

    /**
     * 执行延迟（毫秒），仅在 autoExecute=true 时生效
     */
    long delayMs() default 0;
}

