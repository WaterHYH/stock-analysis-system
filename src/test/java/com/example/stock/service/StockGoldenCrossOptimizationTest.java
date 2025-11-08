package com.example.stock.service;

import com.example.stock.dto.StockAnalysisDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 均线金叉优化方案测试类
 */
@SpringBootTest
@ActiveProfiles("test")
class StockGoldenCrossOptimizationTest {

    @Autowired
    private StockAnalysisService stockAnalysisService;

    @Test
    void testGoldenCrossOptimization() {
        System.out.println("🧪 开始测试均线金叉优化方案...");

        // 测试新的优化方案
        long startTime = System.nanoTime();
        List<StockAnalysisDTO> results = stockAnalysisService.getAllAnalysisResults();
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // 转换为毫秒

        System.out.println("✅ 优化方案测试完成");
        System.out.println("📊 总结果数: " + results.size());
        System.out.println("⏱️  总耗时: " + duration + "ms");

        // 统计各类条件的结果数
        long condition1 = results.stream().filter(r -> r.getMatchedCondition().contains("低于历史最高值")).count();
        long condition2 = results.stream().filter(r -> r.getMatchedCondition().contains("高波动且处于低位")).count();
        long condition3 = results.stream().filter(r -> r.getMatchedCondition().contains("连续上涨")).count();
        long condition4 = results.stream().filter(r -> r.getMatchedCondition().contains("接近年度最高点")).count();
        long condition5 = results.stream().filter(r -> r.getMatchedCondition().contains("成交量激增")).count();
        long condition6 = results.stream().filter(r -> r.getMatchedCondition().contains("均线金叉")).count();

        System.out.println("📈 各条件结果统计:");
        System.out.println("   条件1 (跌幅超25%): " + condition1 + " 只");
        System.out.println("   条件2 (高波动低位): " + condition2 + " 只");
        System.out.println("   条件3 (连续上涨): " + condition3 + " 只");
        System.out.println("   条件4 (接近年度高点): " + condition4 + " 只");
        System.out.println("   条件5 (成交量激增): " + condition5 + " 只");
        System.out.println("   条件6 (均线金叉): " + condition6 + " 只");

        // 验证结果不为空
        assertNotNull(results, "结果不应为null");
        assertFalse(results.isEmpty(), "结果不应为空");

        // 验证至少有一个金叉股票
        assertTrue(condition6 > 0, "应该至少找到一个金叉股票");

        // 验证金叉股票信息完整
        StockAnalysisDTO goldenCrossStock = results.stream()
                .filter(r -> r.getMatchedCondition().contains("均线金叉"))
                .findFirst()
                .orElse(null);

        assertNotNull(goldenCrossStock, "应该找到至少一个金叉股票");
        assertNotNull(goldenCrossStock.getSymbol(), "股票代码不应为null");
        assertNotNull(goldenCrossStock.getName(), "股票名称不应为null");
        assertTrue(goldenCrossStock.getCurrentPrice() > 0, "当前价格应大于0");

        System.out.println("📋 金叉股票示例:");
        System.out.println("   股票代码: " + goldenCrossStock.getSymbol());
        System.out.println("   股票名称: " + goldenCrossStock.getName());
        System.out.println("   当前价格: " + goldenCrossStock.getCurrentPrice());
        System.out.println("   匹配条件: " + goldenCrossStock.getMatchedCondition());

        System.out.println("✅ 均线金叉优化方案测试通过");
    }
}