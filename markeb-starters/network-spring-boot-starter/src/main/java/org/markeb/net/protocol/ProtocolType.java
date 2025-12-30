package org.markeb.net.protocol;

/**
 * 协议类型枚举
 */
public enum ProtocolType {

    /**
     * 网关协议（面向客户端）
     * 4 length + 4 messageId + 2 seq + 2 magicNum
     * 总协议头长度: 12 bytes
     */
    GATEWAY(12),

    /**
     * 网关内部协议（游戏服接收网关转发的消息）
     * 4 length + 4 sessionId + 4 msgId + 4 seq + body
     * 总协议头长度: 16 bytes
     */
    GATEWAY_INTERNAL(16),

    /**
     * 游戏服协议
     * 4 length + 4 messageId + 2 seq + 2 gateId + 8 roleId + 8 conId
     * 总协议头长度: 28 bytes
     */
    GAME_SERVER(28);

    private final int headerLength;

    ProtocolType(int headerLength) {
        this.headerLength = headerLength;
    }

    public int getHeaderLength() {
        return headerLength;
    }
}

