package org.markeb.robot.config;

import org.markeb.robot.manager.RobotManager;
import org.markeb.robot.message.RobotMessageParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Robot 自动配置
 */
@Configuration
@EnableConfigurationProperties(RobotConfig.class)
public class RobotAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RobotMessageParser robotMessageParser() {
        return new RobotMessageParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public RobotManager robotManager(RobotConfig config, RobotMessageParser messageParser) {
        return new RobotManager(config, messageParser);
    }
}

