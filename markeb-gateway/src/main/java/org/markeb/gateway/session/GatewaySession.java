package org.markeb.gateway.session;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.netty.channel.Channel;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 网关会话
 * 表示一个客户端到网关的连接
 */
public class GatewaySession {

    /**
     * 会话ID（网关内唯一）
     */
    private final int sessionId;

    /**
     * 限流桶
     */
    private final Bucket bucket;

    /**
     * 玩家ID（登录后绑定）
     */
    private volatile Long playerId;

    /**
     * 绑定的区服/节点ID
     */
    private volatile String nodeId;

    /**
     * 前端连接（客户端到网关）
     */
    private final Channel frontendChannel;

    /**
     * 会话创建时间
     */
    private final LocalDateTime createTime;

    /**
     * 最后活跃时间
     */
    private volatile LocalDateTime lastActiveTime;

    /**
     * 消息序列号生成器
     */
    private final AtomicInteger seqGenerator = new AtomicInteger(0);

    /**
     * 会话状态
     */
    private volatile SessionState state = SessionState.CONNECTED;

    public GatewaySession(int sessionId, Channel frontendChannel) {
        this.sessionId = sessionId;
        this.frontendChannel = frontendChannel;
        this.createTime = LocalDateTime.now();
        this.lastActiveTime = this.createTime;
        
        // 初始化限流桶：每秒 50 个请求
        Bandwidth limit = Bandwidth.classic(50, Refill.greedy(50, Duration.ofSeconds(1)));
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume() {
        return bucket.tryConsume(1);
    }

    public int getSessionId() {
        return sessionId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Channel getFrontendChannel() {
        return frontendChannel;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void updateActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    public int nextSeq() {
        return seqGenerator.incrementAndGet();
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public boolean isActive() {
        return frontendChannel != null && frontendChannel.isActive();
    }

    public boolean isAuthenticated() {
        return playerId != null && state == SessionState.AUTHENTICATED;
    }

    /**
     * 发送消息到客户端
     */
    public void send(Object msg) {
        if (isActive()) {
            frontendChannel.writeAndFlush(msg);
        }
    }

    /**
     * 关闭会话
     */
    public void close() {
        state = SessionState.CLOSED;
        if (frontendChannel != null && frontendChannel.isActive()) {
            frontendChannel.close();
        }
    }

    public enum SessionState {
        /**
         * 已连接（未认证）
         */
        CONNECTED,

        /**
         * 认证中
         */
        AUTHENTICATING,

        /**
         * 已认证
         */
        AUTHENTICATED,

        /**
         * 已关闭
         */
        CLOSED
    }

    @Override
    public String toString() {
        return "GatewaySession{" +
                "sessionId=" + sessionId +
                ", playerId=" + playerId +
                ", nodeId='" + nodeId + '\'' +
                ", state=" + state +
                '}';
    }
}

