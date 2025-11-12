package com.example.stock.service;

import com.example.stock.entity.StockHistory;
import com.example.stock.repository.StockHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * K线分析集成测试
 * 测试历史数据获取、K线分析和数据持久化的完整流程
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("K线分析集成测试")
class KLineAnalysisIntegrationTest {
    
    @Autowired
    private StockHistoryFetchService stockHistoryFetchService;
    
    @Autowired
    private StockHistoryRepository stockHistoryRepository;
    
    @Autowired
    private KLineAnalysisService kLineAnalysisService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final String TEST_SYMBOL = "sh600000_test";
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        cleanTestData();
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        cleanTestData();
    }
    
    /**
     * 清理测试数据
     */
    private void cleanTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM stock_history WHERE symbol LIKE '%_test'");
        } catch (Exception e) {
            // 表可能不存在或已清理
        }
    }
    
    @Test
    @DisplayName("测试1: 完整流程 - 模拟获取数据→分析→保存")
    void testKLineAnalysisFieldsPersistence() {
        // 模拟真实的历史数据列表 (从API获取)
        List<StockHistory> historyList = new java.util.ArrayList<>();
        
        // 创建5条历史数据 (最新到最旧)
        for (int i = 0; i < 5; i++) {
            StockHistory history = new StockHistory();
            history.setSymbol(TEST_SYMBOL);
            history.setCode("600000");
            history.setDay(LocalDate.now().minusDays(i));
            history.setOpen(10.0 + i * 0.1);
            history.setClose(10.5 + i * 0.1); // 逐日上涨
            history.setHigh(11.0 + i * 0.1);
            history.setLow(10.0 + i * 0.1);
            history.setVolume(1000000 + i * 100000);
            history.setMaPrice5(10.3 + i * 0.05);
            history.setMaPrice10(10.2 + i * 0.05);
            history.setMaPrice30(10.1 + i * 0.05);
            history.setMaVolume5(1000000);
            history.setMaVolume10(1000000);
            history.setMaVolume30(1000000);
            historyList.add(history);
        }
        
        // 模拟完整流程：遍历分析每条数据（就像StockHistoryFetchService.fetchAndSaveHistory()中的逻辑）
        for (int i = 0; i < historyList.size(); i++) {
            StockHistory current = historyList.get(i);
            StockHistory previous = (i + 1 < historyList.size()) ? historyList.get(i + 1) : null;
            // 这里调用K线分析，就像实际代码中做的那样
            kLineAnalysisService.analyzeKLine(current, previous, historyList.subList(i, historyList.size()));
        }
        
        // 批量保存到数据库
        stockHistoryRepository.saveAll(historyList);
        
        // 从数据库读取
        List<StockHistory> result = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        
        // 验证数据已保存
        assertFalse(result.isEmpty(), "历史数据应被保存到数据库");
        assertEquals(5, result.size(), "应保存5条记录");
        
        // 验证第一条记录（最新的）的K线分析字段是否已保存
        StockHistory newest = result.stream()
            .max((a, b) -> a.getDay().compareTo(b.getDay()))
            .orElse(null);
        
        assertNotNull(newest, "应能找到最新数据");
        assertNotNull(newest.getKlineType(), "K线类型应被保存");
        assertNotNull(newest.getChangePercent(), "涨跌幅应被保存");
        assertNotNull(newest.getAmplitude(), "振幅应被保存");
        assertNotNull(newest.getVolumeRatio(), "量比应被保存");
        
        assertTrue(newest.getChangePercent() >= 0, "涨跌幅值应该有效");
        assertTrue(newest.getAmplitude() >= 0, "振幅值应该有效");
        assertTrue(newest.getVolumeRatio() > 0, "量比值应该有效");
        
        System.out.println("✅ K线分析字段已正确保存到数据库");
        System.out.println("  - K线类型: " + newest.getKlineType());
        System.out.println("  - 涨跌幅: " + newest.getChangePercent() + "%");
        System.out.println("  - 振幅: " + newest.getAmplitude() + "%");
        System.out.println("  - 量比: " + newest.getVolumeRatio());
    }
    
    @Test
    @DisplayName("测试2: 验证金叉信号的持久化")
    void testGoldenCrossSignalPersistence() {
        // 创建前一天的数据
        StockHistory previous = new StockHistory();
        previous.setSymbol(TEST_SYMBOL);
        previous.setCode("600000");
        previous.setDay(LocalDate.now().minusDays(1));
        previous.setMaPrice5(9.5);
        previous.setMaPrice10(10.0);
        previous.setMaPrice30(10.5);
        previous.setClose(10.0);
        previous.setOpen(10.0);
        previous.setHigh(10.5);
        previous.setLow(9.5);
        previous.setVolume(1000000);
        previous.setMaVolume5(1000000);
        previous.setMaVolume10(1000000);
        previous.setMaVolume30(1000000);
        
        // 创建当天的数据 (发生金叉)
        StockHistory current = new StockHistory();
        current.setSymbol(TEST_SYMBOL);
        current.setCode("600000");
        current.setDay(LocalDate.now());
        current.setMaPrice5(10.2); // > MA10
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.5);
        current.setClose(10.5);
        current.setOpen(10.0);
        current.setHigh(10.5);
        current.setLow(10.0);
        current.setVolume(1000000);
        current.setMaVolume5(1000000);
        current.setMaVolume10(1000000);
        current.setMaVolume30(1000000);
        
        // 执行K线分析
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 保存
        stockHistoryRepository.save(current);
        
        // 验证
        List<StockHistory> result = stockHistoryRepository.findBySymbol(TEST_SYMBOL);
        StockHistory saved = result.stream()
            .filter(h -> h.getDay().equals(LocalDate.now()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(saved, "当天数据应存在");
        assertTrue(saved.getIsMa5GoldenCross(), "应检测到MA5金叉");
        
        System.out.println("✅ 金叉信号已正确持久化");
    }
    
    @Test
    @DisplayName("测试3: 验证大幅度价格变动的K线识别")
    void testLargePriceMovementAnalysis() {
        // 创建前一天的基础数据
        StockHistory previous = new StockHistory();
        previous.setClose(10.0);
        
        // 创建当天的数据 (大幅上涨)
        StockHistory current = new StockHistory();
        current.setSymbol(TEST_SYMBOL);
        current.setCode("600000");
        current.setDay(LocalDate.now());
        current.setOpen(10.0);
        current.setClose(12.0); // 20%上涨
        current.setHigh(12.5);
        current.setLow(10.0);
        current.setVolume(2000000);
        current.setMaPrice5(11.0);
        current.setMaPrice10(10.5);
        current.setMaPrice30(10.0);
        current.setMaVolume5(1000000);
        current.setMaVolume10(1000000);
        current.setMaVolume30(1000000);
        
        // 执行分析
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证
        assertEquals(20.0, current.getChangePercent(), 0.01, "涨幅应为20%");
        assertEquals(1, current.getKlineType(), "应识别为阳线");
        assertEquals(2.0, current.getVolumeRatio(), 0.01, "量比应为2.0");
        assertTrue(current.getIsPriceVolumeMatch(), "应识别为量价配合");
        
        System.out.println("✅ 大幅价格变动已正确分析");
        System.out.println("  - 涨幅: " + current.getChangePercent() + "%");
        System.out.println("  - K线类型: " + (current.getKlineType() == 1 ? "阳线" : "其他"));
        System.out.println("  - 量比: " + current.getVolumeRatio());
    }
    
    @Test
    @DisplayName("测试4: 验证多个K线形态的识别")
    void testMultipleCandlePatternRecognition() {
        List<StockHistory> patterns = new java.util.ArrayList<>();
        
        // 模式1: 阳线
        StockHistory bullish = new StockHistory();
        bullish.setSymbol(TEST_SYMBOL + "_bullish");
        bullish.setOpen(10.0);
        bullish.setClose(11.0);
        bullish.setHigh(11.5);
        bullish.setLow(9.8);
        bullish.setVolume(1000000);
        bullish.setMaPrice5(10.5);
        bullish.setMaPrice10(10.5);
        bullish.setMaPrice30(10.5);
        bullish.setMaVolume5(1000000);
        patterns.add(bullish);
        
        // 模式2: 阴线
        StockHistory bearish = new StockHistory();
        bearish.setSymbol(TEST_SYMBOL + "_bearish");
        bearish.setOpen(11.0);
        bearish.setClose(10.0);
        bearish.setHigh(11.2);
        bearish.setLow(9.8);
        bearish.setVolume(1000000);
        bearish.setMaPrice5(10.5);
        bearish.setMaPrice10(10.5);
        bearish.setMaPrice30(10.5);
        bearish.setMaVolume5(1000000);
        patterns.add(bearish);
        
        // 模式3: 十字星
        StockHistory doji = new StockHistory();
        doji.setSymbol(TEST_SYMBOL + "_doji");
        doji.setOpen(10.0);
        doji.setClose(10.01);
        doji.setHigh(11.0);
        doji.setLow(9.0);
        doji.setVolume(1000000);
        doji.setMaPrice5(10.5);
        doji.setMaPrice10(10.5);
        doji.setMaPrice30(10.5);
        doji.setMaVolume5(1000000);
        patterns.add(doji);
        
        // 分析每个模式
        for (StockHistory pattern : patterns) {
            kLineAnalysisService.analyzeKLine(pattern, null, List.of(pattern));
        }
        
        // 验证
        assertEquals(1, bullish.getKlineType(), "阳线类型应为1");
        assertEquals(0, bearish.getKlineType(), "阴线类型应为0");
        assertEquals(2, doji.getKlineType(), "十字星类型应为2");
        assertTrue(doji.getIsDoji(), "应识别为十字星");
        
        System.out.println("✅ 多个K线形态已正确识别");
    }
    
    @Test
    @DisplayName("测试5: 验证均线系统多组合信号")
    void testMultipleMASignals() {
        // 创建包含金叉信号的数据
        StockHistory previous = new StockHistory();
        previous.setMaPrice5(9.0);
        previous.setMaPrice10(9.5);
        previous.setMaPrice30(10.0);
        
        StockHistory current = new StockHistory();
        current.setSymbol(TEST_SYMBOL);
        current.setCode("600000");
        current.setDay(LocalDate.now());
        current.setMaPrice5(10.1); // > MA10且 > MA30
        current.setMaPrice10(9.5); // 在中间
        current.setMaPrice30(10.0);
        current.setOpen(10.0);
        current.setClose(10.0);
        current.setHigh(10.1);
        current.setLow(9.9);
        current.setVolume(1000000);
        current.setMaVolume5(1000000);
        
        // 分析
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证多个条件
        assertTrue(current.getIsMa5GoldenCross(), "应检测到MA5金叉");
        assertTrue(current.getIsMaBullish(), "应识别为均线多头排列");
        
        System.out.println("✅ 均线系统多组合信号已正确识别");
        System.out.println("  - MA5金叉: " + current.getIsMa5GoldenCross());
        System.out.println("  - 均线多头: " + current.getIsMaBullish());
    }
    
    @Test
    @DisplayName("测试6: 验证成交量异常的检测")
    void testAbnormalVolumeDetection() {
        // 极端放量情况
        StockHistory volumeSurge = new StockHistory();
        volumeSurge.setSymbol(TEST_SYMBOL + "_surge");
        volumeSurge.setVolume(5000000); // 5倍
        volumeSurge.setMaVolume5(1000000);
        volumeSurge.setOpen(10.0);
        volumeSurge.setClose(11.0);
        volumeSurge.setHigh(11.0);
        volumeSurge.setLow(10.0);
        volumeSurge.setMaPrice5(10.5);
        volumeSurge.setMaPrice10(10.5);
        volumeSurge.setMaPrice30(10.5);
        
        StockHistory previous = new StockHistory();
        previous.setClose(10.0);
        
        kLineAnalysisService.analyzeKLine(volumeSurge, previous, List.of(volumeSurge, previous));
        
        assertTrue(volumeSurge.getIsVolumeSurge(), "应检测到极端放量");
        assertEquals(5.0, volumeSurge.getVolumeRatio(), 0.01);
        
        // 极端缩量情况
        StockHistory volumeShrink = new StockHistory();
        volumeShrink.setSymbol(TEST_SYMBOL + "_shrink");
        volumeShrink.setVolume(100000); // 0.1倍
        volumeShrink.setMaVolume5(1000000);
        volumeShrink.setOpen(10.0);
        volumeShrink.setClose(9.5);
        volumeShrink.setHigh(10.0);
        volumeShrink.setLow(9.5);
        volumeShrink.setMaPrice5(10.5);
        volumeShrink.setMaPrice10(10.5);
        volumeShrink.setMaPrice30(10.5);
        
        kLineAnalysisService.analyzeKLine(volumeShrink, previous, List.of(volumeShrink, previous));
        
        assertTrue(volumeShrink.getIsVolumeShrink(), "应检测到极端缩量");
        assertEquals(0.1, volumeShrink.getVolumeRatio(), 0.01);
        
        System.out.println("✅ 成交量异常已正确检测");
        System.out.println("  - 极端放量倍数: " + volumeSurge.getVolumeRatio());
        System.out.println("  - 极端缩量倍数: " + volumeShrink.getVolumeRatio());
    }
    
    @Test
    @DisplayName("测试7: 性能测试 - 单条数据分析耗时")
    void testSingleDataAnalysisPerformance() {
        // 创建包含足够历史数据的列表 (用于计算所有技术指标)
        List<StockHistory> historyList = new java.util.ArrayList<>();
        double lastClose = 10.0;
        
        for (int i = 0; i < 100; i++) {
            StockHistory history = new StockHistory();
            history.setDay(LocalDate.now().minusDays(i));
            history.setClose(lastClose + (Math.random() - 0.5) * 0.5);
            history.setOpen(history.getClose() + (Math.random() - 0.5) * 0.2);
            history.setHigh(Math.max(history.getClose(), history.getOpen()) + 0.2);
            history.setLow(Math.min(history.getClose(), history.getOpen()) - 0.2);
            history.setVolume(1000000 + (long)(Math.random() * 500000));
            history.setMaPrice5(10.0);
            history.setMaPrice10(10.0);
            history.setMaPrice30(10.0);
            history.setMaVolume5(1000000);
            historyList.add(0, history);
            lastClose = history.getClose();
        }
        
        StockHistory current = historyList.get(0);
        
        // 测试性能
        long startTime = System.currentTimeMillis();
        kLineAnalysisService.analyzeKLine(current, null, historyList);
        long duration = System.currentTimeMillis() - startTime;
        
        // 验证: 单条数据分析应在100ms以内
        assertTrue(duration < 100, "单条数据分析耗时应 < 100ms, 实际耗时: " + duration + "ms");
        
        System.out.println("✅ 性能测试通过");
        System.out.println("  - 单条数据分析耗时: " + duration + "ms");
    }
}
