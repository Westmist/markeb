package org.markeb.robot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Robot 配置
 */
@Data
@ConfigurationProperties(prefix = "markeb.robot")
public class RobotConfig {

    /**
     * 网关服务器地址
     */
    private String gatewayHost = "127.0.0.1";

    /**
     * 网关服务器端口
     */
    private int gatewayPort = 7000;

    /**
     * 机器人数量
     */
    private int robotCount = 1;

    /**
     * 机器人 ID 前缀
     */
    private String robotIdPrefix = "robot_";

    /**
     * 是否自动重连
     */
    private boolean autoReconnect = true;

    /**
     * 重连延迟（秒）
     */
    private int reconnectDelaySeconds = 5;

    /**
     * 最大重连次数
     */
    private int maxReconnectAttempts = 10;

    /**
     * 请求超时时间（毫秒）
     */
    private long requestTimeoutMs = 30000;

    /**
     * 是否自动登录
     */
    private boolean autoLogin = true;

    /**
     * 登录间隔（毫秒）- 多个机器人登录时的间隔
     */
    private long loginIntervalMs = 100;
}

