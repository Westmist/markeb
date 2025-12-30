/**
 * 心跳机制模块 - 框架内部使用
 * <p>
 * 本模块提供了完全透明的心跳保活机制，业务层无需感知。
 * </p>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>使用保留的消息ID（0/1），不占用业务消息ID空间</li>
 *   <li>心跳消息在 {@link org.markeb.net.heartbeat.HeartbeatHandler} 中被拦截，不会传递到业务层</li>
 *   <li>自动响应心跳请求，自动发送心跳保活</li>
 * </ul>
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link org.markeb.net.heartbeat.HeartbeatHandler} - Netty 心跳处理器</li>
 *   <li>{@link org.markeb.net.heartbeat.HeartbeatMessageFactory} - 心跳消息工厂接口</li>
 *   <li>{@link org.markeb.net.heartbeat.PacketHeartbeatFactory} - 基于 Packet 协议的实现</li>
 * </ul>
 *
 * <h2>工作流程</h2>
 * <pre>
 * 1. 写空闲（writerIdleTime）触发 → 发送心跳请求（msgId=0）
 * 2. 收到心跳请求 → 自动回复心跳响应（msgId=1）
 * 3. 读空闲（readerIdleTime）触发 → 丢失计数+1
 * 4. 丢失次数超过阈值 → 关闭连接
 * </pre>
 *
 * <h2>配置</h2>
 * <pre>{@code
 * markeb:
 *   network:
 *     netty:
 *       reader-idle-time: 60   # 读空闲时间（秒）
 *       writer-idle-time: 30   # 写空闲时间（秒），触发发送心跳
 *     heartbeat:
 *       enabled: true          # 是否启用心跳（默认开启）
 *       max-missed-heartbeats: 3  # 最大丢失次数
 * }</pre>
 *
 * @see org.markeb.net.heartbeat.HeartbeatHandler
 * @see org.markeb.net.heartbeat.HeartbeatMessageFactory
 */
package org.markeb.net.heartbeat;

