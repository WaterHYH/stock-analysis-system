package com.example.stock.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 股票历史数据传输对象
 * 用于接收和传输新浪财经K线接口返回的股票历史数据
 * 接口文档：https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockHistoryDTO {
    //------------------------------------
    // 核心标识字段（手动注入）
    //------------------------------------
    /**
     * 原始股票标识（如sh600000）
     */
    private String symbol; // 原始股票标识（如sh600000）
    
    /**
     * 解析后的纯数字代码（自动去除sh/sz前缀）（手动注入）
     */
    private String code;  // 解析后的纯数字代码（自动去除sh/sz前缀）（手动注入）

    //------------------------------------
    // 时间与价格字段
    //------------------------------------
    /**
     * 交易日期
     */
    @JsonProperty("day")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate day; // 交易日期

    /**
     * 开盘价（JSON字段名直接映射）
     */
    @JsonProperty("open")
    private double open; // 开盘价（JSON字段名直接映射）

    /**
     * 最高价
     */
    @JsonProperty("high")
    private double high; // 最高价

    /**
     * 最低价
     */
    @JsonProperty("low")
    private double low; // 最低价

    /**
     * 收盘价
     */
    @JsonProperty("close")
    private double close; // 收盘价

    //------------------------------------
    // 成交量/额字段
    //------------------------------------
    /**
     * 成交量（单位：股）
     */
    @JsonProperty("volume")
    private Long volume; // 成交量（单位：股）

    //------------------------------------
    // 均线指标（接口返回带下划线的字段名）
    //------------------------------------
    /**
     * 5日均价
     */
    @JsonProperty("ma_price5")
    private double maPrice5; // 5日均价

    /**
     * 5日均成交量
     */
    @JsonProperty("ma_volume5")
    private long maVolume5; // 5日均成交量

    /**
     * 10日均价
     */
    @JsonProperty("ma_price10")
    private double maPrice10; // 10日均价

    /**
     * 10日均成交量
     */
    @JsonProperty("ma_volume10")
    private long maVolume10;

    /**
     * 30日均价
     */
    @JsonProperty("ma_price30")
    private double maPrice30; // 30日均价

    /**
     * 30日均成交量
     */
    @JsonProperty("ma_volume30")
    private long maVolume30;

}


