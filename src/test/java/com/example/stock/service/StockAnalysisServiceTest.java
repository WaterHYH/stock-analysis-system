package com.example.stock.service;

import com.example.stock.dto.StockAnalysisDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 股票分析服务测试类
 * 基于真实数据库数据测试各个筛选条件的功能
 */
@SpringBootTest
class StockAnalysisServiceTest {

    @Autowired
    private StockAnalysisService stockAnalysisService;

    /**
     * 测试条件1: 筛选低于历史最高值75%以上的股票
     * 基于真实数据库数据测试
     */
    @Test
    void testFindStocksBelowHistoricalHigh() {
        System.out.println("\n📊 测试条件1: 超跌潜力股筛选");
        
        // 执行筛选
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(List.of("below_75_percent"));
        
        // 验证结果
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 找到 " + results.size() + " 只超跌潜力股");
        
        // 打印前10条结果
        results.stream().limit(10).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | 历史最高 %.2f | 跌幅: %.2f%%",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), 
                    r.getHistoricalHigh(), r.getDropPercentage()));
        });
        
        // 验证数据合理性
        if (!results.isEmpty()) {
            StockAnalysisDTO firstResult = results.get(0);
            assertNotNull(firstResult.getSymbol(), "股票代码不应为null");
            assertNotNull(firstResult.getCurrentPrice(), "当前价格不应为null");
            assertNotNull(firstResult.getHistoricalHigh(), "历史最高价不应为null");
            assertTrue(firstResult.getDropPercentage() >= 25.0, "跌幅应大于等于25%");
            assertEquals("低于历史最高值75.0%以上", firstResult.getMatchedCondition());
        }
    }

    /**
     * 测试条件1: 筛选低于历史最高值指定百分比的股票（带参数）
     * 基于真实数据库数据测试
     * 
     * @Test 参数说明:
     * - startDate: 开始日期，格式为YYYY-MM-DD（可选，默认为全部历史数据）
     * - dropPercentage: 跌幅百分比，范围 1-99（默认为25%）
     */
    @Test
    void testFindStocksBelowHistoricalHighWithParams() {
        System.out.println("\n📊 测试条件1 (参数化): 超跌潜力股筛选- 自定义参数");
        
        // 测试用例1：跌幅>=30%从2025年初以来
        System.out.println("\n📌 测试用例1: 跌幅>=30%从2025-01-01以来");
        java.time.LocalDate startDate1 = java.time.LocalDate.parse("2025-01-01");
        Double dropPercentage1 = 30.0;
        List<StockAnalysisDTO> results1 = stockAnalysisService.analyzeStocks(
            List.of("below_75_percent"), startDate1, dropPercentage1
        );
        
        assertNotNull(results1, "结果不应为null");
        System.out.println("✅ 找到 " + results1.size() + " 只跌幅>=30%的超跌潜力股");
        
        // 打印前5条结果
        results1.stream().limit(5).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | 历史最高 %.2f | 跌幅: %.2f%%",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), 
                    r.getHistoricalHigh(), r.getDropPercentage()));
        });
        
        // 验证数据合理性
        if (!results1.isEmpty()) {
            StockAnalysisDTO firstResult = results1.get(0);
            assertTrue(firstResult.getDropPercentage() >= 30.0, "跌幅应大于等于30%");
            assertTrue(firstResult.getMatchedCondition().contains("30.0%"), "条件描述应包含30.0%");
        }
        
        // 测试用例2：跌幅>=20%，最近半年
        System.out.println("\n📌 测试用例2: 跌幅>=20%从2024-05-01以来");
        java.time.LocalDate startDate2 = java.time.LocalDate.parse("2024-05-01");
        Double dropPercentage2 = 20.0;
        List<StockAnalysisDTO> results2 = stockAnalysisService.analyzeStocks(
            List.of("below_75_percent"), startDate2, dropPercentage2
        );
        
        assertNotNull(results2, "结果不应为null");
        System.out.println("✅ 找到 " + results2.size() + " 只跌幅>=20%的超跌潜力股");
        
        // 打印前5条结果
        results2.stream().limit(5).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | 历史最高 %.2f | 跌幅: %.2f%%",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), 
                    r.getHistoricalHigh(), r.getDropPercentage()));
        });
        
        // 验证数据合理性
        if (!results2.isEmpty()) {
            StockAnalysisDTO firstResult = results2.get(0);
            assertTrue(firstResult.getDropPercentage() >= 20.0, "跌幅应大于等于20%");
        }
        
        // 测试用例3：跌幅>=50%，全部历史数据（不指定开始日期）
        System.out.println("\n📌 测试用例3: 跌幅>=50%，全部历史数据");
        List<StockAnalysisDTO> results3 = stockAnalysisService.analyzeStocks(
            List.of("below_75_percent"), null, 50.0
        );
        
        assertNotNull(results3, "结果不应为null");
        System.out.println("✅ 找到 " + results3.size() + " 只跌幅>=50%的超跌潜力股");
        
        // 打印前5条结果
        results3.stream().limit(5).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | 历史最高 %.2f | 跌幅: %.2f%%",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), 
                    r.getHistoricalHigh(), r.getDropPercentage()));
        });
        
        // 验证数据合理性
        if (!results3.isEmpty()) {
            StockAnalysisDTO firstResult = results3.get(0);
            assertTrue(firstResult.getDropPercentage() >= 50.0, "跌幅应大于等于50%");
        }
        
        // 比较结果
        System.out.println("\n📊 参数对比总结:");
        System.out.println("   - 跌幅30%, 2025年初: " + results1.size() + " 只股票");
        System.out.println("   - 跌幅20%, 2024年中: " + results2.size() + " 只股票");
        System.out.println("   - 跌幅50%, 全部数据: " + results3.size() + " 只股票");
        System.out.println("   - 跌幅越大，股票数量越少");
    }

    /**
     * 测试条件2: 筛选高波动低位股
     * 基于真实数据库数据测试
     */
    @Test
    void testFindHighVolatilityLowPriceStocks() {
        System.out.println("\n📊 测试条件2: 高波动低位股筛选");
        
        // 执行筛选
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(List.of("high_volatility_low_price"));
        
        // 验证结果
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 找到 " + results.size() + " 只高波动低位股");
        
        // 打印前10条结果
        results.stream().limit(10).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | 波动次数: %d | 跌幅: %.2f%%",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), 
                    r.getVolatilityCount(), r.getDropPercentage()));
        });
        
        // 验证数据合理性
        if (!results.isEmpty()) {
            StockAnalysisDTO firstResult = results.get(0);
            assertNotNull(firstResult.getVolatilityCount(), "波动次数不应为null");
            assertTrue(firstResult.getVolatilityCount() >= 3, "波动次数应大于等于3");
            assertTrue(firstResult.getMatchedCondition().contains("高波动"), "条件描述应包含高波动");
        }
    }

    /**
     * 测试条件3: 筛选连续上涨股
     * 基于真实数据库数据测试
     */
    @Test
    void testFindContinuousRiseStocks() {
        System.out.println("\n📊 测试条件3: 强势上涨股筛选");
        
        // 执行筛选
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(List.of("continuous_rise"));
        
        // 验证结果
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 找到 " + results.size() + " 只强势上涨股");
        
        // 打印前10条结果
        results.stream().limit(10).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | %s",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), r.getMatchedCondition()));
        });
        
        // 验证数据合理性
        if (!results.isEmpty()) {
            StockAnalysisDTO firstResult = results.get(0);
            assertTrue(firstResult.getMatchedCondition().contains("连续上涨"), "条件描述应包含连续上涨");
        }
    }

    /**
     * 测试条件4: 筛选接近年度最高点的股票
     * 基于真实数据库数据测试
     */
    @Test
    void testFindNearYearHighStocks() {
        System.out.println("\n📊 测试条件4: 创新高潜力股筛选");
        
        // 执行筛选
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(List.of("near_year_high"));
        
        // 验证结果
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 找到 " + results.size() + " 只创新高潜力股");
        
        // 打印前10条结果
        results.stream().limit(10).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | 历史最高 %.2f | 距最高 %.2f%%",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), 
                    r.getHistoricalHigh(), r.getDropPercentage()));
        });
        
        // 验证数据合理性
        if (!results.isEmpty()) {
            StockAnalysisDTO firstResult = results.get(0);
            assertNotNull(firstResult.getDropPercentage(), "跌幅数据不应为null");
            assertTrue(firstResult.getDropPercentage() <= 5.0, "距离最高价应在5%以内");
            assertTrue(firstResult.getMatchedCondition().contains("接近年度最高点"), "条件描述应包含接近年度最高点");
        }
    }

    /**
     * 测试条件5: 筛选成交量激增的股票
     * 基于真实数据库数据测试
     */
    @Test
    void testFindVolumeSurgeStocks() {
        System.out.println("\n📊 测试条件5: 成交量爆发股筛选");
        
        // 执行筛选
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(List.of("volume_surge"));
        
        // 验证结果
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 找到 " + results.size() + " 只成交量爆发股");
        
        // 打印前10条结果
        results.stream().limit(10).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | %s",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), r.getMatchedCondition()));
        });
        
        // 验证数据合理性
        if (!results.isEmpty()) {
            StockAnalysisDTO firstResult = results.get(0);
            assertTrue(firstResult.getMatchedCondition().contains("成交量激增"), "条件描述应包含成交量激增");
        }
    }

    /**
     * 测试条件6: 筛选均线金叉的股票
     * 基于真实数据库数据测试
     */
    @Test
    void testFindGoldenCrossStocks() {
        System.out.println("\n📊 测试条件6: 均线金叉股筛选");
        
        long startTime = System.currentTimeMillis();
        long methodStartTime = startTime;
        
        System.out.println("⏱️  开始时间: " + startTime);
        
        // 执行筛选
        long beforeCall = System.currentTimeMillis();
        System.out.println("⏱️  调用服务前: " + beforeCall + " (距离开始: " + (beforeCall - startTime) + "ms)");
        
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(List.of("ma_golden_cross"));
        
        long afterCall = System.currentTimeMillis();
        long callTime = afterCall - beforeCall;
        System.out.println("⏱️  调用服务后: " + afterCall + " (服务耗时: " + callTime + "ms)");
        
        // 验证结果
        long beforeAssert = System.currentTimeMillis();
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 找到 " + results.size() + " 只均线金叉股");
        long afterAssert = System.currentTimeMillis();
        System.out.println("⏱️  验证结果耗时: " + (afterAssert - beforeAssert) + "ms");
        
        // 打印前10条结果
        long beforePrint = System.currentTimeMillis();
        results.stream().limit(10).forEach(r -> {
            System.out.println(String.format("   - %s %s | 当前价 %.2f | %s",
                    r.getSymbol(), r.getSymbol(), r.getCurrentPrice(), r.getMatchedCondition()));
        });
        long afterPrint = System.currentTimeMillis();
        System.out.println("⏱️  打印结果耗时: " + (afterPrint - beforePrint) + "ms");
        
        // 验证数据合理性
        long beforeValidate = System.currentTimeMillis();
        if (!results.isEmpty()) {
            StockAnalysisDTO firstResult = results.get(0);
            assertTrue(firstResult.getMatchedCondition().contains("均线金叉"), "条件描述应包含均线金叉");
        }
        long afterValidate = System.currentTimeMillis();
        System.out.println("⏱️  数据验证耗时: " + (afterValidate - beforeValidate) + "ms");
        
        long totalTime = System.currentTimeMillis() - methodStartTime;
        System.out.println("⏱️  总耗时: " + totalTime + "ms");
        System.out.println("   └─ 服务调用: " + callTime + "ms");
        System.out.println("   └─ 验证结果: " + (afterAssert - beforeAssert) + "ms");
        System.out.println("   └─ 打印输出: " + (afterPrint - beforePrint) + "ms");
        System.out.println("   └─ 数据验证: " + (afterValidate - beforeValidate) + "ms");
    }

    /**
     * 测试多条件组合筛选
     * 基于真实数据库数据测试
     */
    @Test
    void testMultipleConditions() {
        System.out.println("\n📊 测试多条件组合筛选");
        
        // 执行多条件筛选
        List<String> conditions = List.of("below_75_percent", "continuous_rise", "volume_surge");
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(conditions);
        
        // 验证结果
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 多条件筛选找到 " + results.size() + " 只股票");
        
        // 打印前10条结果
        results.stream().limit(10).forEach(r -> {
            System.out.println(String.format("   - %s %s | %s",
                    r.getSymbol(), r.getSymbol(), r.getMatchedCondition()));
        });
    }

    /**
     * 测试空条件处理
     */
    @Test
    void testEmptyConditions() {
        System.out.println("\n📊 测试空条件处理");
        
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(new ArrayList<>());
        
        assertNotNull(results, "结果不应为null");
        assertTrue(results.isEmpty(), "空条件应返回空结果");
        
        System.out.println("✅ 空条件处理正确，返回空列表");
    }

    /**
     * 测试无效条件处理
     */
    @Test
    void testInvalidCondition() {
        System.out.println("\n📊 测试无效条件处理");
        
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(List.of("invalid_condition"));
        
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 无效条件处理正确，返回 " + results.size() + " 条结果");
    }

    /**
     * 测试全部条件组合
     */
    @Test
    void testAllConditions() {
        System.out.println("\n📊 测试全部6个条件组合筛选");
        
        List<String> allConditions = List.of(
            "below_75_percent",
            "high_volatility_low_price",
            "continuous_rise",
            "near_year_high",
            "volume_surge",
            "ma_golden_cross"
        );
        
        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(allConditions);
        
        assertNotNull(results, "结果不应为null");
        System.out.println("✅ 全部条件筛选找到 " + results.size() + " 只股票");
        
        // 按条件分组统计
        results.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                StockAnalysisDTO::getMatchedCondition,
                java.util.stream.Collectors.counting()))
            .forEach((condition, count) -> 
                System.out.println("   - " + condition + ": " + count + " 只股票"));
    }
}