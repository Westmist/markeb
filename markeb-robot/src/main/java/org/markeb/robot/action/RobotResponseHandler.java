package org.markeb.robot.action;

import java.lang.annotation.*;

/**
 * 机器人响应处理器注解
 * <p>
 * 标注在方法上，定义消息响应处理器
 * 
 * <pre>
 * {@code
 * @RobotResponseHandler(ResLoginMessage.class)
 * public void onLoginResponse(RobotClient robot, ResLoginMessage response) {
 *     log.info("Login success: {}", response.getSuccess());
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RobotResponseHandler {

    /**
     * 响应消息类型
     */
    Class<?> value();
}

