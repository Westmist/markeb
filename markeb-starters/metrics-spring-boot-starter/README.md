# Metrics Spring Boot Starter

游戏服务监控指标模块，基于 Micrometer 提供全面的监控能力。

## 功能特性

- **游戏指标 (GameMetrics)**：在线玩家数、消息处理统计、请求延迟
- **Actor 指标 (ActorMetrics)**：Actor 数量、消息处理耗时、邮箱状态
- **网络指标 (NetworkMetrics)**：连接数、流量统计、编解码耗时
- **JVM 扩展指标**：虚拟线程数量、系统负载等
- **注解支持**：`@Metered`、`@Timed`、`@Counted`
- **Prometheus 集成**：开箱即用的 Prometheus 导出

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.markeb</groupId>
    <artifactId>metrics-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- 如果需要 Prometheus 导出 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2. 配置 application.yaml

```yaml
# Actuator 端点配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

# Markeb 监控配置
markeb:
  metrics:
    enabled: true
    game:
      enabled: true
    actor:
      enabled: true
      bind-actor-system: true
    network:
      enabled: true
    jvm:
      extended-enabled: true
    common-tags:
      application: ${spring.application.name}
      environment: dev
      server-id: node-1
```

### 3. 使用指标

#### 注入并使用 GameMetrics

```java
@Service
public class PlayerService {

    private final GameMetrics gameMetrics;

    public PlayerService(GameMetrics gameMetrics) {
        this.gameMetrics = gameMetrics;
    }

    public void login(long playerId) {
        Timer.Sample sample = gameMetrics.startTimer();
        try {
            // 登录逻辑...
            gameMetrics.playerOnline();
        } finally {
            gameMetrics.recordMessageProcessed("LoginRequest", sample);
        }
    }

    public void logout(long playerId) {
        gameMetrics.playerOffline();
    }
}
```

#### 注入并使用 ActorMetrics

```java
@Service
public class ActorService {

    private final ActorMetrics actorMetrics;

    public ActorService(ActorMetrics actorMetrics) {
        this.actorMetrics = actorMetrics;
    }

    public void createActor(String actorType) {
        // 创建 Actor...
        actorMetrics.actorCreated(actorType);
    }

    public void processMessage(String actorType, Runnable handler) {
        long start = System.nanoTime();
        try {
            handler.run();
        } finally {
            actorMetrics.recordMessageProcessed(actorType, System.nanoTime() - start);
        }
    }
}
```

#### 注入并使用 NetworkMetrics

```java
@Component
public class ConnectionHandler {

    private final NetworkMetrics networkMetrics;

    public ConnectionHandler(NetworkMetrics networkMetrics) {
        this.networkMetrics = networkMetrics;
    }

    public void onConnect(Channel channel) {
        networkMetrics.connectionOpened();
    }

    public void onDisconnect(Channel channel, String reason) {
        networkMetrics.connectionClosed(reason);
    }

    public void onMessage(String messageType, int bytes) {
        networkMetrics.recordMessageReceived(messageType, bytes);
    }
}
```

#### 使用 @Metered 注解

```java
@Service
public class GameService {

    @Metered(name = "game.battle.start", description = "开始战斗")
    public void startBattle(long playerId) {
        // 战斗逻辑...
    }

    @Metered(name = "game.item.use", description = "使用道具", tags = {"category=consumable"})
    public void useItem(long playerId, int itemId) {
        // 使用道具逻辑...
    }
}
```

#### 使用 Micrometer 原生注解

```java
@Service
public class OrderService {

    @Timed(value = "order.process.time", description = "订单处理耗时")
    @Counted(value = "order.process.count", description = "订单处理次数")
    public void processOrder(Order order) {
        // 处理订单...
    }
}
```

## 指标列表

### 游戏指标 (game.*)

| 指标名 | 类型 | 描述 |
|--------|------|------|
| `game.players.online` | Gauge | 当前在线玩家数 |
| `game.players.login.total` | Counter | 玩家登录总次数 |
| `game.players.logout.total` | Counter | 玩家登出总次数 |
| `game.messages.received.total` | Counter | 接收消息总数 |
| `game.messages.sent.total` | Counter | 发送消息总数 |
| `game.messages.process.duration` | Timer | 消息处理耗时 |
| `game.messages.errors.total` | Counter | 消息处理错误总数 |

### Actor 指标 (actor.*)

| 指标名 | 类型 | 描述 |
|--------|------|------|
| `actor.count.total` | Gauge | 当前 Actor 总数 |
| `actor.count.by.type` | Gauge | 按类型统计的 Actor 数量 |
| `actor.created.total` | Counter | Actor 创建总数 |
| `actor.destroyed.total` | Counter | Actor 销毁总数 |
| `actor.messages.processed.total` | Counter | Actor 处理消息总数 |
| `actor.messages.process.duration` | Timer | Actor 消息处理耗时 |
| `actor.messages.exceptions.total` | Counter | Actor 消息处理异常总数 |
| `actor.mailbox.full.total` | Counter | 邮箱满拒绝消息总数 |

### 网络指标 (network.*)

| 指标名 | 类型 | 描述 |
|--------|------|------|
| `network.connections.active` | Gauge | 当前活跃连接数 |
| `network.connections.opened.total` | Counter | 连接建立总数 |
| `network.connections.closed.total` | Counter | 连接关闭总数 |
| `network.bytes.received.total` | Counter | 接收字节总数 |
| `network.bytes.sent.total` | Counter | 发送字节总数 |
| `network.messages.received.total` | Counter | 接收消息总数 |
| `network.messages.sent.total` | Counter | 发送消息总数 |
| `network.message.latency` | Timer | 消息处理延迟 |
| `network.codec.duration` | Timer | 编解码耗时 |

## 访问监控端点

启动服务后，可以通过以下端点访问监控数据：

- **健康检查**: `GET /actuator/health`
- **所有指标**: `GET /actuator/metrics`
- **特定指标**: `GET /actuator/metrics/game.players.online`
- **Prometheus 格式**: `GET /actuator/prometheus`

## 与 Prometheus + Grafana 集成

### Prometheus 配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'markeb-node'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
```

### Grafana Dashboard

推荐使用以下 Grafana Dashboard：
- JVM (Micrometer): ID 4701
- Spring Boot Statistics: ID 12900

## 自定义指标

### 注册自定义 Gauge

```java
@Component
public class CustomMetrics {

    public CustomMetrics(GameMetrics gameMetrics) {
        // 注册自定义 Gauge
        gameMetrics.registerGauge(
            "queue.size",
            "消息队列大小",
            () -> messageQueue.size()
        );
    }
}
```

### 创建自定义 Counter

```java
@Component
public class CustomMetrics {

    private final Counter customCounter;

    public CustomMetrics(GameMetrics gameMetrics) {
        this.customCounter = gameMetrics.createCounter(
            "custom.events",
            "自定义事件计数"
        );
    }

    public void recordEvent() {
        customCounter.increment();
    }
}
```

### 创建自定义 Timer

```java
@Component
public class CustomMetrics {

    private final Timer customTimer;

    public CustomMetrics(GameMetrics gameMetrics) {
        this.customTimer = gameMetrics.createTimer(
            "custom.operation",
            "自定义操作耗时"
        );
    }

    public void recordOperation(Runnable operation) {
        customTimer.record(operation);
    }
}
```

## 配置项

| 配置项 | 默认值 | 描述 |
|--------|--------|------|
| `markeb.metrics.enabled` | `true` | 是否启用监控 |
| `markeb.metrics.game.enabled` | `true` | 是否启用游戏指标 |
| `markeb.metrics.actor.enabled` | `true` | 是否启用 Actor 指标 |
| `markeb.metrics.actor.bind-actor-system` | `true` | 是否绑定 ActorSystem 指标 |
| `markeb.metrics.network.enabled` | `true` | 是否启用网络指标 |
| `markeb.metrics.jvm.extended-enabled` | `true` | 是否启用 JVM 扩展指标 |
| `markeb.metrics.common-tags.application` | - | 应用名称标签 |
| `markeb.metrics.common-tags.environment` | - | 环境标签 |
| `markeb.metrics.common-tags.server-id` | - | 服务器 ID 标签 |

## 最佳实践

1. **合理使用标签**：标签值应该是有限集合，避免使用玩家 ID 等高基数值
2. **选择合适的指标类型**：
   - Counter：只增不减的累计值（如请求数）
   - Gauge：可增可减的瞬时值（如在线人数）
   - Timer：耗时统计（自动包含 count、sum、max）
3. **设置合理的百分位**：默认提供 p50、p90、p95、p99
4. **监控告警**：基于关键指标设置告警规则

