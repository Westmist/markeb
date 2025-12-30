package org.markeb.net.config;

import org.markeb.net.protocol.ProtocolType;
import org.markeb.net.serialization.CodecType;
import org.markeb.net.transport.TransportType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网络配置属性
 */
@Data
@ConfigurationProperties(prefix = "markeb.network")
public class NetworkProperties {

    /**
     * 是否启用网络模块
     */
    private boolean enabled = true;

    /**
     * 服务器端口
     */
    private int port = 9200;

    /**
     * 传输协议类型
     */
    private TransportType transport = TransportType.TCP;

    /**
     * 协议类型（网关/游戏服）
     */
    private ProtocolType protocol = ProtocolType.GATEWAY;

    /**
     * 编解码类型
     */
    private CodecType codec = CodecType.PROTOBUF;

    /**
     * 最大帧长度
     */
    private int maxFrameLength = 1024 * 1024;

    /**
     * Netty 配置
     */
    private NettyConfig netty = new NettyConfig();

    /**
     * 网关特定配置
     */
    private GatewayConfig gateway = new GatewayConfig();

    /**
     * 游戏服特定配置
     */
    private GameServerConfig gameServer = new GameServerConfig();

    @Data
    public static class NettyConfig {
        /**
         * Boss 线程数
         */
        private int bossThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

        /**
         * Worker 线程数
         */
        private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

        /**
         * 读空闲时间（秒）
         */
        private long readerIdleTime = 60;

        /**
         * 写空闲时间（秒）
         */
        private long writerIdleTime = 30;

        /**
         * 读写空闲时间（秒）
         */
        private long allIdleTime = 0;
    }

    /**
     * 心跳配置
     */
    private HeartbeatConfig heartbeat = new HeartbeatConfig();

    @Data
    public static class HeartbeatConfig {
        /**
         * 是否启用心跳
         */
        private boolean enabled = true;

        /**
         * 最大丢失心跳次数，超过后断开连接
         */
        private int maxMissedHeartbeats = 3;
    }

    @Data
    public static class GatewayConfig {
        /**
         * 是否启用网关模式
         */
        private boolean enabled = false;

        /**
         * 网关ID
         */
        private int gateId = 1;

        /**
         * 魔数
         */
        private int magicNum = 0xABCD;
    }

    @Data
    public static class GameServerConfig {
        /**
         * 是否启用游戏服模式
         */
        private boolean enabled = false;
    }

    /**
     * WebSocket 特定配置
     */
    private WebSocketConfig websocket = new WebSocketConfig();

    @Data
    public static class WebSocketConfig {
        /**
         * WebSocket 路径
         */
        private String path = "/ws";

        /**
         * 最大帧大小（字节）
         */
        private int maxFrameSize = 65536;

        /**
         * 是否启用压缩
         */
        private boolean enableCompression = true;

        /**
         * 是否启用 SSL
         */
        private boolean sslEnabled = false;

        /**
         * SSL 证书路径
         */
        private String sslCertPath;

        /**
         * SSL 私钥路径
         */
        private String sslKeyPath;

        /**
         * SSL 私钥密码
         */
        private String sslKeyPassword;

        /**
         * 子协议（可选）
         */
        private String subprotocols;
    }
}

