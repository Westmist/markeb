package org.markeb.net.codec;

import org.markeb.net.msg.IGameParser;
import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.AttributeKey;

/**
 * 网关内部协议解码器
 * <p>
 * 用于游戏服接收网关转发的消息，协议格式：
 * length(4) + sessionId(4) + msgId(4) + seq(4) + body(n)
 * <p>
 * 解码后将 sessionId 和 seq 存储到 Channel 属性中，供业务层使用。
 */
public class ProtoBuffGatewayDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * Channel 属性 Key - sessionId
     */
    public static final AttributeKey<Integer> SESSION_ID_KEY = AttributeKey.valueOf("sessionId");

    /**
     * Channel 属性 Key - seq
     */
    public static final AttributeKey<Integer> SEQ_KEY = AttributeKey.valueOf("seq");

    private final IGameParser<Message> parser;

    public ProtoBuffGatewayDecoder(IGameParser<Message> parser) {
        super(1024 * 1024, 0, 4, 0, 4);
        this.parser = parser;
    }

    public ProtoBuffGatewayDecoder(IGameParser<Message> parser, int maxFrameLength) {
        super(maxFrameLength, 0, 4, 0, 4);
        this.parser = parser;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        try {
            // 读取协议头
            int sessionId = frame.readInt();
            int msgId = frame.readInt();
            int seq = frame.readInt();

            // 将 sessionId 和 seq 存储到 Channel 属性中
            ctx.channel().attr(SESSION_ID_KEY).set(sessionId);
            ctx.channel().attr(SEQ_KEY).set(seq);

            // 读取消息体
            byte[] bodyBytes = new byte[frame.readableBytes()];
            frame.readBytes(bodyBytes);

            // 解析 protobuf 消息
            return parser.parseFrom(msgId, bodyBytes);
        } finally {
            frame.release();
        }
    }

}

