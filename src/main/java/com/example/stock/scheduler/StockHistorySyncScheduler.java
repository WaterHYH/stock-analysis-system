package com.example.stock.scheduler;

import com.example.stock.service.StockHistoryFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 股票历史数据同步定时任务调度器
 * 负责定时触发股票历史数据的同步任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockHistorySyncScheduler {
    private final StockHistoryFetchService historyFetchService;
    private final TaskExecutor syncTaskExecutor;

    /**
     * 项目启动时立即执行的股票历史数据同步任务
     * 使用异步线程池执行，避免阻塞主线程
     */
    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    public void syncStockHistoryOnStartup() {
        syncTaskExecutor.execute(() -> {
            log.info("项目启动时开始同步股票历史数据，时间：{}", LocalDateTime.now());
            historyFetchService.fetchAllStockHistory();
            log.info("项目启动时股票历史数据同步完成");
        });
    }

    /**
     * 每天16:00执行的股票历史数据同步任务
     * 使用异步线程池执行，避免阻塞主线程
     */
    @Scheduled(cron = "0 0 16 * * ?")
    public void syncStockHistoryDaily() {
        syncTaskExecutor.execute(() -> {
            log.info("开始每日定时同步股票历史数据，时间：{}", LocalDateTime.now());
            historyFetchService.fetchAllStockHistory();
            log.info("每日股票历史数据同步完成");
        });
    }
}
