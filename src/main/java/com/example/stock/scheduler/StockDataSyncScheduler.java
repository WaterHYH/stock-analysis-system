package com.example.stock.scheduler;

import com.example.stock.service.StockDataFetchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 股票数据同步定时任务调度器
 * 负责定时触发股票实时数据的同步任务
 */
@Component
@RequiredArgsConstructor
public class StockDataSyncScheduler {
    private static final Logger logger = LoggerFactory.getLogger(StockDataSyncScheduler.class);
    private final StockDataFetchService dataFetchService;
    private final TaskExecutor syncTaskExecutor;

    /**
     * 每天3:10执行的股票数据同步任务
     * 使用异步线程池执行，避免阻塞主线程
     */
    @Scheduled(cron = "0 10 3 * * ?")
    public void syncStockData() {
        syncTaskExecutor.execute(() -> {
            logger.info("开始同步股票实时数据，时间：{}", LocalDateTime.now());
            int count = dataFetchService.fetchAndSaveStockData();
            logger.info("股票实时数据同步完成，同步记录数：{}", count);
        });
    }
}
