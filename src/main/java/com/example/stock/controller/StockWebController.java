package com.example.stock.controller;

import com.example.stock.entity.Stock;
import com.example.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 股票Web页面控制器
 * 处理股票数据展示相关的页面请求
 */
@Controller
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockWebController {
    private final StockQueryService stockQueryService;

    /**
     * 股票列表页面
     * 支持按股票代码搜索和分页展示
     * @param symbol 股票代码搜索参数（可选）
     * @param page 页码（默认0）
     * @param size 每页大小（默认30）
     * @param model 视图模型
     * @return 股票列表页面视图名称
     */
    @GetMapping
    public String listStocks(
            @RequestParam(value = "symbol", required = false) String symbol,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            Model model
    ) {
        Page<Stock> stocks = stockQueryService.findStocks(symbol, page, size);
        model.addAttribute("stocks", stocks);
        model.addAttribute("symbol", symbol);
        return "stocks/list";
    }
}