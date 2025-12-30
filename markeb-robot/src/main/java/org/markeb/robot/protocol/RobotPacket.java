package org.markeb.robot.protocol;

/**
 * Robot 协议数据包
 * <p>
 * 客户端 -> 网关协议格式: length(4) + msgId(4) + seq(4) + body(n)
 * 网关 -> 客户端协议格式: length(4) + msgId(4) + seq(4) + body(n)
 */
public class RobotPacket {

    /**
     * 消息ID
     */
    private final int msgId;

    /**
     * 序列号（用于请求-响应匹配）
     */
    private final int seq;

    /**
     * 消息体（protobuf 序列化后的字节数组）
     */
    private final byte[] body;

    public RobotPacket(int msgId, int seq, byte[] body) {
        this.msgId = msgId;
        this.seq = seq;
        this.body = body;
    }

    public int getMsgId() {
        return msgId;
    }

    public int getSeq() {
        return seq;
    }

    public byte[] getBody() {
        return body;
    }

    /**
     * 计算数据长度（不包含 length 字段本身）
     * msgId(4) + seq(4) + body(n)
     */
    public int getDataLength() {
        return 8 + (body != null ? body.length : 0);
    }

    @Override
    public String toString() {
        return "RobotPacket{" +
                "msgId=" + msgId +
                ", seq=" + seq +
                ", bodyLength=" + (body != null ? body.length : 0) +
                '}';
    }
}

