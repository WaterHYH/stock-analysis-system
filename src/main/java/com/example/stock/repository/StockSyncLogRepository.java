package com.example.stock.repository;

import com.example.stock.entity.StockSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 股票同步日志 Repository
 */
@Repository
public interface StockSyncLogRepository extends JpaRepository<StockSyncLog, String> {
    /**
     * 查询所有同步日志
     */
    List<StockSyncLog> findAll();
    
    /**
     * 根据symbol查询同步日志
     */
    Optional<StockSyncLog> findBySymbol(String symbol);
}
