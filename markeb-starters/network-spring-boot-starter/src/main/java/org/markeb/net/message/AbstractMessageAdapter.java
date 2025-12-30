package org.markeb.net.message;

/**
 * 消息适配器抽象基类
 * <p>
 * 提供通用的消息适配器实现，子类只需实现特定协议的序列化逻辑。
 * </p>
 *
 * @param <T> 原始消息类型
 */
public abstract class AbstractMessageAdapter<T> implements IMessage {

    protected final T payload;
    protected final int messageId;

    protected AbstractMessageAdapter(T payload, int messageId) {
        this.payload = payload;
        this.messageId = messageId;
    }

    @Override
    public int getMessageId() {
        return messageId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R unwrap() {
        return (R) payload;
    }

    @Override
    public Class<?> getPayloadType() {
        return payload.getClass();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "messageId=" + messageId +
                ", payloadType=" + getPayloadType().getSimpleName() +
                '}';
    }
}

