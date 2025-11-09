package com.example.stock;

import com.example.stock.service.StockAnalysisService;
import com.example.stock.dto.StockAnalysisDTO;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
public class PerformanceTest {

    public static void main(String[] args) {
        // 启动Spring Boot应用上下文
        ConfigurableApplicationContext context = SpringApplication.run(PerformanceTest.class, args);
        
        // 获取StockAnalysisService bean
        StockAnalysisService stockAnalysisService = context.getBean(StockAnalysisService.class);
        
        // 测试findGoldenCrossStocksWithDateInternal方法的性能
        testGoldenCrossPerformance(stockAnalysisService);
        
        // 关闭应用上下文
        context.close();
    }
    
    private static void testGoldenCrossPerformance(StockAnalysisService service) {
        System.out.println("开始测试均线金叉方法性能...");
        
        // 使用最近的交易日进行测试
        LocalDate testDate = LocalDate.of(2025, 5, 23); // 假设这是一个交易日
        
        // 多次运行以获得更准确的性能数据
        for (int i = 0; i < 3; i++) {
            System.out.println("\n第 " + (i + 1) + " 次测试:");
            
            long startTime = System.currentTimeMillis();
            // 直接调用带有日期参数的方法来测试性能
            List<StockAnalysisDTO> results = service.findGoldenCrossStocksWithDate(testDate);
            long endTime = System.currentTimeMillis();
            
            long duration = endTime - startTime;
            System.out.println("  找到 " + results.size() + " 只金叉股票");
            System.out.println("  耗时: " + duration + " ms");
            
            // 显示前几个结果作为示例
            if (!results.isEmpty()) {
                System.out.println("  前3只金叉股票:");
                results.stream().limit(3).forEach(dto -> 
                    System.out.println("    " + dto.getSymbol() + " - " + dto.getMatchedCondition())
                );
            }
        }
        
        System.out.println("\n性能测试完成!");
    }
}