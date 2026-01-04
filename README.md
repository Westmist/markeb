# markeb - 高性能游戏服务端框架

基于 **Spring Boot 3 + Netty + Actor 模型** 构建的模块化游戏服务端框架。

## 特性

- **Actor 并发模型**：支持虚拟线程/平台线程，消息串行化处理
- **高性能网络层**：Netty 封装，支持 TCP/KCP 传输
- **安全与限流**：
    - **双层限流**：网关层 Bucket4j 连接级限流 + 业务层 Resilience4j 精细化限流
    - **防刷保护**：基于 IP 和 Session 的请求频率控制
- **异步持久化**：Redis 缓存 + MongoDB 存储 + RocketMQ 异步落盘
- **服务治理**：服务注册发现、分布式锁、配置中心
- **热更新支持**：Java Agent 热更新 + Groovy 脚本执行
- **Spring Boot Starter**：开箱即用，约定大于配置

## 项目结构

```
markeb/
├── markeb-api/                    # 协议定义（Protobuf）
├── markeb-common/                 # 公共工具类
├── markeb-gateway/                # 游戏网关
├── markeb-node/                   # 游戏服务节点
├── markeb-mesh/                   # 匹配服务
└── markeb-starters/               # Spring Boot Starters
    ├── actor-spring-boot-starter       # Actor 模型
    ├── network-spring-boot-starter     # 网络层（Netty）
    ├── registry-spring-boot-starter    # 服务注册（Nacos/Etcd/Consul）
    ├── persistent-spring-boot-starter  # 异步持久化
    ├── eventbus-spring-boot-starter    # 事件总线
    ├── locate-spring-boot-starter      # 玩家定位
    ├── lock-spring-boot-starter        # 分布式锁
    ├── id-spring-boot-starter          # 分布式 ID
    ├── config-spring-boot-starter      # 配置中心
    ├── transport-spring-boot-starter   # RPC 通信
    └── hotswap-spring-boot-starter     # 热更新
```

## 模块说明

| 模块 | 说明 |
|------|------|
| **markeb-api** | Protobuf 协议定义，客户端服务端共用 |
| **markeb-common** | 公共工具类、常量、扫描器 |
| **markeb-gateway** | 网关服务，管理客户端连接，路由消息到后端节点 |
| **markeb-node** | 游戏逻辑节点，处理业务逻辑 |
| **markeb-mesh** | 匹配服务，玩家匹配队列 |

### Starter 模块

| Starter | 说明 |
|---------|------|
| **actor** | Actor 模型，虚拟线程/平台线程双模式 |
| **network** | Netty 网络层，TCP/KCP，Protobuf/JSON 序列化 |
| **registry** | 服务注册发现，Nacos/Etcd/Consul |
| **persistent** | 异步持久化，Redis + MongoDB + MQ |
| **eventbus** | 分布式事件总线，Redis/RocketMQ/Kafka |
| **locate** | 玩家定位服务 |
| **lock** | 分布式锁 |
| **id** | 雪花算法 ID 生成 |
| **config** | 配置中心 |
| **transport** | 服务间 RPC |
| **hotswap** | 热更新，Java Agent + Groovy 脚本 |

## 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.8+ |
| Spring Boot | 3.x |
| Redis | 7.x |
| MongoDB | 6.x |
| RocketMQ | 5.x (可选) |
| Nacos | 2.x (可选) |

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/your-repo/markeb.git
cd markeb
```

### 2. 构建

```bash
mvn clean install -DskipTests
```

### 3. 启动服务

```bash
# 启动游戏节点
cd markeb-node
mvn spring-boot:run
```

### 4. 最小配置

```yaml
spring:
  application:
    name: my-game-server
  data:
    redis:
      url: redis://127.0.0.1:6379
    mongodb:
      uri: mongodb://127.0.0.1:27017
      database: markeb

network:
  port: 8000

# 速率限制（Resilience4j）
resilience4j:
  ratelimiter:
    instances:
      gameAction:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
      chat:
        limitForPeriod: 1
        limitRefreshPeriod: 3s

# 其他配置使用默认值，无需显式配置
# - Actor: 默认启用虚拟线程
# - Registry: 默认从 spring.application.name 获取服务名
# - Locate: 默认 Redis，30分钟过期
```

## 架构图

```
      ┌──────────┐      ┌──────────┐      ┌──────────┐      ┌──────────┐
      │  Client  │      │  Client  │      │  Client  │      │  Client  │
      └────┬─────┘      └────┬─────┘      └────┬─────┘      └────┬─────┘
           │                 │                 │                 │
           └─────────────────┴────────┬────────┴─────────────────┘
                                      │
                                      │ TCP/WebSocket
                                      ▼
                              ┌───────────────┐
                              │    Gateway    │
                              └───────┬───────┘
                                      │
                                      │ TCP
                  ┌───────────────────┼───────────────────┐
                  │                   │                   │
                  ▼                   ▼                   ▼
           ┌────────────┐      ┌────────────┐      ┌────────────┐
           │    Node    │      │    Node    │      │    Node    │
           └──────┬─────┘      └──────┬─────┘      └──────┬─────┘
                  │                   │                   │
                  │                   │                   │
      ┌───────────┴───────────────────┼───────────────────┴───────────┐
      │                               │                               │
      │                               │ gRPC                          │
      │                       ┌───────┴───────┐                       │
      │                       │               │                       │
      │                       ▼               ▼                       │
      │                ┌────────────┐  ┌────────────┐                 │
      │                │    Mesh    │  │    Mesh    │                 │
      │                └──────┬─────┘  └─────┬──────┘                 │
      │                       │              │                        │
      └───────────────┬───────┴──────────────┴────┬───────────────────┘
                      │                           │
                      ▼                           ▼
               ┌────────────┐  ┌────────────┐  ┌────────────┐
               │   Redis    │  │  MongoDB   │  │  RocketMQ  │
               └────────────┘  └────────────┘  └────────────┘
```

## 设计原则

- **约定大于配置**：所有模块零配置可用
- **模块化**：按需引入，互不依赖
- **高性能**：虚拟线程 + 异步 IO + 批量操作
- **易扩展**：清晰的接口抽象

## License

MIT
