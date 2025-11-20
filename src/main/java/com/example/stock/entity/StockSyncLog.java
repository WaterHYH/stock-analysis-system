package com.example.stock.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * 股票同步日志实体类
 * 记录每个股票在每天是否已经同步过历史数据
 */
@Entity
@Table(name = "stock_sync_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSyncLog {
    @Id
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "sync_date", nullable = false)
    private LocalDate syncDate;
}
