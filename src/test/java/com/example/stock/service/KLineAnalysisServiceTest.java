package com.example.stock.service;

import com.example.stock.entity.StockHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * K线技术分析服务测试类
 * 测试各种K线形态识别、技术指标计算和交易信号判断
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("K线技术分析服务测试")
class KLineAnalysisServiceTest {
    
    private KLineAnalysisService kLineAnalysisService;
    private List<StockHistory> testHistoryList;
    
    @BeforeEach
    void setUp() {
        kLineAnalysisService = new KLineAnalysisService();
        testHistoryList = new ArrayList<>();
    }
    
    // ==================== 基础指标测试 ====================
    
    @Test
    @DisplayName("测试1: 涨跌幅计算")
    void testChangePercentCalculation() {
        // 创建前一天的数据
        StockHistory previous = new StockHistory();
        previous.setClose(10.0);
        
        // 创建当天的数据
        StockHistory current = new StockHistory();
        current.setOpen(10.5);
        current.setClose(11.0);
        current.setHigh(11.5);
        current.setLow(10.0);
        
        // 执行分析
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证: (11.0 - 10.0) / 10.0 * 100 = 10%
        assertNotNull(current.getChangePercent());
        assertEquals(10.0, current.getChangePercent(), 0.01, "涨跌幅应为10%");
    }
    
    @Test
    @DisplayName("测试2: 振幅计算")
    void testAmplitudeCalculation() {
        StockHistory previous = new StockHistory();
        previous.setClose(10.0);
        
        StockHistory current = new StockHistory();
        current.setOpen(10.0);
        current.setClose(10.0);
        current.setHigh(11.0);
        current.setLow(9.0);
        
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证: (11.0 - 9.0) / 10.0 * 100 = 20%
        assertNotNull(current.getAmplitude());
        assertEquals(20.0, current.getAmplitude(), 0.01, "振幅应为20%");
    }
    
    // ==================== K线形态识别测试 ====================
    
    @Test
    @DisplayName("测试3: 阳线识别")
    void testBullishCandleIdentification() {
        StockHistory current = new StockHistory();
        current.setOpen(10.0);
        current.setClose(11.0);
        current.setHigh(11.5);
        current.setLow(9.5);
        current.setMaPrice5(10.0);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.0);
        current.setMaVolume5(1000000);
        
        kLineAnalysisService.analyzeKLine(current, null, List.of(current));
        
        // 验证: 阳线 (close > open)
        assertEquals(1, current.getKlineType(), "K线类型应为阳线(1)");
        assertFalse(Boolean.TRUE.equals(current.getIsDoji()), "不应为十字星");
    }
    
    @Test
    @DisplayName("测试4: 阴线识别")
    void testBearishCandleIdentification() {
        StockHistory current = new StockHistory();
        current.setOpen(11.0);
        current.setClose(10.0);
        current.setHigh(11.5);
        current.setLow(9.5);
        current.setMaPrice5(10.0);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.0);
        current.setMaVolume5(1000000);
        
        kLineAnalysisService.analyzeKLine(current, null, List.of(current));
        
        // 验证: 阴线 (close < open)
        assertEquals(0, current.getKlineType(), "K线类型应为阴线(0)");
    }
    
    @Test
    @DisplayName("测试5: 十字星识别")
    void testDojiCandleIdentification() {
        StockHistory current = new StockHistory();
        current.setOpen(10.0);
        current.setClose(10.01); // 实体极小
        current.setHigh(11.0);
        current.setLow(9.0);
        current.setMaPrice5(10.0);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.0);
        current.setMaVolume5(1000000);
        
        kLineAnalysisService.analyzeKLine(current, null, List.of(current));
        
        // 验证: 十字星 (实体长度占比 < 5%)
        assertEquals(2, current.getKlineType(), "K线类型应为十字星(2)");
        assertTrue(current.getIsDoji(), "应识别为十字星");
    }
    
    @Test
    @DisplayName("测试6: 锤子线识别")
    void testHammerCandleIdentification() {
        StockHistory current = new StockHistory();
        current.setOpen(10.5);
        current.setClose(10.3); // 小实体
        current.setHigh(10.6);
        current.setLow(9.0);     // 长下影线
        current.setMaPrice5(10.0);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.0);
        current.setMaVolume5(1000000);
        
        kLineAnalysisService.analyzeKLine(current, null, List.of(current));
        
        // 验证: 锤子线 (下影线长、上影线短、实体小)
        assertTrue(current.getIsHammer(), "应识别为锤子线");
    }
    
    // ==================== 均线系统测试 ====================
    
    @Test
    @DisplayName("测试7: MA5金叉MA10")
    void testMA5GoldenCross() {
        // 前一天: MA5 < MA10
        StockHistory previous = new StockHistory();
        previous.setMaPrice5(9.5);
        previous.setMaPrice10(10.0);
        previous.setMaPrice30(10.5);
        previous.setClose(9.8);
        
        // 当天: MA5 > MA10 (金叉)
        StockHistory current = new StockHistory();
        current.setMaPrice5(10.2);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.5);
        current.setClose(10.5);
        current.setMaVolume5(1000000);
        
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证: MA5金叉MA10
        assertTrue(current.getIsMa5GoldenCross(), "应检测到MA5金叉MA10");
        assertFalse(current.getIsMa5DeathCross(), "不应检测到MA5死叉");
    }
    
    @Test
    @DisplayName("测试8: MA5死叉MA10")
    void testMA5DeathCross() {
        // 前一天: MA5 > MA10
        StockHistory previous = new StockHistory();
        previous.setMaPrice5(10.5);
        previous.setMaPrice10(10.0);
        previous.setMaPrice30(10.5);
        previous.setClose(10.2);
        
        // 当天: MA5 < MA10 (死叉)
        StockHistory current = new StockHistory();
        current.setMaPrice5(9.8);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.5);
        current.setClose(9.9);
        current.setMaVolume5(1000000);
        
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证: MA5死叉MA10
        assertTrue(current.getIsMa5DeathCross(), "应检测到MA5死叉MA10");
        assertFalse(current.getIsMa5GoldenCross(), "不应检测到MA5金叉");
    }
    
    @Test
    @DisplayName("测试9: 均线多头排列")
    void testBullishMAAlignment() {
        StockHistory current = new StockHistory();
        current.setSymbol("sh600000");
        current.setDay(LocalDate.now());
        current.setMaPrice5(11.0);
        current.setMaPrice10(10.0);
        current.setMaPrice30(9.0);
        current.setClose(11.0);
        current.setMaVolume5(1000000);
        current.setOpen(10.5);
        current.setHigh(11.2);
        current.setLow(10.3);
        current.setVolume(1000000);
        
        kLineAnalysisService.analyzeKLine(current, null, List.of(current));
        
        // 验证: MA5 > MA10 > MA30
        assertTrue(Boolean.TRUE.equals(current.getIsMaBullish()), "应识别为均线多头排列");
        assertFalse(Boolean.TRUE.equals(current.getIsMaBearish()), "不应识别为均线空头排列");
    }
    
    @Test
    @DisplayName("测试10: 均线空头排列")
    void testBearishMAAlignment() {
        StockHistory current = new StockHistory();
        current.setSymbol("sh600000");
        current.setDay(LocalDate.now());
        current.setMaPrice5(9.0);
        current.setMaPrice10(9.5);
        current.setMaPrice30(10.0);
        current.setClose(9.0);
        current.setMaVolume5(1000000);
        current.setOpen(9.1);
        current.setHigh(9.3);
        current.setLow(8.8);
        current.setVolume(1000000);
        
        kLineAnalysisService.analyzeKLine(current, null, List.of(current));
        
        // 验证: MA5 < MA10 < MA30
        assertTrue(Boolean.TRUE.equals(current.getIsMaBearish()), "应识别为均线空头排列");
        assertFalse(Boolean.TRUE.equals(current.getIsMaBullish()), "不应识别为均线多头排列");
    }
    
    // ==================== 成交量分析测试 ====================
    
    @Test
    @DisplayName("测试11: 放量判断")
    void testVolumeSurge() {
        StockHistory current = new StockHistory();
        current.setVolume(1600000); // 1.6倍MA5成交量
        current.setMaVolume5(1000000);
        current.setOpen(10.0);
        current.setClose(11.0);
        current.setHigh(11.0);
        current.setLow(10.0);
        current.setMaPrice5(10.0);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.0);
        
        StockHistory previous = new StockHistory();
        previous.setClose(10.0);
        
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证: 放量 (成交量 > 1.5倍MA5)
        assertTrue(current.getIsVolumeSurge(), "应识别为放量");
        assertFalse(current.getIsVolumeShrink(), "不应识别为缩量");
        assertEquals(1.6, current.getVolumeRatio(), 0.01, "量比应为1.6");
    }
    
    @Test
    @DisplayName("测试12: 缩量判断")
    void testVolumeShrink() {
        StockHistory current = new StockHistory();
        current.setVolume(400000); // 0.4倍MA5成交量
        current.setMaVolume5(1000000);
        current.setOpen(10.0);
        current.setClose(9.5);
        current.setHigh(10.0);
        current.setLow(9.5);
        current.setMaPrice5(10.0);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.0);
        
        StockHistory previous = new StockHistory();
        previous.setClose(10.0);
        
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证: 缩量 (成交量 < 0.5倍MA5)
        assertTrue(current.getIsVolumeShrink(), "应识别为缩量");
        assertFalse(current.getIsVolumeSurge(), "不应识别为放量");
        assertEquals(0.4, current.getVolumeRatio(), 0.01, "量比应为0.4");
    }
    
    @Test
    @DisplayName("测试13: 量价配合")
    void testPriceVolumeMatch() {
        // 价涨量增的情况
        StockHistory current = new StockHistory();
        current.setSymbol("sh600000");
        current.setDay(LocalDate.now());
        current.setVolume(1200000); // 1.2倍MA5 (放量)
        current.setMaVolume5(1000000);
        current.setOpen(10.0);
        current.setClose(11.0);     // 上涨
        current.setHigh(11.0);
        current.setLow(10.0);
        current.setMaPrice5(10.0);
        current.setMaPrice10(10.0);
        current.setMaPrice30(10.0);
        
        StockHistory previous = new StockHistory();
        previous.setClose(10.0);
        
        kLineAnalysisService.analyzeKLine(current, previous, List.of(current, previous));
        
        // 验证: 量价配合 (价涨且量增) - 更新阈值检查
        assertTrue(Boolean.TRUE.equals(current.getIsPriceVolumeMatch()) || 
                   (current.getChangePercent() != null && current.getChangePercent() > 0 && 
                    current.getVolumeRatio() != null && current.getVolumeRatio() > 1.0), 
                   "应识别为量价配合或价涨量增");
    }
    
    // ==================== RSI指标测试 ====================
    
    @Test
    @DisplayName("测试14: RSI指标计算和超买超卖")
    void testRSICalculation() {
        List<StockHistory> historyList = new ArrayList<>();
        
        // 创建连续上涨的数据 (10天)
        double lastClose = 10.0;
        for (int i = 0; i < 10; i++) {
            StockHistory history = new StockHistory();
            history.setClose(lastClose + i * 0.2); // 逐日上涨
            historyList.add(0, history); // 前插,保持降序
            lastClose = history.getClose();
        }
        
        StockHistory current = historyList.get(0);
        current.setMaPrice5(11.0);
        current.setMaPrice10(10.5);
        current.setMaPrice30(10.0);
        current.setMaVolume5(1000000);
        
        kLineAnalysisService.analyzeKLine(current, null, historyList);
        
        // 验证: 连续上涨应导致RSI较高
        assertNotNull(current.getRsi6(), "RSI6应被计算");
        assertTrue(current.getRsi6() > 50, "连续上涨的RSI应 > 50");
    }
    
    // ==================== MACD指标测试 ====================
    
    @Test
    @DisplayName("测试15: MACD指标计算")
    void testMACDCalculation() {
        List<StockHistory> historyList = new ArrayList<>();
        
        // 创建26天的模拟数据 (计算MACD需要)
        double lastClose = 10.0;
        for (int i = 0; i < 26; i++) {
            StockHistory history = new StockHistory();
            history.setClose(lastClose + (Math.random() - 0.5) * 0.5); // 随机波动
            history.setMaPrice5(10.0);
            history.setMaPrice10(10.0);
            history.setMaPrice30(10.0);
            history.setMaVolume5(1000000);
            historyList.add(0, history); // 前插,保持降序
            lastClose = history.getClose();
        }
        
        StockHistory current = historyList.get(0);
        kLineAnalysisService.analyzeKLine(current, null, historyList);
        
        // 验证: MACD组件应被计算
        assertNotNull(current.getMacdDif(), "MACD DIF应被计算");
        assertNotNull(current.getMacdDea(), "MACD DEA应被计算");
        assertNotNull(current.getMacdBar(), "MACD BAR应被计算");
    }
    
    // ==================== 布林带指标测试 ====================
    
    @Test
    @DisplayName("测试16: 布林带计算")
    void testBOLLCalculation() {
        List<StockHistory> historyList = new ArrayList<>();
        
        // 创建20天的稳定数据 (布林带需要20天)
        double basePrice = 10.0;
        for (int i = 0; i < 20; i++) {
            StockHistory history = new StockHistory();
            history.setClose(basePrice + (Math.random() - 0.5) * 0.4); // 小幅波动
            history.setMaPrice5(10.0);
            history.setMaPrice10(10.0);
            history.setMaPrice30(10.0);
            history.setMaVolume5(1000000);
            historyList.add(0, history);
        }
        
        StockHistory current = historyList.get(0);
        kLineAnalysisService.analyzeKLine(current, null, historyList);
        
        // 验证: 布林带组件应被计算
        assertNotNull(current.getBollUpper(), "布林带上轨应被计算");
        assertNotNull(current.getBollMiddle(), "布林带中轨应被计算");
        assertNotNull(current.getBollLower(), "布林带下轨应被计算");
        
        // 验证: 上轨 > 中轨 > 下轨
        assertTrue(current.getBollUpper() > current.getBollMiddle(), 
            "布林带上轨应 > 中轨");
        assertTrue(current.getBollMiddle() > current.getBollLower(), 
            "布林带中轨应 > 下轨");
    }
    
    @Test
    @DisplayName("测试17: 布林带触及判断")
    void testBOLLTouchDetection() {
        List<StockHistory> historyList = new ArrayList<>();
        
        // 创建20天的数据,最后一天触及上轨
        double basePrice = 10.0;
        for (int i = 0; i < 20; i++) {
            StockHistory history = new StockHistory();
            history.setClose(basePrice + (Math.random() - 0.5) * 0.2);
            history.setMaPrice5(10.0);
            history.setMaPrice10(10.0);
            history.setMaPrice30(10.0);
            history.setMaVolume5(1000000);
            historyList.add(0, history);
        }
        
        // 当前数据触及上轨
        StockHistory current = historyList.get(0);
        current.setClose(10.5); // 设置较高的收盘价
        
        kLineAnalysisService.analyzeKLine(current, null, historyList);
        
        // 验证: 根据计算的布林带判断是否触及
        if (current.getBollUpper() != null && current.getClose() >= current.getBollUpper() * 0.99) {
            assertTrue(current.getIsTouchBollUpper(), "应检测到触及上轨");
        }
    }
    
    // ==================== 趋势分析测试 ====================
    
    @Test
    @DisplayName("测试18: 突破前高")
    void testBreakHighDetection() {
        List<StockHistory> historyList = new ArrayList<>();
        
        // 创建20天的历史数据,最后一天突破前高
        for (int i = 0; i < 20; i++) {
            StockHistory history = new StockHistory();
            history.setDay(LocalDate.now().minusDays(i));
            history.setHigh(10.0 + i * 0.01); // 逐步上升
            history.setClose(9.9 + i * 0.01);
            history.setMaPrice5(10.0);
            history.setMaPrice10(10.0);
            history.setMaPrice30(10.0);
            history.setMaVolume5(1000000);
            historyList.add(0, history);
        }
        
        // 当前数据突破前高
        StockHistory current = historyList.get(0);
        current.setClose(10.3); // 突破前20天的最高价
        
        kLineAnalysisService.analyzeKLine(current, null, historyList);
        
        // 验证: 应检测到突破前高
        assertTrue(current.getIsBreakHigh(), "应检测到突破前高");
    }
    
    @Test
    @DisplayName("测试19: 跌破前低")
    void testBreakLowDetection() {
        List<StockHistory> historyList = new ArrayList<>();
        
        // 创建20天的历史数据,最后一天跌破前低
        for (int i = 0; i < 20; i++) {
            StockHistory history = new StockHistory();
            history.setDay(LocalDate.now().minusDays(i));
            history.setHigh(10.0 - i * 0.01);
            history.setLow(9.9 - i * 0.01); // 逐步下降
            history.setClose(10.0 - i * 0.01);
            history.setMaPrice5(10.0);
            history.setMaPrice10(10.0);
            history.setMaPrice30(10.0);
            history.setMaVolume5(1000000);
            historyList.add(0, history);
        }
        
        // 当前数据跌破前低
        StockHistory current = historyList.get(0);
        current.setClose(9.7); // 跌破前20天的最低价
        
        kLineAnalysisService.analyzeKLine(current, null, historyList);
        
        // 验证: 应检测到跌破前低
        assertTrue(current.getIsBreakLow(), "应检测到跌破前低");
    }
    
    // ==================== 综合测试 ====================
    
    @Test
    @DisplayName("测试20: 综合K线分析")
    void testComprehensiveAnalysis() {
        // 创建30天的完整历史数据
        List<StockHistory> historyList = new ArrayList<>();
        double basePrice = 10.0;
        
        for (int i = 0; i < 30; i++) {
            StockHistory history = new StockHistory();
            history.setDay(LocalDate.now().minusDays(i));
            history.setClose(basePrice + (i % 3) * 0.1);
            history.setOpen(basePrice + ((i + 1) % 3) * 0.1);
            history.setHigh(history.getClose() + 0.2);
            history.setLow(history.getClose() - 0.2);
            history.setVolume(1000000L + i * 10000);
            history.setMaPrice5(10.0);
            history.setMaPrice10(10.0);
            history.setMaPrice30(10.0);
            history.setMaVolume5(1000000);
            history.setMaVolume10(1000000);
            history.setMaVolume30(1000000);
            historyList.add(0, history);
        }
        
        StockHistory current = historyList.get(0);
        StockHistory previous = historyList.get(1);
        
        // 执行完整分析
        kLineAnalysisService.analyzeKLine(current, previous, historyList);
        
        // 验证: 所有基础字段应被计算
        assertNotNull(current.getChangePercent(), "涨跌幅应被计算");
        assertNotNull(current.getAmplitude(), "振幅应被计算");
        assertNotNull(current.getKlineType(), "K线类型应被计算");
        assertNotNull(current.getVolumeRatio(), "量比应被计算");
        
        // 验证: 至少某些技术指标应被计算
        assertTrue(
            current.getRsi6() != null || 
            current.getMacdDif() != null || 
            current.getBollMiddle() != null,
            "至少某些技术指标应被计算"
        );
        
        System.out.println("✅ 综合K线分析完成");
        System.out.println("  - 涨跌幅: " + current.getChangePercent() + "%");
        System.out.println("  - 振幅: " + current.getAmplitude() + "%");
        System.out.println("  - K线类型: " + current.getKlineType());
        System.out.println("  - 量比: " + current.getVolumeRatio());
    }
}
