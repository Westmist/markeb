package org.markeb.net.message;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Protostuff 消息适配器
 * <p>
 * 将 POJO 对象适配为统一的 {@link IMessage} 接口，使用 Protostuff 进行序列化。
 * Protostuff 比 Protobuf 更快，且不需要 .proto 文件。
 * </p>
 */
public class ProtostuffMessageAdapter extends AbstractMessageAdapter<Object> {

    private static final ThreadLocal<LinkedBuffer> BUFFER_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> LinkedBuffer.allocate(512));

    public ProtostuffMessageAdapter(Object payload, int messageId) {
        super(payload, messageId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] toBytes() {
        Schema<Object> schema = (Schema<Object>) RuntimeSchema.getSchema(payload.getClass());
        LinkedBuffer buffer = BUFFER_THREAD_LOCAL.get();
        try {
            return ProtostuffIOUtil.toByteArray(payload, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 获取原始对象
     *
     * @param <T> 对象类型
     * @return 原始对象
     */
    public <T> T getObject() {
        return unwrap();
    }
}

