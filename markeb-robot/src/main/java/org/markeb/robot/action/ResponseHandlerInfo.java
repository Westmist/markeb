package org.markeb.robot.action;

import java.lang.reflect.Method;

/**
 * 响应处理器信息
 */
public record ResponseHandlerInfo(
        Class<?> messageClass,
        Object bean,
        Method method
) {
}

