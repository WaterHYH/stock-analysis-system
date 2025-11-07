package com.example.stock.repository;

import com.example.stock.entity.StockHistory;

import java.util.List;

/**
 * 股票历史数据自定义仓库接口
 * 提供股票历史数据的自定义数据访问方法
 */
public interface StockHistoryCustomRepository {
    /**
     * 批量插入股票历史数据
     * @param histories 股票历史数据列表
     * @return 插入结果数组
     */
    int[] batchInsertStockHistory(List<StockHistory> histories);
}

