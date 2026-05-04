package com.example.stock.service;

import com.example.stock.dto.StockHistoryDTO;
import com.example.stock.entity.StockHistory;
import com.example.stock.entity.StockSyncLog;
import com.example.stock.repository.StockHistoryRepository;
import com.example.stock.repository.StockSyncLogRepository;
import com.example.stock.service.client.SinaStockClient;
import com.example.stock.service.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 股票历史数据获取服务类
 * 提供股票历史数据的获取和保存功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoryFetchService {
    private final SinaStockClient stockClient;
    private final StockHistoryRepository stockHistoryRepository;
    private final StockSyncLogRepository stockSyncLogRepository;
    private final StockMapper stockMapper;
    private final KLineAnalysisService kLineAnalysisService;
    private final TaskExecutor syncTaskExecutor;

    private static final long BATCH_THROTTLE_MS = 2000;

    /**
     * 批量获取所有A股股票历史数据
     */
    public void fetchAllStockHistory() {
        log.info("开始批量获取所有A股股票历史数据...");

        List<StockSyncLog> syncLogs = stockSyncLogRepository.findAll();
        Map<String, StockSyncLog> syncLogMap = new HashMap<>();
        for (StockSyncLog log : syncLogs) {
            syncLogMap.put(log.getSymbol(), log);
        }

        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);

        runStockBatch("沪市主板", 600000, 605999, "600-605", syncLogMap, processed, skipped);
        runStockBatch("沪市新增号段", 607000, 609999, "607-609", syncLogMap, processed, skipped);
        runStockBatch("沪市科创板", 688000, 688999, "688", syncLogMap, processed, skipped);
        runStockBatch("深市主板", 1, 3999, "000-003", syncLogMap, processed, skipped);
        runStockBatch("深市创业板", 300000, 399999, "300", syncLogMap, processed, skipped);

        log.info("✅ 所有A股股票历史数据获取完成, 本次处理: {}只, 跳过: {}只", processed.get(), skipped.get());
    }

    private void runStockBatch(String name, int codeFrom, int codeTo, String codeRange,
            Map<String, StockSyncLog> syncLogMap, AtomicInteger processed, AtomicInteger skipped) {
        int batchSkipped = 0;
        int batchProcessed = 0;

        for (int code = codeFrom; code <= codeTo; code++) {
            int result = processStock(code, syncLogMap);
            if (result == 0) {
                batchSkipped++;
            } else if (result > 0) {
                batchProcessed++;
            }
            // result < 0 means error, don't count
        }

        processed.addAndGet(batchProcessed);
        skipped.addAndGet(batchSkipped);

        log.info("{} ({}): 已跳过 {}只, 新处理 {}只, 总范围: {}", name, codeRange, batchSkipped, batchProcessed, codeTo - codeFrom + 1);

        // 号段间限速：降低 CPU 和磁盘压力
        try {
            Thread.sleep(BATCH_THROTTLE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 处理单个股票代码
     * @return 0=已跳过, >0=成功插入的记录数, -1=出错
     */
    private int processStock(int code, Map<String, StockSyncLog> syncLogMap) {
        String symbol = generateSymbol(code);
        if (symbol == null) {
            return 0;
        }
        try {
            if (syncLogMap.containsKey(symbol)) {
                StockSyncLog syncLog = syncLogMap.get(symbol);
                LocalDate syncDate = syncLog.getSyncDate();
                LocalDate nowDate = LocalDate.now();
                if (isWeekend(nowDate) || syncDate.equals(nowDate)) {
                    return 0;
                }
            }

            int insertedCount = fetchAndSaveHistory(symbol);
            if (syncLogMap.containsKey(symbol)) {
                StockSyncLog syncLog = syncLogMap.get(symbol);
                syncLog.setSyncDate(LocalDate.now());
                stockSyncLogRepository.save(syncLog);
            } else {
                StockSyncLog syncLog = new StockSyncLog();
                syncLog.setSymbol(symbol);
                syncLog.setSyncDate(LocalDate.now());
                stockSyncLogRepository.save(syncLog);
                syncLogMap.put(symbol, syncLog);
            }

            if (insertedCount > 0) {
                try {
                    log.info("成功插入{}条新记录，执行延时", insertedCount);
                    Thread.sleep(3000 + insertedCount * 5L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("延时被中断", e);
                }
            }
            return insertedCount;
        } catch (org.springframework.dao.DataAccessResourceFailureException e) {
            log.error("数据库连接异常，跳过该股票: symbol={}, 错误: {}", symbol, e.getMessage());
            return -1;
        } catch (Exception e) {
            log.error("处理股票时发生异常: symbol={}, 错误: {}", symbol, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 获取并保存单个股票的历史数据
     * 优化：第一次获取全部数据，然后基于数据库最新记录日期实现增量同步
     * @param symbol 股票Symbol（例如 "sh600000"）
     * @return 实际插入数据库的记录数，如果没有插入任何数据则返回0
     */
    public int fetchAndSaveHistory(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            log.error("Symbol不能为空");
            return 0;
        }

        log.info("开始获取股票历史数据: symbol={}", symbol);
        long totalStartTime = System.currentTimeMillis();

        // 1. 查询数据库中此股票的最新记录日期，判断是增量还是全量同步
        LocalDate latestDbDate = stockHistoryRepository.findLatestTradeDateBySymbol(symbol);
        boolean isFullSync = latestDbDate == null;  // 记录是否为全量获取

        // ... existing code ...

        // 优化：如果数据库最新记录就是最近的交易日，则跳过API调用
        LocalDate today = LocalDate.now();
        if (latestDbDate != null) {
            // 获取最近的交易日（今天或前一个交易日）
            LocalDate lastTradingDay = getLastTradingDay(today);
            // 只有当数据库最新记录就是最近的交易日时，才跳过API调用
            if (latestDbDate.equals(lastTradingDay)) {
                log.info("✅ 数据库中最新记录已是最近的交易日({})，无需调用API，直接跳过", latestDbDate);
                return 0;
            }
        }

        int datalen = 70000; // 默认获取全部

        if (latestDbDate != null) {
            // 数据库中已经有数据，实现增量同步
            // 为了安全起见，提前往回取一些日期（考虑datalen是自然日，不是交易日）
            long daysAhead = 10; // 提前10天，以便不漏到真实的最新数据
            LocalDate fromDate = latestDbDate.minusDays(daysAhead);
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, LocalDate.now());
            datalen = (int) Math.min(daysBetween + 10, 70000); // 确保足够大，但不超过上限
            log.info("数据库中最新记录: {}, 将从{}开始处理，仅获取{}天的数据",
                    latestDbDate, fromDate, datalen);
        } else {
            log.info("数据库中无此股票数据，将推出全量获取");
        }

        // 2. 获取数据阶段
        long fetchStartTime = System.currentTimeMillis();
        List<StockHistoryDTO> historyList = stockClient.getStockHistory(symbol, datalen);
        long fetchDuration = System.currentTimeMillis() - fetchStartTime;
        log.info("⏱️ 获取数据耗时: {}ms, symbol={}", fetchDuration, symbol);

        if (historyList == null || historyList.isEmpty()) {
            log.info("未获取到股票历史数据: symbol={}", symbol);
            return 0;
        }

        // 3. 数据转换阶段
        long mapStartTime = System.currentTimeMillis();
        List<StockHistory> entities = stockMapper.toStockHistoryList(historyList);
        long mapDuration = System.currentTimeMillis() - mapStartTime;
        log.info("⏱️ 数据转换耗时: {}ms, 记录数={}", mapDuration, entities.size());

        // 4. 数据排序阶段：按日期降序排列（最新到最旧），确保K线分析能正确获取前一天数据
        long sortStartTime = System.currentTimeMillis();
        entities.sort((a, b) -> b.getDay().compareTo(a.getDay()));
        long sortDuration = System.currentTimeMillis() - sortStartTime;
        log.info("⏱️ 数据排序完成，按日期降序排列，耗时: {}ms", sortDuration);

        // 5. 关键优化：过滤出数据库中已经存在的记录，只保留新记录
        long filterStartTime = System.currentTimeMillis();
        List<StockHistory> newRecords = new java.util.ArrayList<>();
        LocalDate existingLatestDate = latestDbDate;

        for (StockHistory entity : entities) {
            // 只保留数据库中没有的记录（日期比最新记录日期更新）
            if (existingLatestDate == null || entity.getDay().isAfter(existingLatestDate)) {
                newRecords.add(entity);
            }
        }
        long filterDuration = System.currentTimeMillis() - filterStartTime;
        log.info("⏱️ 过滤新记录耗时: {}ms, 原数据={}, 新记录数={}",
                filterDuration, entities.size(), newRecords.size());

        // 如果没有新记录，不需要执行后续处理
        if (newRecords.isEmpty()) {
            log.info("✅ symbol={}的数据已是最新，没有新记录需要写入", symbol);
            return 0;
        }

        // 6. K线分析阶段（仅处理新记录）
        long analysisStartTime = System.currentTimeMillis();
        for (int i = 0; i < newRecords.size(); i++) {
            StockHistory current = newRecords.get(i);
            StockHistory previous = (i + 1 < newRecords.size()) ? newRecords.get(i + 1) : null;
            // 向下查找前一天数据（有可能是已有的旧数据）
            if (previous == null && existingLatestDate != null) {
                // 如果新新记录列表末尾无上一条记录，从旧数据库中查找
                previous = stockHistoryRepository.findPreviousDayDataForStock(symbol, current.getDay());
            }
            // 会传入整个数据列表，思维是这个列表包含了最旧的数据。会找到应沾的指标值
            kLineAnalysisService.analyzeKLine(current, previous, entities);
        }
        long analysisDuration = System.currentTimeMillis() - analysisStartTime;
        log.info("⏱️ K线分析耗时: {}ms, 记录数={}", analysisDuration, newRecords.size());

        // 7. 批量插入阶段（仅插入新记录）
        long insertStartTime = System.currentTimeMillis();
        int[] result = stockHistoryRepository.batchInsertStockHistory(newRecords);
        long insertDuration = System.currentTimeMillis() - insertStartTime;
        log.info("⏱️ 批量插入耗时: {}ms, 记录数={}", insertDuration, result.length);

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        log.info("✅ 成功保存股票历史数据: symbol={}, 新增数据数={}, 总耗时={}ms (获取:{}ms, 转换:{}ms, 过滤:{}ms, 分析:{}ms, 插入:{}ms)",
                symbol, result.length, totalDuration, fetchDuration, mapDuration, filterDuration, analysisDuration, insertDuration);

        return result.length;  // 返回实际插入的记录数
    }

    /**
     * 根据股票代码生成股票symbol
     * 沪市 (sh): 60xxxx, 607xxx, 608xxx, 609xxx, 688xxx
     * 深市 (sz): 000xxx-003xxx (主板), 300xxx (创业板)
     * @param code 股票代码
     * @return 股票symbol（如sh600000或sz000001）
     */
    private String generateSymbol(int code) {
        String paddedCode = String.format("%06d", code);
        int prefixValue = Integer.parseInt(paddedCode.substring(0, 3));

        // 沪市 (A股) - 60开头、607-609、688
        if (paddedCode.startsWith("60") || paddedCode.startsWith("688")) {
            return "sh" + paddedCode;
        }
        // 沪市新增号段 (607-609)
        else if (prefixValue >= 607 && prefixValue <= 609) {
            return "sh" + paddedCode;
        }
        // 深市 (A股) - 000-003 (主板) 或 300-399 (创业板)
        else if ((prefixValue >= 0 && prefixValue <= 3) || (prefixValue >= 300 && prefixValue <= 399)) {
            return "sz" + paddedCode;
        }
        else {
            return null;
        }
    }

    /**
     * 判断是否为周末
     * @param date 日期
     * @return 是否为周末
     */
    private boolean isWeekend(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7; // 6=周六, 7=周日
    }

    /**
     * 获取最近的交易日（如果是周末，则返回上周五）
     * @param date 当前日期
     * @return 最近的交易日
     */
    private LocalDate getLastTradingDay(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        if (dayOfWeek == 6) { // 周六
            return date.minusDays(1); // 返回周五
        } else if (dayOfWeek == 7) { // 周日
            return date.minusDays(2); // 返回周五
        }
        return date; // 工作日直接返回
    }
}
