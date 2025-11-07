package com.example.stock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * 线程池配置类
 * 配置用于股票数据同步的线程池
 */
@Configuration //必须添加此注解
public class ExecutorConfig {

    /**
     * 配置股票数据同步任务的线程池
     * @return TaskExecutor线程池实例
     */
    @Bean(name = "syncTaskExecutor") // 明确指定Bean名称
    public TaskExecutor syncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("stock-sync-"); // 建议添加线程名前缀
        executor.initialize(); //必须初始化
        return executor;
    }
}

