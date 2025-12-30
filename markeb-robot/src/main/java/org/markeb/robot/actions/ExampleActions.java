package org.markeb.robot.actions;

import org.markeb.robot.action.*;
import org.markeb.robot.client.RobotClient;
import org.springframework.stereotype.Component;

/**
 * 示例动作模板
 * <p>
 * 复制此文件并修改，快速添加新的测试动作
 * 
 * <h2>使用方法：</h2>
 * <ol>
 *   <li>复制此文件，重命名为 XxxActions.java</li>
 *   <li>在 @RobotMessages 中声明需要的消息类型</li>
 *   <li>使用 @RobotAction 定义请求动作</li>
 *   <li>使用 @RobotResponseHandler 定义响应处理器</li>
 * </ol>
 * 
 * <h2>动作执行方式：</h2>
 * <ul>
 *   <li>autoExecute=true: 机器人连接后自动执行</li>
 *   <li>通过 RobotActionExecutor.execute() 手动执行</li>
 *   <li>通过控制台命令执行（如果启用了控制台）</li>
 * </ul>
 */
@Component
// @RobotMessages({
//     YourReqMessage.class,
//     YourResMessage.class
// })
public class ExampleActions extends AbstractRobotActions {

    // ==================== 动作定义示例 ====================

    /**
     * 简单动作示例
     * <p>
     * autoExecute=false 表示不会自动执行，需要手动调用
     */
    @RobotAction(
            name = "example_simple",
            description = "简单动作示例",
            order = 100,
            autoExecute = false
    )
    public void simpleAction(RobotClient robot) {
        log.info("[{}] Executing simple action", getRobotId(robot));
        
        // 发送消息示例：
        // YourReqMessage request = YourReqMessage.newBuilder()
        //     .setField("value")
        //     .build();
        // send(robot, request);
    }

    /**
     * 自动执行动作示例
     * <p>
     * autoExecute=true 表示机器人连接后自动执行
     * order=10 表示执行顺序，数值越小越先执行
     */
    // @RobotAction(
    //     name = "example_auto",
    //     description = "自动执行动作示例",
    //     order = 10,
    //     autoExecute = true
    // )
    // public void autoAction(RobotClient robot) {
    //     log.info("[{}] Auto action executed", getRobotId(robot));
    // }

    /**
     * 延迟执行动作示例
     * <p>
     * delayMs=1000 表示延迟 1 秒后执行
     */
    // @RobotAction(
    //     name = "example_delayed",
    //     description = "延迟执行动作示例",
    //     order = 20,
    //     autoExecute = true,
    //     delayMs = 1000
    // )
    // public void delayedAction(RobotClient robot) {
    //     log.info("[{}] Delayed action executed", getRobotId(robot));
    // }

    /**
     * 请求-响应动作示例
     * <p>
     * 发送请求并等待响应
     */
    // @RobotAction(
    //     name = "example_request",
    //     description = "请求-响应动作示例",
    //     order = 30,
    //     autoExecute = false
    // )
    // public void requestAction(RobotClient robot) {
    //     YourReqMessage request = YourReqMessage.newBuilder()
    //         .setField("value")
    //         .build();
    //     
    //     request(robot, request)
    //         .thenAccept(response -> {
    //             log.info("[{}] Got response: {}", getRobotId(robot), response);
    //         })
    //         .exceptionally(ex -> {
    //             log.error("[{}] Request failed", getRobotId(robot), ex);
    //             return null;
    //         });
    // }

    // ==================== 响应处理器示例 ====================

    /**
     * 响应处理器示例
     * <p>
     * 当收到指定类型的消息时自动调用
     * 
     * @param robot 机器人客户端
     * @param response 响应消息
     */
    // @RobotResponseHandler(YourResMessage.class)
    // public void onYourResponse(RobotClient robot, YourResMessage response) {
    //     log.info("[{}] Received response: {}", getRobotId(robot), response);
    // }

    /**
     * 只接收消息的处理器（不需要 robot 参数）
     */
    // @RobotResponseHandler(YourResMessage.class)
    // public void onYourResponseSimple(YourResMessage response) {
    //     log.info("Received response: {}", response);
    // }
}

