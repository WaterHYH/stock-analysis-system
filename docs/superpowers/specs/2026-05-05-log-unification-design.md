# 日志输出统一 — 设计文档

**日期**：2026-05-05  
**状态**：已确认  
**版本**：v54

---

## 1. 背景与目标

当前日志系统通过 logback-spring.xml 配置了 4 个独立的 RollingFileAppender：

| Appender | 文件 | 内容 |
|----------|------|------|
| FILE_APP | `stock-app.log` | 主应用日志 |
| FILE_SYNC | `stock-sync.log` | 同步调度日志 |
| FILE_KLINE | `stock-kline.log` | K线分析日志 |
| FILE_ERROR | `stock-error.log` | 错误专用日志 |

问题：排查一条请求链路需要在 4 个文件中来回切换，无法关联上下文。Spring 启动/停止日志散落在不同文件中。

目标：**所有日志输出到单一文件**，每次运行自动归档，按大小滚动拆分。

## 2. 设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 文件数量 | 1 个 (`stock.log`) | 用户要求单文件 |
| 每次启动 | 自动归档旧文件为 `stock-{时间}.log` | 不跟上次日志混在一起 |
| 滚动策略 | 50MB 大小滚动 | 用户指定 |
| 归档格式 | `stock.%d{yyyy-MM-dd}.%i.log.gz` | 每天 + 序号 + gz 压缩 |
| 保留天数 | 30 天 | 保持现有配置 |
| 总容量上限 | 2GB | 保持现有配置 |
| 控制台 | 保留 | 开发调试 |

## 3. 日志文件结构

```
logs/
├── stock.log                          ← 当前运行实时日志
├── stock-2026-05-05-143052.log        ← 上次启动归档（启动时间戳）
├── stock-2026-05-04-091500.log        ← 更早的启动归档
├── stock.2026-05-05.0.log.gz          ← 当前运行 >50MB 的溢出归档
├── stock.2026-05-05.1.log.gz
└── ... (滚动归档，保留 30 天)
```

## 4. 实现方案

### 4.1 logback-spring.xml（重构）

合并 4 个 Appender → 1 个 UnifiedAppender：

```xml
<!-- 统一日志文件 - 包含全部日志 -->
<appender name="UNIFIED_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${APP_LOG_PATH}/stock.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${APP_LOG_PATH}/stock.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>50MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>2GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        <charset>UTF-8</charset>
    </encoder>
</appender>

<!-- 异步包装 -->
<appender name="ASYNC_UNIFIED" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <appender-ref ref="UNIFIED_FILE"/>
</appender>
```

所有 Logger 统一挂载 `CONSOLE + ASYNC_UNIFIED`。

### 4.2 LogStartupListener.java（新增）

启动时检测 `stock.log` 是否存在，若存在则归档：

```java
@Component
public class LogStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        File currentLog = new File("logs/stock.log");
        if (currentLog.exists() && currentLog.length() > 0) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
            File archive = new File("logs/stock-" + timestamp + ".log");
            currentLog.renameTo(archive);
        }
    }
}
```

## 5. 涉及文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/main/resources/logback-spring.xml` | **重写** | 合并为单 Appender |
| `src/main/java/com/example/stock/config/LogStartupListener.java` | **新增** | 启动归档旧日志 |
| `src/main/resources/application.properties` | **微调** | 清理旧注释 |
| 其他文件 | 不变 | 无需改动 |

## 6. 降级与兼容

- **文件归档失败不影响运行**：renameTo 失败仅记录警告，继续使用 stock.log
- **旧日志文件手动清理**：优化前的 `stock-app.log`、`stock-sync.log`、`stock-kline.log`、`stock-error.log` 及其归档文件需手动删除（首次部署后不再产生）
- **控制台不受影响**：开发时仍然实时看到控制台输出

## 7. 测试检查点

1. 启动应用，`logs/stock.log` 创建
2. 第二次启动，`logs/stock.log` 归档为 `stock-{时间}.log`，新的 `stock.log` 开始写入
3. 日志包含：Spring 启动信息、业务日志、SQL 日志、ERROR 日志
4. 文件超过 50MB 自动滚动到 `stock.{日期}.{序号}.log.gz`
5. 控制台正常输出
