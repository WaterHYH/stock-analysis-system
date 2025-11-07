package com.example.stock.service;

import com.example.stock.dto.StockDTO;
import com.example.stock.entity.Stock;
import com.example.stock.repository.StockRepository;
import com.example.stock.service.client.SinaStockClient;
import com.example.stock.service.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 股票数据获取服务类
 * 提供从新浪财经API获取股票数据并保存到本地数据库的功能
 */
@Service
@RequiredArgsConstructor
public class StockDataFetchService {
    private static final Logger logger = LoggerFactory.getLogger(StockDataFetchService.class);
    private final SinaStockClient stockClient;
    private final StockRepository stockRepository;
    private final StockMapper stockMapper;

    /**
     * 获取并保存股票数据到数据库
     * @return 同步成功的记录数
     */
    @Transactional
    public int fetchAndSaveStockData() {
        try {
            StockDTO[] stocks = stockClient.fetchAllStocks();
            
            Arrays.stream(stocks)
                    .map(stockMapper::dtoToEntity)
                    .forEach(stockRepository::upsertStock);

            logger.info("成功同步 {} 条股票数据", stocks.length);
            return stocks.length;
        } catch (Exception e) {
            logger.error("股票数据同步失败", e);
            return -1;
        }
    }
}
