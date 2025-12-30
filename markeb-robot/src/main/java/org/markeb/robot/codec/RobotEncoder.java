package org.markeb.robot.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.markeb.robot.message.RobotMessageParser;
import org.markeb.robot.protocol.RobotPacket;

/**
 * Robot 协议编码器
 * <p>
 * 编码发送给网关的消息
 * 协议格式: length(4) + msgId(4) + seq(4) + body(n)
 */
public class RobotEncoder extends MessageToByteEncoder<RobotPacket> {

    private final RobotMessageParser messageParser;

    public RobotEncoder(RobotMessageParser messageParser) {
        this.messageParser = messageParser;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RobotPacket packet, ByteBuf out) {
        byte[] body = packet.getBody();
        int bodyLen = body != null ? body.length : 0;

        // 写入长度字段: msgId(4) + seq(4) + body(n)
        out.writeInt(8 + bodyLen);

        // 写入协议头
        out.writeInt(packet.getMsgId());
        out.writeInt(packet.getSeq());

        // 写入消息体
        if (bodyLen > 0) {
            out.writeBytes(body);
        }
    }
}

