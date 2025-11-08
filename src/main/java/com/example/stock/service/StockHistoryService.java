package com.example.stock.service;

import com.example.stock.entity.StockHistory;
import com.example.stock.repository.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 股票历史数据服务类
 * 提供股票历史数据相关的业务逻辑处理
 */
@Service
@RequiredArgsConstructor
public class StockHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockHistoryService.class);
    
    private final StockHistoryRepository stockHistoryRepository;
    
    /**
     * 根据股票代码分页查询历史数据（优化版本）
     * @param symbol 股票代码（可选）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 股票历史数据分页结果
     */
    public Page<StockHistory> findHistoryBySymbol(String symbol, int page, int size) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 创建分页请求，按交易日期降序排列
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "day"));
            
            Page<StockHistory> result;
            if (symbol == null || symbol.trim().isEmpty()) {
                // 如果没有提供股票代码，则返回所有历史数据（使用无count查询优化）
                logger.info("开始查询所有股票历史数据（优化版），分页参数: page={}, size={}", page, size);
                result = stockHistoryRepository.findAllWithoutCount(pageable);
            } else {
                // 根据股票代码模糊查询历史数据（使用无count查询优化）
                logger.info("开始查询股票 {} 的历史数据（优化版），分页参数: page={}, size={}", symbol, page, size);
                result = stockHistoryRepository.findBySymbolContainingWithoutCount(symbol, pageable);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("查询完成，返回 {} 条记录，总耗时: {}ms", result.getContent().size(), duration);
            return result;
        } catch (Exception e) {
            logger.error("查询股票历史数据时发生错误: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 根据股票代码分页查询历史数据（兼容旧版本）
     * @param symbol 股票代码（可选）
     * @param pageable 分页参数
     * @return 股票历史数据分页结果
     */
    public Page<StockHistory> findHistoryBySymbol(String symbol, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        Page<StockHistory> result;
        
        try {
            if (symbol == null || symbol.trim().isEmpty()) {
                // 如果没有提供股票代码，则返回所有历史数据
                logger.info("开始查询所有股票历史数据，分页参数: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
                result = stockHistoryRepository.findAll(pageable);
            } else {
                // 根据股票代码模糊查询历史数据
                logger.info("开始查询股票 {} 的历史数据，分页参数: page={}, size={}", symbol, pageable.getPageNumber(), pageable.getPageSize());
                result = stockHistoryRepository.findBySymbolContaining(symbol, pageable);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("查询完成，返回 {} 条记录，总耗时: {}ms", result.getContent().size(), duration);
            return result;
        } catch (Exception e) {
            logger.error("查询股票历史数据时发生错误: {}", e.getMessage(), e);
            throw e;
        }
    }
}