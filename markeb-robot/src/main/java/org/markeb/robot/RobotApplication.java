package org.markeb.robot;

import org.markeb.robot.action.RobotActionExecutor;
import org.markeb.robot.action.RobotActionRegistry;
import org.markeb.robot.config.RobotConfig;
import org.markeb.robot.manager.RobotManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Robot 应用启动类
 */
@SpringBootApplication
public class RobotApplication {

    private static final Logger log = LoggerFactory.getLogger(RobotApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RobotApplication.class, args);
    }

    /**
     * 启动机器人
     */
    @Bean
    public CommandLineRunner startRobots(
            RobotManager robotManager,
            RobotConfig config,
            RobotActionRegistry actionRegistry,
            RobotActionExecutor actionExecutor) {

        return args -> {
            // 打印所有可用动作
            actionRegistry.printActions();

            log.info("Starting robots with config: host={}, port={}, count={}",
                    config.getGatewayHost(), config.getGatewayPort(), config.getRobotCount());

            // 创建并连接机器人
            robotManager.createAndConnectAll(config.getRobotCount())
                    .thenAccept(robots -> {
                        log.info("All {} robots connected", robots.size());

                        // 为每个机器人注册响应处理器并执行自动动作
                        robots.forEach(robot -> {
                            actionExecutor.registerResponseHandlers(robot);

                            if (config.isAutoLogin()) {
                                actionExecutor.executeAutoActions(robot);
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to start robots", ex);
                        return null;
                    });
        };
    }
}
