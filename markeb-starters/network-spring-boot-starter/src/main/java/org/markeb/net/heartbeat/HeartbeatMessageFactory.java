package org.markeb.net.heartbeat;

/**
 * 心跳消息工厂接口
 * <p>
 * 框架内部使用，用于创建和识别心跳消息。
 * 心跳消息使用保留的消息ID（0/1），对业务层完全透明。
 * </p>
 */
public interface HeartbeatMessageFactory {

    /**
     * 创建心跳请求消息
     *
     * @return 心跳请求消息对象
     */
    Object createHeartbeatRequest();

    /**
     * 创建心跳响应消息
     *
     * @param request 心跳请求消息
     * @return 心跳响应消息对象
     */
    Object createHeartbeatResponse(Object request);

    /**
     * 判断消息是否为心跳请求
     *
     * @param msg 消息对象
     * @return 是否为心跳请求
     */
    boolean isHeartbeatRequest(Object msg);

    /**
     * 判断消息是否为心跳响应
     *
     * @param msg 消息对象
     * @return 是否为心跳响应
     */
    boolean isHeartbeatResponse(Object msg);

    /**
     * 计算心跳延迟（毫秒）
     *
     * @param response 心跳响应消息
     * @return 延迟时间，如果无法计算返回 -1
     */
    default long calculateLatency(Object response) {
        return -1;
    }
}

