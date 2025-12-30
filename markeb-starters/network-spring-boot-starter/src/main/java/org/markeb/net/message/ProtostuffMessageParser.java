package org.markeb.net.message;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.markeb.net.serialization.CodecType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protostuff 消息解析器
 * <p>
 * 实现 {@link IMessageParser} 接口，支持 Protostuff 消息的解析和适配。
 * </p>
 */
public class ProtostuffMessageParser implements IMessageParser {

    private static final Logger log = LoggerFactory.getLogger(ProtostuffMessageParser.class);

    private static final ThreadLocal<LinkedBuffer> BUFFER_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> LinkedBuffer.allocate(512));

    private final Map<Integer, Class<?>> idToClass = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> classToId = new ConcurrentHashMap<>();
    private final Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

    @Override
    public CodecType getCodecType() {
        return CodecType.PROTOSTUFF;
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
    @SuppressWarnings("unchecked")
    public IMessage parse(int messageId, byte[] data) {
        Class<?> clazz = idToClass.get(messageId);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown message id: " + messageId);
        }
        try {
            Schema<Object> schema = (Schema<Object>) getSchema(clazz);
            Object message = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return new ProtostuffMessageAdapter(message, messageId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Protostuff message with id " + messageId, e);
        }
    }

    @Override
    public IMessage wrap(Object payload) {
        int messageId = getMessageId(payload.getClass());
        return new ProtostuffMessageAdapter(payload, messageId);
    }

    @Override
    public IMessage wrap(Object payload, int messageId) {
        return new ProtostuffMessageAdapter(payload, messageId);
    }

    @Override
    public void register(Class<?> clazz) {
        MessageId annotation = clazz.getAnnotation(MessageId.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Protostuff message class must have @MessageId annotation: " + clazz.getName());
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
        // 预热 Schema
        getSchema(clazz);
        log.info("Registered Protostuff message: {} with msgId: {}", clazz.getName(), messageId);
    }

    @Override
    public boolean isRegistered(int messageId) {
        return idToClass.containsKey(messageId);
    }

    @Override
    public boolean isRegistered(Class<?> clazz) {
        return classToId.containsKey(clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> Schema<T> getSchema(Class<T> clazz) {
        return (Schema<T>) schemaCache.computeIfAbsent(clazz, RuntimeSchema::getSchema);
    }
}

