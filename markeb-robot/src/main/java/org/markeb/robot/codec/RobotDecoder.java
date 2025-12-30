package org.markeb.robot.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.markeb.robot.protocol.RobotPacket;

/**
 * Robot 协议解码器
 * <p>
 * 解码网关发送的消息
 * 协议格式: length(4) + msgId(4) + seq(4) + body(n)
 */
public class RobotDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MAX_FRAME_LENGTH = 1024 * 1024; // 1MB

    public RobotDecoder() {
        super(MAX_FRAME_LENGTH,
                0,      // lengthFieldOffset
                4,      // lengthFieldLength
                0,      // lengthAdjustment
                4);     // initialBytesToStrip (strip length field)
    }

    public RobotDecoder(int maxFrameLength) {
        super(maxFrameLength, 0, 4, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            // 读取协议头
            int msgId = frame.readInt();
            int seq = frame.readInt();

            // 读取消息体
            byte[] body = new byte[frame.readableBytes()];
            frame.readBytes(body);

            return new RobotPacket(msgId, seq, body);
        } finally {
            frame.release();
        }
    }
}

