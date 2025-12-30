package org.markeb.net.heartbeat;

import org.markeb.net.protocol.GameServerPacket;
import org.markeb.net.protocol.GatewayInternalPacket;
import org.markeb.net.protocol.GatewayPacket;
import org.markeb.net.protocol.Packet;
import org.markeb.net.protocol.ProtocolType;

import java.nio.ByteBuffer;

/**
 * 基于 Packet 协议的心跳消息工厂
 * <p>
 * 框架内部使用，业务层无感知。
 * 使用保留的消息ID（0）作为心跳消息，不占用业务消息ID空间。
 * </p>
 * <p>
 * 心跳协议格式：
 * <ul>
 *   <li>请求体：clientTime (8 bytes)</li>
 *   <li>响应体：serverTime (8 bytes) + clientTime (8 bytes)</li>
 * </ul>
 * </p>
 */
public class PacketHeartbeatFactory implements HeartbeatMessageFactory {

    /**
     * 心跳消息使用 messageId = 0，这是保留的系统消息ID
     * 业务消息ID 通常从 10000+ 开始
     */
    public static final int INTERNAL_HEARTBEAT_REQUEST_ID = 0;
    public static final int INTERNAL_HEARTBEAT_RESPONSE_ID = 1;

    private final ProtocolType protocolType;

    public PacketHeartbeatFactory() {
        this(ProtocolType.GATEWAY);
    }

    public PacketHeartbeatFactory(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }

    @Override
    public Object createHeartbeatRequest() {
        long clientTime = System.currentTimeMillis();
        byte[] body = longToBytes(clientTime);

        return switch (protocolType) {
            case GATEWAY -> new GatewayPacket(INTERNAL_HEARTBEAT_REQUEST_ID, (short) 0, (short) 0, body);
            case GATEWAY_INTERNAL -> new GatewayInternalPacket(0, INTERNAL_HEARTBEAT_REQUEST_ID, 0, body);
            case GAME_SERVER -> new GameServerPacket(INTERNAL_HEARTBEAT_REQUEST_ID, (short) 0, (short) 0, 0L, 0L, body);
        };
    }

    @Override
    public Object createHeartbeatResponse(Object request) {
        if (!(request instanceof Packet packet)) {
            return null;
        }

        long serverTime = System.currentTimeMillis();
        long clientTime = 0;

        // 从请求中提取客户端时间戳
        byte[] requestBody = packet.getBody();
        if (requestBody != null && requestBody.length >= 8) {
            clientTime = bytesToLong(requestBody);
        }

        // 响应体：serverTime (8 bytes) + clientTime (8 bytes)
        byte[] body = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(body);
        buffer.putLong(serverTime);
        buffer.putLong(clientTime);

        // 根据请求类型创建对应的响应
        if (request instanceof GatewayPacket gp) {
            return new GatewayPacket(INTERNAL_HEARTBEAT_RESPONSE_ID, gp.getSeq(), gp.getMagicNum(), body);
        } else if (request instanceof GatewayInternalPacket gip) {
            return new GatewayInternalPacket(gip.getSessionId(), INTERNAL_HEARTBEAT_RESPONSE_ID, gip.getSeqInt(), body);
        } else if (request instanceof GameServerPacket gsp) {
            return new GameServerPacket(INTERNAL_HEARTBEAT_RESPONSE_ID, gsp.getSeq(), gsp.getGateId(), gsp.getRoleId(), gsp.getConId(), body);
        }

        return new GatewayPacket(INTERNAL_HEARTBEAT_RESPONSE_ID, (short) 0, (short) 0, body);
    }

    @Override
    public boolean isHeartbeatRequest(Object msg) {
        if (msg instanceof Packet packet) {
            return packet.getMessageId() == INTERNAL_HEARTBEAT_REQUEST_ID;
        }
        return false;
    }

    @Override
    public boolean isHeartbeatResponse(Object msg) {
        if (msg instanceof Packet packet) {
            return packet.getMessageId() == INTERNAL_HEARTBEAT_RESPONSE_ID;
        }
        return false;
    }

    @Override
    public long calculateLatency(Object response) {
        if (!(response instanceof Packet packet)) {
            return -1;
        }

        byte[] body = packet.getBody();
        if (body == null || body.length < 16) {
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

    private static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putLong(value);
        return bytes;
    }

    private static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }
}

