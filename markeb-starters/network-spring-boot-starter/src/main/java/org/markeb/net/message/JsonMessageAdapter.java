package org.markeb.net.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 消息适配器
 * <p>
 * 将 POJO 对象适配为统一的 {@link IMessage} 接口，使用 Jackson 进行序列化。
 * </p>
 */
public class JsonMessageAdapter extends AbstractMessageAdapter<Object> {

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private final ObjectMapper objectMapper;

    public JsonMessageAdapter(Object payload, int messageId) {
        this(payload, messageId, DEFAULT_MAPPER);
    }

    public JsonMessageAdapter(Object payload, int messageId, ObjectMapper objectMapper) {
        super(payload, messageId);
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] toBytes() {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON message", e);
        }
    }

    /**
     * 获取 JSON 对象
     *
     * @param <T> 对象类型
     * @return JSON 对象
     */
    public <T> T getJsonObject() {
        return unwrap();
    }
}

