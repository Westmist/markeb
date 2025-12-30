package org.markeb.metrics;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网络层监控指标
 * <p>
 * 提供网络层相关的监控指标，包括：
 * <ul>
 *     <li>连接数统计</li>
 *     <li>流量统计</li>
 *     <li>消息吞吐量</li>
 *     <li>延迟统计</li>
 * </ul>
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Autowired
 * private NetworkMetrics networkMetrics;
 *
 * // 连接建立
 * networkMetrics.connectionOpened();
 *
 * // 记录接收字节
 * networkMetrics.recordBytesReceived(1024);
 *
 * // 记录消息延迟
 * networkMetrics.recordLatency(50);
 * }</pre>
 */
@Slf4j
public class NetworkMetrics {

    private static final String METRIC_PREFIX = "network";

    private final MeterRegistry registry;

    // ============ 连接指标 ============

    /**
     * 当前活跃连接数
     */
    private final AtomicLong activeConnections = new AtomicLong(0);

    /**
     * 连接建立计数器
     */
    private final Counter connectionOpenedCounter;

    /**
     * 连接关闭计数器
     */
    private final Counter connectionClosedCounter;

    /**
     * 连接异常关闭计数器
     */
    private final Counter connectionExceptionCounter;

    // ============ 流量指标 ============

    /**
     * 接收字节计数器
     */
    private final Counter bytesReceivedCounter;

    /**
     * 发送字节计数器
     */
    private final Counter bytesSentCounter;

    /**
     * 接收消息计数器
     */
    private final Counter messagesReceivedCounter;

    /**
     * 发送消息计数器
     */
    private final Counter messagesSentCounter;

    // ============ 延迟指标 ============

    /**
     * 消息延迟（从接收到处理完成）
     */
    private final Timer messageLatencyTimer;

    /**
     * 编解码耗时
     */
    private final Timer codecTimer;

    // ============ 错误指标 ============

    /**
     * 解码错误计数器
     */
    private final Counter decodeErrorCounter;

    /**
     * 编码错误计数器
     */
    private final Counter encodeErrorCounter;

    public NetworkMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 注册活跃连接数 Gauge
        Gauge.builder(METRIC_PREFIX + ".connections.active", activeConnections, AtomicLong::get)
                .description("当前活跃连接数")
                .register(registry);

        // 连接计数器
        this.connectionOpenedCounter = Counter.builder(METRIC_PREFIX + ".connections.opened.total")
                .description("连接建立总数")
                .register(registry);

        this.connectionClosedCounter = Counter.builder(METRIC_PREFIX + ".connections.closed.total")
                .description("连接关闭总数")
                .register(registry);

        this.connectionExceptionCounter = Counter.builder(METRIC_PREFIX + ".connections.exceptions.total")
                .description("连接异常总数")
                .register(registry);

        // 流量计数器
        this.bytesReceivedCounter = Counter.builder(METRIC_PREFIX + ".bytes.received.total")
                .description("接收字节总数")
                .baseUnit("bytes")
                .register(registry);

        this.bytesSentCounter = Counter.builder(METRIC_PREFIX + ".bytes.sent.total")
                .description("发送字节总数")
                .baseUnit("bytes")
                .register(registry);

        this.messagesReceivedCounter = Counter.builder(METRIC_PREFIX + ".messages.received.total")
                .description("接收消息总数")
                .register(registry);

        this.messagesSentCounter = Counter.builder(METRIC_PREFIX + ".messages.sent.total")
                .description("发送消息总数")
                .register(registry);

        // 延迟计时器
        this.messageLatencyTimer = Timer.builder(METRIC_PREFIX + ".message.latency")
                .description("消息处理延迟")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.codecTimer = Timer.builder(METRIC_PREFIX + ".codec.duration")
                .description("编解码耗时")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);

        // 错误计数器
        this.decodeErrorCounter = Counter.builder(METRIC_PREFIX + ".decode.errors.total")
                .description("解码错误总数")
                .register(registry);

        this.encodeErrorCounter = Counter.builder(METRIC_PREFIX + ".encode.errors.total")
                .description("编码错误总数")
                .register(registry);

        log.info("NetworkMetrics initialized");
    }

    // ============ 连接方法 ============

    /**
     * 连接建立
     */
    public void connectionOpened() {
        activeConnections.incrementAndGet();
        connectionOpenedCounter.increment();
    }

    /**
     * 连接建立（带来源标签）
     *
     * @param source 连接来源（如 IP 地址段、区域等）
     */
    public void connectionOpened(String source) {
        connectionOpened();
        Counter.builder(METRIC_PREFIX + ".connections.opened.by.source")
                .tag("source", source)
                .description("按来源统计的连接建立数")
                .register(registry)
                .increment();
    }

    /**
     * 连接关闭
     */
    public void connectionClosed() {
        activeConnections.decrementAndGet();
        connectionClosedCounter.increment();
    }

    /**
     * 连接关闭（带原因标签）
     *
     * @param reason 关闭原因
     */
    public void connectionClosed(String reason) {
        connectionClosed();
        Counter.builder(METRIC_PREFIX + ".connections.closed.by.reason")
                .tag("reason", reason)
                .description("按原因统计的连接关闭数")
                .register(registry)
                .increment();
    }

    /**
     * 连接异常
     */
    public void connectionException() {
        connectionExceptionCounter.increment();
    }

    /**
     * 连接异常（带异常类型）
     *
     * @param exceptionType 异常类型
     */
    public void connectionException(String exceptionType) {
        connectionExceptionCounter.increment();
        Counter.builder(METRIC_PREFIX + ".connections.exceptions.by.type")
                .tag("type", exceptionType)
                .description("按类型统计的连接异常数")
                .register(registry)
                .increment();
    }

    /**
     * 获取当前活跃连接数
     */
    public long getActiveConnectionCount() {
        return activeConnections.get();
    }

    /**
     * 设置活跃连接数（用于同步）
     */
    public void setActiveConnectionCount(long count) {
        activeConnections.set(count);
    }

    // ============ 流量方法 ============

    /**
     * 记录接收字节
     *
     * @param bytes 字节数
     */
    public void recordBytesReceived(long bytes) {
        bytesReceivedCounter.increment(bytes);
    }

    /**
     * 记录发送字节
     *
     * @param bytes 字节数
     */
    public void recordBytesSent(long bytes) {
        bytesSentCounter.increment(bytes);
    }

    /**
     * 记录接收消息
     */
    public void recordMessageReceived() {
        messagesReceivedCounter.increment();
    }

    /**
     * 记录接收消息（带消息类型）
     *
     * @param messageType 消息类型
     */
    public void recordMessageReceived(String messageType) {
        messagesReceivedCounter.increment();
        Counter.builder(METRIC_PREFIX + ".messages.received.by.type")
                .tag("type", messageType)
                .description("按类型统计的接收消息数")
                .register(registry)
                .increment();
    }

    /**
     * 记录接收消息（带消息类型和字节数）
     *
     * @param messageType 消息类型
     * @param bytes       字节数
     */
    public void recordMessageReceived(String messageType, long bytes) {
        recordMessageReceived(messageType);
        recordBytesReceived(bytes);
    }

    /**
     * 记录发送消息
     */
    public void recordMessageSent() {
        messagesSentCounter.increment();
    }

    /**
     * 记录发送消息（带消息类型）
     *
     * @param messageType 消息类型
     */
    public void recordMessageSent(String messageType) {
        messagesSentCounter.increment();
        Counter.builder(METRIC_PREFIX + ".messages.sent.by.type")
                .tag("type", messageType)
                .description("按类型统计的发送消息数")
                .register(registry)
                .increment();
    }

    /**
     * 记录发送消息（带消息类型和字节数）
     *
     * @param messageType 消息类型
     * @param bytes       字节数
     */
    public void recordMessageSent(String messageType, long bytes) {
        recordMessageSent(messageType);
        recordBytesSent(bytes);
    }

    // ============ 延迟方法 ============

    /**
     * 开始计时
     */
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    /**
     * 记录消息延迟
     *
     * @param sample 计时器采样
     */
    public void recordLatency(Timer.Sample sample) {
        sample.stop(messageLatencyTimer);
    }

    /**
     * 记录消息延迟
     *
     * @param latencyMs 延迟（毫秒）
     */
    public void recordLatency(long latencyMs) {
        messageLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录消息延迟（带消息类型）
     *
     * @param messageType 消息类型
     * @param latencyMs   延迟（毫秒）
     */
    public void recordLatency(String messageType, long latencyMs) {
        messageLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);
        Timer.builder(METRIC_PREFIX + ".message.latency.by.type")
                .tag("type", messageType)
                .description("按消息类型统计的处理延迟")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录编解码耗时
     *
     * @param durationNanos 耗时（纳秒）
     * @param operation     操作类型（encode/decode）
     */
    public void recordCodecDuration(long durationNanos, String operation) {
        codecTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        Timer.builder(METRIC_PREFIX + ".codec.duration.by.operation")
                .tag("operation", operation)
                .description("按操作类型统计的编解码耗时")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    // ============ 错误方法 ============

    /**
     * 记录解码错误
     */
    public void recordDecodeError() {
        decodeErrorCounter.increment();
    }

    /**
     * 记录解码错误（带错误类型）
     *
     * @param errorType 错误类型
     */
    public void recordDecodeError(String errorType) {
        decodeErrorCounter.increment();
        Counter.builder(METRIC_PREFIX + ".decode.errors.by.type")
                .tag("type", errorType)
                .description("按类型统计的解码错误数")
                .register(registry)
                .increment();
    }

    /**
     * 记录编码错误
     */
    public void recordEncodeError() {
        encodeErrorCounter.increment();
    }

    /**
     * 记录编码错误（带错误类型）
     *
     * @param errorType 错误类型
     */
    public void recordEncodeError(String errorType) {
        encodeErrorCounter.increment();
        Counter.builder(METRIC_PREFIX + ".encode.errors.by.type")
                .tag("type", errorType)
                .description("按类型统计的编码错误数")
                .register(registry)
                .increment();
    }

    /**
     * 获取 MeterRegistry
     */
    public MeterRegistry getRegistry() {
        return registry;
    }
}

