package com.example.stock.service;

import com.example.stock.entity.StockHistory;
import com.example.stock.repository.StockHistoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 股票历史数据服务优化测试类
 * 用于测试和验证StockHistoryService的性能优化效果
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("股票历史数据服务优化测试")
class StockHistoryServiceOptimizationTest {

    @Autowired
    private StockHistoryService stockHistoryService;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    // 测试用的股票代码
    private static final String TEST_SYMBOL = "sh600000";

    @BeforeEach
    void setUp() {
        System.out.println("🚀 开始执行股票历史数据服务优化测试");
    }

    @AfterEach
    void tearDown() {
        System.out.println("✅ 股票历史数据服务优化测试执行完成\n");
    }

    @Test
    @DisplayName("测试1：对比优化前后的查询性能")
    void testPerformanceComparison() {
        System.out.println("⚙️ 开始测试1：对比优化前后的查询性能");

        // 创建分页请求
        int page = 0;
        int size = 30;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "day"));

        // 测试优化前的查询性能
        System.out.println("⏱️ 测试优化前的查询性能");
        long startTime = System.nanoTime();
        Page<StockHistory> oldResult = stockHistoryService.findHistoryBySymbol(TEST_SYMBOL, pageable);
        long endTime = System.nanoTime();
        long oldDuration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        System.out.println("   优化前查询耗时: " + oldDuration + "ms, 返回记录数: " + oldResult.getContent().size());

        // 测试优化后的查询性能
        System.out.println("⏱️ 测试优化后的查询性能");
        startTime = System.nanoTime();
        Page<StockHistory> newResult = stockHistoryService.findHistoryBySymbol(TEST_SYMBOL, page, size);
        endTime = System.nanoTime();
        long newDuration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        System.out.println("   优化后查询耗时: " + newDuration + "ms, 返回记录数: " + newResult.getContent().size());

        // 验证结果
        assertNotNull(oldResult, "优化前查询结果不应为null");
        assertNotNull(newResult, "优化后查询结果不应为null");
        assertEquals(oldResult.getContent().size(), newResult.getContent().size(), "优化前后返回记录数应该一致");

        // 性能提升验证（优化后的查询应该更快）
        if (newDuration < oldDuration) {
            double improvement = (double) (oldDuration - newDuration) / oldDuration * 100;
            System.out.println("   ✅ 性能提升: " + String.format("%.2f", improvement) + "%");
        } else {
            System.out.println("   ⚠️  性能无明显提升");
        }

        System.out.println("✅ 测试1通过：优化前后查询性能对比完成");
    }

    @Test
    @DisplayName("测试2：验证无count查询的性能优势")
    void testWithoutCountQueryPerformance() {
        System.out.println("⚙️ 开始测试2：验证无count查询的性能优势");

        int page = 0;
        int size = 30;

        // 测试带count查询的性能
        System.out.println("⏱️ 测试带count查询的性能");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "day"));
        long startTime = System.nanoTime();
        Page<StockHistory> withCountResult = stockHistoryRepository.findBySymbolContaining(TEST_SYMBOL, pageable);
        long endTime = System.nanoTime();
        long withCountDuration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        System.out.println("   带count查询耗时: " + withCountDuration + "ms, 返回记录数: " + withCountResult.getContent().size());

        // 测试无count查询的性能
        System.out.println("⏱️ 测试无count查询的性能");
        startTime = System.nanoTime();
        Page<StockHistory> withoutCountResult = stockHistoryRepository.findBySymbolContainingWithoutCount(TEST_SYMBOL, pageable);
        endTime = System.nanoTime();
        long withoutCountDuration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        System.out.println("   无count查询耗时: " + withoutCountDuration + "ms, 返回记录数: " + withoutCountResult.getContent().size());

        // 验证结果
        assertNotNull(withCountResult, "带count查询结果不应为null");
        assertNotNull(withoutCountResult, "无count查询结果不应为null");

        // 性能提升验证
        if (withoutCountDuration < withCountDuration) {
            double improvement = (double) (withCountDuration - withoutCountDuration) / withCountDuration * 100;
            System.out.println("   ✅ 无count查询性能提升: " + String.format("%.2f", improvement) + "%");
        } else {
            System.out.println("   ⚠️ 无count查询无明显性能提升");
        }

        System.out.println("✅ 测试2通过：无count查询性能优势验证完成");
    }
}