package org.markeb.net.message;

import com.google.protobuf.Message;

/**
 * Protobuf 消息适配器
 * <p>
 * 将 Google Protobuf 的 {@link Message} 适配为统一的 {@link IMessage} 接口。
 * </p>
 */
public class ProtobufMessageAdapter extends AbstractMessageAdapter<Message> {

    public ProtobufMessageAdapter(Message payload, int messageId) {
        super(payload, messageId);
    }

    @Override
    public byte[] toBytes() {
        return payload.toByteArray();
    }

    /**
     * 获取 Protobuf 消息
     *
     * @param <T> Protobuf 消息类型
     * @return Protobuf 消息对象
     */
    public <T extends Message> T getProtoMessage() {
        return unwrap();
    }
}

