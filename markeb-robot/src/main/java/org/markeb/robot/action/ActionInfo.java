package org.markeb.robot.action;

import java.lang.reflect.Method;

/**
 * 动作信息
 */
public record ActionInfo(
        String name,
        String description,
        int order,
        boolean autoExecute,
        long delayMs,
        Object bean,
        Method method
) {

    @Override
    public String toString() {
        return String.format("%-20s - %s (order=%d, auto=%s)", 
                name, description, order, autoExecute);
    }
}

