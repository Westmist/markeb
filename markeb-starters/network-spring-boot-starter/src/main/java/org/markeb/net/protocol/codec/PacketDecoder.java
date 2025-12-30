package org.markeb.net.protocol.codec;

import org.markeb.net.protocol.GameServerPacket;
import org.markeb.net.protocol.GatewayInternalPacket;
import org.markeb.net.protocol.GatewayPacket;
import org.markeb.net.protocol.Packet;
import org.markeb.net.protocol.ProtocolType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 协议解码器
 * 根据协议类型解码为对应的 Packet
 */
public class PacketDecoder extends LengthFieldBasedFrameDecoder {

    private final ProtocolType protocolType;

    public PacketDecoder(ProtocolType protocolType) {
        this(protocolType, 1024 * 1024); // 默认最大 1MB
    }

    public PacketDecoder(ProtocolType protocolType, int maxFrameLength) {
        // length 字段在偏移量 0，长度 4 字节
        // lengthAdjustment = -4 表示 length 包含自身
        // initialBytesToStrip = 4 表示跳过 length 字段
        super(maxFrameLength, 0, 4, -4, 4);
        this.protocolType = protocolType;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            return switch (protocolType) {
                case GATEWAY -> decodeGatewayPacket(frame);
                case GATEWAY_INTERNAL -> decodeGatewayInternalPacket(frame, ctx);
                case GAME_SERVER -> decodeGameServerPacket(frame);
            };
        } finally {
            frame.release();
        }
    }

    /**
     * 解码网关协议
     * 4 messageId + 2 seq + 2 magicNum + body
     */
    private Packet decodeGatewayPacket(ByteBuf frame) {
        int messageId = frame.readInt();
        short seq = frame.readShort();
        short magicNum = frame.readShort();

        byte[] body = new byte[frame.readableBytes()];
        frame.readBytes(body);

        return new GatewayPacket(messageId, seq, magicNum, body);
    }

    /**
     * 解码网关内部协议
     * 4 sessionId + 4 msgId + 4 seq + body
     */
    private Packet decodeGatewayInternalPacket(ByteBuf frame, ChannelHandlerContext ctx) {
        int sessionId = frame.readInt();
        int messageId = frame.readInt();
        int seqInt = frame.readInt();

        byte[] body = new byte[frame.readableBytes()];
        frame.readBytes(body);

        return new GatewayInternalPacket(sessionId, messageId, seqInt, body);
    }

    /**
     * 解码游戏服协议
     * 4 messageId + 2 seq + 2 gateId + 8 roleId + 8 conId + body
     */
    private Packet decodeGameServerPacket(ByteBuf frame) {
        int messageId = frame.readInt();
        short seq = frame.readShort();
        short gateId = frame.readShort();
        long roleId = frame.readLong();
        long conId = frame.readLong();

        byte[] body = new byte[frame.readableBytes()];
        frame.readBytes(body);

        return new GameServerPacket(messageId, seq, gateId, roleId, conId, body);
    }
}

