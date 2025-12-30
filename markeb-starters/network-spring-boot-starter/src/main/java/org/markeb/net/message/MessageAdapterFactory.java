package org.markeb.net.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import org.markeb.net.serialization.CodecType;

import java.util.function.Function;

/**
 * 消息适配器工厂
 * <p>
 * 根据编解码类型创建对应的消息适配器。
 * </p>
 */
public class MessageAdapterFactory {

    private final CodecType codecType;
    private final Function<Class<?>, Integer> messageIdResolver;
    private ObjectMapper objectMapper;

    public MessageAdapterFactory(CodecType codecType, Function<Class<?>, Integer> messageIdResolver) {
        this.codecType = codecType;
        this.messageIdResolver = messageIdResolver;
    }

    /**
     * 设置 JSON ObjectMapper（仅 JSON 编码时使用）
     */
    public MessageAdapterFactory withObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    /**
     * 创建消息适配器
     *
     * @param payload 原始消息对象
     * @return 消息适配器
     */
    public IMessage create(Object payload) {
        int messageId = messageIdResolver.apply(payload.getClass());
        return create(payload, messageId);
    }

    /**
     * 创建消息适配器（指定消息ID）
     *
     * @param payload   原始消息对象
     * @param messageId 消息ID
     * @return 消息适配器
     */
    public IMessage create(Object payload, int messageId) {
        return switch (codecType) {
            case PROTOBUF -> {
                if (!(payload instanceof Message)) {
                    throw new IllegalArgumentException("PROTOBUF codec requires Message type, got: " + payload.getClass());
                }
                yield new ProtobufMessageAdapter((Message) payload, messageId);
            }
            case JSON -> objectMapper != null
                    ? new JsonMessageAdapter(payload, messageId, objectMapper)
                    : new JsonMessageAdapter(payload, messageId);
            case PROTOSTUFF -> new ProtostuffMessageAdapter(payload, messageId);
        };
    }

    /**
     * 根据编解码类型和字节数据创建适配器
     *
     * @param messageId 消息ID
     * @param payload   已解析的消息对象
     * @return 消息适配器
     */
    public IMessage wrap(int messageId, Object payload) {
        return create(payload, messageId);
    }

    /**
     * 获取当前编解码类型
     */
    public CodecType getCodecType() {
        return codecType;
    }
}

