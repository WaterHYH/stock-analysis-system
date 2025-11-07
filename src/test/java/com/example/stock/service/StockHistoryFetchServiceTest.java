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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 股票历史数据获取服务测试类
 * 测试fetchAndSaveHistory方法的各种场景
 * 
 * 注意：此测试使用真实MySQL数据库，不会自动回滚
 */
@SpringBootTest
@ActiveProfiles("test") // 使用测试配置文件
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
    private static final String TEST_CODE = "000001";

    @BeforeEach
    void setUp() {
        // 清理测试数据（只清理测试用的股票数据）
        cleanTestData();
    }

    @AfterEach
    void tearDown() {
        // 测试后清理数据
        cleanTestData();
    }

    /**
     * 清理测试数据（使用JdbcTemplate直接执行SQL，不需要事务）
     */
    private void cleanTestData() {
        // 使用JDBC直接执行DELETE，避免事务问题
        jdbcTemplate.update("DELETE FROM stock_history WHERE symbol = ?", TEST_SYMBOL);
        jdbcTemplate.update("DELETE FROM stock_history WHERE symbol = ?", "sz000002");
        jdbcTemplate.update("DELETE FROM stock_history WHERE symbol = ?", "xx999999");
        System.out.println("🧹 已清理测试数据");
    }

    @Test
    @DisplayName("测试1：验证从API获取数据是否成功")
    void testFetchDataFromApiSuccess() {
        // 直接调用API获取数据
        List<StockHistoryDTO> historyList = sinaStockClient.getStockHistory(TEST_SYMBOL, TEST_CODE);

        // 验证1：数据不为空
        assertNotNull(historyList, "API返回的数据不应为null");

        // 验证2：数据列表不为空
        assertFalse(historyList.isEmpty(), "API应该返回历史数据");

        // 验证3：数据包含必要字段
        StockHistoryDTO firstRecord = historyList.get(0);
        assertNotNull(firstRecord.getDay(), "交易日期不应为null");
        assertNotNull(firstRecord.getSymbol(), "股票代码不应为null");
        assertEquals(TEST_SYMBOL, firstRecord.getSymbol(), "股票代码应该匹配");
        assertEquals(TEST_CODE, firstRecord.getCode(), "股票代码应该匹配");

        // 验证4：价格数据合理性
        assertTrue(firstRecord.getOpen() > 0, "开盘价应该大于0");
        assertTrue(firstRecord.getHigh() > 0, "最高价应该大于0");
        assertTrue(firstRecord.getLow() > 0, "最低价应该大于0");
        assertTrue(firstRecord.getClose() > 0, "收盘价应该大于0");

        // 验证5：最高价 >= 最低价
        assertTrue(firstRecord.getHigh() >= firstRecord.getLow(), 
                "最高价应该大于等于最低价");

        // 验证6：成交量应该大于0
        assertTrue(firstRecord.getVolume() > 0, "成交量应该大于0");

        System.out.println("✅ 测试1通过：成功从API获取了 " + historyList.size() + " 条历史数据");
        System.out.println("   第一条数据：" + firstRecord.getDay() + ", 收盘价: " + firstRecord.getClose());
    }

    @Test
    @DisplayName("测试2：验证存入数据库的数据与获取到的数据一致")
    void testDataConsistencyBetweenApiAndDatabase() {
        // 执行方法：获取并保存数据
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL, TEST_CODE);

        // 从API获取原始数据
        List<StockHistoryDTO> apiData = sinaStockClient.getStockHistory(TEST_SYMBOL, TEST_CODE);
        assertNotNull(apiData, "API数据不应为null");
        assertFalse(apiData.isEmpty(), "API数据不应为空");

        // 从数据库查询保存的数据
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertNotNull(dbData, "数据库数据不应为null");
        assertFalse(dbData.isEmpty(), "数据库应该有数据");

        // 验证1：数据量一致
        assertEquals(apiData.size(), dbData.size(), 
                "数据库中的记录数应该与API返回的数据量一致");

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
            assertEquals(dto.getCode(), entity.getCode(), "Code应该一致");
            assertEquals(dto.getDay(), entity.getDay(), "交易日期应该一致");
            assertEquals(dto.getOpen(), entity.getOpen(), 0.001, "开盘价应该一致");
            assertEquals(dto.getHigh(), entity.getHigh(), 0.001, "最高价应该一致");
            assertEquals(dto.getLow(), entity.getLow(), 0.001, "最低价应该一致");
            assertEquals(dto.getClose(), entity.getClose(), 0.001, "收盘价应该一致");
            assertEquals(dto.getVolume(), entity.getVolume(), "成交量应该一致");
        }

        System.out.println("✅ 测试2通过：数据库中的 " + dbData.size() + " 条数据与API数据完全一致");
    }

    @Test
    @DisplayName("测试3：验证空参数处理")
    void testNullOrEmptyParameters() {
        // 测试null参数
        assertDoesNotThrow(() -> stockHistoryFetchService.fetchAndSaveHistory(null, TEST_CODE),
                "null symbol应该被优雅处理");
        
        assertDoesNotThrow(() -> stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL, null),
                "null code应该被优雅处理");

        // 测试空字符串参数
        assertDoesNotThrow(() -> stockHistoryFetchService.fetchAndSaveHistory("", TEST_CODE),
                "空symbol应该被优雅处理");
        
        assertDoesNotThrow(() -> stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL, ""),
                "空code应该被优雅处理");

        // 验证没有数据被保存
        List<StockHistory> dbData = stockHistoryRepository.findAll();
        assertTrue(dbData.isEmpty(), "无效参数不应该保存任何数据");

        System.out.println("✅ 测试3通过：空参数被正确处理，未保存任何数据");
    }

    @Test
    @DisplayName("测试4：验证不存在的股票代码处理")
    void testInvalidStockSymbol() {
        String invalidSymbol = "xx999999"; // 不存在的股票代码
        String invalidCode = "999999";

        // 执行方法
        assertDoesNotThrow(() -> stockHistoryFetchService.fetchAndSaveHistory(invalidSymbol, invalidCode),
                "不存在的股票代码应该被优雅处理");

        // 验证没有数据被保存
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol(invalidSymbol);
        assertTrue(dbData.isEmpty(), "不存在的股票不应该保存任何数据");

        System.out.println("✅ 测试4通过：不存在的股票代码被正确处理");
    }

    @Test
    @DisplayName("测试5：验证数据去重（重复调用不会产生重复数据）")
    @org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS)
    void testDataDeduplication() {
        System.out.println("⚙️ 开始测试5：数据去重验证");
        
        // 第一次调用
        System.out.println("🔄 第一次调用 fetchAndSaveHistory...");
        long start1 = System.currentTimeMillis();
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL, TEST_CODE);
        long duration1 = System.currentTimeMillis() - start1;
        
        List<StockHistory> firstCallData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        int firstCount = firstCallData.size();
        System.out.println("✅ 第一次调用完成：保存 " + firstCount + " 条数据，耗时 " + duration1 + "ms");

        assertTrue(firstCount > 0, "第一次调用应该保存数据");

        // 第二次调用相同的股票
        System.out.println("🔄 第二次调用 fetchAndSaveHistory...");
        long start2 = System.currentTimeMillis();
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL, TEST_CODE);
        long duration2 = System.currentTimeMillis() - start2;
        
        List<StockHistory> secondCallData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        int secondCount = secondCallData.size();
        System.out.println("✅ 第二次调用完成：数据量 " + secondCount + " 条，耗时 " + duration2 + "ms");

        // 验证：由于使用了ON DUPLICATE KEY UPDATE，数据量应该保持一致
        assertEquals(firstCount, secondCount, 
                "重复调用不应该产生重复数据，数据量应该保持一致");

        // 验证每个日期只有一条记录
        long distinctDates = secondCallData.stream()
                .map(StockHistory::getDay)
                .distinct()
                .count();
        
        assertEquals(secondCount, distinctDates, 
                "每个交易日期应该只有一条记录");

        System.out.println("✅ 测试5通过：MySQL环境下重复调用不会产生重复数据（ON DUPLICATE KEY UPDATE有效）");
        System.out.println("📊 总耗时：" + (duration1 + duration2) + "ms");
    }

    @Test
    @DisplayName("测试6：验证DTO到Entity的映射正确性")
    void testDtoToEntityMapping() {
        // 创建测试DTO
        StockHistoryDTO dto = new StockHistoryDTO();
        dto.setSymbol(TEST_SYMBOL);
        dto.setCode(TEST_CODE);
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
        assertEquals(dto.getCode(), entity.getCode());
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

        System.out.println("✅ 测试6通过：DTO到Entity映射正确");
    }

    @Test
    @DisplayName("测试7：验证批量插入性能")
    void testBatchInsertPerformance() {
        long startTime = System.currentTimeMillis();

        // 执行数据获取和保存
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL, TEST_CODE);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证数据已保存
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertTrue(dbData.size() > 0, "应该有数据被保存");

        // 性能验证：通常几百到几千条数据应该在10秒内完成
        assertTrue(duration < 30000, 
                "批量插入应该在30秒内完成，实际耗时: " + duration + "ms");

        System.out.println("✅ 测试7通过：批量插入 " + dbData.size() + 
                " 条数据，耗时: " + duration + "ms");
    }

    @Test
    @DisplayName("测试8：验证日期范围的合理性")
    void testDateRangeValidity() {
        // 获取并保存数据
        stockHistoryFetchService.fetchAndSaveHistory(TEST_SYMBOL, TEST_CODE);

        // 从数据库查询数据
        List<StockHistory> dbData = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        assertFalse(dbData.isEmpty(), "应该有历史数据");

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

        // 最早日期应该早于最晚日期
        assertTrue(minDate.isBefore(maxDate) || minDate.isEqual(maxDate), 
                "最早日期应该早于或等于最晚日期");

        // 最晚日期不应该晚于今天
        assertTrue(maxDate.isBefore(LocalDate.now()) || maxDate.isEqual(LocalDate.now()),
                "最晚交易日期不应该晚于今天");

        System.out.println("✅ 测试8通过：日期范围合理 [" + minDate + " ~ " + maxDate + "]");
    }

    @Test
    @DisplayName("测试9：验证多个股票代码的独立性")
    void testMultipleStockIndependence() {
        String symbol1 = "sz000001"; // 平安银行
        String code1 = "000001";
        String symbol2 = "sz000002"; // 万科A
        String code2 = "000002";

        // 保存第一个股票的数据
        stockHistoryFetchService.fetchAndSaveHistory(symbol1, code1);
        List<StockHistory> stock1Data = stockHistoryRepository.findBySymbol(symbol1);

        // 保存第二个股票的数据
        stockHistoryFetchService.fetchAndSaveHistory(symbol2, code2);
        List<StockHistory> stock2Data = stockHistoryRepository.findBySymbol(symbol2);

        // 验证两个股票的数据都存在且独立
        assertFalse(stock1Data.isEmpty(), "第一个股票应该有数据");
        assertFalse(stock2Data.isEmpty(), "第二个股票应该有数据");

        // 验证数据不会混淆
        assertTrue(stock1Data.stream().allMatch(h -> h.getSymbol().equals(symbol1)),
                "第一个股票的所有数据应该属于symbol1");
        assertTrue(stock2Data.stream().allMatch(h -> h.getSymbol().equals(symbol2)),
                "第二个股票的所有数据应该属于symbol2");

        System.out.println("✅ 测试9通过：多个股票数据独立存储");
        System.out.println("   " + symbol1 + ": " + stock1Data.size() + " 条数据");
        System.out.println("   " + symbol2 + ": " + stock2Data.size() + " 条数据");
    }
}
