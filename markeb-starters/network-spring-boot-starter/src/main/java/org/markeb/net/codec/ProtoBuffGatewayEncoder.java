package org.markeb.net.codec;

import org.markeb.net.msg.IGameParser;
import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 网关内部协议编码器
 * <p>
 * 用于游戏服向网关发送响应消息，协议格式：
 * length(4) + sessionId(4) + msgId(4) + seq(4) + body(n)
 * <p>
 * sessionId 和 seq 从 Channel 属性中获取（由 ProtoBuffGatewayDecoder 设置）。
 */
public class ProtoBuffGatewayEncoder extends MessageToByteEncoder<Message> {

    private final IGameParser<Message> parser;

    public ProtoBuffGatewayEncoder(IGameParser<Message> parser) {
        this.parser = parser;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
        // 获取 sessionId 和 seq
        Integer sessionId = ctx.channel().attr(ProtoBuffGatewayDecoder.SESSION_ID_KEY).get();
        Integer seq = ctx.channel().attr(ProtoBuffGatewayDecoder.SEQ_KEY).get();

        if (sessionId == null) {
            sessionId = 0;
        }
        if (seq == null) {
            seq = 0;
        }

        // 获取消息 ID 和序列化消息体
        Class<? extends Message> aClass = msg.getClass();
        int msgId = parser.messageId((Class<Message>) aClass);
        byte[] bytes = msg.toByteArray();

        // 写入协议头和消息体
        // length = sessionId(4) + msgId(4) + seq(4) + body(n)
        out.writeInt(12 + bytes.length);
        out.writeInt(sessionId);
        out.writeInt(msgId);
        out.writeInt(seq);
        out.writeBytes(bytes);
    }
}

