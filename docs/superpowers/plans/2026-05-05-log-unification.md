# 日志统一输出 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 4 个分散日志文件合并为单文件 `stock.log`，每次启动归档旧文件，50MB 滚动拆分。

**Architecture:** 重写 logback-spring.xml（合并 Appender）+ 新增 LogStartupListener（启动归档）+ 清理 application.properties 注释。仅配置层改动，无业务代码影响。

**Tech Stack:** Logback 1.4.x (Spring Boot 3.4.3 内置), Spring ApplicationListener

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/resources/logback-spring.xml` | **重写** | 统一单 Appender 配置 |
| `src/main/java/com/example/stock/config/LogStartupListener.java` | **新建** | 启动时归档旧 stock.log |
| `src/main/resources/application.properties` | **微调** | 清理注释，新增归档目录配置 |

---

### Task 1: 重写 logback-spring.xml

**Files:**
- Modify: `src/main/resources/logback-spring.xml` (完整重写)

- [ ] **Step 1: 用以下内容替换 logback-spring.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>
    
    <springProfile name="default,dev">
        <property name="APP_LOG_PATH" value="./logs"/>
        <property name="LOG_LEVEL" value="INFO"/>
    </springProfile>
    <springProfile name="test">
        <property name="APP_LOG_PATH" value="./logs/test"/>
        <property name="LOG_LEVEL" value="DEBUG"/>
    </springProfile>
    <springProfile name="prod">
        <property name="APP_LOG_PATH" value="/var/log/stock"/>
        <property name="LOG_LEVEL" value="WARN"/>
    </springProfile>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <target>System.out</target>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${APP_LOG_PATH}/stock.log</file>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${APP_LOG_PATH}/stock.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>50MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <appender-ref ref="FILE"/>
    </appender>

    <logger name="com.example.stock" level="${LOG_LEVEL}"/>

    <logger name="org.springframework" level="WARN"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.hibernate" level="WARN"/>
    <logger name="com.zaxxer.hikari" level="INFO"/>

    <root level="${LOG_LEVEL}">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
    </root>
</configuration>
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 2: 新建 LogStartupListener

**Files:**
- Create: `src/main/java/com/example/stock/config/LogStartupListener.java`
- Modify: `src/main/resources/application.properties` (微调)

- [ ] **Step 1: 创建 LogStartupListener.java**

```java
package com.example.stock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class LogStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "stock.log";

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            Path currentLog = logDir.resolve(LOG_FILE);
            if (Files.exists(currentLog) && Files.size(currentLog) > 0) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
                Path archive = logDir.resolve("stock-" + timestamp + ".log");
                Files.move(currentLog, archive, StandardCopyOption.ATOMIC_MOVE);
                log.info("已将上次运行日志归档到: {}", archive.getFileName());
            }
        } catch (Exception e) {
            log.warn("日志归档失败: {}", e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 3: 清理旧配置文件 + 旧日志文件

**Files:**
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: 清理 application.properties 中的注释日志配置**

将第 30-37 行（注释掉的日志配置段）精简：

```properties
# 日志编码配置
logging.charset.file=UTF-8
logging.charset.console=UTF-8
file.encoding=UTF-8

# 设置com.example.stock包的日志级别为DEBUG
logging.level.com.example.stock=DEBUG
```

- [ ] **Step 2: 删除旧分散日志文件**

Run:
```
Remove-Item -Path "logs/stock-app.log","logs/stock-sync.log","logs/stock-kline.log","logs/stock-error.log" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "logs/stock-app.*.log.gz","logs/stock-sync.*.log.gz","logs/stock-kline.*.log.gz","logs/stock-error.*.log.gz" -Force -ErrorAction SilentlyContinue
Write-Host "Old log files cleaned"
```

---

### Task 4: 完整验证

- [ ] **Step 1: 停止旧进程 + 全新启动**

Run:
```
taskkill /F /IM java.exe 2>$null; Start-Sleep -Seconds 3
mvn spring-boot:run
```
Expected: 服务启动成功

- [ ] **Step 2: 验证 stock.log 存在**

Run: `Test-Path "logs/stock.log"`  
Expected: `True`

- [ ] **Step 3: 验证日志包含 Spring 启动信息**

Run: `Select-String -Path "logs/stock.log" -Pattern "Started StockApplication" -SimpleMatch`
Expected: 匹配到

- [ ] **Step 4: 验证日志包含业务/错误信息**

Run: `Select-String -Path "logs/stock.log" -Pattern "INFO|ERROR|DEBUG|WARN" -SimpleMatch | Select-Object -First 5`
Expected: 各级别日志都存在

- [ ] **Step 5: 验证旧分散日志文件不再产生**

Run: `Get-ChildItem "logs/stock-app*.log","logs/stock-sync*.log","logs/stock-kline*.log","logs/stock-error*.log" -ErrorAction SilentlyContinue`
Expected: 无输出

- [ ] **Step 6: 验证第二次启动时归档**

Run:
```
taskkill /F /IM java.exe 2>$null; Start-Sleep -Seconds 3
mvn spring-boot:run
Start-Sleep -Seconds 15
Get-ChildItem "logs/stock-*.log" | Select-Object Name
```
Expected: 存在 `stock-2026-05-05-HHmmss.log` 归档文件

- [ ] **Step 7: 提交**

```bash
git add src/main/resources/logback-spring.xml src/main/java/com/example/stock/config/LogStartupListener.java src/main/resources/application.properties
git commit -m "feat: 统一日志输出为单文件 stock.log" -m "- logback-spring.xml: 合并4个Appender为1个统一FILE Appender" -m "- 新增LogStartupListener: 启动时自动归档旧日志+创建logs目录" -m "- 清理application.properties注释掉的日志配置" -m "- 滚动策略: 50MB+日期, 保留30天, 总上限2GB"
```

- [ ] **Step 8: 推送**

Run: `git push origin main`
