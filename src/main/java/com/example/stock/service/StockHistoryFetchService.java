package com.example.stock.service;

import com.example.stock.dto.StockHistoryDTO;
import com.example.stock.entity.StockHistory;
import com.example.stock.entity.StockSyncLog;
import com.example.stock.repository.StockHistoryRepository;
import com.example.stock.repository.StockSyncLogRepository;
import com.example.stock.service.client.SinaStockClient;
import com.example.stock.service.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
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
    private final StockSyncLogRepository stockSyncLogRepository;
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
     * 如果当天已经获取过则跳过
     * @param code 股票代码
     */
    private void processStock(int code) {
        String symbol = generateSymbol(code);
        if (symbol != null) {
            // ... existing code ...
            // 检查该股票今天是否已经同步过
            if (stockSyncLogRepository.findBySymbolAndSyncDate(symbol, LocalDate.now()).isPresent()) {
                logger.debug("今天已同步过此股票，跳过: symbol={}", symbol);
                return;
            }
            
            int insertedCount = fetchAndSaveHistory(symbol);
            // 只有实际插入了数据才需要延时
            if (insertedCount > 0) {
                logger.info("成功插入{}条新记录，执行延时", insertedCount);
                // 记录同步日志
                StockSyncLog syncLog = new StockSyncLog();
                syncLog.setSymbol(symbol);
                syncLog.setSyncDate(LocalDate.now());
                stockSyncLogRepository.save(syncLog);
                
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("历史数据获取被中断", e);
                }
            } else {
                // 即使没有新数据也要记录同步
                StockSyncLog syncLog = new StockSyncLog();
                syncLog.setSymbol(symbol);
                syncLog.setSyncDate(LocalDate.now());
                stockSyncLogRepository.save(syncLog);
            }
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
            logger.error("Symbol不能为空");
            return 0;
        }

        logger.info("开始获取股票历史数据: symbol={}", symbol);
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
                logger.info("✅ 数据库中最新记录已是最近的交易日({})，无需调用API，直接跳过", latestDbDate);
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
            logger.info("数据库中最新记录: {}, 将从{}开始处理，仅获取{}天的数据",
                    latestDbDate, fromDate, datalen);
        } else {
            logger.info("数据库中无此股票数据，将推出全量获取");
        }

        // 2. 获取数据阶段
        long fetchStartTime = System.currentTimeMillis();
        List<StockHistoryDTO> historyList = stockClient.getStockHistory(symbol, datalen);
        long fetchDuration = System.currentTimeMillis() - fetchStartTime;
        logger.info("⏱️ 获取数据耗时: {}ms, symbol={}", fetchDuration, symbol);

        if (historyList == null || historyList.isEmpty()) {
            logger.info("未获取到股票历史数据: symbol={}", symbol);
            return 0;
        }

        // 3. 数据转换阶段
        long mapStartTime = System.currentTimeMillis();
        List<StockHistory> entities = stockMapper.toStockHistoryList(historyList);
        long mapDuration = System.currentTimeMillis() - mapStartTime;
        logger.info("⏱️ 数据转换耗时: {}ms, 记录数={}", mapDuration, entities.size());

        // 4. 数据排序阶段：按日期降序排列（最新到最旧），确保K线分析能正确获取前一天数据
        long sortStartTime = System.currentTimeMillis();
        entities.sort((a, b) -> b.getDay().compareTo(a.getDay()));
        long sortDuration = System.currentTimeMillis() - sortStartTime;
        logger.info("⏱️ 数据排序完成，按日期降序排列，耗时: {}ms", sortDuration);

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
        logger.info("⏱️ 过滤新记录耗时: {}ms, 原数据={}, 新记录数={}",
                filterDuration, entities.size(), newRecords.size());

        // 如果没有新记录，不需要执行后续处理
        if (newRecords.isEmpty()) {
            logger.info("✅ symbol={}的数据已是最新，没有新记录需要写入", symbol);
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
        logger.info("⏱️ K线分析耗时: {}ms, 记录数={}", analysisDuration, newRecords.size());

        // 7. 批量插入阶段（仅插入新记录）
        long insertStartTime = System.currentTimeMillis();
        int[] result = stockHistoryRepository.batchInsertStockHistory(newRecords);
        long insertDuration = System.currentTimeMillis() - insertStartTime;
        logger.info("⏱️ 批量插入耗时: {}ms, 记录数={}", insertDuration, result.length);

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        logger.info("✅ 成功保存股票历史数据: symbol={}, 新增数据数={}, 总耗时={}ms (获取:{}ms, 转换:{}ms, 过滤:{}ms, 分析:{}ms, 插入:{}ms)",
                symbol, result.length, totalDuration, fetchDuration, mapDuration, filterDuration, analysisDuration, insertDuration);

        return result.length;  // 返回实际插入的记录数
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
