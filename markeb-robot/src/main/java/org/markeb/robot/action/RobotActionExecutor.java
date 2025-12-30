package org.markeb.robot.action;

import com.google.protobuf.Message;
import org.markeb.robot.client.RobotClient;
import org.markeb.robot.manager.RobotManager;
import org.markeb.robot.message.RobotMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 机器人动作执行器
 * <p>
 * 提供便捷的动作执行方法
 */
@Component
public class RobotActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(RobotActionExecutor.class);

    @Autowired
    private RobotActionRegistry actionRegistry;

    @Autowired
    private RobotManager robotManager;

    @Autowired
    private RobotMessageParser messageParser;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 为机器人注册响应处理器
     */
    public void registerResponseHandlers(RobotClient robot) {
        // 注册通用消息处理器，将消息转发给注解定义的处理器
        robot.registerHandler(0, msg -> {
            // 这个处理器不会被调用，因为 msgId=0 不存在
        });

        // 为每个已注册的消息类型添加处理器
        actionRegistry.getResponseHandlers(Message.class); // 触发初始化

        // 通过反射获取所有已注册的消息类型并添加处理器
        for (var entry : getRegisteredMessageClasses()) {
            int msgId = entry;
            robot.registerHandler(msgId, message -> {
                actionRegistry.invokeResponseHandlers(robot, message);
            });
        }
    }

    /**
     * 获取已注册的消息ID列表
     */
    private List<Integer> getRegisteredMessageClasses() {
        // 从 messageParser 获取所有已注册的 msgId
        return messageParser.getRegisteredMsgIds();
    }

    /**
     * 执行指定动作（对所有机器人）
     */
    public void executeForAll(String actionName) {
        ActionInfo action = actionRegistry.getAction(actionName).orElse(null);
        if (action == null) {
            log.warn("Action not found: {}", actionName);
            return;
        }

        for (RobotClient robot : robotManager.getAllRobots()) {
            if (robot.isConnected()) {
                actionRegistry.executeAction(action, robot);
            }
        }
    }

    /**
     * 执行指定动作（对指定机器人）
     */
    public void execute(String robotId, String actionName) {
        robotManager.getRobot(robotId).ifPresentOrElse(
                robot -> actionRegistry.executeAction(actionName, robot),
                () -> log.warn("Robot not found: {}", robotId)
        );
    }

    /**
     * 延迟执行动作
     */
    public void executeDelayed(String actionName, long delayMs) {
        scheduler.schedule(() -> executeForAll(actionName), delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 周期性执行动作
     */
    public void executeAtFixedRate(String actionName, long initialDelayMs, long periodMs) {
        scheduler.scheduleAtFixedRate(
                () -> executeForAll(actionName),
                initialDelayMs,
                periodMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 执行所有自动执行的动作
     */
    public void executeAutoActions(RobotClient robot) {
        List<ActionInfo> autoActions = actionRegistry.getAutoExecuteActions();
        for (ActionInfo action : autoActions) {
            if (action.delayMs() > 0) {
                scheduler.schedule(
                        () -> actionRegistry.executeAction(action, robot),
                        action.delayMs(),
                        TimeUnit.MILLISECONDS
                );
            } else {
                actionRegistry.executeAction(action, robot);
            }
        }
    }

    /**
     * 打印所有可用动作
     */
    public void printActions() {
        actionRegistry.printActions();
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}

