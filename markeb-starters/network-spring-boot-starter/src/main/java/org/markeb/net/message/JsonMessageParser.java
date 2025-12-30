package org.markeb.net.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.markeb.net.serialization.CodecType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON 消息解析器
 * <p>
 * 实现 {@link IMessageParser} 接口，支持 JSON 消息的解析和适配。
 * </p>
 */
public class JsonMessageParser implements IMessageParser {

    private static final Logger log = LoggerFactory.getLogger(JsonMessageParser.class);

    private final ObjectMapper objectMapper;
    private final Map<Integer, Class<?>> idToClass = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> classToId = new ConcurrentHashMap<>();

    public JsonMessageParser() {
        this(new ObjectMapper());
    }

    public JsonMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public CodecType getCodecType() {
        return CodecType.JSON;
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
        Class<?> clazz = idToClass.get(messageId);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown message id: " + messageId);
        }
        try {
            Object message = objectMapper.readValue(data, clazz);
            return new JsonMessageAdapter(message, messageId, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON message with id " + messageId, e);
        }
    }

    @Override
    public IMessage wrap(Object payload) {
        int messageId = getMessageId(payload.getClass());
        return new JsonMessageAdapter(payload, messageId, objectMapper);
    }

    @Override
    public IMessage wrap(Object payload, int messageId) {
        return new JsonMessageAdapter(payload, messageId, objectMapper);
    }

    @Override
    public void register(Class<?> clazz) {
        // JSON 消息需要通过注解或其他方式获取消息ID
        // 这里提供一个默认实现，使用类名的 hashCode 作为消息ID
        // 实际使用时建议通过 @MessageId 注解或配置文件指定
        MessageId annotation = clazz.getAnnotation(MessageId.class);
        if (annotation == null) {
            throw new IllegalArgumentException("JSON message class must have @MessageId annotation: " + clazz.getName());
        }
        register(annotation.value(), clazz);
    }

    @Override
    public void register(int messageId, Class<?> clazz) {
        if (idToClass.containsKey(messageId)) {
            throw new IllegalArgumentException("Message ID already registered: " + messageId);
        }
        idToClass.put(messageId, clazz);
        classToId.put(clazz, messageId);
        log.info("Registered JSON message: {} with msgId: {}", clazz.getName(), messageId);
    }

    @Override
    public boolean isRegistered(int messageId) {
        return idToClass.containsKey(messageId);
    }

    @Override
    public boolean isRegistered(Class<?> clazz) {
        return classToId.containsKey(clazz);
    }

    /**
     * 获取 ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}

