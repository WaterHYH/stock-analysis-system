package com.example.stock.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票同步日志实体类
 * 记录每个股票在每天是否已经同步过历史数据
 */
@Entity
@Table(name = "stock_sync_log", indexes = {
    @Index(name = "uk_symbol_sync_date", columnList = "symbol,sync_date", unique = true),
    @Index(name = "idx_sync_date", columnList = "sync_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "sync_date", nullable = false)
    private LocalDate syncDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
