package org.markeb.robot.console;

import org.markeb.robot.action.RobotActionExecutor;
import org.markeb.robot.action.RobotActionRegistry;
import org.markeb.robot.config.RobotConfig;
import org.markeb.robot.manager.RobotManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 机器人控制台
 * <p>
 * 提供命令行交互功能，可以在运行时执行动作
 * 
 * <h2>可用命令：</h2>
 * <ul>
 *   <li>help - 显示帮助信息</li>
 *   <li>list - 列出所有可用动作</li>
 *   <li>exec &lt;action&gt; - 对所有机器人执行动作</li>
 *   <li>exec &lt;robotId&gt; &lt;action&gt; - 对指定机器人执行动作</li>
 *   <li>status - 显示机器人状态</li>
 *   <li>quit - 退出程序</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "markeb.robot.console.enabled", havingValue = "true", matchIfMissing = true)
public class RobotConsole {

    private static final Logger log = LoggerFactory.getLogger(RobotConsole.class);

    @Autowired
    private RobotActionRegistry actionRegistry;

    @Autowired
    private RobotActionExecutor actionExecutor;

    @Autowired
    private RobotManager robotManager;

    @Autowired
    private RobotConfig config;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    @PostConstruct
    public void start() {
        executor.submit(this::runConsole);
    }

    @PreDestroy
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private void runConsole() {
        // 等待一小段时间让应用启动完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        printHelp();
        System.out.println("\nRobot Console Ready. Type 'help' for available commands.\n");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running) {
                System.out.print("robot> ");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    processCommand(line);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (running) {
                log.error("Console error", e);
            }
        }
    }

    private void processCommand(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help", "h", "?" -> printHelp();
            case "list", "ls", "actions" -> listActions();
            case "exec", "e", "run" -> execAction(parts);
            case "status", "s" -> printStatus();
            case "quit", "exit", "q" -> {
                System.out.println("Shutting down...");
                System.exit(0);
            }
            default -> System.out.println("Unknown command: " + cmd + ". Type 'help' for available commands.");
        }
    }

    private void printHelp() {
        System.out.println("""
                
                ╔════════════════════════════════════════════════════════════╗
                ║                    Robot Console Help                       ║
                ╠════════════════════════════════════════════════════════════╣
                ║  help, h, ?              - Show this help message           ║
                ║  list, ls, actions       - List all available actions       ║
                ║  exec <action>           - Execute action for all robots    ║
                ║  exec <robotId> <action> - Execute action for specific robot║
                ║  status, s               - Show robot status                ║
                ║  quit, exit, q           - Exit the program                 ║
                ╚════════════════════════════════════════════════════════════╝
                """);
    }

    private void listActions() {
        System.out.println("\n=== Available Actions ===");
        actionRegistry.getAllActions().stream()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .forEach(action -> {
                    String auto = action.autoExecute() ? " [AUTO]" : "";
                    System.out.printf("  %-25s - %s%s%n", action.name(), action.description(), auto);
                });
        System.out.println("=========================\n");
    }

    private void execAction(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: exec <action> or exec <robotId> <action>");
            return;
        }

        if (parts.length == 2) {
            // exec <action> - 对所有机器人执行
            String actionName = parts[1];
            System.out.println("Executing action '" + actionName + "' for all robots...");
            actionExecutor.executeForAll(actionName);
        } else {
            // exec <robotId> <action> - 对指定机器人执行
            String robotId = parts[1];
            String actionName = parts[2];
            System.out.println("Executing action '" + actionName + "' for robot '" + robotId + "'...");
            actionExecutor.execute(robotId, actionName);
        }
    }

    private void printStatus() {
        System.out.println("\n=== Robot Status ===");
        System.out.printf("Gateway: %s:%d%n", config.getGatewayHost(), config.getGatewayPort());
        System.out.printf("Total Robots: %d%n", robotManager.getTotalCount());
        System.out.printf("Connected: %d%n", robotManager.getConnectedCount());
        System.out.println();

        robotManager.getAllRobots().forEach(robot -> {
            String status = robot.isConnected() ? "✓ Connected" : "✗ Disconnected";
            System.out.printf("  %-20s %s%n", robot.getRobotId(), status);
        });
        System.out.println("====================\n");
    }
}

