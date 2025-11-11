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
    private final KLineAnalysisService kLineAnalysisService;

    /**
     * 批量获取所有A股股票历史数据
     * 覆盖沪市（A股）、深市、北交所的所有股票代码
     * 
     * 沪市（A股）:
     *   - 600-605: 主板 (600000-605999)
     *   - 607: 新增股票 (607000-607999)
     *   - 608: 新增股票 (608000-608999)
     *   - 609: 新增股票 (609000-609999)
     *   - 688: 科创板 (688000-688999)
     * 
     * 深市:
     *   - 000-003: 主板 (000000-003999)
     *   - 100-103: 中小板 (100000-103999)
     *   - 300: 创业板 (300000-300999)
     *   - 004-009: 创业板 (004000-009999)
     * 
     * 北交所:
     *   - 83, 87, 88, 89: 北交所股票 (830000-899999)
     */
    public void fetchAllStockHistory() {
        logger.info("开始批量获取所有A股股票历史数据...");
        
        // 沪市主板 (600-605)
        logger.info("正在获取沪市主板股票数据 (600-605)...");
        for (int code = 600000; code <= 605999; code++) {
            processStock(code);
        }
        
        // 沪市新增号段 (607-609)
        logger.info("正在获取沪市新增号段股票数据 (607-609)...");
        for (int code = 607000; code <= 609999; code++) {
            processStock(code);
        }
        
        // 沪市科创板 (688)
        logger.info("正在获取沪市科创板股票数据 (688)...");
        for (int code = 688000; code <= 688999; code++) {
            processStock(code);
        }
        
        // 深市主板 (000-003)
        logger.info("正在获取深市主板股票数据 (000-003)...");
        for (int code = 0; code <= 3999; code++) {
            processStock(code);
        }
        
        // 深市中小板 (100-103)
        logger.info("正在获取深市中小板股票数据 (100-103)...");
        for (int code = 100000; code <= 103999; code++) {
            processStock(code);
        }
        
        // 深市创业板 (300, 004-009)
        logger.info("正在获取深市创业板股票数据 (300, 004-009)...");
        for (int code = 300000; code <= 399999; code++) {
            processStock(code);
        }
        for (int code = 4000; code <= 99999; code++) {
            processStock(code);
        }
        
        // 北交所 (83, 87, 88, 89)
        logger.info("正在获取北交所股票数据 (83, 87, 88, 89)...");
        int[] bjPrefixes = {83, 87, 88, 89};
        for (int prefix : bjPrefixes) {
            int start = prefix * 10000;
            int end = start + 9999;
            for (int code = start; code <= end; code++) {
                processStock(code);
            }
        }
        
        logger.info("✅ 所有A股股票历史数据获取完成");
    }

    /**
     * 处理单个股票代码
     * 生成股票symbol并获取保存其历史数据
     * 根据实际API调用耗时智能延时：如果调用耗时≥1秒，则无需额外延时；否则补足到1秒
     * @param code 股票代码
     */
    private void processStock(int code) {
        String symbol = generateSymbol(code);
        if (symbol != null) {
            long startTime = System.currentTimeMillis();
            fetchAndSaveHistory(symbol);
            long duration = System.currentTimeMillis() - startTime;
            
            // 如果本次调用耗时少于1秒，则补足延时到1秒
            long remainingDelay = 1000 - duration;
            if (remainingDelay > 0) {
                try {
                    Thread.sleep(remainingDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("历史数据获取被中断", e);
                }
            } else {
                logger.info("⚡ 本次调用耗时{}ms≥1秒，跳过额外延时", duration);
            }
        }
    }

    /**
     * 获取并保存单个股票的历史数据
     * @param symbol 股票Symbol（例如 "sh600000"）
     */
    public void fetchAndSaveHistory(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            logger.error("Symbol不能为空");
            return;
        }

        logger.info("开始获取股票历史数据: symbol={}", symbol);
        long totalStartTime = System.currentTimeMillis();
        
        // 1. 获取数据阶段
        long fetchStartTime = System.currentTimeMillis();
        List<StockHistoryDTO> historyList = stockClient.getStockHistory(symbol);
        long fetchDuration = System.currentTimeMillis() - fetchStartTime;
        logger.info("⏱️ 获取数据耗时: {}ms, symbol={}", fetchDuration, symbol);

        if (historyList == null || historyList.isEmpty()) {
            logger.info("未获取到股票历史数据: symbol={}", symbol);
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
        logger.info("✅ 成功保存股票历史数据: symbol={}, count={}, 总耗时={}ms (获取:{}ms, 转换:{}ms, 插入:{}ms)", 
                symbol, result.length, totalDuration, fetchDuration, mapDuration, insertDuration);
    }

    /**
     * 根据股票代码生成股票symbol
     * 沪京交易所 (sh): 60xxxx, 607xxx, 608xxx, 609xxx, 688xxx
     * 深圳交易所 (sz): 000xxx-099xxx, 100xxx-103xxx, 300xxx
     * 北京交易所 (bj): 83xxxx, 87xxxx, 88xxxx, 89xxxx
     * @param code 股票代码
     * @return 股票symbol（如sh600000或sz000001或bj830000）
     */
    private String generateSymbol(int code) {
        String paddedCode = String.format("%06d", code);
        String twoDigitPrefix = paddedCode.substring(0, 2);
        int prefixValue = Integer.parseInt(twoDigitPrefix);
        
        // 沪京交易所 (A股) - 60开头、607-609、688
        if (paddedCode.startsWith("60") || paddedCode.startsWith("688")) {
            return "sh" + paddedCode;
        }
        // 沪市新增号段 (607-609)
        else if (prefixValue >= 607 && prefixValue <= 609) {
            return "sh" + paddedCode;
        }
        // 深圳交易所 (A股) - 北交所号段外的数字
        // 000-099 (主板、中小板、创业板混合)
        // 100-103 (中小板)
        // 300-309+ (创业板)
        else if (prefixValue <= 103 || prefixValue == 300) {
            return "sz" + paddedCode;
        }
        // 北京交易所 (A股) - 83, 87, 88, 89
        else if (prefixValue == 83 || prefixValue == 87 
                 || prefixValue == 88 || prefixValue == 89) {
            return "bj" + paddedCode;
        } 
        else {
            return null;
        }
    }
}
