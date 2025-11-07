package com.example.stock.repository;

import com.example.stock.entity.StockHistory;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 股票历史数据自定义仓库实现类
 * 提供股票历史数据的批量插入功能
 */
@Repository
@RequiredArgsConstructor
public class StockHistoryCustomRepositoryImpl implements StockHistoryCustomRepository {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StockHistoryCustomRepositoryImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private static final int BATCH_SIZE = 1000; // 每批处理1000条

    /**
     * 批量插入股票历史数据
     * 优化：一次数据库连接插入所有数据，避免多次网络往返
     * @param histories 股票历史数据列表
     * @return 插入结果数组
     */
    @Override
    @Transactional
    public int[] batchInsertStockHistory(List<StockHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return new int[0];
        }

        String sql = """
            INSERT INTO stock_history
                (symbol, code, trade_date, open, high, low, close, volume,
                 ma_price5, ma_price10, ma_price30, ma_volume5, ma_volume10, ma_volume30)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                open = VALUES(open),
                high = VALUES(high),
                low = VALUES(low),
                close = VALUES(close),
                volume = VALUES(volume),
                ma_price5 = VALUES(ma_price5),
                ma_price10 = VALUES(ma_price10),
                ma_price30 = VALUES(ma_price30),
                ma_volume5 = VALUES(ma_volume5),
                ma_volume10 = VALUES(ma_volume10),
                ma_volume30 = VALUES(ma_volume30)
            """;

        int totalSize = histories.size();
        logger.info("一次性批量插入: 总记录数={}", totalSize);
        
        long startTime = System.currentTimeMillis();
        
        // 一次性批量插入所有数据，利用JDBC的批处理和MySQL的rewriteBatchedStatements优化
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setStockHistoryValues(ps, histories.get(i));
            }

            @Override
            public int getBatchSize() {
                return totalSize;
            }
        });
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("批量插入完成: 记录数={}, 耗时={}ms, 平均={}/ms", 
                totalSize, duration, String.format("%.2f", (double)totalSize / duration));
        
        return results;
    }

    /**
     * 设置PreparedStatement的参数值
     * @param ps PreparedStatement对象
     * @param history 股票历史数据
     * @throws SQLException SQL异常
     */
    private void setStockHistoryValues(PreparedStatement ps, StockHistory history) throws SQLException {
        ps.setString(1, history.getSymbol());
        ps.setString(2, history.getCode());
        ps.setDate(3, Date.valueOf(history.getDay()));
        ps.setDouble(4, history.getOpen());
        ps.setDouble(5, history.getHigh());
        ps.setDouble(6, history.getLow());
        ps.setDouble(7, history.getClose());
        ps.setLong(8, history.getVolume());
        ps.setDouble(9, history.getMaPrice5());
        ps.setDouble(10, history.getMaPrice10());
        ps.setDouble(11, history.getMaPrice30());
        ps.setLong(12, history.getMaVolume5());
        ps.setLong(13, history.getMaVolume10());
        ps.setLong(14, history.getMaVolume30());
    }
}

