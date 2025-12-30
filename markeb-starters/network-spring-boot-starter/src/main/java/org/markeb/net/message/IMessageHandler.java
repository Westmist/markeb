package org.markeb.net.message;

import org.markeb.net.handler.MessageContext;

/**
 * 消息处理器接口
 * <p>
 * 协议无关的消息处理器，处理 {@link IMessage} 类型的消息。
 * </p>
 *
 * @param <T> 消息载荷类型
 */
@FunctionalInterface
public interface IMessageHandler<T> {

    /**
     * 处理消息
     *
     * @param context 消息上下文
     * @param message 消息（已解包的载荷）
     * @return 响应消息，如果不需要响应返回 null
     */
    Object handle(MessageContext context, T message);
}

