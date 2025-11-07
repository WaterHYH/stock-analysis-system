package com.example.stock.service;

import com.example.stock.entity.StockHistory;
import com.example.stock.repository.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 股票历史数据服务类
 * 提供股票历史数据相关的业务逻辑处理
 */
@Service
@RequiredArgsConstructor
public class StockHistoryService {
    
    private final StockHistoryRepository stockHistoryRepository;
    
    /**
     * 根据股票代码分页查询历史数据
     * @param symbol 股票代码（可选）
     * @param pageable 分页参数
     * @return 股票历史数据分页结果
     */
    public Page<StockHistory> findHistoryBySymbol(String symbol, Pageable pageable) {
        if (symbol == null || symbol.trim().isEmpty()) {
            // 如果没有提供股票代码，则返回所有历史数据
            return stockHistoryRepository.findAll(pageable);
        } else {
            // 根据股票代码模糊查询历史数据
            return stockHistoryRepository.findBySymbolContaining(symbol, pageable);
        }
    }
}