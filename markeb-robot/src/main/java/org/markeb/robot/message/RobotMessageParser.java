package org.markeb.robot.message;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Robot 消息解析器
 * <p>
 * 用于注册和解析 Protobuf 消息
 */
public class RobotMessageParser {

    private static final Logger log = LoggerFactory.getLogger(RobotMessageParser.class);

    private static final String MSG_ID_OPTION = "msgId";
    private static final String NOTICE_ID_OPTION = "noticeId";

    /**
     * msgId -> Parser 映射
     */
    private final Map<Integer, Parser<? extends Message>> parserMap = new HashMap<>();

    /**
     * Class -> msgId 映射
     */
    private final Map<Class<? extends Message>, Integer> classToMsgId = new HashMap<>();

    /**
     * 注册消息类型
     */
    public void register(Class<? extends Message> messageClass) {
        int msgId = findMsgIdFromClass(messageClass);
        Parser<? extends Message> parser = findParserFromClass(messageClass);

        if (parserMap.containsKey(msgId)) {
            log.warn("Message ID {} already registered, overwriting with {}", msgId, messageClass.getName());
        }

        parserMap.put(msgId, parser);
        classToMsgId.put(messageClass, msgId);
        log.info("Registered message: {} with msgId: {}", messageClass.getSimpleName(), msgId);
    }

    /**
     * 批量注册消息类型
     */
    @SafeVarargs
    public final void registerAll(Class<? extends Message>... messageClasses) {
        for (Class<? extends Message> clazz : messageClasses) {
            register(clazz);
        }
    }

    /**
     * 获取消息的 msgId
     */
    public int getMsgId(Class<? extends Message> messageClass) {
        Integer msgId = classToMsgId.get(messageClass);
        if (msgId == null) {
            throw new IllegalArgumentException("Message class not registered: " + messageClass.getName());
        }
        return msgId;
    }

    /**
     * 解析消息
     */
    public Message parse(int msgId, byte[] body) {
        Parser<? extends Message> parser = parserMap.get(msgId);
        if (parser == null) {
            throw new IllegalArgumentException("Unknown message id: " + msgId);
        }
        try {
            return parser.parseFrom(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message with id " + msgId, e);
        }
    }

    /**
     * 检查消息是否已注册
     */
    public boolean isRegistered(int msgId) {
        return parserMap.containsKey(msgId);
    }

    /**
     * 检查消息类是否已注册
     */
    public boolean isRegistered(Class<? extends Message> messageClass) {
        return classToMsgId.containsKey(messageClass);
    }

    /**
     * 获取所有已注册的消息ID
     */
    public java.util.List<Integer> getRegisteredMsgIds() {
        return new java.util.ArrayList<>(parserMap.keySet());
    }

    /**
     * 获取所有已注册的消息类
     */
    public java.util.Set<Class<? extends Message>> getRegisteredClasses() {
        return new java.util.HashSet<>(classToMsgId.keySet());
    }

    /**
     * 从 Protobuf 类中提取 msgId
     */
    private int findMsgIdFromClass(Class<? extends Message> messageClass) {
        try {
            Method method = messageClass.getMethod("getDescriptor");
            Descriptors.Descriptor descriptor = (Descriptors.Descriptor) method.invoke(null);
            DescriptorProtos.MessageOptions opts = descriptor.toProto().getOptions();

            // 尝试从扩展字段获取
            Map<Descriptors.FieldDescriptor, Object> allFields = descriptor.getOptions().getAllFields();
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : allFields.entrySet()) {
                Descriptors.FieldDescriptor fd = entry.getKey();
                Object value = entry.getValue();
                if (fd.isExtension() && 
                    (MSG_ID_OPTION.equals(fd.getName()) || NOTICE_ID_OPTION.equals(fd.getName())) && 
                    value instanceof Integer) {
                    return (Integer) value;
                }
            }

            // 尝试从未解释的选项获取
            for (DescriptorProtos.UninterpretedOption uo : opts.getUninterpretedOptionList()) {
                String optionName = uo.getNameCount() == 1 ? uo.getName(0).getNamePart() : null;
                if (MSG_ID_OPTION.equals(optionName) || NOTICE_ID_OPTION.equals(optionName)) {
                    if (uo.hasPositiveIntValue()) {
                        return (int) uo.getPositiveIntValue();
                    }
                    if (uo.hasNegativeIntValue()) {
                        return (int) uo.getNegativeIntValue();
                    }
                    if (uo.hasIdentifierValue()) {
                        return Integer.parseInt(uo.getIdentifierValue());
                    }
                }
            }

            throw new IllegalArgumentException(MSG_ID_OPTION + "/" + NOTICE_ID_OPTION + 
                    " option not found in " + messageClass.getName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get msgId from " + messageClass.getName(), e);
        }
    }

    /**
     * 从 Protobuf 类中获取 Parser
     */
    @SuppressWarnings("unchecked")
    private Parser<? extends Message> findParserFromClass(Class<? extends Message> messageClass) {
        try {
            Field field = messageClass.getDeclaredField("PARSER");
            field.setAccessible(true);
            return (Parser<? extends Message>) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get PARSER field from " + messageClass.getName(), e);
        }
    }
}

