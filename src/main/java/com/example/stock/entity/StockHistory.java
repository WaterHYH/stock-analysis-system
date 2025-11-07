package com.example.stock.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 股票历史数据实体类
 * 映射数据库中的stock_history表，存储股票的历史交易数据
 */
@Entity
@Table(name = "stock_history")
@Data  // Lombok 自动生成 getter/setter
public class StockHistory {
    /**
     * 主键ID，自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码（如sh600000）
     */
    @Column(name = "symbol", nullable = false, length = 20) // 股票代码（如sh600000）
    private String symbol;

    /**
     * 解析后的纯数字代码（如600000）
     */
    @Column(name = "code", nullable = false, length = 10) // 解析后的纯数字代码（如600000）‌
    private String code;//股票代码

    /**
     * 交易日期（YYYY-MM-DD）
     */
    @Column(name = "trade_date", nullable = false) // 交易日期（YYYY-MM-DD）
    private LocalDate day;

    /**
     * 开盘价（示例值：10.870）
     */
    @Column(name = "open")
    private double open; // 开盘价（示例值：10.870）

    /**
     * 最高价
     */
    @Column(name = "high")
    private double high; // 最高价

    /**
     * 最低价
     */
    @Column(name = "low")
    private double low; // 最低价

    /**
     * 收盘价
     */
    @Column(name = "close")
    private double close; // 收盘价

    /**
     * 成交量（示例值：58387902）
     */
    @Column(name = "volume")
    private long volume; // 成交量（示例值：58387902）‌

    /**
     * 5日均价（示例值：10.822）
     */
    @Column(name = "maPrice5")
    private double maPrice5; // 5日均价（示例值：10.822）

    /**
     * 10日均价
     */
    @Column(name = "maPrice10")
    private double maPrice10; // 10日均价

    /**
     * 30日均价
     */
    @Column(name = "maPrice30")
    private double maPrice30; // 30日均价

    /**
     * 5日均成交量
     */
    @Column(name = "maVolume5")
    private long maVolume5; // 5日均成交量
    
    /**
     * 10日均成交量
     */
    @Column(name = "maVolume10")
    private long maVolume10; // 10日均成交量
    
    /**
     * 30日均成交量
     */
    @Column(name = "maVolume30")
    private long maVolume30; // 30日均成交量

    // Getters and Setters...

    /*@PrePersist
    @PreUpdate
    private void parseCodeFromSymbol() {
        if (this.symbol != null && this.symbol.length() >= 8) {
            this.code = this.symbol.substring(2); // 去除前两位交易所标识（如sh/sz）
        }
    }*/

}

