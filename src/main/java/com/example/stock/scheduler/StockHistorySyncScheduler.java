package com.example.stock.scheduler;

import com.example.stock.service.StockHistoryFetchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 股票历史数据同步定时任务调度器
 * 负责定时触发股票历史数据的同步任务
 */
@Component
@RequiredArgsConstructor
public class StockHistorySyncScheduler {
    private static final Logger logger = LoggerFactory.getLogger(StockHistorySyncScheduler.class);
    private final StockHistoryFetchService historyFetchService;
    private final TaskExecutor syncTaskExecutor;

    /**
     * 每天16:30执行的股票历史数据同步任务
     * 使用异步线程池执行，避免阻塞主线程
     */
    @Scheduled(cron = "0 20 7 * * ?")
    public void syncStockHistory() {
        syncTaskExecutor.execute(() -> {
            logger.info("开始同步股票历史数据，时间：{}", LocalDateTime.now());
            historyFetchService.fetchAllStockHistory();
            logger.info("股票历史数据同步完成");
        });
    }
}
