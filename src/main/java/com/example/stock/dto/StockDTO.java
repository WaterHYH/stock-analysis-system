package com.example.stock.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票数据传输对象
 * 用于接收和传输新浪财经API返回的股票实时数据
 * 适配新浪财经API返回字段结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDTO {
    /**
     * 股票代码（如sh600000）
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * 股票数字代码（如600000）
     */
    @JsonProperty("code")
    private String code;

    /**
     * 股票名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 最新价
     */
    @JsonProperty("trade")
    private BigDecimal tradePrice;

    /**
     * 涨跌额
     */
    @JsonProperty("pricechange")
    private BigDecimal priceChange;

    /**
     * 涨跌幅（示例值 1.098 表示 1.098%）
     */
    @JsonProperty("changepercent")
    private BigDecimal changePercent;  // 示例值 1.098 表示 1.098%

    /**
     * 买一价
     */
    @JsonProperty("buy")
    private BigDecimal bidPrice;

    /**
     * 卖一价
     */
    @JsonProperty("sell")
    private BigDecimal askPrice;

    /**
     * 昨日收盘价
     */
    @JsonProperty("settlement")
    private BigDecimal previousClose;

    /**
     * 开盘价
     */
    @JsonProperty("open")
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    @JsonProperty("high")
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    @JsonProperty("low")
    private BigDecimal lowPrice;

    /**
     * 成交量（整数）
     */
    @JsonProperty("volume")
    private Long volume;          // 成交量（整数）

    /**
     * 成交金额（可能含小数）
     */
    @JsonProperty("amount")
    private BigDecimal amount;     // 成交金额（可能含小数）

    /**
     * 最后交易时间
     */
    @JsonProperty("ticktime")
    private String lastTradeTime;

    /**
     * 市盈率
     */
    @JsonProperty("per")
    private BigDecimal peRatio;

    /**
     * 市净率
     */
    @JsonProperty("pb")
    private BigDecimal pbRatio;

    /**
     * 总市值
     */
    @JsonProperty("mktcap")
    private BigDecimal marketCap;

    /**
     * 流通市值
     */
    @JsonProperty("nmc")
    private BigDecimal circulatingMarketCap;

    /**
     * 换手率
     */
    @JsonProperty("turnoverratio")
    private BigDecimal turnoverRate;

    /**
     * 交易日期
     */
    @JsonProperty("trade_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate tradeDate;

    // 省略 Getter/Setter（需生成或使用 Lombok @Data）
}


