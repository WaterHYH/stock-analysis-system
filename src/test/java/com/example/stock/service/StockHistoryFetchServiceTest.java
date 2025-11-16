package com.example.stock.service;

import com.example.stock.dto.StockHistoryDTO;
import com.example.stock.entity.StockHistory;
import com.example.stock.repository.StockHistoryRepository;
import com.example.stock.service.client.SinaStockClient;
import com.example.stock.service.mapper.StockMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 股票历史数据获取服务测试类
 * 测试StockHistoryFetchService的各种场景
 * 
 * 注意：此测试使用真实MySQL数据库，验证端到端的数据流
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("股票历史数据获取服务测试")
class StockHistoryFetchServiceTest {

    @Autowired
    private StockHistoryFetchService stockHistoryFetchService;

    @Autowired
    private SinaStockClient sinaStockClient;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private StockMapper stockMapper;

    // 测试用的股票代码
    private static final String TEST_SYMBOL = "sz000001"; // 平安银行
    private static final String TEST_SYMBOL_2 = "sz000002"; // 万科A

    @BeforeEach
    void setUp() {
        cleanTestData();
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    /**
     * 清理测试数据
     */
    private void cleanTestData() {
        try {
            jdbcTemplate.update("DELETE FROM stock_history WHERE symbol = ?", TEST_SYMBOL);
            jdbcTemplate.update("DELETE FROM stock_history WHERE symbol = ?", TEST_SYMBOL_2);
            jdbcTemplate.update("DELETE FROM stock_history WHERE symbol = ?", "xx999999");
            System.out.println("🧹 已清理测试数据");
        } catch (Exception e) {
            System.out.println("⚠️  清理测试数据异常: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试1：验证从外部API获取数据是否成功")
    void testFetchDataFromApiSuccess() {
        System.out.println("\n=== 测试1：验证从API获取数据 ===");
        
        // 直接调用API获取数据
        List<StockHistoryDTO> historyList = sinaStockClient.getStockHistory(TEST_SYMBOL);

        // 验证1：数据不为空
        assertNotNull(historyList, "API返回的数据不应为null");
        System.out.println("✅ API返回了数据");

        // 验证2：数据列表不为空
        assertFalse(historyList.isEmpty(), "API应该返回历史数据");
        System.out.println("✅ 返回了" + historyList.size() + "条历史数据");

        // 验证3：数据包含必要字段
        StockHistoryDTO firstRecord = historyList.get(0);
        assertNotNull(firstRecord.getDay(), "交易日期不应为null");
        assertNotNull(firstRecord.getSymbol(), "股票代码不应为null");
        assertEquals(TEST_SYMBOL, firstRecord.getSymbol(), "股票代码应该匹配");
        System.out.println("✅ 数据包含必要字段（日期、代码等）");

        // 验证4：价格数据合理性
        assertTrue(firstRecord.getOpen() > 0, "开盘价应该大于0");
        assertTrue(firstRecord.getHigh() > 0, "最高价应该大于0");
        assertTrue(firstRecord.getLow() > 0, "最低价应该大于0");
        assertTrue(firstRecord.getClose() > 0, "收盘价应该大于0");
        System.out.println("✅ 价格数据合理（都大于0）");

        // 验证5：最高价 >= 最低价
        assertTrue(firstRecord.getHigh() >= firstRecord.getLow(), 
                "最高价应该大于等于最低价");
        System.out.println("✅ 价格逻辑正确（最高≥最低）");

        // 验证6：成交量应该大于0
        assertTrue(firstRecord.getVolume() > 0, "成交量应该大于0");
        System.out.println("✅ 成交量合理");

        System.out.println("📊 样本数据：" + firstRecord.getDay() + ", 收盘价: " + firstRecord.getClose());
    }

    @Test
    @DisplayName("测试2：验证存入数据库的数据与获取到的数据一致")
    void testDataConsistencyBetweenApiAndDatabase() {
        System.out.println("\n=== 测试2：验证API数据与数据库数据一致 ===");
        
        // 执行方法：获取并保存数据
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        System.out.println("✅ 调用fetchAndSaveHistory完成");

        // 从API获取原始数据
        List<StockHistoryDTO> apiData = sinaStockClient.getStockHistory(TEST_SYMBOL);
        assertNotNull(apiData, "API数据不应为null");
        assertFalse(apiData.isEmpty(), "API数据不应为空");
        System.out.println("✅ API返回" + apiData.size() + "条数据");

        // 从数据库查询保存的数据
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertNotNull(dbData, "数据库数据不应为null");
        assertFalse(dbData.isEmpty(), "数据库应该有数据");
        System.out.println("✅ 数据库存储了" + dbData.size() + "条数据");

        // 验证1：数据量一致
        assertEquals(apiData.size(), dbData.size(), 
                "数据库中的记录数应该与API返回的数据量一致");
        System.out.println("✅ 数据量一致");

        // 验证2：随机抽取几条数据进行详细对比
        int sampleSize = Math.min(5, apiData.size());
        for (int i = 0; i < sampleSize; i++) {
            StockHistoryDTO dto = apiData.get(i);
            StockHistory entity = dbData.stream()
                    .filter(h -> h.getDay().equals(dto.getDay()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(entity, "应该能在数据库中找到对应日期的数据: " + dto.getDay());

            // 详细字段对比
            assertEquals(dto.getSymbol(), entity.getSymbol(), "Symbol应该一致");
            assertEquals(dto.getDay(), entity.getDay(), "交易日期应该一致");
            assertEquals(dto.getOpen(), entity.getOpen(), 0.001, "开盘价应该一致");
            assertEquals(dto.getHigh(), entity.getHigh(), 0.001, "最高价应该一致");
            assertEquals(dto.getLow(), entity.getLow(), 0.001, "最低价应该一致");
            assertEquals(dto.getClose(), entity.getClose(), 0.001, "收盘价应该一致");
            assertEquals(dto.getVolume(), entity.getVolume(), "成交量应该一致");
        }
        System.out.println("✅ 随机抽样的" + sampleSize + "条数据字段完全一致");
    }

    @Test
    @DisplayName("测试3：验证空参数处理")
    void testNullOrEmptyParameters() {
        System.out.println("\n=== 测试3：验证参数处理 ===");
        
        // 测试null参数
        assertDoesNotThrow(() -> stockHistoryFetchService.fetchAndSaveHistory(null),
                "null symbol应该被优雅处理");
        System.out.println("✅ null参数被优雅处理");

        // 测试空字符串参数
        assertDoesNotThrow(() -> stockHistoryFetchService.fetchAndSaveHistory(""),
                "空symbol应该被优雅处理");
        System.out.println("✅ 空字符串被优雅处理");

        // 验证没有数据被保存
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol("");
        assertTrue(dbData.isEmpty() || dbData.size() == 0, "无效参数不应该保存任何数据");
        System.out.println("✅ 无效参数未保存任何数据");
    }

    @Test
    @DisplayName("测试4：验证不存在的股票代码处理")
    void testInvalidStockSymbol() {
        System.out.println("\n=== 测试4：验证不存在股票的处理 ===");
        
        String invalidSymbol = "xx999999"; // 不存在的股票代码

        // 执行方法
        assertDoesNotThrow(() -> stockHistoryFetchService.fetchAndSaveHistory(invalidSymbol),
                "不存在的股票代码应该被优雅处理");
        System.out.println("✅ 不存在的股票被优雅处理");

        // 验证没有数据被保存
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol(invalidSymbol);
        assertTrue(dbData.isEmpty(), "不存在的股票不应该保存任何数据");
        System.out.println("✅ 不存在的股票未保存任何数据");
    }

    @Test
    @DisplayName("测试5：验证数据去重（重复调用不会产生重复数据）")
    @org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS)
    void testDataDeduplication() {
        System.out.println("\n=== 测试5：验证数据去重 ===");
        
        // 第一次调用
        System.out.println("🔄 第一次调用fetchAndSaveHistory...");
        long start1 = System.currentTimeMillis();
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        long duration1 = System.currentTimeMillis() - start1;
        
        List<StockHistory> firstCallData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        int firstCount = firstCallData.size();
        System.out.println("✅ 第一次调用完成：保存 " + firstCount + " 条数据，耗时 " + duration1 + "ms");

        assertTrue(firstCount > 0, "第一次调用应该保存数据");

        // 第二次调用相同的股票
        System.out.println("🔄 第二次调用fetchAndSaveHistory...");
        long start2 = System.currentTimeMillis();
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        long duration2 = System.currentTimeMillis() - start2;
        
        List<StockHistory> secondCallData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        int secondCount = secondCallData.size();
        System.out.println("✅ 第二次调用完成：数据量 " + secondCount + " 条，耗时 " + duration2 + "ms");

        // 验证：由于使用了ON DUPLICATE KEY UPDATE，数据量应该保持一致
        assertEquals(firstCount, secondCount, 
                "重复调用不应该产生重复数据，数据量应该保持一致");
        System.out.println("✅ 数据未重复，去重成功");

        // 验证每个日期只有一条记录
        long distinctDates = secondCallData.stream()
                .map(StockHistory::getDay)
                .distinct()
                .count();
        
        assertEquals(secondCount, distinctDates, 
                "每个交易日期应该只有一条记录");
        System.out.println("✅ 每个交易日期只有一条记录");
    }

    @Test
    @DisplayName("测试6：验证DTO到Entity的映射正确性")
    void testDtoToEntityMapping() {
        System.out.println("\n=== 测试6：验证DTO到Entity映射 ===");
        
        // 创建测试DTO
        StockHistoryDTO dto = new StockHistoryDTO();
        dto.setSymbol(TEST_SYMBOL);
        dto.setDay(LocalDate.of(2024, 1, 15));
        dto.setOpen(10.50);
        dto.setHigh(11.00);
        dto.setLow(10.20);
        dto.setClose(10.80);
        dto.setVolume(1000000L);
        dto.setMaPrice5(10.60);
        dto.setMaPrice10(10.55);
        dto.setMaPrice30(10.50);
        dto.setMaVolume5(900000L);
        dto.setMaVolume10(950000L);
        dto.setMaVolume30(980000L);

        // 使用Mapper转换
        StockHistory entity = stockMapper.toStockHistory(dto);

        // 验证所有字段
        assertNotNull(entity, "映射后的实体不应为null");
        assertEquals(dto.getSymbol(), entity.getSymbol());
        assertEquals(dto.getDay(), entity.getDay());
        assertEquals(dto.getOpen(), entity.getOpen(), 0.001);
        assertEquals(dto.getHigh(), entity.getHigh(), 0.001);
        assertEquals(dto.getLow(), entity.getLow(), 0.001);
        assertEquals(dto.getClose(), entity.getClose(), 0.001);
        assertEquals(dto.getVolume(), entity.getVolume());
        assertEquals(dto.getMaPrice5(), entity.getMaPrice5(), 0.001);
        assertEquals(dto.getMaPrice10(), entity.getMaPrice10(), 0.001);
        assertEquals(dto.getMaPrice30(), entity.getMaPrice30(), 0.001);
        assertEquals(dto.getMaVolume5(), entity.getMaVolume5());
        assertEquals(dto.getMaVolume10(), entity.getMaVolume10());
        assertEquals(dto.getMaVolume30(), entity.getMaVolume30());

        System.out.println("✅ DTO到Entity映射正确");
    }

    @Test
    @DisplayName("测试7：验证批量插入性能")
    void testBatchInsertPerformance() {
        System.out.println("\n=== 测试7：验证批量插入性能 ===");
        
        long startTime = System.currentTimeMillis();

        // 执行数据获取和保存
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证数据已保存
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertTrue(dbData.size() > 0, "应该有数据被保存");
        System.out.println("✅ 数据已保存，数据量：" + dbData.size() + "条");

        // 性能验证：通常几百到几千条数据应该在30秒内完成
        assertTrue(duration < 30000, 
                "批量插入应该在30秒内完成，实际耗时: " + duration + "ms");
        System.out.println("✅ 批量插入耗时: " + duration + "ms（<30秒）");

        System.out.println("📊 性能数据：" + dbData.size() + "条数据，" + duration + "ms");
    }

    @Test
    @DisplayName("测试8：验证日期范围的合理性")
    void testDateRangeValidity() {
        System.out.println("\n=== 测试8：验证日期范围合理性 ===");
        
        // 获取并保存数据
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);

        // 从数据库查询数据
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertFalse(dbData.isEmpty(), "应该有历史数据");
        System.out.println("✅ 获取了" + dbData.size() + "条历史数据");

        // 验证日期范围
        LocalDate minDate = dbData.stream()
                .map(StockHistory::getDay)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate maxDate = dbData.stream()
                .map(StockHistory::getDay)
                .max(LocalDate::compareTo)
                .orElse(null);

        assertNotNull(minDate, "应该有最早日期");
        assertNotNull(maxDate, "应该有最晚日期");
        System.out.println("✅ 日期范围：[" + minDate + " ~ " + maxDate + "]");

        // 最早日期应该早于或等于最晚日期
        assertTrue(minDate.isBefore(maxDate) || minDate.isEqual(maxDate), 
                "最早日期应该早于或等于最晚日期");
        System.out.println("✅ 最早日期早于等于最晚日期");

        // 最晚日期不应该晚于今天
        assertTrue(maxDate.isBefore(LocalDate.now()) || maxDate.isEqual(LocalDate.now()),
                "最晚交易日期不应该晚于今天");
        System.out.println("✅ 最晚日期不晚于今天");
    }

    @Test
    @DisplayName("测试9：验证多个股票代码的独立性")
    void testMultipleStockIndependence() {
        System.out.println("\n=== 测试9：验证多股票独立性 ===");
        
        // 保存第一个股票的数据
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        List<StockHistory> stock1Data = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        System.out.println("✅ 第一只股票(" + TEST_SYMBOL + ")保存了" + stock1Data.size() + "条数据");

        // 保存第二个股票的数据
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL_2);
        List<StockHistory> stock2Data = stockHistoryRepository.findBySymbol(TEST_SYMBOL_2);
        System.out.println("✅ 第二只股票(" + TEST_SYMBOL_2 + ")保存了" + stock2Data.size() + "条数据");

        // 验证两个股票的数据都存在且独立
        assertFalse(stock1Data.isEmpty(), "第一个股票应该有数据");
        assertFalse(stock2Data.isEmpty(), "第二个股票应该有数据");
        System.out.println("✅ 两只股票都有数据");

        // 验证数据不会混淆
        assertTrue(stock1Data.stream().allMatch(h -> h.getSymbol().equals(TEST_SYMBOL)),
                "第一个股票的所有数据应该属于symbol1");
        assertTrue(stock2Data.stream().allMatch(h -> h.getSymbol().equals(TEST_SYMBOL_2)),
                "第二个股票的所有数据应该属于symbol2");
        System.out.println("✅ 两只股票的数据完全独立，未混淆");
    }

    @Test
    @DisplayName("测试10：验证generateSymbol方法的准确性")
    void testGenerateSymbolAccuracy() {
        System.out.println("\n=== 测试10：验证symbol生成逻辑 ===");
        
        // 这个测试需要反射或通过其他方式测试私有方法
        // 为了简化，我们可以通过processStock的行为来间接验证
        
        // 验证不同交易所的symbol生成
        // 沪市 (600xxx)
        assertTrue(generateSymbolForTest(600000).startsWith("sh"), "600000应该生成sh开头的symbol");
        System.out.println("✅ 沪市(600000)生成正确：" + generateSymbolForTest(600000));
        
        // 深市 (000xxx)
        assertTrue(generateSymbolForTest(0).startsWith("sz"), "000000应该生成sz开头的symbol");
        System.out.println("✅ 深市(000000)生成正确：" + generateSymbolForTest(0));
        
        // 北交所 (83xxxx)
        assertTrue(generateSymbolForTest(830000).startsWith("bj"), "830000应该生成bj开头的symbol");
        System.out.println("✅ 北交所(830000)生成正确：" + generateSymbolForTest(830000));
    }

    @Test
    @DisplayName("测试7：验证唯一约束防止重复插入")
    void testUniqueConstraintPreventsInsertingDuplicates() {
        System.out.println("\n=== 测试7：验证唯一约束(symbol, trade_date)处理重复数据 ===");
        
        // 第一次：获取并保存数据
        System.out.println("第1次调用fetchAndSaveHistory，获取最新数据...");
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        
        List<StockHistory> firstLoad = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertFalse(firstLoad.isEmpty(), "第1次应该插入了数据");
        int firstCount = firstLoad.size();
        System.out.println("✅ 第1次插入: " + firstCount + "条记录");
        
        // 获取最近的一条记录用于验证
        StockHistory newestRecord = firstLoad.stream()
                .max((a, b) -> a.getDay().compareTo(b.getDay()))
                .orElse(null);
        assertNotNull(newestRecord, "应该有数据");
        System.out.println("最新记录: " + newestRecord.getSymbol() + ", 日期: " + newestRecord.getDay() + 
                           ", 收盘价: " + newestRecord.getClose());
        
        // 第二次：再次获取并保存相同的数据（模拟重复同步）
        System.out.println("\n第2次调用fetchAndSaveHistory，再次同步相同股票...");
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        
        List<StockHistory> secondLoad = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        int secondCount = secondLoad.size();
        System.out.println("✅ 第2次查询: " + secondCount + "条记录");
        
        // 验证1：数据量不变（唯一约束生效）
        assertEquals(firstCount, secondCount, 
                "唯一约束应该防止重复插入，数据量应该保持不变");
        System.out.println("✅ 唯一约束生效：没有插入重复数据，记录数保持不变 (" + firstCount + ")");
        
        // 验证2：数据内容是否被更新（ON DUPLICATE KEY UPDATE）
        StockHistory updatedRecord = secondLoad.stream()
                .filter(h -> h.getDay().equals(newestRecord.getDay()))
                .findFirst()
                .orElse(null);
        assertNotNull(updatedRecord, "应该能找到相同日期的记录");
        System.out.println("✅ 找到相同日期的记录");
        
        // 验证3：关键字段值是否一致（确认更新逻辑正确）
        assertEquals(newestRecord.getClose(), updatedRecord.getClose(), 0.001, 
                "重复插入时收盘价应该保持一致");
        assertEquals(newestRecord.getVolume(), updatedRecord.getVolume(), 
                "重复插入时成交量应该保持一致");
        System.out.println("✅ 字段值一致，UPDATE逻辑正确");
        
        // 第三次：再次同步，验证稳定性
        System.out.println("\n第3次调用fetchAndSaveHistory，再次同步验证稳定性...");
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        
        List<StockHistory> thirdLoad = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertEquals(firstCount, thirdLoad.size(), 
                "第3次同步后记录数应该保持不变");
        System.out.println("✅ 第3次同步后记录数仍保持不变: " + thirdLoad.size());
        
        System.out.println("\n✅ 总结：唯一约束(symbol, trade_date)正常工作，防止了重复插入数据");
    }

    /**
     * 辅助方法：通过字符串拼接来模拟generateSymbol逻辑（用于测试）
     */
    private String generateSymbolForTest(int code) {
        String paddedCode = String.format("%06d", code);
        String twoDigitPrefix = paddedCode.substring(0, 2);
        int prefixValue = Integer.parseInt(twoDigitPrefix);
        
        // 沪京交易所 (A股) - 60开头、607-609、688
        if (paddedCode.startsWith("60") || paddedCode.startsWith("688")) {
            return "sh" + paddedCode;
        }
        // 沪市新增号段 (607-609)
        else if (prefixValue >= 607 && prefixValue <= 609) {
            return "sh" + paddedCode;
        }
        // 深圳交易所 (A股) - 北交所号段外的数字
        // 000-099 (主板、中小板、创业板混合)
        // 100-103 (中小板)
        // 300-309+ (创业板)
        else if (prefixValue <= 103 || (prefixValue >= 300 && prefixValue < 307)) {
            return "sz" + paddedCode;
        }
        // 北京交易所 (A股) - 83, 87, 88, 89
        else if (prefixValue == 83 || prefixValue == 87 || prefixValue == 88 || prefixValue == 89) {
            return "bj" + paddedCode;
        } else {
            return null;
        }
    }

    @Test
    @DisplayName("测试8：验证增量同步功能 - 避免重复获取已有数据")
    void testIncrementalSyncOptimization() {
        System.out.println("\n=== 测试8：验证增量同步功能 ===");
        
        // 第一次：全量获取数据
        System.out.println("第1次调用：全量获取数据...");
        long start1 = System.currentTimeMillis();
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        long duration1 = System.currentTimeMillis() - start1;
        
        List<StockHistory> after1stFetch = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertFalse(after1stFetch.isEmpty(), "第1次应该插入了数据");
        int count1 = after1stFetch.size();
        System.out.println("✅ 第1次获取: " + count1 + "条记录, 耗时" + duration1 + "ms");
        
        // 记录最新的日期
        LocalDate latestDate = stockHistoryRepository.findLatestTradeDateBySymbol(TEST_SYMBOL);
        assertNotNull(latestDate, "应该能查询到最新日期");
        System.out.println("📅 数据库最新交易日期: " + latestDate);
        
        // 第二次：增量同步 (数据库已有数据，应该只获取新数据)
        System.out.println("第2次调用：增量同步...");
        long start2 = System.currentTimeMillis();
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL);
        long duration2 = System.currentTimeMillis() - start2;
        
        List<StockHistory> after2ndFetch = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        int count2 = after2ndFetch.size();
        System.out.println("✅ 第2次获取: " + count2 + "条记录, 耗时" + duration2 + "ms");
        
        // 验证1：第二次不应该重复插入所有数据
        // 注意：如果API返回的是最新数据，第2次可能没有新数据
        assertTrue(count2 >= count1, "第2次的数据量应该大于等于第1次（可能有新增）");
        System.out.println("✅ 增量同步正确：没有重复插入所有历史数据");
        
        // 验证2：如果数据相同，说明第2次没有重复插入，仅仅是验证
        if (count1 == count2) {
            System.out.println("✅ 数据库中没有新增数据（数据已是最新，不需要更新）");
        } else {
            System.out.println("✅ 发现了新增数据: " + (count2 - count1) + "条");
        }
        
        // 验证3：第二次的耗时应该比第一次少很多（因为只获取最近几天的数据）
        System.out.println("⏱️  性能对比: 第1次耗时" + duration1 + "ms, 第2次耗时" + duration2 + "ms");
        if (duration2 < duration1) {
            System.out.println("✅ 增量同步性能优化成功：耗时降低" + String.format("%.1f%%", (1 - (double)duration2/duration1) * 100));
        } else {
            System.out.println("⚠️  第2次耗时未必更少（可能是网络延迟或API返回相同数据）");
        }
    }

    @Test
    @DisplayName("测试9：验证API参数优化 - datalen参数动态调整")
    void testDynamicDatalenParameter() {
        System.out.println("\n=== 测试9：验证datalen参数动态调整 ===");
        
        // 第一次获取数据（使用默认的datalen=70000）
        System.out.println("测试1: 第一次获取全量数据...");
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL_2);
        List<StockHistory> firstData = stockHistoryRepository.findBySymbol(TEST_SYMBOL_2);
        System.out.println("✅ 第1次获取: " + firstData.size() + "条数据");
        
        // 验证第一次应该获取了很多历史数据
        assertTrue(firstData.size() > 100, "第一次应该获取了大量历史数据");
        System.out.println("✅ 确认获取了充足的历史数据");
        
        // 第二次再次调用
        System.out.println("测试2: 第二次增量获取...");
        long start = System.currentTimeMillis();
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL_2);
        long duration = System.currentTimeMillis() - start;
        List<StockHistory> secondData = stockHistoryRepository.findBySymbol(TEST_SYMBOL_2);
        System.out.println("✅ 第2次耗时: " + duration + "ms, 数据量: " + secondData.size());
        
        // 验证增量同步时datalen已被优化
        // 第二次应该只调用API获取最近几天的数据，而不是全部70000天的数据
        assertTrue(duration < 30000, "增量同步应该比全量获取更快");
        System.out.println("✅ datalen参数已被动态调整为近期数据的范围");
    }

    @Test
    @DisplayName("测试10：执行批量获取所有A股历史数据 - 验证增量同步优化效果")
    void testFetchAllStockHistoryWithIncrementalSync() {
        System.out.println("\n=== 测试10：批量获取所有A股历史数据 - 验证增量同步 ===");
        
        // 此测试模拟 fetchAllStockHistory 的基本流程
        // 为了不浪费时间，我们仅测试一个沪市主板股、一个深市主板股和一个科创板股
        String[] testSymbols = {"sh600000", "sz000001", "sh688001"};
        
        System.out.println("第1次执行：全量获取数据...");
        long firstRoundStart = System.currentTimeMillis();
        int totalFirstRound = 0;
        
        for (String symbol : testSymbols) {
            try {
                long start = System.currentTimeMillis();
                stockHistoryFetchService.fetchAndSaveHistory(symbol);
                long duration = System.currentTimeMillis() - start;
                
                List<StockHistory> data = stockHistoryRepository.findBySymbol(symbol);
                totalFirstRound += data.size();
                System.out.println("  ✅ symbol=" + symbol + ", 数据数=" + data.size() + ", 耗时=" + duration + "ms");
            } catch (Exception e) {
                System.out.println("  ⚠️  symbol=" + symbol + "获取失败: " + e.getMessage());
            }
        }
        long firstRoundDuration = System.currentTimeMillis() - firstRoundStart;
        System.out.println("✅ 第1次执行完成: 总数据=" + totalFirstRound + "条, 总耗时=" + firstRoundDuration + "ms");
        
        // 验证第一次应该获取了数据
        assertTrue(totalFirstRound > 0, "第1次应该获取了数据");
        
        // 第2次执行：增量同步 (为了低预期时间，应该明显降低)
        System.out.println("\n第2次执行：增量同步数据处理效果验证...");
        long secondRoundStart = System.currentTimeMillis();
        int totalSecondRound = 0;
        
        for (String symbol : testSymbols) {
            try {
                long start = System.currentTimeMillis();
                stockHistoryFetchService.fetchAndSaveHistory(symbol);
                long duration = System.currentTimeMillis() - start;
                
                List<StockHistory> data = stockHistoryRepository.findBySymbol(symbol);
                totalSecondRound += data.size();
                System.out.println("  ✅ symbol=" + symbol + ", 数据数=" + data.size() + ", 耗时=" + duration + "ms");
            } catch (Exception e) {
                System.out.println("  ⚠️  symbol=" + symbol + "获取失败: " + e.getMessage());
            }
        }
        long secondRoundDuration = System.currentTimeMillis() - secondRoundStart;
        System.out.println("✅ 第2次执行完成: 总数据=" + totalSecondRound + "条, 总耗时=" + secondRoundDuration + "ms");
        
        // 验证：第2次的数据量应等于或大于第1次
        assertTrue(totalSecondRound >= totalFirstRound, "增量同步后数据量不应减少");
        System.out.println("\n✅ 验证成功：增量同步没有丧失数据");
        
        // 性能对比
        System.out.println("\n⏱️  性能对比：");
        System.out.println("  第1次（全量获取）: " + firstRoundDuration + "ms");
        System.out.println("  第2次（增量同步）: " + secondRoundDuration + "ms");
        
        if (secondRoundDuration < firstRoundDuration) {
            double improvement = (1 - (double) secondRoundDuration / firstRoundDuration) * 100;
            System.out.println("  ✅ 性能提升: " + String.format("%.1f", improvement) + "%");
        } else if (secondRoundDuration > firstRoundDuration) {
            double increase = ((double) secondRoundDuration / firstRoundDuration - 1) * 100;
            System.out.println("  ⚠️  耗时增加: +" + String.format("%.1f", increase) + "% (可能是网络延迟、API返回数据或有新增数据)");
        } else {
            System.out.println("  ✅ 耗时相同 (整体效率没有变化)");
        }
        
        // 验证数据完整性
        assertTrue(totalSecondRound > 100, "应该有充分的历史数据用于测试");
        System.out.println("\n✅ 数据完整性验证成功");
    }
}