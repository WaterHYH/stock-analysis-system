package com.example.stock.service;

import com.example.stock.dto.StockAnalysisDTO;
import com.example.stock.entity.Stock;
import com.example.stock.entity.StockHistory;
import com.example.stock.repository.StockHistoryRepository;
import com.example.stock.repository.StockRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 股票分析服务类
 * 提供各种股票筛选条件的实现
 */
@Service
public class StockAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(StockAnalysisService.class);

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private StockRepository stockRepository;

    /**
     * 根据多个条件分析股票
     * @param conditions 筛选条件列表
     * @return 股票分析结果列表
     */
    public List<StockAnalysisDTO> analyzeStocks(List<String> conditions) {
        return analyzeStocks(conditions, null, null);
    }

    /**
     * 根据多个条件分析股票（带参数）
     * @param conditions 筛选条件列表
     * @param startDate 开始日期（可选）
     * @param dropPercentage 跌幅百分比（可选）
     * @return 股票分析结果列表
     */
    public List<StockAnalysisDTO> analyzeStocks(List<String> conditions, LocalDate startDate, Double dropPercentage) {
        long startTime = System.currentTimeMillis();
        logger.info("开始根据条件分析股票: {}, startDate: {}, dropPercentage: {}", conditions, startDate, dropPercentage);
        
        List<StockAnalysisDTO> allResults = new ArrayList<>();
        
        if (conditions == null || conditions.isEmpty()) {
            logger.info("条件列表为空，返回空结果");
            return allResults;
        }
        
        // 条件1: 跌幅超过指定百分比的股票
        if (conditions.contains("below_75_percent")) {
            if (startDate != null || dropPercentage != null) {
                // 使用带参数的版本
                allResults.addAll(findStocksBelowHistoricalHighWithParams(startDate, dropPercentage));
            } else {
                // 使用默认版本
                allResults.addAll(findStocksBelowHistoricalHigh());
            }
        }
        
        // 条件2: 最近半年出现至少3次最高值和最低值差距大于20%，并且当前价格处于低点的股票
        if (conditions.contains("high_volatility_low_price")) {
            allResults.addAll(findHighVolatilityLowPriceStocks());
        }
        
        // 条件3: 连续上涨趋势（最近10个交易日有8天收盘价高于开盘价）
        if (conditions.contains("continuous_rise")) {
            allResults.addAll(findContinuousRiseStocks());
        }
        
        // 条件4: 接近年度最高点（距离年度最高价5%以内）
        if (conditions.contains("near_year_high")) {
            allResults.addAll(findNearYearHighStocks());
        }
        
        // 条件5: 成交量激增（最近一天成交量是前30天平均成交量的2倍以上）
        if (conditions.contains("volume_surge")) {
            allResults.addAll(findVolumeSurgeStocks());
        }
        
        // 条件6: 均线金叉（5日均线上穿10日均线）
        if (conditions.contains("ma_golden_cross")) {
            allResults.addAll(findGoldenCrossStocks());
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("条件分析完成，共找到 {} 只符合条件的股票，总耗时 {}ms", allResults.size(), totalTime);
        
        return allResults;
    }

    /**
     * 获取所有满足条件的股票分析结果
     *
     * @return 股票分析结果列表
     */
    public List<StockAnalysisDTO> getAllAnalysisResults() {
        long startTime = System.currentTimeMillis();
        logger.info("开始执行所有股票筛选条件");

        List<StockAnalysisDTO> allResults = new ArrayList<>();

        // 条件1: 跌幅超过25%的股票
        allResults.addAll(findStocksBelowHistoricalHigh());

        // 条件2: 最近半年出现至少3次最高值和最低值差距大于20%，并且当前价格处于低点的股票
        allResults.addAll(findHighVolatilityLowPriceStocks());

        // 条件3: 连续上涨趋势（最近10个交易日有8天收盘价高于开盘价）
        allResults.addAll(findContinuousRiseStocks());

        // 条件4: 接近年度最高点（距离年度最高价5%以内）
        allResults.addAll(findNearYearHighStocks());

        // 条件5: 成交量激增（最近一天成交量是前30天平均成交量的2倍以上）
        allResults.addAll(findVolumeSurgeStocks());

        // 条件6: 均线金叉（5日均线上穿10日均线）
        allResults.addAll(findGoldenCrossStocks());

        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("所有筛选条件执行完成，共找到 {} 只符合条件的股票，总耗时 {}ms", allResults.size(), totalTime);

        return allResults;
    }

    /**
     * 条件1: 跌幅超过25%的股票（相对于历史最高点）
     * 优化：使用数据库聚合查询一次性完成筛选，避免N+1查询问题
     */
    public List<StockAnalysisDTO> findStocksBelowHistoricalHigh() {
        return findStocksBelowHistoricalHighWithParams(null, 25.0);
    }

    /**
     * 条件1: 跌幅超过指定百分比的股票（带参数）
     * @param startDate 开始日期（可选）
     * @param dropPercentage 跌幅百分比（默认25%）
     */
    public List<StockAnalysisDTO> findStocksBelowHistoricalHighWithParams(LocalDate startDate, Double dropPercentage) {
        long startTime = System.currentTimeMillis();
        double actualDropPercentage = dropPercentage != null ? dropPercentage : 25.0;
        logger.info("开始筛选跌幅超过{}%的股票，开始日期: {}", actualDropPercentage, startDate);

        try {
            // 使用数据库聚合查询一次性找出所有符合条件的股票
            long queryStart = System.currentTimeMillis();
            List<Map<String, Object>> queryResults;
            
            if (startDate != null) {
                // 使用带参数的查询
                queryResults = stockHistoryRepository.findStocksBelowHistoricalHighWithParams(
                    startDate.toString(), actualDropPercentage);
            } else {
                // 使用默认查询（全部历史数据）
                queryResults = stockHistoryRepository.findStocksBelowHistoricalHigh();
            }
            
            long queryTime = System.currentTimeMillis() - queryStart;
            logger.info("数据库查询完成，找到{}条符合条件的记录，耗时{}ms", queryResults.size(), queryTime);

            List<StockAnalysisDTO> results = queryResults.stream().map(row -> {
                String symbol = (String) row.get("symbol");
                Double maxHigh = row.get("max_high") != null ? ((Number) row.get("max_high")).doubleValue() : 0.0;
                Double currentPrice = row.get("current_price") != null ? ((Number) row.get("current_price")).doubleValue() : 0.0;

                double dropPercentageResult = ((maxHigh - currentPrice) / maxHigh) * 100;

                return StockAnalysisDTO.builder()
                        .symbol(symbol)
                        .currentPrice(currentPrice)
                        .historicalHigh(maxHigh)
                        .dropPercentage(dropPercentageResult)
                        .matchedCondition("低于历史最高值" + String.format("%.1f", actualDropPercentage) + "%以上")
                        .build();
            }).collect(Collectors.toList());

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("找到 {} 只符合条件的股票，总耗时{}ms（数据库查询{}ms）",
                    results.size(), totalTime, queryTime);
            return results;
        } catch (Exception e) {
            logger.error("数据库查询失败，使用应用层事后查询: {}", e.getMessage());
            return findStocksBelowHistoricalHighFallback(startDate, actualDropPercentage);
        }
    }

    /**
     * 回退方案：当数据库查询失败时使用
     */
    private List<StockAnalysisDTO> findStocksBelowHistoricalHighFallback(LocalDate startDate, double dropPercentage) {
        logger.info("使用回退方案：应用层查询模式，跌幅阈值: {}%，开始日期: {}", dropPercentage, startDate);
        List<StockAnalysisDTO> results = new ArrayList<>();
        int batchSize = 1000;

        // 获取所有股票代码并分批处理
        // 优化：不使用全表扫描，而是通过查询获取所有股票代码
        List<String> allStocks = stockHistoryRepository.findAllSymbols();

        for (int i = 0; i < allStocks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allStocks.size());
            List<String> batchSymbols = allStocks.subList(i, end);

            for (String symbol : batchSymbols) {
                List<StockHistory> histories = stockHistoryRepository.findBySymbol(symbol);

                // 根据开始日期过滤数据
                if (startDate != null) {
                    histories = histories.stream()
                            .filter(h -> !h.getDay().isBefore(startDate))
                            .collect(Collectors.toList());
                }

                if (!histories.isEmpty()) {
                    double maxHigh = histories.stream()
                            .mapToDouble(StockHistory::getHigh)
                            .max()
                            .orElse(0);

                    StockHistory latest = histories.stream()
                            .max(Comparator.comparing(StockHistory::getDay))
                            .orElse(null);

                    if (latest != null) {
                        double currentPrice = latest.getClose();
                        double actualDropPercentage = ((maxHigh - currentPrice) / maxHigh) * 100;

                        if (actualDropPercentage >= dropPercentage) {
                            results.add(StockAnalysisDTO.builder()
                                    .symbol(symbol)
                                    .currentPrice(currentPrice)
                                    .historicalHigh(maxHigh)
                                    .dropPercentage(actualDropPercentage)
                                    .matchedCondition("低于历史最高值" + String.format("%.1f", dropPercentage) + "%以上")
                                    .build());
                        }
                    }
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

        // 获取所有股票代码
        // 优化：不使用全表扫描，而是通过查询获取所有股票代码
        List<String> allSymbols = stockHistoryRepository.findAllSymbols();

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
                    results.add(StockAnalysisDTO.builder()
                            .symbol(symbol)
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

        // 获取所有股票代码
        // 优化：不使用全表扫描，而是通过查询获取所有股票代码
        List<String> allSymbols = stockHistoryRepository.findAllSymbols();

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
                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
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
        logger.info("开始筛选接近年度最高点的股票");
        List<StockAnalysisDTO> results = new ArrayList<>();
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);

        // 获取所有股票代码
        // 优化：不使用全表扫描，而是通过查询获取所有股票代码
        List<String> allSymbols = stockHistoryRepository.findAllSymbols();

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
                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
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

        // 获取所有股票代码
        // 优化：不使用全表扫描，而是通过查询获取所有股票代码
        List<String> allSymbols = stockHistoryRepository.findAllSymbols();

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
                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
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
    public List<StockAnalysisDTO> findGoldenCrossStocks() {
        // 使用默认日期（当前日期）
        return findGoldenCrossStocksWithDate(LocalDate.now());
    }
    
    /**
     * 条件6: 均线金叉（5日均线上穿10日均线）
     * @param latestDate 用户指定的最新交易日
     */
    public List<StockAnalysisDTO> findGoldenCrossStocksWithDate(LocalDate latestDate) {
        return findGoldenCrossStocksWithDateInternal(latestDate);
    }
    
    /**
     * 原始的数据库查询方案（备选）
     */
    private List<StockAnalysisDTO> findGoldenCrossStocksOriginal() {
        long startTime = System.currentTimeMillis();
        logger.info("开始筛选均线金叉股票（原始方案）");
        List<StockAnalysisDTO> results = new ArrayList<>();

        try {
            // 使用数据库聚合查询一次性找出所有金叉股票
            long queryStart = System.currentTimeMillis();
            List<Map<String, Object>> queryResults = stockHistoryRepository.findGoldenCrossStocksOptimized();
            long queryTime = System.currentTimeMillis() - queryStart;
            logger.info("数据库查询完成，找到{}条符合条件的记录，耗时{}ms", queryResults.size(), queryTime);

            for (Map<String, Object> row : queryResults) {
                String symbol = (String) row.get("symbol");
                Double currentPrice = row.get("current_price") != null ? ((Number) row.get("current_price")).doubleValue() : 0.0;

                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
                        .currentPrice(currentPrice)
                        .matchedCondition("均线金叉（5日均线上穿10日均线）")
                        .build());
            }

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("找到 {} 只符合条件的股票，总耗时{}ms（数据库查询{}ms）",
                    results.size(), totalTime, queryTime);
            return results;
        } catch (Exception e) {
            logger.error("数据库查询失败: {}", e.getMessage());
            return results; // 返回空列表
        }
    }
    
    /**
     * 条件6: 均线金叉（5日均线上穿10日均线）
     * 优化方案：先获取最近两个交易日的数据，然后通过代码过滤出金叉股票
     * @param latestDate 用户指定的最新交易日
     */
    private List<StockAnalysisDTO> findGoldenCrossStocksWithDateInternal(LocalDate latestDate) {
        long startTime = System.currentTimeMillis();
        logger.info("开始筛选均线金叉股票（优化方案），最新日期: {}", latestDate);
        List<StockAnalysisDTO> results = new ArrayList<>();

        try {
            // 根据用户输入的latestDate获取previousDate（前一个交易日）
            LocalDate previousDate = latestDate.minusDays(1); // 简化处理，实际应根据交易日历调整
            logger.info("使用用户输入日期: {} 和前一个交易日: {}", latestDate, previousDate);

            // 获取最近两个交易日的所有股票数据
            long dataQueryStart = System.currentTimeMillis();
            List<StockHistory> latestTwoDaysData = stockHistoryRepository.findLatestTwoDaysData(latestDate, previousDate);
            long dataQueryTime = System.currentTimeMillis() - dataQueryStart;
            logger.info("获取最近两个交易日数据完成，共{}条记录，耗时{}ms", latestTwoDaysData.size(), dataQueryTime);

            // 按股票代码分组
            long groupStart = System.currentTimeMillis();
            Map<String, List<StockHistory>> groupedBySymbol = latestTwoDaysData.stream()
                    .collect(Collectors.groupingBy(StockHistory::getSymbol));
            long groupTime = System.currentTimeMillis() - groupStart;
            logger.info("数据分组完成，共{}只股票，耗时{}ms", groupedBySymbol.size(), groupTime);

            // 分析金叉
            long analysisStart = System.currentTimeMillis();
            int goldenCrossCount = 0;
            for (Map.Entry<String, List<StockHistory>> entry : groupedBySymbol.entrySet()) {
                String symbol = entry.getKey();
                List<StockHistory> histories = entry.getValue();

                // 确保有两天的数据
                if (histories.size() < 2) continue;

                // 按日期排序，最新的在前
                histories.sort(Comparator.comparing(StockHistory::getDay).reversed());
                StockHistory today = histories.get(0);
                StockHistory yesterday = histories.get(1);

                // 确保是同一只股票的连续两天数据
                if (!today.getDay().equals(latestDate) || !yesterday.getDay().equals(previousDate)) {
                    continue;
                }

                // 判断金叉：昨天5日均线<=10日均线，今天5日均线>10日均线
                // 同时确保均线数据不为null且大于0
                if (yesterday.getMaPrice5() > 0 && yesterday.getMaPrice10() > 0 && 
                    today.getMaPrice5() > 0 && today.getMaPrice10() > 0 &&
                    yesterday.getMaPrice5() <= yesterday.getMaPrice10() && 
                    today.getMaPrice5() > today.getMaPrice10()) {
                    
                    results.add(StockAnalysisDTO.builder()
                            .symbol(symbol)
                            .currentPrice(today.getClose())
                            .matchedCondition("均线金叉（5日均线上穿10日均线）")
                            .build());
                    
                    goldenCrossCount++;
                }
            }
            long analysisTime = System.currentTimeMillis() - analysisStart;
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("金叉分析完成，找到 {} 只符合条件的股票，总耗时{}ms（数据查询{}ms + 分组{}ms + 分析{}ms）", 
                    goldenCrossCount, totalTime, dataQueryTime, groupTime, analysisTime);
            return results;
        } catch (Exception e) {
            logger.error("优化方案执行失败: {}", e.getMessage(), e);
            logger.info("使用原有数据库查询方案");
            return findGoldenCrossStocksOriginal();
        }
    }

    /**
     * 回退方案：当数据库查询失败时使用
     */
    private List<StockAnalysisDTO> findGoldenCrossStocksFallback() {
        logger.info("使用回退方案：应用层查询模式");
        List<StockAnalysisDTO> results = new ArrayList<>();

        // 优化：不使用全表扫描，而是通过查询获取所有股票代码
        List<String> allSymbols = stockHistoryRepository.findAllSymbols();

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

                results.add(StockAnalysisDTO.builder()
                        .symbol(symbol)
                        .currentPrice(today.getClose())
                        .matchedCondition("均线金叉（5日均线上穿10日均线）")
                        .build());
            }
        }

        logger.info("回退方案完成，找到 {} 只符合条件的股票", results.size());
        return results;
    }

    /**
     * 根据股票代码获取股票名称（仅母回退方案使用）
     */
    private String getStockName(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol);
        return stock != null ? stock.getName() : "未知";
    }
}