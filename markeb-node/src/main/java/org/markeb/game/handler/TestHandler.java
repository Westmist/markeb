package org.markeb.game.handler;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.markeb.game.actor.Player;
import org.markeb.game.entity.TestEntity;
import org.markeb.net.register.MessageHandler;
import org.markeb.persistent.DataCenter;
import org.markeb.proto.message.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TestHandler {

    private static final Logger log = LoggerFactory.getLogger(TestHandler.class);

    @MessageHandler
    @RateLimiter(name = "gameAction")
    public Test.ResTestMessage test(Player player, Test.ReqTestMessage reqTestMessage) {
        log.info("TestHandler test method called with message: {}", reqTestMessage);

        // Create and save test entity
        TestEntity entity = new TestEntity(reqTestMessage.getId(), reqTestMessage.getName());
        
        try {
            // Save entity asynchronously
            DataCenter.saveAsync(entity);
            log.info("TestEntity saved async: id={}, name={}", entity.getId(), entity.getName());
            
            return Test.ResTestMessage.newBuilder()
                    .setResult(true)
                    .build();
        } catch (Exception e) {
            log.error("Failed to save TestEntity: id={}, name={}", entity.getId(), entity.getName(), e);
            return Test.ResTestMessage.newBuilder()
                    .setResult(false)
                    .build();
        }
    }

    @MessageHandler
    public void testVoid(Player player, Test.ReqTestVoidMessage reqTestVoidMessage) {
        log.info("TestHandler test void method called with message: {}", reqTestVoidMessage);
    }

}
