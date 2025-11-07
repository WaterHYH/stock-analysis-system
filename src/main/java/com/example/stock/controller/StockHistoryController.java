package com.example.stock.controller;

import com.example.stock.entity.StockHistory;
import com.example.stock.service.StockHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 股票历史数据Web页面控制器
 * 处理股票历史数据展示相关的页面请求
 */
@Controller
@RequestMapping("/stock-history")
@RequiredArgsConstructor
public class StockHistoryController {
    
    private final StockHistoryService stockHistoryService;

    /**
     * 股票历史数据页面
     * 支持按股票代码搜索和分页展示历史价格数据
     * @param symbol 股票代码搜索参数（可选）
     * @param page 页码（默认0）
     * @param size 每页大小（默认30）
     * @param model 视图模型
     * @return 股票历史数据页面视图名称
     */
    @GetMapping
    public String listHistory(
            @RequestParam(value = "symbol", required = false) String symbol,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            Model model
    ) {
        // 创建分页请求，按交易日期降序排列
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "day"));
        
        Page<StockHistory> historyData = stockHistoryService.findHistoryBySymbol(symbol, pageable);
        model.addAttribute("historyData", historyData);
        model.addAttribute("symbol", symbol);
        return "stocks/history_price";
    }
}