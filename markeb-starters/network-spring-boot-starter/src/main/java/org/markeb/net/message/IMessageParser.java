package org.markeb.net.message;

import org.markeb.net.serialization.CodecType;

/**
 * 消息解析器接口
 * <p>
 * 协议无关的消息解析器，支持将字节数据解析为 {@link IMessage}。
 * </p>
 */
public interface IMessageParser {

    /**
     * 获取编解码类型
     *
     * @return 编解码类型
     */
    CodecType getCodecType();

    /**
     * 根据消息ID获取消息类型
     *
     * @param messageId 消息ID
     * @return 消息类型，如果未注册返回 null
     */
    Class<?> getMessageClass(int messageId);

    /**
     * 根据消息类型获取消息ID
     *
     * @param clazz 消息类型
     * @return 消息ID
     * @throws IllegalArgumentException 如果消息类型未注册
     */
    int getMessageId(Class<?> clazz);

    /**
     * 解析消息
     *
     * @param messageId 消息ID
     * @param data      字节数据
     * @return 解析后的 IMessage
     */
    IMessage parse(int messageId, byte[] data);

    /**
     * 将对象包装为 IMessage
     *
     * @param payload 原始消息对象
     * @return IMessage
     */
    IMessage wrap(Object payload);

    /**
     * 将对象包装为 IMessage（指定消息ID）
     *
     * @param payload   原始消息对象
     * @param messageId 消息ID
     * @return IMessage
     */
    IMessage wrap(Object payload, int messageId);

    /**
     * 注册消息类型
     *
     * @param clazz 消息类型
     */
    void register(Class<?> clazz);

    /**
     * 注册消息类型（指定消息ID）
     *
     * @param messageId 消息ID
     * @param clazz     消息类型
     */
    void register(int messageId, Class<?> clazz);

    /**
     * 检查消息ID是否已注册
     *
     * @param messageId 消息ID
     * @return 是否已注册
     */
    boolean isRegistered(int messageId);

    /**
     * 检查消息类型是否已注册
     *
     * @param clazz 消息类型
     * @return 是否已注册
     */
    boolean isRegistered(Class<?> clazz);
}

