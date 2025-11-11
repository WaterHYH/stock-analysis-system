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

    // ==================== K线分析字段 ====================
    
    /**
     * 涨跌幅 (相对前一交易日的涨跌百分比)
     */
    @Column(name = "change_percent")
    private Double changePercent;
    
    /**
     * 振幅 ((最高价-最低价)/前收盘价*100)
     */
    @Column(name = "amplitude")
    private Double amplitude;
    
    /**
     * 换手率 (成交量/流通股本*100)
     */
    @Column(name = "turnover_rate")
    private Double turnoverRate;
    
    // ==================== 均线系统分析 ====================
    
    /**
     * MA5金叉标志 (MA5向上穿过MA10)
     */
    @Column(name = "is_ma5_golden_cross")
    private Boolean isMa5GoldenCross;
    
    /**
     * MA5死叉标志 (MA5向下穿过MA10)
     */
    @Column(name = "is_ma5_death_cross")
    private Boolean isMa5DeathCross;
    
    /**
     * MA10金叉标志 (MA10向上穿过MA30)
     */
    @Column(name = "is_ma10_golden_cross")
    private Boolean isMa10GoldenCross;
    
    /**
     * MA10死叉标志 (MA10向下穿过MA30)
     */
    @Column(name = "is_ma10_death_cross")
    private Boolean isMa10DeathCross;
    
    /**
     * 均线多头排列 (MA5 > MA10 > MA30)
     */
    @Column(name = "is_ma_bullish")
    private Boolean isMaBullish;
    
    /**
     * 均线空头排列 (MA5 < MA10 < MA30)
     */
    @Column(name = "is_ma_bearish")
    private Boolean isMaBearish;
    
    // ==================== K线形态分析 ====================
    
    /**
     * K线类型 (0:阴线, 1:阳线, 2:十字星)
     */
    @Column(name = "kline_type")
    private Integer klineType;
    
    /**
     * 上影线长度占比 ((最高价-max(开盘,收盘))/(最高-最低)*100)
     */
    @Column(name = "upper_shadow_ratio")
    private Double upperShadowRatio;
    
    /**
     * 下影线长度占比 ((min(开盘,收盘)-最低价)/(最高-最低)*100)
     */
    @Column(name = "lower_shadow_ratio")
    private Double lowerShadowRatio;
    
    /**
     * 实体长度占比 (abs(收盘-开盘)/(最高-最低)*100)
     */
    @Column(name = "body_ratio")
    private Double bodyRatio;
    
    /**
     * 是否为十字星 (实体长度占比 < 5%)
     */
    @Column(name = "is_doji")
    private Boolean isDoji;
    
    /**
     * 是否为锤子线 (下影线长、上影线短、实体小)
     */
    @Column(name = "is_hammer")
    private Boolean isHammer;
    
    /**
     * 是否为倒锤子线 (上影线长、下影线短、实体小)
     */
    @Column(name = "is_inverted_hammer")
    private Boolean isInvertedHammer;
    
    // ==================== 趋势分析 ====================
    
    /**
     * 连续上涨天数 (正数表示连涨,负数表示连跌)
     */
    @Column(name = "consecutive_rise_days")
    private Integer consecutiveRiseDays;
    
    /**
     * 是否突破前高 (收盘价 > 近N日最高价)
     */
    @Column(name = "is_break_high")
    private Boolean isBreakHigh;
    
    /**
     * 是否跌破前低 (收盘价 < 近N日最低价)
     */
    @Column(name = "is_break_low")
    private Boolean isBreakLow;
    
    // ==================== 成交量分析 ====================
    
    /**
     * 成交量相对5日均量比例 (当日成交量/MA5成交量)
     */
    @Column(name = "volume_ratio")
    private Double volumeRatio;
    
    /**
     * 是否放量 (成交量 > 1.5倍5日均量)
     */
    @Column(name = "is_volume_surge")
    private Boolean isVolumeSurge;
    
    /**
     * 是否缩量 (成交量 < 0.5倍5日均量)
     */
    @Column(name = "is_volume_shrink")
    private Boolean isVolumeShrink;
    
    /**
     * 量价配合 (价涨量增或价跌量缩)
     */
    @Column(name = "is_price_volume_match")
    private Boolean isPriceVolumeMatch;
    
    // ==================== 技术指标 ====================
    
    /**
     * MACD DIF值
     */
    @Column(name = "macd_dif")
    private Double macdDif;
    
    /**
     * MACD DEA值
     */
    @Column(name = "macd_dea")
    private Double macdDea;
    
    /**
     * MACD柱状图值
     */
    @Column(name = "macd_bar")
    private Double macdBar;
    
    /**
     * MACD金叉 (DIF向上穿过DEA)
     */
    @Column(name = "is_macd_golden_cross")
    private Boolean isMacdGoldenCross;
    
    /**
     * MACD死叉 (DIF向下穿过DEA)
     */
    @Column(name = "is_macd_death_cross")
    private Boolean isMacdDeathCross;
    
    /**
     * RSI指标值 (6日)
     */
    @Column(name = "rsi6")
    private Double rsi6;
    
    /**
     * RSI指标值 (12日)
     */
    @Column(name = "rsi12")
    private Double rsi12;
    
    /**
     * RSI指标值 (24日)
     */
    @Column(name = "rsi24")
    private Double rsi24;
    
    /**
     * 是否超买 (RSI6 > 80)
     */
    @Column(name = "is_overbought")
    private Boolean isOverbought;
    
    /**
     * 是否超卖 (RSI6 < 20)
     */
    @Column(name = "is_oversold")
    private Boolean isOversold;
    
    /**
     * 布林带上轨
     */
    @Column(name = "boll_upper")
    private Double bollUpper;
    
    /**
     * 布林带中轨
     */
    @Column(name = "boll_middle")
    private Double bollMiddle;
    
    /**
     * 布林带下轨
     */
    @Column(name = "boll_lower")
    private Double bollLower;
    
    /**
     * 是否触及布林带上轨
     */
    @Column(name = "is_touch_boll_upper")
    private Boolean isTouchBollUpper;
    
    /**
     * 是否触及布林带下轨
     */
    @Column(name = "is_touch_boll_lower")
    private Boolean isTouchBollLower;

    // Getters and Setters...

    /*@PrePersist
    @PreUpdate
    private void parseCodeFromSymbol() {
        if (this.symbol != null && this.symbol.length() >= 8) {
            this.code = this.symbol.substring(2); // 去除前两位交易所标识（如sh/sz）
        }
    }*/

}

