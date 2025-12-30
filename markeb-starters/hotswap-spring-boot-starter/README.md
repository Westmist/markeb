# HotSwap Spring Boot Starter

基于 Java Agent 的热更新模块，支持从 OSS 加载 class 文件并动态替换。

## 功能特性

- 🔥 **Java Agent 热更新** - 基于 `Instrumentation.redefineClasses()` 实现真正的热更新
- ☁️ **多 OSS 支持** - 阿里云 OSS、腾讯云 COS、Google Cloud Storage
- 🌐 **HTTP 接口** - 提供 REST API 触发热更新
- 📦 **版本管理** - 支持版本号追踪和持久化
- 🔒 **安全认证** - 可选的 Token 认证
- 📜 **Groovy 脚本** - 支持动态执行 Groovy 脚本，可访问 Spring Bean

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.markeb</groupId>
    <artifactId>hotswap-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 配置

### 阿里云 OSS

```yaml
markeb:
  hotswap:
    enabled: true
    version: v1.0.0
    oss:
      type: aliyun
      endpoint: oss-cn-hangzhou.aliyuncs.com
      access-key-id: your-access-key
      access-key-secret: your-secret
      bucket-name: your-bucket
      path-prefix: hotswap/
    http:
      auth-enabled: true
      auth-token: your-secret-token
    script:
      enabled: true
```

### 腾讯云 COS

```yaml
markeb:
  hotswap:
    oss:
      type: tencent
      region: ap-guangzhou
      access-key-id: your-secret-id
      access-key-secret: your-secret-key
      bucket-name: your-bucket-1234567890
      path-prefix: hotswap/
```

### Google Cloud Storage

```yaml
markeb:
  hotswap:
    oss:
      type: google
      bucket-name: your-bucket
      path-prefix: hotswap/
```

> Google GCS 使用默认凭证（Application Default Credentials），需要设置 `GOOGLE_APPLICATION_CREDENTIALS` 环境变量指向服务账号密钥文件。

## HTTP 接口

### 获取当前版本

```bash
GET /hotswap/version
```

响应：
```json
{
  "version": "v1.0.0",
  "timestamp": 1703923200000
}
```

### 上传并热更新单个类

```bash
POST /hotswap/upload
Content-Type: multipart/form-data

className=org.markeb.game.actor.PlayerActorBehavior
file=@PlayerActorBehavior.class
```

### 从 OSS 热更新单个类

```bash
POST /hotswap/reload?className=org.markeb.game.actor.PlayerActorBehavior
```

### 应用热更包

```bash
POST /hotswap/apply?package=hotswap-v1.0.0-to-v1.0.1.tar.gz&version=v1.0.1
```

响应：
```json
{
  "results": [
    {"className": "org.markeb.game.actor.PlayerActorBehavior", "success": true},
    {"className": "org.markeb.game.actor.PlayerState", "success": true}
  ],
  "total": 2,
  "success": 2,
  "failed": 0,
  "currentVersion": "v1.0.1"
}
```

### 列出可用热更包

```bash
GET /hotswap/packages
```

### 健康检查

```bash
GET /hotswap/health
```

## Groovy 脚本接口

### 执行脚本

```bash
POST /hotswap/script/execute
Content-Type: application/json

{
  "script": "def service = ctx.getBean('playerActorService')\nreturn service.getOnlineCount()",
  "variables": {
    "playerId": 12345
  }
}
```

响应：
```json
{
  "success": true,
  "result": 100,
  "resultType": "java.lang.Integer",
  "costMs": 15
}
```

### 注册脚本（预编译）

```bash
POST /hotswap/script/register
Content-Type: application/json

{
  "name": "getOnlineCount",
  "script": "ctx.getBean('playerActorService').getOnlineCount()"
}
```

### 执行已注册的脚本

```bash
POST /hotswap/script/run/getOnlineCount
Content-Type: application/json

{
  "playerId": 12345
}
```

### 移除脚本

```bash
DELETE /hotswap/script/remove/getOnlineCount
```

### 清空脚本缓存

```bash
DELETE /hotswap/script/clear
```

## Groovy 脚本示例

### 访问 Spring Bean

```groovy
// 通过 ctx 获取 Bean
def service = ctx.getBean('playerActorService')
return service.getOnlineCount()

// 或通过 getBean 快捷方法
def service = getBean('playerActorService')
return service.getOnlineCount()
```

### 查询玩家状态

```groovy
def actorSystem = ctx.getBean('actorSystem')
def playerRef = actorSystem.lookup(playerId)
if (playerRef.isPresent()) {
    return "Player ${playerId} is online"
} else {
    return "Player ${playerId} is offline"
}
```

### 广播消息

```groovy
def service = ctx.getBean('playerActorService')
service.broadcast(message)
return "Broadcast completed"
```

### 修改配置

```groovy
def metaManager = ctx.getBean('metaManager')
metaManager.reloadAll()
return "Config reloaded"
```

## 热更包格式

热更包为 `.tar.gz` 或 `.zip` 格式，结构如下：

```
hotswap-v1.0.0-to-v1.0.1.tar.gz
└── classes/
    └── org/
        └── markeb/
            └── game/
                └── actor/
                    ├── PlayerActorBehavior.class
                    └── PlayerState.class
```

## CI 脚本示例

```bash
#!/bin/bash

FROM_VERSION=$1   # 如 v1.0.0
TO_VERSION=$2     # 如 v1.0.1

# 编译
git checkout $TO_VERSION
mvn clean compile -DskipTests

# 找出变更文件
CHANGED_FILES=$(git diff --name-only $FROM_VERSION $TO_VERSION -- "*.java")

# 打包
mkdir -p hotswap-package/classes
for file in $CHANGED_FILES; do
    class_file=$(echo $file | sed 's|src/main/java/||' | sed 's|\.java|\.class|')
    full_path="target/classes/$class_file"
    if [ -f "$full_path" ]; then
        mkdir -p "hotswap-package/classes/$(dirname $class_file)"
        cp "$full_path" "hotswap-package/classes/$class_file"
    fi
done

# 压缩并上传
tar -czvf hotswap-${FROM_VERSION}-to-${TO_VERSION}.tar.gz -C hotswap-package .
aliyun oss cp hotswap-*.tar.gz oss://your-bucket/hotswap/
```

## 监听热更新事件

```java
@Component
public class HotSwapListener {

    @EventListener
    public void onHotSwap(HotSwapEvent event) {
        if (event.isAllSuccess()) {
            log.info("Hot swap completed, version: {}", event.getTargetVersion());
        }
        
        // 检查特定类是否被更新
        if (event.isClassReloaded("org.markeb.game.actor.PlayerActorBehavior")) {
            // 刷新缓存等操作
        }
    }
}
```

## 注意事项

### Agent 热更新限制

`redefineClasses()` 只能修改方法体，不能：
- 增加/删除字段
- 增加/删除方法
- 修改方法签名
- 修改继承关系

### 安全建议

1. 生产环境务必开启 `auth-enabled`
2. HTTP 接口只对内网开放
3. 记录所有热更新操作日志

## 目录结构

```
hotswap-spring-boot-starter/
├── src/main/java/org/markeb/hotswap/
│   ├── agent/
│   │   ├── HotSwapAgent.java       # Java Agent
│   │   └── AgentLoader.java        # 动态加载 Agent
│   ├── config/
│   │   ├── HotSwapAutoConfiguration.java
│   │   └── HotSwapProperties.java
│   ├── loader/
│   │   ├── OssClient.java          # OSS 客户端接口
│   │   ├── AliyunOssClient.java    # 阿里云 OSS 实现
│   │   ├── TencentCosClient.java   # 腾讯云 COS 实现
│   │   ├── GoogleGcsClient.java    # Google GCS 实现
│   │   ├── ClassBytesLoader.java   # 类加载器接口
│   │   └── OssClassBytesLoader.java
│   ├── script/
│   │   ├── ScriptExecutor.java     # Groovy 脚本执行器
│   │   ├── ScriptResult.java       # 执行结果
│   │   └── ScriptController.java   # HTTP 接口
│   ├── web/
│   │   ├── HotSwapController.java  # 热更新 HTTP 接口
│   │   └── HotSwapAuthInterceptor.java
│   ├── HotSwapService.java         # 核心服务
│   ├── HotSwapResult.java          # 结果对象
│   └── HotSwapEvent.java           # 事件
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 支持的 OSS

| 类型 | 配置值 | 依赖 |
|-----|-------|------|
| 阿里云 OSS | `aliyun` (默认) | `com.aliyun.oss:aliyun-sdk-oss` |
| 腾讯云 COS | `tencent` | `com.qcloud:cos_api` |
| Google Cloud Storage | `google` | `com.google.cloud:google-cloud-storage` |

用户也可以自己实现 `OssClient` 接口，注入到 Spring 容器中。

