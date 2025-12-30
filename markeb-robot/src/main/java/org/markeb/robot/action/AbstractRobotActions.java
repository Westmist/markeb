package org.markeb.robot.action;

import com.google.protobuf.Message;
import org.markeb.robot.client.RobotClient;
import org.markeb.robot.config.RobotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;

/**
 * 机器人动作基类
 * <p>
 * 提供便捷的发送消息、请求响应等方法
 */
public abstract class AbstractRobotActions {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected RobotConfig config;

    /**
     * 发送消息（不等待响应）
     */
    protected void send(RobotClient robot, Message message) {
        robot.send(message);
    }

    /**
     * 发送请求并等待响应
     */
    protected <T extends Message> CompletableFuture<T> request(RobotClient robot, Message message) {
        return robot.request(message, config.getRequestTimeoutMs());
    }

    /**
     * 发送请求并等待响应（自定义超时）
     */
    protected <T extends Message> CompletableFuture<T> request(RobotClient robot, Message message, long timeoutMs) {
        return robot.request(message, timeoutMs);
    }

    /**
     * 延迟执行
     */
    protected void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取机器人ID
     */
    protected String getRobotId(RobotClient robot) {
        return robot.getRobotId();
    }

    /**
     * 检查机器人是否已连接
     */
    protected boolean isConnected(RobotClient robot) {
        return robot.isConnected();
    }
}

