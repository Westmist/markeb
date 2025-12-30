# Markeb Robot

机器人客户端模块，用于测试网关服务器的连接、登录和协议发送。

## 功能特性

- **TCP 连接管理**：连接网关服务器并维持长连接
- **协议编解码**：支持网关协议格式（length + msgId + seq + body）
- **Protobuf 消息**：自动注册和解析 Protobuf 消息
- **自动重连**：断线后自动重连，可配置重连策略
- **请求-响应匹配**：通过 seq 序列号匹配请求和响应
- **多机器人支持**：支持同时运行多个机器人客户端
- **注解驱动**：通过注解便捷地添加新的测试动作
- **控制台交互**：运行时可通过控制台执行动作

## 快速开始

### 1. 配置

编辑 `application.yaml`：

```yaml
markeb:
  robot:
    gateway-host: 127.0.0.1    # 网关服务器地址
    gateway-port: 7000          # 网关服务器端口
    robot-count: 1              # 机器人数量
    auto-login: true            # 是否自动执行动作
```

### 2. 运行

```bash
# 在项目根目录执行
mvn spring-boot:run -pl markeb-robot
```

## 添加新的测试动作

### 方式一：创建动作类（推荐）

1. 创建一个新的动作类，继承 `AbstractRobotActions`
2. 使用 `@RobotMessages` 声明需要的消息类型
3. 使用 `@RobotAction` 定义动作
4. 使用 `@RobotResponseHandler` 定义响应处理器

```java
@Component
@RobotMessages({
    ReqYourMessage.class,
    ResYourMessage.class
})
public class YourActions extends AbstractRobotActions {

    @RobotAction(
        name = "your_action",
        description = "你的动作描述",
        order = 10,
        autoExecute = true  // 连接后自动执行
    )
    public void yourAction(RobotClient robot) {
        ReqYourMessage request = ReqYourMessage.newBuilder()
            .setField("value")
            .build();
        send(robot, request);
        log.info("[{}] Request sent", getRobotId(robot));
    }

    @RobotResponseHandler(ResYourMessage.class)
    public void onYourResponse(RobotClient robot, ResYourMessage response) {
        log.info("[{}] Response received: {}", getRobotId(robot), response);
    }
}
```

### 方式二：复制模板

复制 `ExampleActions.java` 文件，按照注释修改即可。

## 注解说明

### @RobotMessages

声明需要注册的 Protobuf 消息类型：

```java
@RobotMessages({
    ReqLoginMessage.class,
    ResLoginMessage.class
})
```

### @RobotAction

定义机器人动作：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 动作名称（唯一标识） |
| `description` | String | "" | 动作描述 |
| `order` | int | 100 | 执行顺序，数值越小越先执行 |
| `autoExecute` | boolean | false | 是否在连接后自动执行 |
| `delayMs` | long | 0 | 延迟执行时间（毫秒） |

### @RobotResponseHandler

定义响应处理器：

```java
// 带 robot 参数
@RobotResponseHandler(ResLoginMessage.class)
public void onLogin(RobotClient robot, ResLoginMessage response) { }

// 不带 robot 参数
@RobotResponseHandler(ResLoginMessage.class)
public void onLogin(ResLoginMessage response) { }
```

## 控制台命令

启动后可以在控制台输入命令：

| 命令 | 说明 |
|------|------|
| `help` | 显示帮助信息 |
| `list` | 列出所有可用动作 |
| `exec <action>` | 对所有机器人执行动作 |
| `exec <robotId> <action>` | 对指定机器人执行动作 |
| `status` | 显示机器人状态 |
| `quit` | 退出程序 |

示例：

```
robot> list
=== Available Actions ===
  login                     - 发送登录请求 [AUTO]
=========================

robot> exec login
Executing action 'login' for all robots...

robot> status
=== Robot Status ===
Gateway: 127.0.0.1:7000
Total Robots: 1
Connected: 1

  robot_0              ✓ Connected
====================
```

## 编程方式执行动作

```java
@Autowired
private RobotActionExecutor actionExecutor;

// 对所有机器人执行动作
actionExecutor.executeForAll("login");

// 对指定机器人执行动作
actionExecutor.execute("robot_0", "login");

// 延迟执行
actionExecutor.executeDelayed("some_action", 5000);

// 周期性执行
actionExecutor.executeAtFixedRate("heartbeat", 0, 30000);
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `gateway-host` | 127.0.0.1 | 网关服务器地址 |
| `gateway-port` | 7000 | 网关服务器端口 |
| `robot-count` | 1 | 机器人数量 |
| `robot-id-prefix` | robot_ | 机器人 ID 前缀 |
| `auto-reconnect` | true | 是否自动重连 |
| `reconnect-delay-seconds` | 5 | 重连延迟（秒） |
| `max-reconnect-attempts` | 10 | 最大重连次数 |
| `request-timeout-ms` | 30000 | 请求超时时间（毫秒） |
| `auto-login` | true | 是否自动执行动作 |
| `login-interval-ms` | 100 | 多机器人连接间隔（毫秒） |
| `console.enabled` | true | 是否启用控制台 |

## 协议格式

客户端与网关之间的协议格式：

```
+--------+--------+--------+--------+
| length | msgId  |  seq   |  body  |
| 4bytes | 4bytes | 4bytes | nbytes |
+--------+--------+--------+--------+
```

- **length**: 数据长度（不包含 length 字段本身）= 8 + body.length
- **msgId**: 消息 ID，对应 Protobuf 消息的 `option (msgId) = xxx`
- **seq**: 序列号，用于请求-响应匹配
- **body**: Protobuf 序列化后的消息体

## 项目结构

```
markeb-robot/
├── src/main/java/org/markeb/robot/
│   ├── RobotApplication.java       # 启动类
│   ├── action/                     # 动作框架
│   │   ├── RobotAction.java        # 动作注解
│   │   ├── RobotMessages.java      # 消息注册注解
│   │   ├── RobotResponseHandler.java # 响应处理器注解
│   │   ├── AbstractRobotActions.java # 动作基类
│   │   ├── RobotActionRegistry.java  # 动作注册中心
│   │   └── RobotActionExecutor.java  # 动作执行器
│   ├── actions/                    # 动作实现
│   │   ├── LoginActions.java       # 登录动作
│   │   └── ExampleActions.java     # 示例模板
│   ├── client/
│   │   └── RobotClient.java        # 客户端核心类
│   ├── codec/                      # 编解码器
│   ├── config/                     # 配置类
│   ├── console/
│   │   └── RobotConsole.java       # 控制台
│   ├── handler/                    # Netty 处理器
│   ├── manager/
│   │   └── RobotManager.java       # 机器人管理器
│   ├── message/
│   │   └── RobotMessageParser.java # 消息解析器
│   └── protocol/
│       └── RobotPacket.java        # 协议包
└── src/main/resources/
    └── application.yaml            # 配置文件
```

## 依赖模块

- `proto-message`: Protobuf 消息定义
- `proto-notice`: Protobuf 通知消息定义
- Netty: 网络通信框架
- Spring Boot: 应用框架
