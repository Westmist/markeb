package org.markeb.net.message;

/**
 * 协议无关的消息接口
 * <p>
 * 所有消息类型（Protobuf、JSON、Protostuff 等）都通过适配器实现此接口，
 * 使得上层业务代码可以统一处理不同协议的消息。
 * </p>
 */
public interface IMessage {

    /**
     * 获取消息ID
     *
     * @return 消息ID
     */
    int getMessageId();

    /**
     * 将消息序列化为字节数组
     *
     * @return 字节数组
     */
    byte[] toBytes();

    /**
     * 获取原始消息对象
     * <p>
     * 用于需要访问底层具体消息类型的场景
     * </p>
     *
     * @param <T> 消息类型
     * @return 原始消息对象
     */
    <T> T unwrap();

    /**
     * 获取原始消息对象的类型
     *
     * @return 消息类型
     */
    Class<?> getPayloadType();
}

