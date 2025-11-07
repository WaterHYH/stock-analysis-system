package com.example.stock.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 股票实体类
 * 映射数据库中的stock表，存储股票的实时交易数据
 */
@Entity
@Table(name = "stock")
@Data
public class Stock {
    /**
     * 主键ID，自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票唯一标识符（如：sh600000）
     */
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /**
     * 股票代码（如：600000）
     */
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    /**
     * 股票名称（如：浦发银行）
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 最新价
     */
    @Column(name = "trade_price", precision = 12, scale = 4)
    private BigDecimal tradePrice;

    /**
     * 涨跌额
     */
    @Column(name = "price_change", precision = 12, scale = 4)
    private BigDecimal priceChange;

    /**
     * 涨跌幅（示例：1.098 表示 1.098%）
     */
    @Column(name = "change_percent", precision = 6, scale = 3)
    private BigDecimal changePercent;

    /**
     * 买一价
     */
    @Column(name = "bid_price", precision = 12, scale = 4)
    private BigDecimal bidPrice;

    /**
     * 卖一价
     */
    @Column(name = "ask_price", precision = 12, scale = 4)
    private BigDecimal askPrice;

    /**
     * 昨日收盘价
     */
    @Column(name = "previous_close", precision = 12, scale = 4)
    private BigDecimal previousClose;

    /**
     * 开盘价
     */
    @Column(name = "open_price", precision = 12, scale = 4)
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    @Column(name = "high_price", precision = 12, scale = 4)
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    @Column(name = "low_price", precision = 12, scale = 4)
    private BigDecimal lowPrice;

    /**
     * 成交量（股数）
     */
    @Column(name = "volume")
    private Long volume;

    /**
     * 成交金额（元）
     */
    @Column(name = "amount", precision = 20, scale = 4)
    private BigDecimal amount;

    /**
     * 最后交易时间
     */
    @Column(name = "last_trade_time")
    private LocalTime lastTradeTime;

    /**
     * 市盈率
     */
    @Column(name = "pe_ratio", precision = 10, scale = 2)
    private BigDecimal peRatio;

    /**
     * 市净率
     */
    @Column(name = "pb_ratio", precision = 10, scale = 3)
    private BigDecimal pbRatio;

    /**
     * 总市值
     */
    @Column(name = "market_cap", precision = 20, scale = 4)
    private BigDecimal marketCap;

    /**
     * 流通市值
     */
    @Column(name = "circulating_market_cap", precision = 20, scale = 4)
    private BigDecimal circulatingMarketCap;

    /**
     * 换手率
     */
    @Column(name = "turnover_rate", precision = 10, scale = 5)
    private BigDecimal turnoverRate;

    /**
     * 数据入库时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 交易日期
     */
    @Column(name = "trade_date")
    private LocalDate tradeDate;
}
