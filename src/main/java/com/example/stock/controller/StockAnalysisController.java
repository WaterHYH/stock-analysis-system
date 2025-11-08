package com.example.stock.controller;

import com.example.stock.dto.StockAnalysisDTO;
import com.example.stock.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * 股票分析Web页面控制器
 * 提供股票分析筛选功能
 */
@Controller
@RequestMapping("/stock-analysis")
@RequiredArgsConstructor
public class StockAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(StockAnalysisController.class);
    
    private final StockAnalysisService stockAnalysisService;

    /**
     * 股票分析页面
     * @param model 视图模型
     * @return 股票分析页面视图名称
     */
    @GetMapping
    public String analysisPage(Model model) {
        return "stocks/analysis";
    }

    /**
     * 根据条件筛选股票
     * @param conditions 筛选条件（多选）
     * @param startDate 开始日期（可选，用于条件1）
     * @param dropPercentage 跌幅百分比（可选，用于条件1，默认25%）
     * @param goldenCrossDate 均线金叉日期（可选，用于条件6，默认当前日期）
     * @param model 视图模型
     * @return 股票分析页面视图名称
     */
    @GetMapping("/search")
    public String searchStocks(
            @RequestParam(value = "conditions", required = false) List<String> conditions,
            @RequestParam(value = "startDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "dropPercentage", required = false) Double dropPercentage,
            @RequestParam(value = "goldenCrossDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate goldenCrossDate,
            Model model
    ) {
        if (conditions == null || conditions.isEmpty()) {
            model.addAttribute("message", "请至少选择一个筛选条件");
            return "stocks/analysis";
        }

        List<StockAnalysisDTO> results = analyzeStocksWithParams(conditions, startDate, dropPercentage, goldenCrossDate);
        
        model.addAttribute("results", results);
        model.addAttribute("selectedConditions", conditions);
        model.addAttribute("resultCount", results.size());
        model.addAttribute("startDate", startDate);
        model.addAttribute("dropPercentage", dropPercentage);
        model.addAttribute("goldenCrossDate", goldenCrossDate);
        
        return "stocks/analysis";
    }
    
    /**
     * 根据条件筛选股票（带参数）
     * @param conditions 筛选条件（多选）
     * @param startDate 开始日期（可选，用于条件1）
     * @param dropPercentage 跌幅百分比（可选，用于条件1，默认25%）
     * @param goldenCrossDate 均线金叉日期（可选，用于条件6，默认当前日期）
     * @return 股票分析结果列表
     */
    private List<StockAnalysisDTO> analyzeStocksWithParams(List<String> conditions, LocalDate startDate, 
            Double dropPercentage, LocalDate goldenCrossDate) {
        long startTime = System.currentTimeMillis();
        logger.info("开始根据条件分析股票: {}, startDate: {}, dropPercentage: {}, goldenCrossDate: {}", 
                conditions, startDate, dropPercentage, goldenCrossDate);
        
        List<StockAnalysisDTO> allResults = new ArrayList<>();
        
        if (conditions == null || conditions.isEmpty()) {
            logger.info("条件列表为空，返回空结果");
            return allResults;
        }
        
        // 条件1: 跌幅超过指定百分比的股票
        if (conditions.contains("below_75_percent")) {
            // 只处理条件1，不处理其他条件
            List<String> condition1Only = Collections.singletonList("below_75_percent");
            allResults.addAll(stockAnalysisService.analyzeStocks(condition1Only, startDate, dropPercentage));
        }
        
        // 条件2-5: 其他条件保持不变
        List<String> otherConditions = new ArrayList<>();
        for (String condition : conditions) {
            if (!condition.equals("below_75_percent") && !condition.equals("ma_golden_cross")) {
                otherConditions.add(condition);
            }
        }
        
        if (!otherConditions.isEmpty()) {
            allResults.addAll(stockAnalysisService.analyzeStocks(otherConditions));
        }
        
        // 条件6: 均线金叉（5日均线上穿10日均线）
        if (conditions.contains("ma_golden_cross")) {
            LocalDate dateToUse = goldenCrossDate != null ? goldenCrossDate : LocalDate.now();
            allResults.addAll(stockAnalysisService.findGoldenCrossStocksWithDate(dateToUse));
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("条件分析完成，共找到 {} 只符合条件的股票，总耗时 {}ms", allResults.size(), totalTime);
        
        return allResults;
    }
}