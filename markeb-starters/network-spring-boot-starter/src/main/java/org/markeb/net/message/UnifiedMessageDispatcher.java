package org.markeb.net.message;

import io.netty.channel.ChannelHandlerContext;
import org.markeb.net.handler.MessageContext;
import org.markeb.net.protocol.GameServerPacket;
import org.markeb.net.protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一消息分发器
 * <p>
 * 协议无关的消息分发器，支持多种编解码格式。
 * 使用 {@link IMessageParser} 解析消息，使用 {@link IMessageHandler} 处理消息。
 * </p>
 */
public class UnifiedMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UnifiedMessageDispatcher.class);

    private final IMessageParser messageParser;
    private final Map<Integer, IMessageHandler<?>> handlers = new ConcurrentHashMap<>();

    public UnifiedMessageDispatcher(IMessageParser messageParser) {
        this.messageParser = messageParser;
    }

    /**
     * 注册消息处理器
     *
     * @param messageId 消息ID
     * @param handler   处理器
     */
    public void registerHandler(int messageId, IMessageHandler<?> handler) {
        if (handlers.containsKey(messageId)) {
            throw new IllegalArgumentException("Handler already registered for message ID: " + messageId);
        }
        handlers.put(messageId, handler);
        log.debug("Registered handler for message ID: {}", messageId);
    }

    /**
     * 分发消息
     *
     * @param ctx    Channel 上下文
     * @param packet 数据包
     */
    @SuppressWarnings("unchecked")
    public void dispatch(ChannelHandlerContext ctx, Packet packet) {
        int messageId = packet.getMessageId();
        byte[] body = packet.getBody();

        // 解析消息
        IMessage message;
        try {
            message = messageParser.parse(messageId, body);
        } catch (Exception e) {
            log.warn("Failed to parse message with ID: {}", messageId, e);
            return;
        }

        // 查找处理器
        IMessageHandler<Object> handler = (IMessageHandler<Object>) handlers.get(messageId);
        if (handler == null) {
            log.warn("No handler found for message ID: {}", messageId);
            return;
        }

        // 构建上下文
        MessageContext context = buildContext(ctx, packet);

        // 执行处理
        try {
            Object payload = message.unwrap();
            Object response = handler.handle(context, payload);
            if (response != null) {
                sendResponse(ctx, packet, response);
            }
        } catch (Exception e) {
            log.error("Error handling message ID: {}", messageId, e);
        }
    }

    /**
     * 分发已解析的消息
     *
     * @param ctx     Channel 上下文
     * @param message 已解析的消息
     * @param packet  原始数据包（用于获取元信息）
     */
    @SuppressWarnings("unchecked")
    public void dispatch(ChannelHandlerContext ctx, IMessage message, Packet packet) {
        int messageId = message.getMessageId();

        // 查找处理器
        IMessageHandler<Object> handler = (IMessageHandler<Object>) handlers.get(messageId);
        if (handler == null) {
            log.warn("No handler found for message ID: {}", messageId);
            return;
        }

        // 构建上下文
        MessageContext context = buildContext(ctx, packet);

        // 执行处理
        try {
            Object payload = message.unwrap();
            Object response = handler.handle(context, payload);
            if (response != null) {
                sendResponse(ctx, packet, response);
            }
        } catch (Exception e) {
            log.error("Error handling message ID: {}", messageId, e);
        }
    }

    private MessageContext buildContext(ChannelHandlerContext ctx, Packet packet) {
        MessageContext context = new MessageContext();
        context.setChannel(ctx.channel());
        context.setMessageId(packet.getMessageId());
        context.setSeq(packet.getSeq());

        if (packet instanceof GameServerPacket gsp) {
            context.setGateId(gsp.getGateId());
            context.setRoleId(gsp.getRoleId());
            context.setConId(gsp.getConId());
        }

        return context;
    }

    private void sendResponse(ChannelHandlerContext ctx, Packet request, Object response) {
        // 将响应包装为 IMessage
        IMessage responseMessage = messageParser.wrap(response);
        // 发送响应（这里需要根据具体协议封装）
        ctx.writeAndFlush(responseMessage);
    }

    /**
     * 获取消息解析器
     */
    public IMessageParser getMessageParser() {
        return messageParser;
    }

    /**
     * 检查是否有指定消息ID的处理器
     */
    public boolean hasHandler(int messageId) {
        return handlers.containsKey(messageId);
    }
}

