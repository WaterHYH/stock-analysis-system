package com.example.stock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class LogStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "stock.log";

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            Path currentLog = logDir.resolve(LOG_FILE);
            if (Files.exists(currentLog) && Files.size(currentLog) > 0) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
                Path archive = logDir.resolve("stock-" + timestamp + ".log");
                Files.move(currentLog, archive, StandardCopyOption.ATOMIC_MOVE);
                log.info("已将上次运行日志归档到: {}", archive.getFileName());
            }
        } catch (Exception e) {
            log.warn("日志归档失败: {}", e.getMessage());
        }
    }
}
