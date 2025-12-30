package org.markeb.robot.action;

import java.lang.annotation.*;

/**
 * 机器人消息注册注解
 * <p>
 * 标注在类上，声明该类需要注册的 Protobuf 消息
 * 
 * <pre>
 * {@code
 * @Component
 * @RobotMessages({
 *     ReqLoginMessage.class,
 *     ResLoginMessage.class
 * })
 * public class LoginActions extends AbstractRobotActions {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RobotMessages {

    /**
     * 需要注册的消息类型
     */
    Class<?>[] value();
}

