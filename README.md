# 股票数据管理系统

## 项目简介

这是一个基于Spring Boot 3.x开发的股票数据管理系统，主要功能包括：
1. **股票实时数据展示**：通过Web页面展示所有股票的实时行情数据
2. **股票历史数据获取**：从新浪财经API定期获取股票历史价格数据并更新到数据库

## 技术栈

- **框架**: Spring Boot 3.4.3
- **语言**: Java 17
- **构建工具**: Maven
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA / Hibernate
- **模板引擎**: Thymeleaf
- **对象映射**: MapStruct
- **工具库**: Lombok, FastJSON
- **HTTP客户端**: Apache HttpClient, RestTemplate

## 项目结构

```
src/main/java/com/example/stock/
├── StockApplication.java              # Spring Boot启动类
├── config/                            # 配置类
│   ├── ExecutorConfig.java           # 线程池配置
│   └── RestTemplateConfig.java       # HTTP客户端配置
├── controller/                        # 控制器层
│   ├── StockWebController.java       # 股票Web页面控制器
│   └── StockApiController.java       # 股票数据API控制器
├── dto/                              # 数据传输对象
│   ├── StockDTO.java                 # 股票实时数据DTO
│   └── StockHistoryDTO.java          # 股票历史数据DTO
├── entity/                           # 实体类
│   ├── Stock.java                    # 股票实体
│   └── StockHistory.java             # 股票历史数据实体
├── repository/                       # 数据访问层
│   ├── StockRepository.java          # 股票数据仓库
│   ├── StockHistoryRepository.java   # 股票历史数据仓库
│   ├── StockHistoryCustomRepository.java              # 自定义仓库接口
│   └── StockHistoryCustomRepositoryImpl.java          # 自定义仓库实现
├── scheduler/                        # 定时任务
│   ├── StockDataSyncScheduler.java   # 股票实时数据同步调度器
│   └── StockHistorySyncScheduler.java # 股票历史数据同步调度器
├── service/                          # 业务逻辑层
│   ├── StockQueryService.java        # 股票数据查询服务
│   ├── StockDataFetchService.java    # 股票实时数据获取服务
│   ├── StockHistoryFetchService.java # 股票历史数据获取服务
│   ├── client/                       # 外部API客户端
│   │   └── SinaStockClient.java     # 新浪财经API客户端
│   └── mapper/                       # 对象映射器
│       └── StockMapper.java         # 股票数据映射器
└── utils/                            # 工具类
    └── MyStringUtil.java             # 字符串工具类
```

## 核心功能

### 1. 股票数据展示
- **访问地址**: http://localhost:8080/stocks
- **功能**: 支持分页展示股票数据，可按股票代码搜索
- **实现**: 使用Thymeleaf模板引擎渲染页面

### 2. 股票数据同步
- **手动同步**: `GET /api/stocks/sync` - 手动触发数据同步
- **定时同步**: 
  - 股票实时数据：每天3:10自动同步
  - 股票历史数据：每天16:30自动同步
- **数据源**: 新浪财经API

## 数据库表结构

### stock（股票实时数据表）
- 存储股票的实时交易数据
- 主键：id
- 唯一索引：symbol

### stock_history（股票历史数据表）
- 存储股票的历史K线数据
- 主键：id
- 唯一索引：symbol + trade_date

## 配置说明

主要配置项在 `application.properties` 中：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/stock_db
spring.datasource.username=root
spring.datasource.password=your_password

# 服务器端口
server.port=8080

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库初始化
```sql
CREATE DATABASE stock_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 修改配置
编辑 `src/main/resources/application.properties`，配置数据库连接信息。

### 4. 运行项目
```bash
mvn clean install
mvn spring-boot:run
```

### 5. 访问应用
打开浏览器访问：http://localhost:8080/stocks

## API接口

### 手动同步股票数据
```
GET /api/stocks/sync
```
**响应示例**:
```json
{
  "success": true,
  "message": "同步成功，更新 4000 条记录"
}
```

## 定时任务说明

- **StockDataSyncScheduler**: 每天3:10执行股票实时数据同步
- **StockHistorySyncScheduler**: 每天16:30执行股票历史数据同步

定时任务使用独立线程池执行，不会阻塞主线程。

## 开发指南

### 项目架构
采用经典的MVC三层架构：
- **Controller层**: 处理HTTP请求，返回响应
- **Service层**: 实现业务逻辑
- **Repository层**: 数据持久化操作

### 代码规范
- 使用Lombok简化代码
- 使用MapStruct进行对象映射
- 使用@RequiredArgsConstructor进行构造函数依赖注入
- 遵循RESTful API设计规范

## 许可证

MIT License
