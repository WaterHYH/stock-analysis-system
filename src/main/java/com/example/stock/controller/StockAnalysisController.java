package com.example.stock.controller;

import com.example.stock.dto.StockAnalysisDTO;
import com.example.stock.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票分析Web页面控制器
 * 提供股票分析筛选功能
 */
@Controller
@RequestMapping("/stock-analysis")
@RequiredArgsConstructor
public class StockAnalysisController {
    
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
     * @param model 视图模型
     * @return 股票分析页面视图名称
     */
    @GetMapping("/search")
    public String searchStocks(
            @RequestParam(value = "conditions", required = false) List<String> conditions,
            @RequestParam(value = "startDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "dropPercentage", required = false) Double dropPercentage,
            Model model
    ) {
        if (conditions == null || conditions.isEmpty()) {
            model.addAttribute("message", "请至少选择一个筛选条件");
            return "stocks/analysis";
        }

        List<StockAnalysisDTO> results = stockAnalysisService.analyzeStocks(conditions, startDate, dropPercentage);
        
        model.addAttribute("results", results);
        model.addAttribute("selectedConditions", conditions);
        model.addAttribute("resultCount", results.size());
        model.addAttribute("startDate", startDate);
        model.addAttribute("dropPercentage", dropPercentage);
        
        return "stocks/analysis";
    }
}
