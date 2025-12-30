package org.markeb.metrics;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Actor 系统监控指标
 * <p>
 * 提供 Actor 系统相关的监控指标，包括：
 * <ul>
 *     <li>Actor 数量统计</li>
 *     <li>消息处理统计</li>
 *     <li>邮箱状态监控</li>
 *     <li>调度任务统计</li>
 * </ul>
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Autowired
 * private ActorMetrics actorMetrics;
 *
 * // Actor 创建
 * actorMetrics.actorCreated("PlayerActor");
 *
 * // 记录消息处理
 * long start = System.nanoTime();
 * // ... 处理消息 ...
 * actorMetrics.recordMessageProcessed("PlayerActor", System.nanoTime() - start);
 * }</pre>
 */
@Slf4j
public class ActorMetrics {

    private static final String METRIC_PREFIX = "actor";

    private final MeterRegistry registry;

    // ============ Actor 数量指标 ============

    /**
     * 总 Actor 数量
     */
    private final AtomicLong totalActors = new AtomicLong(0);

    /**
     * 按类型统计的 Actor 数量
     */
    private final Map<String, AtomicLong> actorCountByType = new ConcurrentHashMap<>();

    /**
     * Actor 创建计数器
     */
    private final Counter actorCreatedCounter;

    /**
     * Actor 销毁计数器
     */
    private final Counter actorDestroyedCounter;

    // ============ 消息处理指标 ============

    /**
     * 消息处理计数器
     */
    private final Counter messageProcessedCounter;

    /**
     * 消息处理耗时
     */
    private final Timer messageProcessTimer;

    /**
     * 消息处理异常计数器
     */
    private final Counter messageExceptionCounter;

    // ============ 邮箱指标 ============

    /**
     * 邮箱满拒绝计数器
     */
    private final Counter mailboxFullCounter;

    // ============ 调度指标 ============

    /**
     * 调度任务计数器
     */
    private final Counter scheduleCounter;

    public ActorMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 注册总 Actor 数量 Gauge
        Gauge.builder(METRIC_PREFIX + ".count.total", totalActors, AtomicLong::get)
                .description("当前 Actor 总数")
                .register(registry);

        // Actor 创建/销毁计数器
        this.actorCreatedCounter = Counter.builder(METRIC_PREFIX + ".created.total")
                .description("Actor 创建总数")
                .register(registry);

        this.actorDestroyedCounter = Counter.builder(METRIC_PREFIX + ".destroyed.total")
                .description("Actor 销毁总数")
                .register(registry);

        // 消息处理计数器
        this.messageProcessedCounter = Counter.builder(METRIC_PREFIX + ".messages.processed.total")
                .description("Actor 处理消息总数")
                .register(registry);

        // 消息处理耗时
        this.messageProcessTimer = Timer.builder(METRIC_PREFIX + ".messages.process.duration")
                .description("Actor 消息处理耗时")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        // 消息处理异常计数器
        this.messageExceptionCounter = Counter.builder(METRIC_PREFIX + ".messages.exceptions.total")
                .description("Actor 消息处理异常总数")
                .register(registry);

        // 邮箱满计数器
        this.mailboxFullCounter = Counter.builder(METRIC_PREFIX + ".mailbox.full.total")
                .description("邮箱满拒绝消息总数")
                .register(registry);

        // 调度任务计数器
        this.scheduleCounter = Counter.builder(METRIC_PREFIX + ".schedules.total")
                .description("调度任务总数")
                .register(registry);

        log.info("ActorMetrics initialized");
    }

    // ============ Actor 生命周期方法 ============

    /**
     * Actor 创建
     *
     * @param actorType Actor 类型
     */
    public void actorCreated(String actorType) {
        totalActors.incrementAndGet();
        actorCreatedCounter.increment();

        // 按类型统计
        actorCountByType.computeIfAbsent(actorType, type -> {
            AtomicLong count = new AtomicLong(0);
            Gauge.builder(METRIC_PREFIX + ".count.by.type", count, AtomicLong::get)
                    .tag("type", type)
                    .description("按类型统计的 Actor 数量")
                    .register(registry);
            return count;
        }).incrementAndGet();

        // 按类型统计创建数
        Counter.builder(METRIC_PREFIX + ".created.by.type")
                .tag("type", actorType)
                .description("按类型统计的 Actor 创建数")
                .register(registry)
                .increment();
    }

    /**
     * Actor 销毁
     *
     * @param actorType Actor 类型
     */
    public void actorDestroyed(String actorType) {
        totalActors.decrementAndGet();
        actorDestroyedCounter.increment();

        // 按类型统计
        AtomicLong count = actorCountByType.get(actorType);
        if (count != null) {
            count.decrementAndGet();
        }

        // 按类型统计销毁数
        Counter.builder(METRIC_PREFIX + ".destroyed.by.type")
                .tag("type", actorType)
                .description("按类型统计的 Actor 销毁数")
                .register(registry)
                .increment();
    }

    /**
     * 获取当前 Actor 总数
     */
    public long getTotalActorCount() {
        return totalActors.get();
    }

    /**
     * 设置 Actor 总数（用于同步）
     */
    public void setTotalActorCount(long count) {
        totalActors.set(count);
    }

    // ============ 消息处理方法 ============

    /**
     * 记录消息处理完成
     */
    public void recordMessageProcessed() {
        messageProcessedCounter.increment();
    }

    /**
     * 记录消息处理完成（带 Actor 类型）
     *
     * @param actorType Actor 类型
     */
    public void recordMessageProcessed(String actorType) {
        messageProcessedCounter.increment();
        Counter.builder(METRIC_PREFIX + ".messages.processed.by.type")
                .tag("type", actorType)
                .description("按 Actor 类型统计的消息处理数")
                .register(registry)
                .increment();
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
     * 记录消息处理耗时（带 Actor 类型）
     *
     * @param actorType     Actor 类型
     * @param durationNanos 耗时（纳秒）
     */
    public void recordMessageProcessed(String actorType, long durationNanos) {
        messageProcessedCounter.increment();
        messageProcessTimer.record(durationNanos, TimeUnit.NANOSECONDS);

        // 按类型统计
        Timer.builder(METRIC_PREFIX + ".messages.process.duration.by.type")
                .tag("type", actorType)
                .description("按 Actor 类型统计的消息处理耗时")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
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
        messageProcessedCounter.increment();
    }

    /**
     * 记录消息处理完成（带 Actor 类型）
     *
     * @param actorType Actor 类型
     * @param sample    计时器采样
     */
    public void recordMessageProcessed(String actorType, Timer.Sample sample) {
        sample.stop(messageProcessTimer);
        messageProcessedCounter.increment();

        Timer timer = Timer.builder(METRIC_PREFIX + ".messages.process.duration.by.type")
                .tag("type", actorType)
                .description("按 Actor 类型统计的消息处理耗时")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
        // Note: sample already stopped, so we just increment the counter
        Counter.builder(METRIC_PREFIX + ".messages.processed.by.type")
                .tag("type", actorType)
                .register(registry)
                .increment();
    }

    /**
     * 记录消息处理异常
     */
    public void recordMessageException() {
        messageExceptionCounter.increment();
    }

    /**
     * 记录消息处理异常（带异常类型）
     *
     * @param exceptionType 异常类型
     */
    public void recordMessageException(String exceptionType) {
        messageExceptionCounter.increment();
        Counter.builder(METRIC_PREFIX + ".messages.exceptions.by.type")
                .tag("exception", exceptionType)
                .description("按异常类型统计的消息处理异常数")
                .register(registry)
                .increment();
    }

    // ============ 邮箱方法 ============

    /**
     * 记录邮箱满拒绝
     */
    public void recordMailboxFull() {
        mailboxFullCounter.increment();
    }

    /**
     * 记录邮箱满拒绝（带 Actor 类型）
     *
     * @param actorType Actor 类型
     */
    public void recordMailboxFull(String actorType) {
        mailboxFullCounter.increment();
        Counter.builder(METRIC_PREFIX + ".mailbox.full.by.type")
                .tag("type", actorType)
                .description("按 Actor 类型统计的邮箱满拒绝数")
                .register(registry)
                .increment();
    }

    /**
     * 注册邮箱大小 Gauge
     *
     * @param actorId       Actor ID
     * @param sizeSupplier  邮箱大小提供者
     */
    public void registerMailboxSize(String actorId, java.util.function.Supplier<Number> sizeSupplier) {
        Gauge.builder(METRIC_PREFIX + ".mailbox.size", sizeSupplier)
                .tag("actor", actorId)
                .description("Actor 邮箱大小")
                .register(registry);
    }

    // ============ 调度方法 ============

    /**
     * 记录调度任务创建
     */
    public void recordSchedule() {
        scheduleCounter.increment();
    }

    /**
     * 记录调度任务创建（带类型）
     *
     * @param scheduleType 调度类型（once/periodic）
     */
    public void recordSchedule(String scheduleType) {
        scheduleCounter.increment();
        Counter.builder(METRIC_PREFIX + ".schedules.by.type")
                .tag("type", scheduleType)
                .description("按类型统计的调度任务数")
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

