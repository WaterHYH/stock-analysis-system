package com.example.stock.service;

import com.example.stock.dto.StockHistoryDTO;
import com.example.stock.entity.StockHistory;
import com.example.stock.repository.StockHistoryRepository;
import com.example.stock.service.client.SinaStockClient;
import com.example.stock.service.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 股票历史数据获取服务类
 * 提供股票历史数据的获取和保存功能
 */
@Service
@RequiredArgsConstructor
public class StockHistoryFetchService {
    private static final Logger logger = LoggerFactory.getLogger(StockHistoryFetchService.class);
    private final SinaStockClient stockClient;
    private final StockHistoryRepository stockHistoryRepository;
    private final StockMapper stockMapper;

    /**
     * 批量获取所有股票历史数据
     * 遍历沪市和深市的所有股票代码，获取其历史数据
     */
    public void fetchAllStockHistory() {
        // 遍历沪市主板（60开头）
        for (int code = 600000; code <= 609999; code++) {
            processStock(code);
        }
        // 遍历科创板（688开头）
        for (int code = 688000; code <= 688999; code++) {
            processStock(code);
        }
        // 遍历深市主板、中小板、创业板
        int[] szPrefixes = {0, 1, 2, 300};
        for (int prefix : szPrefixes) {
            int start = prefix * 1000;
            int end = start + 999;
            for (int code = start; code <= end; code++) {
                processStock(code);
            }
        }
    }

    /**
     * 处理单个股票代码
     * 生成股票symbol并获取保存其历史数据
     * @param code 股票代码
     */
    private void processStock(int code) {
        String symbol = generateSymbol(code);
        if (symbol != null) {
            fetchAndSaveHistory(symbol, symbol.substring(2));
            try {
                Thread.sleep(1000); // 延时1秒，避免触发API限制
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("历史数据获取被中断", e);
            }
        }
    }

    /**
     * 获取并保存单个股票的历史数据
     * @param symbol 股票Symbol（例如 "sh600000"）
     * @param code 股票Code（例如 "600000"）
     */
    public void fetchAndSaveHistory(String symbol, String code) {
        if (!StringUtils.hasText(symbol) || !StringUtils.hasText(code)) {
            logger.error("Symbol或Code不能为空");
            return;
        }

        logger.info("开始获取股票历史数据: symbol={}, code={}", symbol, code);
        long totalStartTime = System.currentTimeMillis();
        
        // 1. 获取数据阶段
        long fetchStartTime = System.currentTimeMillis();
        List<StockHistoryDTO> historyList = stockClient.getStockHistory(symbol, code);
        long fetchDuration = System.currentTimeMillis() - fetchStartTime;
        logger.info("⏱️ 获取数据耗时: {}ms, symbol={}, code={}", fetchDuration, symbol, code);

        if (historyList == null || historyList.isEmpty()) {
            logger.info("未获取到股票历史数据: symbol={}, code={}", symbol, code);
            return;
        }

        // 2. 数据转换阶段
        long mapStartTime = System.currentTimeMillis();
        List<StockHistory> entities = stockMapper.toStockHistoryList(historyList);
        long mapDuration = System.currentTimeMillis() - mapStartTime;
        logger.info("⏱️ 数据转换耗时: {}ms, 记录数={}", mapDuration, entities.size());

        // 3. 批量插入阶段
        long insertStartTime = System.currentTimeMillis();
        int[] result = stockHistoryRepository.batchInsertStockHistory(entities);
        long insertDuration = System.currentTimeMillis() - insertStartTime;
        logger.info("⏱️ 批量插入耗时: {}ms, 记录数={}", insertDuration, result.length);

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        logger.info("✅ 成功保存股票历史数据: symbol={}, code={}, count={}, 总耗时={}ms (获取:{}ms, 转换:{}ms, 插入:{}ms)", 
                symbol, code, result.length, totalDuration, fetchDuration, mapDuration, insertDuration);
    }

    /**
     * 根据股票代码生成股票symbol
     * @param code 股票代码
     * @return 股票symbol（如sh600000或sz000001）
     */
    private String generateSymbol(int code) {
        String paddedCode = String.format("%06d", code);
        if (paddedCode.startsWith("60") || paddedCode.startsWith("688")) {
            return "sh" + paddedCode;
        } else if (paddedCode.startsWith("000") || paddedCode.startsWith("001")
                || paddedCode.startsWith("002") || paddedCode.startsWith("300")) {
            return "sz" + paddedCode;
        } else {
            return null;
        }
    }
}
