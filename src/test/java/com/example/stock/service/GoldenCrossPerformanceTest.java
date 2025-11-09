package com.example.stock.service;

import com.example.stock.dto.StockAnalysisDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 均线金叉性能测试类
 */
@SpringBootTest
@ActiveProfiles("test")
class GoldenCrossPerformanceTest {

    @Autowired
    private StockAnalysisService stockAnalysisService;

    @Test
    void testGoldenCrossOptimizationPerformance() {
        System.out.println("🧪 开始测试均线金叉优化性能...");

        // 测试新的优化方案
        long startTime = System.currentTimeMillis();
        List<StockAnalysisDTO> results = stockAnalysisService.getAllAnalysisResults();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("✅ 优化方案测试完成");
        System.out.println("📊 总结果数: " + results.size());
        System.out.println("⏱️  总耗时: " + duration + "ms");

        // 统计金叉股票数量
        long goldenCrossCount = results.stream().filter(r -> r.getMatchedCondition().contains("均线金叉")).count();
        System.out.println("📈 均线金叉股票数量: " + goldenCrossCount + " 只");

        // 验证结果
        assertNotNull(results, "结果不应为null");
        assertFalse(results.isEmpty(), "结果不应为空");

        System.out.println("✅ 均线金叉性能测试完成");
    }

    @Test
    void testGoldenCrossDetailedPerformance() {
        System.out.println("🧪 开始详细测试均线金叉优化性能...");

        // 单独测试金叉方法
        long startTime = System.currentTimeMillis();
        List<StockAnalysisDTO> results = ((StockAnalysisService) stockAnalysisService).findGoldenCrossStocks();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("✅ 金叉方法单独测试完成");
        System.out.println("📊 金叉股票数量: " + results.size());
        System.out.println("⏱️  耗时: " + duration + "ms");

        // 验证结果
        assertNotNull(results, "结果不应为null");

        if (!results.isEmpty()) {
            StockAnalysisDTO first = results.get(0);
            System.out.println("📋 首个金叉股票:");
            System.out.println("   股票代码: " + first.getSymbol());
            System.out.println("   当前价格: " + first.getCurrentPrice());
            System.out.println("   匹配条件: " + first.getMatchedCondition());
        }

        System.out.println("✅ 金叉详细性能测试完成");
    }
}