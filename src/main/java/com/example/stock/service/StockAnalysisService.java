package com.example.stock.service;

import com.example.stock.dto.StockAnalysisDTO;
import com.example.stock.entity.Stock;
import com.example.stock.entity.StockHistory;
import com.example.stock.repository.StockHistoryRepository;
import com.example.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 股票分析服务类
 * 提供多种股票筛选条件的分析功能
 */
@Service
@RequiredArgsConstructor
public class StockAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(StockAnalysisService.class);
    
    private final StockHistoryRepository stockHistoryRepository;
    private final StockRepository stockRepository;

    /**
     * 根据多个条件分析股票
     * @param conditions 筛选条件列表
     * @return 符合条件的股票分析结果
     */
    public List<StockAnalysisDTO> analyzeStocks(List<String> conditions) {
        return analyzeStocks(conditions, null, null);
    }

    /**
     * 根据多个条件分析股票（带参数）
     * @param conditions 筛选条件列表
     * @param startDate 开始日期（用于条件1）
     * @param dropPercentage 跌幅百分比（用于条件1）
     * @return 符合条件的股票分析结果
     */
    public List<StockAnalysisDTO> analyzeStocks(List<String> conditions, LocalDate startDate, Double dropPercentage) {
        Set<StockAnalysisDTO> allResults = new HashSet<>();
        
        for (String condition : conditions) {
            List<StockAnalysisDTO> results = switch (condition) {
                case "below_75_percent" -> findStocksBelowHistoricalHigh(startDate, dropPercentage);
                case "high_volatility_low_price" -> findHighVolatilityLowPriceStocks();
                case "continuous_rise" -> findContinuousRiseStocks();
                case "near_year_high" -> findNearYearHighStocks();
                case "volume_surge" -> findVolumeSurgeStocks();
                case "ma_golden_cross" -> findGoldenCrossStocks();
                default -> new ArrayList<>();
            };
            allResults.addAll(results);
        }
        
        return new ArrayList<>(allResults);
    }

    /**
     * 条件1: 筛选出比历史最高值的75%还要低的股票（即低于最高值25%以上）
     * 优化版本：使用数据库聚合查询，单次查询完成
     * @param startDate 开始日期，为null则使用全部历史数据
     * @param dropPercentage 跌幅百分比，为null则默认25%
     * @return 符合条件的股票列表
     */
    private List<StockAnalysisDTO> findStocksBelowHistoricalHigh(LocalDate startDate, Double dropPercentage) {
        // 设置默认值
        if (dropPercentage == null || dropPercentage <= 0 || dropPercentage >= 100) {
            dropPercentage = 25.0;
        }
        
        long startTime = System.currentTimeMillis();
        logger.info("开始筛选低于历史最高值{}%的股票（使用数据库聚合查询）", dropPercentage);
        if (startDate != null) {
            logger.info("开始日期: {}", startDate);
        }
        
        try {
            // 使用单次数据库聚合查询获取所有符合条件的股票
            long queryStart = System.currentTimeMillis();
            List<Map<String, Object>> queryResults;
            
            if (startDate != null) {
                // 使用带参数的查询
                queryResults = stockHistoryRepository.findStocksBelowHistoricalHighWithParams(
                    startDate.toString(), dropPercentage);
            } else {
                // 使用默认查询（仅当dropPercentage为25%时）
                if (dropPercentage.equals(25.0)) {
                    queryResults = stockHistoryRepository.findStocksBelowHistoricalHigh();
                } else {
                    // 如果只指定了百分比，使用很早的日期（1900-01-01）
                    queryResults = stockHistoryRepository.findStocksBelowHistoricalHighWithParams(
                        "1900-01-01", dropPercentage);
                }
            }
            
            long queryTime = System.currentTimeMillis() - queryStart;
            logger.info("数据库聚合查询完成，找到{}条符合条件的记录，耗时{}ms", queryResults.size(), queryTime);
            
            // 构建结果对象
            long buildStart = System.currentTimeMillis();
            List<StockAnalysisDTO> results = new ArrayList<>();
            
            // 批量获取所有股票信息，避免N+1查询问题
            List<String> symbols = queryResults.stream()
                    .map(row -> (String) row.get("symbol"))
                    .collect(Collectors.toList());
            
            Map<String, String> stockNameMap = getStockNameMapBatch(symbols);
            
            for (Map<String, Object> row : queryResults) {
                String symbol = (String) row.get("symbol");
                Double maxHigh = row.get("max_high") != null ? ((Number) row.get("max_high")).doubleValue() : 0.0;
                Double currentPrice = row.get("current_price") != null ? ((Number) row.get("current_price")).doubleValue() : 0.0;
                
                if (maxHigh > 0 && currentPrice > 0) {
                    double actualDropPercentage = ((maxHigh - currentPrice) / maxHigh) * 100;
                    
                    // 从Map中获取股票名称，避免逐个查询数据库
                    String name = stockNameMap.getOrDefault(symbol, "未知");
                    
                    String conditionDesc = String.format("低于历史最高值%.1f%%以上", dropPercentage);
                    if (startDate != null) {
                        conditionDesc += String.format("(自%s起)", startDate);
                    }
                    
                    results.add(StockAnalysisDTO.builder()
                            .symbol(symbol)
                            .name(name)
                            .currentPrice(currentPrice)
                            .historicalHigh(maxHigh)
                            .dropPercentage(actualDropPercentage)
                            .matchedCondition(conditionDesc)
                            .build());
                }
            }
            
            long buildTime = System.currentTimeMillis() - buildStart;
            logger.info("构建结果对象完成，耗时{}ms", buildTime);
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("找到 {} 只符合条件的股票，总耗时{}ms（数据库查询{}ms + 构建对象{}ms）", 
                    results.size(), totalTime, queryTime, buildTime);
            return results;
        } catch (Exception e) {
            logger.error("数据库聚合查询失败，回退到普通查询: {}", e.getMessage());
            return findStocksBelowHistoricalHighFallback();
        }
    }
    
    /**
     * 回退方案：当数据库聚合查询失败时使用
     * 使用分批查询减少内存占用
     */
    private List<StockAnalysisDTO> findStocksBelowHistoricalHighFallback() {
        logger.info("使用回退方案：分批查询模式");
        List<StockAnalysisDTO> results = new ArrayList<>();
        
        // 从 Stock 表获取所有股票代码
        List<Stock> allStocks = stockRepository.findAll();
        logger.info("获取{}只股票代码", allStocks.size());
        
        int batchSize = 100;
        for (int i = 0; i < allStocks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allStocks.size());
            List<Stock> batch = allStocks.subList(i, end);
            
            for (Stock stock : batch) {
                String symbol = stock.getSymbol();
                List<StockHistory> histories = stockHistoryRepository.findBySymbol(symbol);
                if (histories.isEmpty()) continue;
                
                // 找出历史最高价和最新价格
                double maxHigh = histories.stream()
                        .mapToDouble(StockHistory::getHigh)
                        .max()
                        .orElse(0);
                
                StockHistory latest = histories.stream()
                        .max(Comparator.comparing(StockHistory::getDay))
                        .orElse(null);
                
                if (latest == null || maxHigh == 0) continue;
                
                double currentPrice = latest.getClose();
                double threshold = maxHigh * 0.75;
                
                if (currentPrice < threshold) {
                    double dropPercentage = ((maxHigh - currentPrice) / maxHigh) * 100;
                    results.add(StockAnalysisDTO.builder()
                            .symbol(symbol)
                            .name(stock.getName())
                            .currentPrice(currentPrice)
                            .historicalHigh(maxHigh)
                            .dropPercentage(dropPercentage)
                            .matchedCondition("低于历史最高值25%以上")
                            .build());
                }
            }
            
            if ((i + batchSize) % 500 == 0) {
                logger.info("处理进度: {}/{}, 已找到{}只符合条件的股票", 
                        Math.min(i + batchSize, allStocks.size()), allStocks.size(), results.size());
            }
        }
        
        logger.info("回退方案完成，找到 {} 只符合条件的股票", results.size());
        return results;
    }

    /**
     * 条件2: 最近半年出现至少3次最高值和最低值差距大于20%，并且当前价格处于低点的股票
     */
    private List<StockAnalysisDTO> findHighVolatilityLowPriceStocks() {
        long startTime = System.currentTimeMillis();
        logger.info("开始筛选高波动低价位股票");
        List<StockAnalysisDTO> results = new ArrayList<>();
        LocalDate halfYearAgo = LocalDate.now().minusMonths(6);
        
        // 获取所有股票代码并批量获取名称
        List<String> allSymbols = stockHistoryRepository.findAll().stream()
                .map(StockHistory::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> stockNameMap = getStockNameMapBatch(allSymbols);
        
        for (String symbol : allSymbols) {
            List<StockHistory> histories = stockHistoryRepository.findBySymbol(symbol);
            
            // 筛选最近半年的数据
            List<StockHistory> recentHistories = histories.stream()
                    .filter(h -> h.getDay().isAfter(halfYearAgo))
                    .sorted(Comparator.comparing(StockHistory::getDay))
                    .collect(Collectors.toList());
            
            if (recentHistories.size() < 30) continue; // 至少需要30天数据
            
            // 统计波动次数（日内波动超过20%）
            int volatilityCount = 0;
            for (StockHistory history : recentHistories) {
                double dayRange = ((history.getHigh() - history.getLow()) / history.getLow()) * 100;
                if (dayRange > 20) {
                    volatilityCount++;
                }
            }
            
            if (volatilityCount >= 3) {
                StockHistory latest = recentHistories.get(recentHistories.size() - 1);
                double recentHigh = recentHistories.stream()
                        .mapToDouble(StockHistory::getHigh)
                        .max()
                        .orElse(0);
                double recentLow = recentHistories.stream()
                        .mapToDouble(StockHistory::getLow)
                        .min()
                        .orElse(0);
                
                // 判断当前价格是否处于低位（低于区间的40%位置）
                double currentPrice = latest.getClose();
                double lowThreshold = recentLow + (recentHigh - recentLow) * 0.4;
                
                if (currentPrice <= lowThreshold) {
                    String name = stockNameMap.getOrDefault(symbol, "未知");
                    
                    results.add(StockAnalysisDTO.builder()
                            .symbol(symbol)
                            .name(name)
                            .currentPrice(currentPrice)
                            .historicalHigh(recentHigh)
                            .historicalLow(recentLow)
                            .volatilityCount(volatilityCount)
                            .matchedCondition("高波动且处于低位（半年内波动" + volatilityCount + "次）")
                            .build());
                }
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("找到 {} 只符合条件的股票，总耗时{}ms", results.size(), totalTime);
        return results;
    }

    /**
     * 条件3: 连续上涨趋势（最近10个交易日有8天收盘价高于开盘价）
     */
    private List<StockAnalysisDTO> findContinuousRiseStocks() {
        long startTime = System.currentTimeMillis();
        logger.info("开始筛选连续上涨趋势股票");
        List<StockAnalysisDTO> results = new ArrayList<>();
        
        // 获取所有股票代码并批量获取名称
        List<String> allSymbols = stockHistoryRepository.findAll().stream()
                .map(StockHistory::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> stockNameMap = getStockNameMapBatch(allSymbols);
        
        for (String symbol : allSymbols) {
            List<StockHistory> histories = stockHistoryRepository.findBySymbol(symbol);
            
            // 获取最近10个交易日
            List<StockHistory> recent10Days = histories.stream()
                    .sorted(Comparator.comparing(StockHistory::getDay).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
            
            if (recent10Days.size() < 10) continue;
            
            // 统计上涨天数
            long riseDays = recent10Days.stream()
                    .filter(h -> h.getClose() > h.getOpen())
                    .count();
            
            if (riseDays >= 8) {
                StockHistory latest = recent10Days.get(0);
                String name = stockNameMap.getOrDefault(symbol, "未知");
                
                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
                        .name(name)
                        .currentPrice(latest.getClose())
                        .matchedCondition("连续上涨（10天中" + riseDays + "天上涨）")
                        .build());
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("找到 {} 只符合条件的股票，总耗时{}ms", results.size(), totalTime);
        return results;
    }

    /**
     * 条件4: 接近年度最高点（距离年度最高价5%以内）
     */
    private List<StockAnalysisDTO> findNearYearHighStocks() {
        long startTime = System.currentTimeMillis();
        logger.info("开始筛选接近年度最高点股票");
        List<StockAnalysisDTO> results = new ArrayList<>();
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        
        // 获取所有股票代码并批量获取名称
        List<String> allSymbols = stockHistoryRepository.findAll().stream()
                .map(StockHistory::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> stockNameMap = getStockNameMapBatch(allSymbols);
        
        for (String symbol : allSymbols) {
            List<StockHistory> histories = stockHistoryRepository.findBySymbol(symbol);
            
            // 筛选最近一年的数据
            List<StockHistory> yearHistories = histories.stream()
                    .filter(h -> h.getDay().isAfter(oneYearAgo))
                    .collect(Collectors.toList());
            
            if (yearHistories.isEmpty()) continue;
            
            double yearHigh = yearHistories.stream()
                    .mapToDouble(StockHistory::getHigh)
                    .max()
                    .orElse(0);
            
            StockHistory latest = yearHistories.stream()
                    .max(Comparator.comparing(StockHistory::getDay))
                    .orElse(null);
            
            if (latest == null || yearHigh == 0) continue;
            
            double currentPrice = latest.getClose();
            double difference = ((yearHigh - currentPrice) / yearHigh) * 100;
            
            if (difference <= 5 && difference >= 0) {
                String name = stockNameMap.getOrDefault(symbol, "未知");
                
                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
                        .name(name)
                        .currentPrice(currentPrice)
                        .historicalHigh(yearHigh)
                        .dropPercentage(difference)
                        .matchedCondition("接近年度最高点（相差" + String.format("%.2f", difference) + "%）")
                        .build());
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("找到 {} 只符合条件的股票，总耗时{}ms", results.size(), totalTime);
        return results;
    }

    /**
     * 条件5: 成交量激增（最近一天成交量是前30天平均成交量的2倍以上）
     */
    private List<StockAnalysisDTO> findVolumeSurgeStocks() {
        long startTime = System.currentTimeMillis();
        logger.info("开始筛选成交量激增股票");
        List<StockAnalysisDTO> results = new ArrayList<>();
        
        // 获取所有股票代码并批量获取名称
        List<String> allSymbols = stockHistoryRepository.findAll().stream()
                .map(StockHistory::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> stockNameMap = getStockNameMapBatch(allSymbols);
        
        for (String symbol : allSymbols) {
            List<StockHistory> histories = stockHistoryRepository.findBySymbol(symbol);
            
            // 获取最近31个交易日
            List<StockHistory> recent31Days = histories.stream()
                    .sorted(Comparator.comparing(StockHistory::getDay).reversed())
                    .limit(31)
                    .collect(Collectors.toList());
            
            if (recent31Days.size() < 31) continue;
            
            StockHistory latest = recent31Days.get(0);
            long latestVolume = latest.getVolume();
            
            // 计算前30天平均成交量
            double avgVolume = recent31Days.stream()
                    .skip(1)
                    .mapToLong(StockHistory::getVolume)
                    .average()
                    .orElse(0);
            
            if (avgVolume > 0 && latestVolume > avgVolume * 2) {
                String name = stockNameMap.getOrDefault(symbol, "未知");
                
                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
                        .name(name)
                        .currentPrice(latest.getClose())
                        .matchedCondition("成交量激增（是平均量的" + String.format("%.2f", latestVolume / avgVolume) + "倍）")
                        .build());
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("找到 {} 只符合条件的股票，总耗时{}ms", results.size(), totalTime);
        return results;
    }

    /**
     * 条件6: 均线金叉（5日均线上穿10日均线）
     */
    private List<StockAnalysisDTO> findGoldenCrossStocks() {
        long startTime = System.currentTimeMillis();
        logger.info("开始筛选均线金叉股票");
        List<StockAnalysisDTO> results = new ArrayList<>();
        
        try {
            // 使用数据库聚合查询一次性找出所有金叉股票
            long queryStart = System.currentTimeMillis();
            List<Map<String, Object>> queryResults = stockHistoryRepository.findGoldenCrossStocksOptimized();
            long queryTime = System.currentTimeMillis() - queryStart;
            logger.info("数据库查询完成，找到{}条符合条件的记录，耗时{}ms", queryResults.size(), queryTime);
            
            // 批量获取股票名称，不使用N+1查询
            long buildStart = System.currentTimeMillis();
            List<String> symbols = queryResults.stream()
                    .map(row -> (String) row.get("symbol"))
                    .collect(Collectors.toList());
            
            long stockQueryStart = System.currentTimeMillis();
            Map<String, String> stockNameMap = stockRepository.findBySymbolIn(symbols).stream()
                    .collect(Collectors.toMap(Stock::getSymbol, Stock::getName));
            long stockQueryTime = System.currentTimeMillis() - stockQueryStart;
            logger.info("批量查询{}\u4e2a股票名称，耗时{}ms", symbols.size(), stockQueryTime);
            
            for (Map<String, Object> row : queryResults) {
                String symbol = (String) row.get("symbol");
                Double currentPrice = row.get("current_price") != null ? ((Number) row.get("current_price")).doubleValue() : 0.0;
                
                String name = stockNameMap.getOrDefault(symbol, "未知");
                
                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
                        .name(name)
                        .currentPrice(currentPrice)
                        .matchedCondition("均线金叉（5日均线上穿10日均线）")
                        .build());
            }
            
            long buildTime = System.currentTimeMillis() - buildStart;
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("找到 {} 只符合条件的股票，总耗时{}ms（数据库查询{}ms + 构建对象{}ms）", 
                    results.size(), totalTime, queryTime, buildTime);
            return results;
        } catch (Exception e) {
            logger.error("数据库查询失败，使用应用层事后查询: {}", e.getMessage());
            return findGoldenCrossStocksFallback();
        }
    }
    
    /**
     * 回退方案：当数据库查询失败时使用
     */
    private List<StockAnalysisDTO> findGoldenCrossStocksFallback() {
        logger.info("使用回退方案：应用层查询模式");
        List<StockAnalysisDTO> results = new ArrayList<>();
        
        List<String> allSymbols = stockHistoryRepository.findAll().stream()
                .map(StockHistory::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量获取所有股票名称
        Map<String, String> stockNameMap = stockRepository.findAll().stream()
                .filter(stock -> allSymbols.contains(stock.getSymbol()))
                .collect(Collectors.toMap(Stock::getSymbol, Stock::getName));
        
        for (String symbol : allSymbols) {
            List<StockHistory> histories = stockHistoryRepository.findBySymbol(symbol);
            
            // 获取最近2个交易日
            List<StockHistory> recent2Days = histories.stream()
                    .sorted(Comparator.comparing(StockHistory::getDay).reversed())
                    .limit(2)
                    .collect(Collectors.toList());
            
            if (recent2Days.size() < 2) continue;
            
            StockHistory today = recent2Days.get(0);
            StockHistory yesterday = recent2Days.get(1);
            
            // 判断金叉：昨天5日均线<=10日均线，今天5日均线>10日均线
            if (yesterday.getMaPrice5() <= yesterday.getMaPrice10() && 
                today.getMaPrice5() > today.getMaPrice10()) {
                
                String name = stockNameMap.getOrDefault(symbol, "未知");
                
                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
                        .name(name)
                        .currentPrice(today.getClose())
                        .matchedCondition("均线金叉（5日均线上穿10日均线）")
                        .build());
            }
        }
        
        logger.info("回退方案完成，找到 {} 只符合条件的股票", results.size());
        return results;
    }

    /**
     * 批量获取股票名称
     */
    private Map<String, String> getStockNameMapBatch(List<String> symbols) {
        return stockRepository.findBySymbolIn(symbols).stream()
                .collect(Collectors.toMap(Stock::getSymbol, Stock::getName));
    }

    /**
     * 根据股票代码获取股票名称（仅母回退方案使用）
     */
    private String getStockName(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol);
        return stock != null ? stock.getName() : "未知";
    }
}
