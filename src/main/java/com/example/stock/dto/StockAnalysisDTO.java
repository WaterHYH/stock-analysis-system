package com.example.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 股票分析结果DTO
 * 用于展示股票分析筛选结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAnalysisDTO {
    /**
     * 股票代码
     */
    private String symbol;
    
    /**
     * 股票名称（从实时数据表获取）
     */
    private String name;
    
    /**
     * 当前价格
     */
    private Double currentPrice;
    
    /**
     * 历史最高价
     */
    private Double historicalHigh;
    
    /**
     * 历史最低价
     */
    private Double historicalLow;
    
    /**
     * 最高价日期
     */
    private LocalDate highDate;
    
    /**
     * 最低价日期
     */
    private LocalDate lowDate;
    
    /**
     * 距离最高价的跌幅百分比
     */
    private Double dropPercentage;
    
    /**
     * 最近半年波动次数
     */
    private Integer volatilityCount;
    
    /**
     * 匹配的条件描述
     */
    private String matchedCondition;
}
