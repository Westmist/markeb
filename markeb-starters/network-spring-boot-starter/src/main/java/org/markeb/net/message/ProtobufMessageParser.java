package org.markeb.net.message;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.markeb.net.serialization.CodecType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protobuf 消息解析器
 * <p>
 * 实现 {@link IMessageParser} 接口，支持 Google Protobuf 消息的解析和适配。
 * </p>
 */
public class ProtobufMessageParser implements IMessageParser {

    private static final Logger log = LoggerFactory.getLogger(ProtobufMessageParser.class);

    private static final String MSG_ID_OPTION = "msgId";
    private static final String NOTICE_ID_OPTION = "noticeId";

    private final Map<Integer, Parser<? extends Message>> parserMap = new ConcurrentHashMap<>();
    private final Map<Integer, Class<?>> idToClass = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> classToId = new ConcurrentHashMap<>();

    @Override
    public CodecType getCodecType() {
        return CodecType.PROTOBUF;
    }

    @Override
    public Class<?> getMessageClass(int messageId) {
        return idToClass.get(messageId);
    }

    @Override
    public int getMessageId(Class<?> clazz) {
        Integer msgId = classToId.get(clazz);
        if (msgId == null) {
            throw new IllegalArgumentException("Message class not registered: " + clazz.getName());
        }
        return msgId;
    }

    @Override
    public IMessage parse(int messageId, byte[] data) {
        Parser<? extends Message> parser = parserMap.get(messageId);
        if (parser == null) {
            throw new IllegalArgumentException("Unknown message id: " + messageId);
        }
        try {
            Message message = parser.parseFrom(data);
            return new ProtobufMessageAdapter(message, messageId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message with id " + messageId, e);
        }
    }

    @Override
    public IMessage wrap(Object payload) {
        if (!(payload instanceof Message message)) {
            throw new IllegalArgumentException("Payload must be a Protobuf Message: " + payload.getClass());
        }
        int messageId = getMessageId(payload.getClass());
        return new ProtobufMessageAdapter(message, messageId);
    }

    @Override
    public IMessage wrap(Object payload, int messageId) {
        if (!(payload instanceof Message message)) {
            throw new IllegalArgumentException("Payload must be a Protobuf Message: " + payload.getClass());
        }
        return new ProtobufMessageAdapter(message, messageId);
    }

    @Override
    public void register(Class<?> clazz) {
        if (!Message.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class must be a Protobuf Message: " + clazz.getName());
        }
        @SuppressWarnings("unchecked")
        Class<? extends Message> messageClass = (Class<? extends Message>) clazz;
        int msgId = findMsgIdFromClass(messageClass);
        Parser<? extends Message> parser = findParserFromClass(messageClass);
        register(msgId, messageClass, parser);
    }

    @Override
    public void register(int messageId, Class<?> clazz) {
        if (!Message.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class must be a Protobuf Message: " + clazz.getName());
        }
        @SuppressWarnings("unchecked")
        Class<? extends Message> messageClass = (Class<? extends Message>) clazz;
        Parser<? extends Message> parser = findParserFromClass(messageClass);
        register(messageId, messageClass, parser);
    }

    /**
     * 注册消息类型（带解析器）
     */
    public void register(int messageId, Class<? extends Message> messageClass, Parser<? extends Message> parser) {
        if (parserMap.containsKey(messageId)) {
            throw new IllegalArgumentException("Message ID already registered: " + messageId);
        }
        parserMap.put(messageId, parser);
        idToClass.put(messageId, messageClass);
        classToId.put(messageClass, messageId);
        log.info("Registered Protobuf message: {} with msgId: {}", messageClass.getName(), messageId);
    }

    @Override
    public boolean isRegistered(int messageId) {
        return parserMap.containsKey(messageId);
    }

    @Override
    public boolean isRegistered(Class<?> clazz) {
        return classToId.containsKey(clazz);
    }

    /**
     * 从 Protobuf 消息类中提取 msgId
     */
    private int findMsgIdFromClass(Class<? extends Message> messageClazz) {
        try {
            Method method = messageClazz.getMethod("getDescriptor");
            Descriptors.Descriptor descriptor = (Descriptors.Descriptor) method.invoke(null);
            DescriptorProtos.MessageOptions opts = descriptor.toProto().getOptions();

            Map<Descriptors.FieldDescriptor, Object> allFields = descriptor.getOptions().getAllFields();
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : allFields.entrySet()) {
                Descriptors.FieldDescriptor fd = entry.getKey();
                Object value = entry.getValue();
                if (fd.isExtension() && (MSG_ID_OPTION.equals(fd.getName()) || NOTICE_ID_OPTION.equals(fd.getName())) && value instanceof Integer) {
                    return (Integer) value;
                }
            }

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
            throw new IllegalArgumentException(MSG_ID_OPTION + "/" + NOTICE_ID_OPTION + " option not found in " + messageClazz.getName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse " + MSG_ID_OPTION + ": " + messageClazz.getName(), e);
        }
    }

    /**
     * 从 Protobuf 消息类中获取 Parser
     */
    @SuppressWarnings("unchecked")
    private Parser<? extends Message> findParserFromClass(Class<? extends Message> messageClazz) {
        try {
            Field field = messageClazz.getDeclaredField("PARSER");
            field.setAccessible(true);
            return (Parser<? extends Message>) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get PARSER field from " + messageClazz.getName(), e);
        }
    }
}

