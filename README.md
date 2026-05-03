# 股票数据采集与分析系统

股票 A 股数据采集、存储、技术分析与多维度筛选系统。数据源来自新浪财经 API，支持 Web 页面和 REST API 双模式访问。

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    前端展示层                                 │
│  微信小程序（规划中） / Web 页面（Thymeleaf）                   │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                   Spring Boot 3.4.3                          │
│  ┌──────────┐ ┌──────────────┐ ┌──────────────────────────┐ │
│  │ Controller│ │   Service    │ │      Repository           │ │
│  │ Web/API   │ │ 数据采集/分析 │ │ JPA + 原生SQL + JDBC批量  │ │
│  └──────────┘ └──────────────┘ └──────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Scheduler: 定时同步实时数据 / 历史K线数据               │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│              MySQL 8.0（stock + stock_history + sync_log）    │
└─────────────────────────────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│              新浪财经 API（实时行情 + 历史K线）                 │
└─────────────────────────────────────────────────────────────┘
```

### 架构演进

- **初始架构**：网页端 + 服务器
- **当前架构**：Web 页面 + REST API 双模式
- **规划架构**：微信小程序 + Web 页面（兼容方案）

## 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.4.3 | 内嵌 Tomcat |
| 语言 | Java 17 | LTS 版本 |
| 构建 | Maven 3.6+ | 依赖管理与打包 |
| 数据库 | MySQL 8.0 | 主数据库 |
| ORM | Spring Data JPA / Hibernate | 对象关系映射 |
| 模板引擎 | Thymeleaf | 服务端页面渲染 |
| 对象映射 | MapStruct 1.5.3 | DTO ↔ Entity 转换 |
| 简化代码 | Lombok 1.18.30 | 减少样板代码 |
| JSON | FastJSON 2.0.34 | JSON 解析 |
| HTTP | Apache HttpClient 4.5.13 | HTTP 请求 |
| 连接池 | HikariCP | 数据库连接池 |
| 容器化 | Docker + Docker Compose | 可选部署方式 |
| 部署 | Ubuntu 24.04 + systemd | 生产环境 |
| IDE | IntelliJ IDEA | 推荐开发工具 |

## 项目结构

```
stock-system/
├── src/main/java/com/example/stock/
│   ├── StockApplication.java             # Spring Boot 启动类
│   ├── config/                            # 配置类
│   │   ├── ExecutorConfig.java            # 线程池配置
│   │   └── RestTemplateConfig.java        # HTTP 客户端配置
│   ├── controller/                        # 控制器层
│   │   ├── StockWebController.java        # 股票列表页面
│   │   ├── StockApiController.java        # 数据同步 API
│   │   ├── StockAnalysisController.java   # 股票筛选分析页面
│   │   ├── StockHistoryController.java    # 历史数据查询页面
│   │   └── TestController.java            # 健康检查
│   ├── dto/                               # 数据传输对象
│   │   ├── StockDTO.java                  # 实时行情 DTO
│   │   ├── StockHistoryDTO.java           # 历史 K 线 DTO
│   │   └── StockAnalysisDTO.java          # 筛选分析结果 DTO
│   ├── entity/                            # 数据实体
│   │   ├── Stock.java                     # 股票实时数据
│   │   ├── StockHistory.java              # 历史 K 线 + 技术分析字段
│   │   └── StockSyncLog.java              # 同步日志
│   ├── repository/                        # 数据访问层
│   │   ├── StockRepository.java           # 实时数据仓库
│   │   ├── StockHistoryRepository.java    # 历史数据仓库（含聚合查询）
│   │   ├── StockSyncLogRepository.java    # 同步日志仓库
│   │   ├── StockHistoryCustomRepository.java      # 自定义仓库接口
│   │   └── StockHistoryCustomRepositoryImpl.java  # JDBC 批量插入实现
│   ├── scheduler/                         # 定时任务
│   │   ├── StockDataSyncScheduler.java    # 实时数据同步
│   │   └── StockHistorySyncScheduler.java # 历史数据同步
│   ├── service/                           # 业务逻辑层
│   │   ├── StockQueryService.java         # 股票查询服务
│   │   ├── StockDataFetchService.java     # 实时数据抓取
│   │   ├── StockHistoryFetchService.java  # 历史数据同步（增量）
│   │   ├── StockHistoryService.java       # 历史数据查询服务
│   │   ├── StockAnalysisService.java      # 多维筛选分析
│   │   ├── KLineAnalysisService.java      # 技术指标计算
│   │   ├── client/
│   │   │   └── SinaStockClient.java       # 新浪 API 客户端
│   │   └── mapper/
│   │       └── StockMapper.java           # MapStruct 映射器
│   └── utils/
│       └── MyStringUtil.java              # 字符串工具类
├── src/main/resources/
│   ├── application.properties             # 主配置文件
│   ├── logback-spring.xml                 # 日志配置（多环境/多文件）
│   └── templates/stocks/                  # Thymeleaf 模板
│       ├── list.html                      # 股票列表页
│       ├── analysis.html                  # 股票筛选分析页
│       ├── history_price.html             # 历史数据页
│       └── kline.html                     # K 线展示页
├── src/test/                               # 测试代码
├── docker-compose.yml                      # Docker 编排
├── Dockerfile                              # Docker 镜像构建
├── deploy.sh                               # 一键部署脚本
└── pom.xml                                 # Maven 配置
```

## 核心功能

### 1. 股票实时数据展示

- **地址**：`/stocks`
- **功能**：分页展示沪深 A 股实时行情，支持按代码搜索
- **数据**：最新价、涨跌幅、成交量、市盈率、市净率、换手率等 20+ 字段

### 2. 历史数据采集（增量同步）

- **数据范围**：覆盖全部沪深 A 股（含科创板、创业板）
- **同步策略**：首次全量拉取，后续增量同步
- **去重机制**：通过 `stock_sync_log` 表记录每只股票的上次同步日期
- **周末智能判断**：自动识别非交易日，跳过无意义请求

### 3. 技术分析指标

系统对每条历史 K 线自动计算以下技术指标：

| 类别 | 指标 |
|------|------|
| 均线系统 | MA5 / MA10 / MA30，金叉/死叉判断，多头/空头排列 |
| K 线形态 | 阴阳线、十字星、锤子线、倒锤子线、上下影线比例 |
| 趋势分析 | 连续涨跌天数、突破前高/跌破前低 |
| 成交量分析 | 量比、放量/缩量、量价配合 |
| MACD | DIF / DEA / MACD 柱状图，金叉/死叉判断 |
| RSI | RSI6 / RSI12 / RSI24，超买/超卖判断 |
| 布林带 | 上轨 / 中轨 / 下轨，触及判断 |

### 4. 多维度股票筛选

| 条件 | 说明 |
|------|------|
| 跌幅超阈值 | 相对于历史最高价，支持自定义起始日期和百分比 |
| 高波动低价格 | 半年内至少 3 次日波动 > 20%，且处于低位 |
| 连续上涨 | 近 10 天至少 8 天收阳 |
| 接近年高点 | 距年度最高价 ≤ 5% |
| 成交量激增 | 当日量 > 前 30 日均量 2 倍 |
| 均线金叉 | 5 日均线上穿 10 日均线 |

## 服务器部署

> 当前部署在阿里云 ECS（Ubuntu 24.04），公网 IP：`120.76.43.179`

### 环境要求

- **服务器**：Ubuntu 22.04 / 24.04 LTS
- **Java**：OpenJDK 17
- **MySQL**：8.0
- **Maven**：3.6+
- **安全组**：开放 22（SSH）、80（HTTP）、443（HTTPS）、8080（应用）

### 服务器基础环境安装

```bash
# 更新系统
sudo apt update
sudo apt upgrade -y
sudo apt install -y curl wget unzip git

# 安装 Java 17
sudo apt install -y openjdk-17-jdk
java -version

# 安装 MySQL
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
sudo mysql_secure_installation

# 安装 Maven
sudo apt install -y maven
mvn -v

# 安装调试工具
sudo apt install -y net-tools htop inotify-tools
```

### 数据库初始化

```sql
-- 登录 MySQL
sudo mysql

-- 创建数据库
CREATE DATABASE stock_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权
CREATE USER 'stock_user'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON stock_db.* TO 'stock_user'@'%';
FLUSH PRIVILEGES;
```

### 项目部署步骤

#### 1. 克隆项目

```bash
sudo mkdir -p /var/www/stock/{app,logs,config,backups,scripts}
cd /var/www/stock/app
git clone git@github.com:WaterHYH/stock-analysis-system.git .
```

#### 2. 构建打包

```bash
# 在项目根目录执行
mvn clean package -DskipTests
```

生成的 JAR 位于 `target/stock-0.0.1-SNAPSHOT.jar`。

#### 3. 上传到服务器（如从本地上传）

```bash
scp target/stock-0.0.1-SNAPSHOT.jar root@120.76.43.179:/var/www/stock/app/
```

#### 4. 创建 systemd 服务

```bash
sudo vim /etc/systemd/system/stock.service
```

写入以下内容：

```ini
[Unit]
Description=Stock System Service
After=network.target

[Service]
User=root
WorkingDirectory=/var/www/stock/app
ExecStart=/usr/bin/java -jar /var/www/stock/app/stock-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
Environment="JAVA_OPTS=-Xms512m -Xmx1024m"

[Install]
WantedBy=multi-user.target
```

#### 5. 启动服务

```bash
sudo systemctl daemon-reload
sudo systemctl enable stock.service
sudo systemctl start stock.service
sudo systemctl status stock.service
```

#### 6. 配置自动重启（监听 JAR 更新）

```bash
# 创建监控脚本
vim /var/www/stock/scripts/monitor.sh
```

```bash
#!/bin/bash
while inotifywait -e close_write /var/www/stock/app/stock-*.jar; do
    sudo systemctl restart stock
done
```

```bash
chmod +x /var/www/stock/scripts/monitor.sh
nohup /var/www/stock/scripts/monitor.sh &
```

### 验证服务

```bash
# 健康检查
curl http://localhost:8080/test

# 查看实时日志
sudo journalctl -u stock -f

# 查看应用日志
tail -f /var/log/stock/stock-app.log

# 查看服务状态
sudo systemctl status stock
```

## 本地开发

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- IntelliJ IDEA（推荐）

### 快速开始

```bash
# 克隆项目
git clone git@github.com:WaterHYH/stock-analysis-system.git
cd stock-system

# 修改数据库配置
# 编辑 src/main/resources/application.properties
# 配置 spring.datasource.url、username、password

# 运行项目
mvn spring-boot:run

# 或使用 Maven Wrapper
./mvnw spring-boot:run
```

<details>
<summary>Windows 环境启动指南（点击展开）</summary>

```powershell
# 打包（跳过测试）
.\mvnw.cmd clean package spring-boot:repackage -DskipTests

# 停止旧进程（如有）
$processId = (Get-NetTCPConnection -LocalPort 8080).OwningProcess
Stop-Process -Id $processId -Force

# 设置 UTF-8 编码（解决中文乱码）
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

# 启动应用
java -jar target\demo-0.0.1-SNAPSHOT.jar
```

</details>

启动后访问：

- 股票列表：http://localhost:8080/stocks
- 筛选分析：http://localhost:8080/stock-analysis
- 历史数据：http://localhost:8080/stock-history
- 健康检查：http://localhost:8080/test

### 打包

```bash
mvn clean package -DskipTests
# JAR 输出: target/stock-0.0.1-SNAPSHOT.jar
```

## 数据库表结构

### stock — 股票实时数据

| 字段 | 说明 |
|------|------|
| symbol / code / name | 股票标识与名称 |
| trade_price / price_change / change_percent | 最新价 / 涨跌额 / 涨跌幅 |
| bid_price / ask_price | 买一价 / 卖一价 |
| open_price / high_price / low_price | 开盘 / 最高 / 最低 |
| volume / amount | 成交量 / 成交金额 |
| pe_ratio / pb_ratio / market_cap | 市盈率 / 市净率 / 总市值 |
| turnover_rate | 换手率 |

### stock_history — 历史 K 线 + 技术分析

| 分类 | 字段 |
|------|------|
| 基础 | symbol, code, trade_date, open, high, low, close, volume |
| 均线 | ma_price5/10/30, ma_volume5/10/30 |
| K 线形态 | kline_type, upper_shadow_ratio, lower_shadow_ratio, body_ratio, is_doji, is_hammer, is_inverted_hammer |
| 趋势 | consecutive_rise_days, is_break_high, is_break_low |
| 成交量 | volume_ratio, is_volume_surge, is_volume_shrink, is_price_volume_match |
| MACD | macd_dif, macd_dea, macd_bar, is_macd_golden_cross, is_macd_death_cross |
| RSI | rsi6, rsi12, rsi24, is_overbought, is_oversold |
| 布林带 | boll_upper, boll_middle, boll_lower, is_touch_boll_upper, is_touch_boll_lower |

### stock_sync_log — 同步日志

| 字段 | 说明 |
|------|------|
| symbol | 股票代码（主键） |
| sync_date | 上次同步日期 |

## API 接口

### 手动触发数据同步

```
GET /api/stocks/sync
```

**响应示例**：

```json
{
  "success": true,
  "message": "同步成功，更新 4000 条记录"
}
```

### 健康检查

```
GET /test
```

返回 `Database connection successful!` 表示服务正常。

### 在线地址

- 健康检查：http://120.76.43.179:8080/test
- 股票列表：http://120.76.43.179:8080/stocks
- 筛选分析：http://120.76.43.179:8080/stock-analysis

## 日志说明

系统按功能模块分离日志文件，支持多环境切换：

| 日志文件 | 内容 | 环境路径 |
|----------|------|----------|
| stock-app.log | 应用主日志 | dev: `./logs` / prod: `/var/log/stock` |
| stock-sync.log | 数据同步日志 | 同上 |
| stock-kline.log | K 线分析日志 | 同上 |
| stock-error.log | 错误日志（仅 ERROR 级别） | 同上 |

日志滚动策略：按天 + 大小（10 MB），保留 30 天，总上限 1 GB / 500 MB。

## Docker 部署（可选）

```bash
# 构建并启动
docker-compose up -d

# 查看状态
docker-compose ps
docker logs -f stock-system
```

## 运维常用命令

```bash
# 查看服务状态
sudo systemctl status stock

# 重启服务
sudo systemctl restart stock

# 查看实时日志
sudo journalctl -u stock -f

# 查看应用日志
tail -f /var/log/stock/stock-app.log

# 查看错误日志
tail -f /var/log/stock/stock-error.log

# 检查端口监听
sudo netstat -tlnp | grep 8080

# 检查进程
ps aux | grep stock
```

## 开发指南

### 代码规范

- 使用 Lombok `@Data` 简化实体类
- 使用 MapStruct 进行 DTO ↔ Entity 映射
- 使用 `@RequiredArgsConstructor` 构造函数注入
- 遵循标准 MVC 三层架构
- 批量操作使用 JDBC `batchUpdate` + `rewriteBatchedStatements`

### 运行测试

```bash
mvn test
```

### 数据库查询优化要点

- `StockHistoryRepository` 中的聚合查询走数据库层，避免 N+1 问题
- 分页查询使用无 count 优化（`findAllWithoutCount`）
- 批量插入走 `StockHistoryCustomRepositoryImpl`，利用 JDBC 原生批处理

## 许可证

MIT License
