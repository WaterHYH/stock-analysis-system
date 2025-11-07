# StockHistoryFetchService 测试指南

## 测试类说明

测试类 `StockHistoryFetchServiceTest` 包含了 9 个全面的测试用例，用于验证股票历史数据获取和保存功能的正确性。

## 测试用例列表

### 1️⃣ **测试1：验证从API获取数据是否成功**
- **测试目标**：验证能否成功从新浪财经API获取股票历史数据
- **验证点**：
  - 数据不为null
  - 数据列表不为空
  - 包含必要字段（日期、代码、价格等）
  - 价格数据合理性（大于0，最高价>=最低价）
  - 成交量大于0

### 2️⃣ **测试2：验证存入数据库的数据与获取到的数据一致**
- **测试目标**：确保数据在保存到数据库后没有丢失或变更
- **验证点**：
  - 数据量一致
  - 随机抽样对比各字段值（symbol、code、日期、价格、成交量等）
  - 所有关键字段完全匹配

### 3️⃣ **测试3：验证空参数处理**
- **测试目标**：验证方法对null和空字符串参数的健壮性
- **验证点**：
  - null参数不会抛出异常
  - 空字符串参数不会抛出异常
  - 无效参数不会保存数据到数据库

### 4️⃣ **测试4：验证不存在的股票代码处理**
- **测试目标**：验证对不存在股票代码的容错处理
- **验证点**：
  - 不会抛出异常
  - 不会保存无效数据

### 5️⃣ **测试5：验证数据去重（重复调用不会产生重复数据）**
- **测试目标**：验证使用ON DUPLICATE KEY UPDATE的去重机制
- **验证点**：
  - 重复调用保持数据量一致
  - 每个交易日期只有一条记录

### 6️⃣ **测试6：验证DTO到Entity的映射正确性**
- **测试目标**：验证MapStruct映射器的正确性
- **验证点**：
  - 所有字段正确映射
  - 数值精度保持一致

### 7️⃣ **测试7：验证批量插入性能**
- **测试目标**：验证批量数据插入的性能表现
- **验证点**：
  - 数据成功保存
  - 执行时间在合理范围内（<30秒）

### 8️⃣ **测试8：验证日期范围的合理性**
- **测试目标**：验证历史数据的日期范围是否合理
- **验证点**：
  - 存在最早和最晚日期
  - 最早日期早于或等于最晚日期
  - 最晚日期不晚于今天

### 9️⃣ **测试9：验证多个股票代码的独立性**
- **测试目标**：验证不同股票的数据不会混淆
- **验证点**：
  - 多个股票都能正确保存
  - 不同股票的数据完全独立
  - 数据不会相互干扰

## 快速运行测试

### 方法1：使用Maven命令行
```bash
# 运行单个测试类
mvn test -Dtest=StockHistoryFetchServiceTest

# 运行特定测试方法
mvn test -Dtest=StockHistoryFetchServiceTest#testFetchDataFromApiSuccess

# 运行所有测试
mvn test
```

### 方法2：使用IDE
1. 打开 `StockHistoryFetchServiceTest.java`
2. 右键点击类名或方法名
3. 选择 "Run 'StockHistoryFetchServiceTest'" 或 "Run 'testFetchDataFromApiSuccess()'"

## 测试前准备

### 1. 确保数据库可用
```sql
-- 创建测试数据库（如果使用独立测试库）
CREATE DATABASE IF NOT EXISTS stock_db_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置测试环境变量（可选）
```bash
# Windows PowerShell
$env:TEST_DB_URL="jdbc:mysql://localhost:3306/stock_db_test"
$env:TEST_DB_USERNAME="root"
$env:TEST_DB_PASSWORD="your_password"

# Linux/Mac
export TEST_DB_URL="jdbc:mysql://localhost:3306/stock_db_test"
export TEST_DB_USERNAME="root"
export TEST_DB_PASSWORD="your_password"
```

### 3. 确保网络连接正常
测试需要访问新浪财经API，请确保网络畅通。

## 测试输出示例

```
✅ 测试1通过：成功从API获取了 1500 条历史数据
   第一条数据：2024-01-15, 收盘价: 10.80

✅ 测试2通过：数据库中的 1500 条数据与API数据完全一致

✅ 测试3通过：空参数被正确处理，未保存任何数据

✅ 测试4通过：不存在的股票代码被正确处理

✅ 测试5通过：重复调用不会产生重复数据

✅ 测试6通过：DTO到Entity映射正确

✅ 测试7通过：批量插入 1500 条数据，耗时: 3250ms

✅ 测试8通过：日期范围合理 [2019-01-02 ~ 2024-12-20]

✅ 测试9通过：多个股票数据独立存储
   sz000001: 1500 条数据
   sz000002: 1480 条数据
```

## 常见问题

### Q1: 测试失败，提示连接数据库失败
**A**: 检查数据库是否启动，配置文件中的数据库连接信息是否正确。

### Q2: 测试1失败，提示API返回null
**A**: 检查网络连接，新浪财经API可能暂时不可用，稍后重试。

### Q3: 测试2失败，数据不一致
**A**: 检查MapStruct配置是否正确，StockMapper是否正确映射所有字段。

### Q4: 测试很慢
**A**: 这是正常的，因为需要从API获取真实数据。如需加快速度，可以使用Mock对象。

## 使用Mock对象加速测试（可选）

如果不想依赖真实API和数据库，可以创建Mock版本：

```java
@ExtendWith(MockitoExtension.class)
class StockHistoryFetchServiceMockTest {
    
    @Mock
    private SinaStockClient sinaStockClient;
    
    @Mock
    private StockHistoryRepository stockHistoryRepository;
    
    @Mock
    private StockMapper stockMapper;
    
    @InjectMocks
    private StockHistoryFetchService stockHistoryFetchService;
    
    // 编写Mock测试...
}
```

## 注意事项

1. **数据库隔离**：测试使用了 `@Transactional` 注解，测试后会自动回滚，不会污染数据库
2. **真实API调用**：测试1、2、7、8、9会调用真实的新浪财经API，需要网络连接
3. **测试时间**：完整运行所有测试大约需要30-60秒，因为涉及真实网络请求
4. **测试股票**：默认使用 sz000001（平安银行）作为测试股票，可以根据需要修改

## 测试覆盖率

这些测试用例覆盖了：
- ✅ 正常流程：API调用 → 数据映射 → 数据保存
- ✅ 异常处理：空参数、无效股票代码
- ✅ 边界条件：重复数据、多股票独立性
- ✅ 性能验证：批量插入效率
- ✅ 数据一致性：API与数据库数据对比
- ✅ 业务逻辑：日期范围、价格合理性

建议在每次修改 `fetchAndSaveHistory` 方法后运行这些测试，确保功能正常。
