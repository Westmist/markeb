package org.markeb.robot.manager;

import com.google.protobuf.Message;
import org.markeb.robot.client.RobotClient;
import org.markeb.robot.config.RobotConfig;
import org.markeb.robot.message.RobotMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Robot 管理器
 * <p>
 * 管理多个 Robot 客户端的生命周期
 */
public class RobotManager {

    private static final Logger log = LoggerFactory.getLogger(RobotManager.class);

    private final RobotConfig config;
    private final RobotMessageParser messageParser;
    private final Map<String, RobotClient> robots = new ConcurrentHashMap<>();

    public RobotManager(RobotConfig config, RobotMessageParser messageParser) {
        this.config = config;
        this.messageParser = messageParser;
    }

    /**
     * 创建并连接一个机器人
     */
    public CompletableFuture<RobotClient> createAndConnect(String robotId) {
        RobotClient client = new RobotClient(
                robotId,
                config.getGatewayHost(),
                config.getGatewayPort(),
                messageParser
        );

        client.setAutoReconnect(config.isAutoReconnect());
        client.setReconnectDelaySeconds(config.getReconnectDelaySeconds());
        client.setMaxReconnectAttempts(config.getMaxReconnectAttempts());

        robots.put(robotId, client);

        return client.connect().thenApply(v -> client);
    }

    /**
     * 创建并连接多个机器人
     */
    public CompletableFuture<List<RobotClient>> createAndConnectAll(int count) {
        List<CompletableFuture<RobotClient>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String robotId = config.getRobotIdPrefix() + i;
            futures.add(createAndConnect(robotId));

            // 添加连接间隔
            if (config.getLoginIntervalMs() > 0 && i < count - 1) {
                try {
                    Thread.sleep(config.getLoginIntervalMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    /**
     * 获取机器人客户端
     */
    public Optional<RobotClient> getRobot(String robotId) {
        return Optional.ofNullable(robots.get(robotId));
    }

    /**
     * 获取所有机器人
     */
    public List<RobotClient> getAllRobots() {
        return new ArrayList<>(robots.values());
    }

    /**
     * 断开并移除机器人
     */
    public void removeRobot(String robotId) {
        RobotClient client = robots.remove(robotId);
        if (client != null) {
            client.disconnect();
        }
    }

    /**
     * 向所有机器人广播消息
     */
    public void broadcast(Message message) {
        robots.values().forEach(client -> {
            if (client.isConnected()) {
                client.send(message);
            }
        });
    }

    /**
     * 为所有机器人注册消息处理器
     */
    public <T extends Message> void registerHandlerForAll(Class<T> messageClass, Consumer<T> handler) {
        robots.values().forEach(client -> client.registerHandler(messageClass, handler));
    }

    /**
     * 获取已连接的机器人数量
     */
    public int getConnectedCount() {
        return (int) robots.values().stream().filter(RobotClient::isConnected).count();
    }

    /**
     * 获取总机器人数量
     */
    public int getTotalCount() {
        return robots.size();
    }

    /**
     * 关闭所有机器人
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down all robots...");
        robots.values().forEach(RobotClient::disconnect);
        robots.clear();
        log.info("All robots disconnected");
    }
}

