package org.markeb.metrics;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 游戏核心监控指标
 * <p>
 * 提供游戏服务通用的监控指标，包括：
 * <ul>
 *     <li>在线玩家数</li>
 *     <li>消息处理统计</li>
 *     <li>请求延迟统计</li>
 * </ul>
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Autowired
 * private GameMetrics gameMetrics;
 *
 * // 玩家上线
 * gameMetrics.playerOnline();
 *
 * // 记录消息处理
 * Timer.Sample sample = gameMetrics.startTimer();
 * // ... 处理消息 ...
 * gameMetrics.recordMessageProcessed("LoginRequest", sample);
 * }</pre>
 */
@Slf4j
public class GameMetrics {

    private static final String METRIC_PREFIX = "game";

    private final MeterRegistry registry;

    // ============ 玩家相关指标 ============

    /**
     * 在线玩家数（Gauge）
     */
    private final AtomicLong onlinePlayers = new AtomicLong(0);

    /**
     * 玩家登录计数器
     */
    private final Counter playerLoginCounter;

    /**
     * 玩家登出计数器
     */
    private final Counter playerLogoutCounter;

    // ============ 消息相关指标 ============

    /**
     * 消息接收计数器
     */
    private final Counter messageReceivedCounter;

    /**
     * 消息发送计数器
     */
    private final Counter messageSentCounter;

    /**
     * 消息处理耗时
     */
    private final Timer messageProcessTimer;

    /**
     * 消息处理错误计数器
     */
    private final Counter messageErrorCounter;

    public GameMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 注册在线玩家 Gauge
        Gauge.builder(METRIC_PREFIX + ".players.online", onlinePlayers, AtomicLong::get)
                .description("当前在线玩家数")
                .register(registry);

        // 玩家登录/登出计数器
        this.playerLoginCounter = Counter.builder(METRIC_PREFIX + ".players.login.total")
                .description("玩家登录总次数")
                .register(registry);

        this.playerLogoutCounter = Counter.builder(METRIC_PREFIX + ".players.logout.total")
                .description("玩家登出总次数")
                .register(registry);

        // 消息计数器
        this.messageReceivedCounter = Counter.builder(METRIC_PREFIX + ".messages.received.total")
                .description("接收消息总数")
                .register(registry);

        this.messageSentCounter = Counter.builder(METRIC_PREFIX + ".messages.sent.total")
                .description("发送消息总数")
                .register(registry);

        // 消息处理耗时
        this.messageProcessTimer = Timer.builder(METRIC_PREFIX + ".messages.process.duration")
                .description("消息处理耗时")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        // 消息错误计数器
        this.messageErrorCounter = Counter.builder(METRIC_PREFIX + ".messages.errors.total")
                .description("消息处理错误总数")
                .register(registry);

        log.info("GameMetrics initialized");
    }

    // ============ 玩家相关方法 ============

    /**
     * 玩家上线
     */
    public void playerOnline() {
        onlinePlayers.incrementAndGet();
        playerLoginCounter.increment();
    }

    /**
     * 玩家下线
     */
    public void playerOffline() {
        onlinePlayers.decrementAndGet();
        playerLogoutCounter.increment();
    }

    /**
     * 获取当前在线玩家数
     */
    public long getOnlinePlayerCount() {
        return onlinePlayers.get();
    }

    /**
     * 设置在线玩家数（用于同步）
     */
    public void setOnlinePlayerCount(long count) {
        onlinePlayers.set(count);
    }

    // ============ 消息相关方法 ============

    /**
     * 记录接收消息
     */
    public void recordMessageReceived() {
        messageReceivedCounter.increment();
    }

    /**
     * 记录接收消息（带消息类型）
     */
    public void recordMessageReceived(String messageType) {
        messageReceivedCounter.increment();
        Counter.builder(METRIC_PREFIX + ".messages.received.by.type")
                .tag("type", messageType)
                .description("按类型统计的接收消息数")
                .register(registry)
                .increment();
    }

    /**
     * 记录发送消息
     */
    public void recordMessageSent() {
        messageSentCounter.increment();
    }

    /**
     * 记录发送消息（带消息类型）
     */
    public void recordMessageSent(String messageType) {
        messageSentCounter.increment();
        Counter.builder(METRIC_PREFIX + ".messages.sent.by.type")
                .tag("type", messageType)
                .description("按类型统计的发送消息数")
                .register(registry)
                .increment();
    }

    /**
     * 开始计时
     */
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    /**
     * 记录消息处理完成
     *
     * @param sample 计时器采样
     */
    public void recordMessageProcessed(Timer.Sample sample) {
        sample.stop(messageProcessTimer);
    }

    /**
     * 记录消息处理完成（带消息类型）
     *
     * @param messageType 消息类型
     * @param sample      计时器采样
     */
    public void recordMessageProcessed(String messageType, Timer.Sample sample) {
        Timer timer = Timer.builder(METRIC_PREFIX + ".messages.process.duration.by.type")
                .tag("type", messageType)
                .description("按类型统计的消息处理耗时")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
        sample.stop(timer);
    }

    /**
     * 记录消息处理耗时
     *
     * @param durationNanos 耗时（纳秒）
     */
    public void recordMessageProcessDuration(long durationNanos) {
        messageProcessTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * 记录消息处理错误
     */
    public void recordMessageError() {
        messageErrorCounter.increment();
    }

    /**
     * 记录消息处理错误（带错误类型）
     */
    public void recordMessageError(String errorType) {
        messageErrorCounter.increment();
        Counter.builder(METRIC_PREFIX + ".messages.errors.by.type")
                .tag("error", errorType)
                .description("按类型统计的消息处理错误数")
                .register(registry)
                .increment();
    }

    // ============ 通用方法 ============

    /**
     * 注册自定义 Gauge
     *
     * @param name          指标名称
     * @param description   描述
     * @param valueSupplier 值提供者
     */
    public void registerGauge(String name, String description, Supplier<Number> valueSupplier) {
        Gauge.builder(METRIC_PREFIX + "." + name, valueSupplier)
                .description(description)
                .register(registry);
    }

    /**
     * 创建自定义 Counter
     *
     * @param name        指标名称
     * @param description 描述
     * @return Counter
     */
    public Counter createCounter(String name, String description) {
        return Counter.builder(METRIC_PREFIX + "." + name)
                .description(description)
                .register(registry);
    }

    /**
     * 创建带标签的 Counter
     *
     * @param name        指标名称
     * @param description 描述
     * @param tags        标签（key-value 对）
     * @return Counter
     */
    public Counter createCounter(String name, String description, String... tags) {
        return Counter.builder(METRIC_PREFIX + "." + name)
                .description(description)
                .tags(tags)
                .register(registry);
    }

    /**
     * 创建自定义 Timer
     *
     * @param name        指标名称
     * @param description 描述
     * @return Timer
     */
    public Timer createTimer(String name, String description) {
        return Timer.builder(METRIC_PREFIX + "." + name)
                .description(description)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
    }

    /**
     * 获取 MeterRegistry
     */
    public MeterRegistry getRegistry() {
        return registry;
    }
}

