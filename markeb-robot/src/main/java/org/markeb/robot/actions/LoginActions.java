package org.markeb.robot.actions;

import org.markeb.proto.message.Login.ReqLoginMessage;
import org.markeb.proto.message.Login.ResLoginMessage;
import org.markeb.proto.message.Login.ResRoleBasicInfoMessage;
import org.markeb.robot.action.*;
import org.markeb.robot.client.RobotClient;
import org.springframework.stereotype.Component;

/**
 * 登录相关动作
 * <p>
 * 示例：如何使用注解定义机器人动作
 */
@Component
@RobotMessages({
        ReqLoginMessage.class,
        ResLoginMessage.class,
        ResRoleBasicInfoMessage.class
})
public class LoginActions extends AbstractRobotActions {

    /**
     * 登录动作
     * <p>
     * 自动执行，优先级最高
     */
    @RobotAction(
            name = "login",
            description = "发送登录请求",
            order = 1,
            autoExecute = true
    )
    public void login(RobotClient robot) {
        ReqLoginMessage request = ReqLoginMessage.newBuilder()
                .setOpenId(getRobotId(robot))
                .setToken("test_token_" + getRobotId(robot))
                .build();

        send(robot, request);
        log.info("[{}] Login request sent", getRobotId(robot));
    }

    /**
     * 登录响应处理
     */
    @RobotResponseHandler(ResLoginMessage.class)
    public void onLoginResponse(RobotClient robot, ResLoginMessage response) {
        log.info("[{}] Login response: success={}, openId={}",
                getRobotId(robot), response.getSuccess(), response.getOpenId());
    }

    /**
     * 角色信息响应处理
     */
    @RobotResponseHandler(ResRoleBasicInfoMessage.class)
    public void onRoleBasicInfo(RobotClient robot, ResRoleBasicInfoMessage response) {
        log.info("[{}] Role info: roleId={}, name={}, level={}",
                getRobotId(robot), response.getRoleId(), response.getName(), response.getLevel());
    }
}

