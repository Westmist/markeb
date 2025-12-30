package org.markeb.net.protocol;

import lombok.Getter;
import lombok.Setter;

/**
 * 网关内部协议数据包（游戏服接收网关转发的消息）
 * 协议格式: 4 length + 4 sessionId + 4 messageId + 4 seq + body
 */
@Getter
@Setter
public class GatewayInternalPacket implements Packet {

    /**
     * 会话ID（网关分配的连接标识）
     */
    private int sessionId;

    /**
     * 消息ID
     */
    private int messageId;

    /**
     * 序列号（完整的 int 值）
     */
    private int seqInt;

    /**
     * 消息体
     */
    private byte[] body;

    public GatewayInternalPacket() {
    }

    public GatewayInternalPacket(int sessionId, int messageId, int seq, byte[] body) {
        this.sessionId = sessionId;
        this.messageId = messageId;
        this.seqInt = seq;
        this.body = body;
    }

    /**
     * 实现 Packet 接口的 getSeq 方法（返回 short）
     */
    @Override
    public short getSeq() {
        return (short) seqInt;
    }

    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.GATEWAY_INTERNAL;
    }

    /**
     * 计算总长度（包含长度字段本身）
     */
    public int getTotalLength() {
        return 16 + (body != null ? body.length : 0);
    }
}

