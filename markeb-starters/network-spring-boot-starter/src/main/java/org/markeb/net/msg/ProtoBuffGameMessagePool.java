package org.markeb.net.msg;

import org.markeb.net.codec.ProtoBuffGameDecoder;
import org.markeb.net.codec.ProtoBuffGameEncoder;
import org.markeb.net.codec.ProtoBuffGatewayDecoder;
import org.markeb.net.codec.ProtoBuffGatewayEncoder;
import org.markeb.net.register.GameActorContext;
import org.markeb.net.register.IContextHandle;
import com.google.protobuf.Message;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.HashMap;
import java.util.Map;

public class ProtoBuffGameMessagePool implements IMessagePool<Message> {

    private final IGameParser<?> protoBuffParser;
    private final boolean gatewayMode;

    private static final Map<Integer, IContextHandle<? extends GameActorContext, Message>> handlerPool = new HashMap<>();

    public ProtoBuffGameMessagePool(IGameParser<?> protoBuffParser) {
        this(protoBuffParser, false);
    }

    /**
     * 创建消息池
     *
     * @param protoBuffParser 消息解析器
     * @param gatewayMode     是否为网关模式（接收网关转发的消息）
     */
    public ProtoBuffGameMessagePool(IGameParser<?> protoBuffParser, boolean gatewayMode) {
        this.protoBuffParser = protoBuffParser;
        this.gatewayMode = gatewayMode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IGameParser<Message> messageParser() {
        return (IGameParser<Message>) protoBuffParser;
    }

    @Override
    public MessageToByteEncoder<Message> encoder() {
        if (gatewayMode) {
            return new ProtoBuffGatewayEncoder(messageParser());
        }
        return new ProtoBuffGameEncoder(messageParser());
    }

    @Override
    public ByteToMessageDecoder decoder() {
        if (gatewayMode) {
            return new ProtoBuffGatewayDecoder(messageParser());
        }
        return new ProtoBuffGameDecoder(messageParser());
    }

    @Override
    public void register(int msgId, IContextHandle<? extends GameActorContext, Message> contextHandle) {
        if (handlerPool.containsKey(msgId)) {
            throw new IllegalArgumentException("Handler already registered for message ID: " + msgId);
        }
        handlerPool.put(msgId, contextHandle);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IContextHandle<? extends GameActorContext, Message> getHandler(Message message) {
        IGameParser<Message> parser = messageParser();
        Class<Message> clazz = (Class<Message>) message.getClass();
        int messageId = parser.messageId(clazz);
        return handlerPool.get(messageId);
    }

}
