package com.example.stock.service;

import com.example.stock.entity.Stock;
import com.example.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 股票数据查询服务类
 * 提供股票数据的查询业务逻辑
 */
@Service
@RequiredArgsConstructor
public class StockQueryService {
    private final StockRepository stockRepository;

    /**
     * 分页查询股票数据（支持按代码搜索）
     * @param symbol 股票代码搜索参数（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 股票数据分页结果
     */
    public Page<Stock> findStocks(String symbol, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (symbol == null || symbol.isEmpty()) {
            return stockRepository.findAll(pageable);
        } else {
            return stockRepository.findBySymbolContaining(symbol, pageable);
        }
    }
}
