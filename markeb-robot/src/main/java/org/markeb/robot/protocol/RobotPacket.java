package org.markeb.robot.protocol;

import java.nio.ByteBuffer;

/**
 * Robot 协议数据包
 * <p>
 * 客户端 -> 网关协议格式: length(4) + msgId(4) + seq(4) + body(n)
 * 网关 -> 客户端协议格式: length(4) + msgId(4) + seq(4) + body(n)
 * </p>
 * <p>
 * 心跳协议（框架内部使用，业务层无感知）：
 * <ul>
 *   <li>msgId=0: 心跳请求，body 为客户端时间戳（8字节）</li>
 *   <li>msgId=1: 心跳响应，body 为服务端时间戳(8字节) + 客户端时间戳(8字节)</li>
 * </ul>
 * </p>
 */
public class RobotPacket {

    /**
     * 心跳请求消息ID（保留）
     */
    public static final int HEARTBEAT_REQUEST_ID = 0;

    /**
     * 心跳响应消息ID（保留）
     */
    public static final int HEARTBEAT_RESPONSE_ID = 1;

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

    /**
     * 是否为心跳请求
     */
    public boolean isHeartbeatRequest() {
        return msgId == HEARTBEAT_REQUEST_ID;
    }

    /**
     * 是否为心跳响应
     */
    public boolean isHeartbeatResponse() {
        return msgId == HEARTBEAT_RESPONSE_ID;
    }

    /**
     * 创建心跳请求包
     */
    public static RobotPacket createHeartbeatRequest() {
        byte[] body = new byte[8];
        ByteBuffer.wrap(body).putLong(System.currentTimeMillis());
        return new RobotPacket(HEARTBEAT_REQUEST_ID, 0, body);
    }

    /**
     * 创建心跳响应包
     */
    public static RobotPacket createHeartbeatResponse(RobotPacket request) {
        long clientTime = 0;
        if (request.body != null && request.body.length >= 8) {
            clientTime = ByteBuffer.wrap(request.body).getLong();
        }

        byte[] body = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(body);
        buffer.putLong(System.currentTimeMillis()); // serverTime
        buffer.putLong(clientTime);                  // clientTime

        return new RobotPacket(HEARTBEAT_RESPONSE_ID, request.seq, body);
    }

    /**
     * 计算心跳延迟（毫秒）
     *
     * @return 延迟时间，如果无法计算返回 -1
     */
    public long calculateHeartbeatLatency() {
        if (!isHeartbeatResponse() || body == null || body.length < 16) {
            return -1;
        }

        ByteBuffer buffer = ByteBuffer.wrap(body);
        buffer.getLong(); // skip serverTime
        long clientTime = buffer.getLong();

        if (clientTime > 0) {
            return System.currentTimeMillis() - clientTime;
        }
        return -1;
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

