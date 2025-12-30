package org.markeb.robot.actions;

import org.markeb.proto.message.Test.ReqTestMessage;
import org.markeb.proto.message.Test.ReqTestVoidMessage;
import org.markeb.proto.message.Test.ResTestMessage;
import org.markeb.robot.action.*;
import org.markeb.robot.client.RobotClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Actions - for testing storage module
 */
@Component
@RobotMessages({
        ReqTestMessage.class,
        ReqTestVoidMessage.class,
        ResTestMessage.class
})
public class TestActions extends AbstractRobotActions {

    /**
     * ID generator for test entities
     */
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    /**
     * Send test message to save entity
     * <p>
     * This action sends a ReqTestMessage to the game server,
     * which will create and save a TestEntity to database.
     */
    @RobotAction(
            name = "test",
            description = "Send test message to save entity",
            order = 50,
            autoExecute = false
    )
    public void sendTestMessage(RobotClient robot) {
        int id = idGenerator.getAndIncrement();
        String name = "test_" + getRobotId(robot) + "_" + id;

        ReqTestMessage request = ReqTestMessage.newBuilder()
                .setId(id)
                .setName(name)
                .build();

        send(robot, request);
        log.info("[{}] Test message sent: id={}, name={}", getRobotId(robot), id, name);
    }

    /**
     * Send test message with custom id and name
     */
    @RobotAction(
            name = "test_custom",
            description = "Send test message with custom params (use: test_custom)",
            order = 51,
            autoExecute = false
    )
    public void sendTestMessageCustom(RobotClient robot) {
        // Use robot index as id for predictable testing
        String robotId = getRobotId(robot);
        int id = extractRobotIndex(robotId);
        String name = "entity_" + robotId;

        ReqTestMessage request = ReqTestMessage.newBuilder()
                .setId(id)
                .setName(name)
                .build();

        send(robot, request);
        log.info("[{}] Custom test message sent: id={}, name={}", robotId, id, name);
    }

    /**
     * Send batch test messages
     * <p>
     * Sends multiple test messages in sequence
     */
    @RobotAction(
            name = "test_batch",
            description = "Send 10 test messages in batch",
            order = 52,
            autoExecute = false
    )
    public void sendBatchTestMessages(RobotClient robot) {
        log.info("[{}] Sending batch test messages...", getRobotId(robot));

        for (int i = 0; i < 10; i++) {
            int id = idGenerator.getAndIncrement();
            String name = "batch_" + getRobotId(robot) + "_" + id;

            ReqTestMessage request = ReqTestMessage.newBuilder()
                    .setId(id)
                    .setName(name)
                    .build();

            send(robot, request);

            // Small delay between messages
            delay(50);
        }

        log.info("[{}] Batch test messages sent", getRobotId(robot));
    }

    /**
     * Send void test message (no response expected)
     */
    @RobotAction(
            name = "test_void",
            description = "Send void test message (no response)",
            order = 53,
            autoExecute = false
    )
    public void sendVoidTestMessage(RobotClient robot) {
        ReqTestVoidMessage request = ReqTestVoidMessage.newBuilder().build();
        send(robot, request);
        log.info("[{}] Void test message sent", getRobotId(robot));
    }

    /**
     * Handle test response
     */
    @RobotResponseHandler(ResTestMessage.class)
    public void onTestResponse(RobotClient robot, ResTestMessage response) {
        if (response.getResult()) {
            log.info("[{}] Test SUCCESS - entity saved to database", getRobotId(robot));
        } else {
            log.warn("[{}] Test FAILED - entity save failed", getRobotId(robot));
        }
    }

    /**
     * Extract robot index from robot ID (e.g., "robot_5" -> 5)
     */
    private int extractRobotIndex(String robotId) {
        try {
            String[] parts = robotId.split("_");
            if (parts.length > 1) {
                return Integer.parseInt(parts[parts.length - 1]);
            }
        } catch (NumberFormatException ignored) {
        }
        return idGenerator.getAndIncrement();
    }
}

