package com.example.stock.repository;

import com.example.stock.entity.StockSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 股票同步日志 Repository
 */
@Repository
public interface StockSyncLogRepository extends JpaRepository<StockSyncLog, Long> {
    /**
     * 查询某个股票在特定日期是否已同步过
     */
    Optional<StockSyncLog> findBySymbolAndSyncDate(String symbol, LocalDate syncDate);
}
