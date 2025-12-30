package org.markeb.robot.action;

import com.google.protobuf.Message;
import org.markeb.robot.client.RobotClient;
import org.markeb.robot.message.RobotMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机器人动作注册中心
 * <p>
 * 扫描并注册所有带有 @RobotAction 和 @RobotResponseHandler 注解的方法
 */
@Component
public class RobotActionRegistry {

    private static final Logger log = LoggerFactory.getLogger(RobotActionRegistry.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RobotMessageParser messageParser;

    /**
     * 动作映射 (name -> ActionInfo)
     */
    private final Map<String, ActionInfo> actions = new ConcurrentHashMap<>();

    /**
     * 响应处理器映射 (messageClass -> List<ResponseHandlerInfo>)
     */
    private final Map<Class<?>, List<ResponseHandlerInfo>> responseHandlers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        scanAndRegister();
    }

    /**
     * 扫描并注册所有动作和处理器
     */
    private void scanAndRegister() {
        Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);

        for (Object bean : beans.values()) {
            Class<?> beanClass = bean.getClass();

            // 注册消息
            registerMessages(beanClass);

            // 扫描方法
            for (Method method : beanClass.getDeclaredMethods()) {
                // 注册动作
                RobotAction actionAnnotation = method.getAnnotation(RobotAction.class);
                if (actionAnnotation != null) {
                    registerAction(bean, method, actionAnnotation);
                }

                // 注册响应处理器
                RobotResponseHandler handlerAnnotation = method.getAnnotation(RobotResponseHandler.class);
                if (handlerAnnotation != null) {
                    registerResponseHandler(bean, method, handlerAnnotation);
                }
            }
        }

        log.info("Registered {} actions, {} response handlers", 
                actions.size(), responseHandlers.values().stream().mapToInt(List::size).sum());
    }

    /**
     * 注册消息类型
     */
    @SuppressWarnings("unchecked")
    private void registerMessages(Class<?> beanClass) {
        RobotMessages messagesAnnotation = beanClass.getAnnotation(RobotMessages.class);
        if (messagesAnnotation != null) {
            for (Class<?> messageClass : messagesAnnotation.value()) {
                if (Message.class.isAssignableFrom(messageClass)) {
                    try {
                        messageParser.register((Class<? extends Message>) messageClass);
                    } catch (Exception e) {
                        log.warn("Failed to register message: {}", messageClass.getName(), e);
                    }
                }
            }
        }
    }

    /**
     * 注册动作
     */
    private void registerAction(Object bean, Method method, RobotAction annotation) {
        String name = annotation.name();
        if (actions.containsKey(name)) {
            log.warn("Duplicate action name: {}, overwriting", name);
        }

        method.setAccessible(true);
        ActionInfo info = new ActionInfo(
                name,
                annotation.description(),
                annotation.order(),
                annotation.autoExecute(),
                annotation.delayMs(),
                bean,
                method
        );
        actions.put(name, info);
        log.debug("Registered action: {}", info);
    }

    /**
     * 注册响应处理器
     */
    private void registerResponseHandler(Object bean, Method method, RobotResponseHandler annotation) {
        Class<?> messageClass = annotation.value();
        method.setAccessible(true);

        ResponseHandlerInfo info = new ResponseHandlerInfo(messageClass, bean, method);
        responseHandlers.computeIfAbsent(messageClass, k -> new ArrayList<>()).add(info);
        log.debug("Registered response handler for: {}", messageClass.getSimpleName());
    }

    /**
     * 获取所有动作
     */
    public Collection<ActionInfo> getAllActions() {
        return actions.values();
    }

    /**
     * 获取动作
     */
    public Optional<ActionInfo> getAction(String name) {
        return Optional.ofNullable(actions.get(name));
    }

    /**
     * 获取自动执行的动作（按 order 排序）
     */
    public List<ActionInfo> getAutoExecuteActions() {
        return actions.values().stream()
                .filter(ActionInfo::autoExecute)
                .sorted(Comparator.comparingInt(ActionInfo::order))
                .toList();
    }

    /**
     * 获取响应处理器
     */
    public List<ResponseHandlerInfo> getResponseHandlers(Class<?> messageClass) {
        return responseHandlers.getOrDefault(messageClass, Collections.emptyList());
    }

    /**
     * 执行动作
     */
    public void executeAction(String name, RobotClient robot) {
        ActionInfo action = actions.get(name);
        if (action == null) {
            log.warn("Action not found: {}", name);
            return;
        }
        executeAction(action, robot);
    }

    /**
     * 执行动作
     */
    public void executeAction(ActionInfo action, RobotClient robot) {
        try {
            Method method = action.method();
            int paramCount = method.getParameterCount();

            if (paramCount == 0) {
                method.invoke(action.bean());
            } else if (paramCount == 1) {
                method.invoke(action.bean(), robot);
            } else {
                log.error("Invalid action method signature: {}", method);
            }
        } catch (Exception e) {
            log.error("Failed to execute action: {}", action.name(), e);
        }
    }

    /**
     * 调用响应处理器
     */
    public void invokeResponseHandlers(RobotClient robot, Message message) {
        List<ResponseHandlerInfo> handlers = responseHandlers.get(message.getClass());
        if (handlers == null || handlers.isEmpty()) {
            return;
        }

        for (ResponseHandlerInfo handler : handlers) {
            try {
                Method method = handler.method();
                int paramCount = method.getParameterCount();

                if (paramCount == 1) {
                    method.invoke(handler.bean(), message);
                } else if (paramCount == 2) {
                    method.invoke(handler.bean(), robot, message);
                } else {
                    log.error("Invalid response handler method signature: {}", method);
                }
            } catch (Exception e) {
                log.error("Failed to invoke response handler", e);
            }
        }
    }

    /**
     * 打印所有可用动作
     */
    public void printActions() {
        log.info("=== Available Robot Actions ===");
        actions.values().stream()
                .sorted(Comparator.comparingInt(ActionInfo::order))
                .forEach(action -> log.info("  {}", action));
        log.info("===============================");
    }
}

