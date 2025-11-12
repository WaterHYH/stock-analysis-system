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
                 ma_price5, ma_price10, ma_price30, ma_volume5, ma_volume10, ma_volume30,
                 change_percent, amplitude, turnover_rate,
                 is_ma5_golden_cross, is_ma5_death_cross, is_ma10_golden_cross, is_ma10_death_cross,
                 is_ma_bullish, is_ma_bearish,
                 kline_type, upper_shadow_ratio, lower_shadow_ratio, body_ratio,
                 is_doji, is_hammer, is_inverted_hammer,
                 consecutive_rise_days, is_break_high, is_break_low,
                 volume_ratio, is_volume_surge, is_volume_shrink, is_price_volume_match,
                 macd_dif, macd_dea, macd_bar, is_macd_golden_cross, is_macd_death_cross,
                 rsi6, rsi12, rsi24, is_overbought, is_oversold,
                 boll_upper, boll_middle, boll_lower, is_touch_boll_upper, is_touch_boll_lower)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                ma_volume30 = VALUES(ma_volume30),
                change_percent = VALUES(change_percent),
                amplitude = VALUES(amplitude),
                turnover_rate = VALUES(turnover_rate),
                is_ma5_golden_cross = VALUES(is_ma5_golden_cross),
                is_ma5_death_cross = VALUES(is_ma5_death_cross),
                is_ma10_golden_cross = VALUES(is_ma10_golden_cross),
                is_ma10_death_cross = VALUES(is_ma10_death_cross),
                is_ma_bullish = VALUES(is_ma_bullish),
                is_ma_bearish = VALUES(is_ma_bearish),
                kline_type = VALUES(kline_type),
                upper_shadow_ratio = VALUES(upper_shadow_ratio),
                lower_shadow_ratio = VALUES(lower_shadow_ratio),
                body_ratio = VALUES(body_ratio),
                is_doji = VALUES(is_doji),
                is_hammer = VALUES(is_hammer),
                is_inverted_hammer = VALUES(is_inverted_hammer),
                consecutive_rise_days = VALUES(consecutive_rise_days),
                is_break_high = VALUES(is_break_high),
                is_break_low = VALUES(is_break_low),
                volume_ratio = VALUES(volume_ratio),
                is_volume_surge = VALUES(is_volume_surge),
                is_volume_shrink = VALUES(is_volume_shrink),
                is_price_volume_match = VALUES(is_price_volume_match),
                macd_dif = VALUES(macd_dif),
                macd_dea = VALUES(macd_dea),
                macd_bar = VALUES(macd_bar),
                is_macd_golden_cross = VALUES(is_macd_golden_cross),
                is_macd_death_cross = VALUES(is_macd_death_cross),
                rsi6 = VALUES(rsi6),
                rsi12 = VALUES(rsi12),
                rsi24 = VALUES(rsi24),
                is_overbought = VALUES(is_overbought),
                is_oversold = VALUES(is_oversold),
                boll_upper = VALUES(boll_upper),
                boll_middle = VALUES(boll_middle),
                boll_lower = VALUES(boll_lower),
                is_touch_boll_upper = VALUES(is_touch_boll_upper),
                is_touch_boll_lower = VALUES(is_touch_boll_lower)
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
        int idx = 1;
        // 基础字段
        ps.setString(idx++, history.getSymbol());
        ps.setString(idx++, history.getCode());
        ps.setDate(idx++, Date.valueOf(history.getDay()));
        ps.setDouble(idx++, history.getOpen());
        ps.setDouble(idx++, history.getHigh());
        ps.setDouble(idx++, history.getLow());
        ps.setDouble(idx++, history.getClose());
        ps.setLong(idx++, history.getVolume());
        ps.setDouble(idx++, history.getMaPrice5());
        ps.setDouble(idx++, history.getMaPrice10());
        ps.setDouble(idx++, history.getMaPrice30());
        ps.setLong(idx++, history.getMaVolume5());
        ps.setLong(idx++, history.getMaVolume10());
        ps.setLong(idx++, history.getMaVolume30());
        
        // K线分析字段
        setDoubleOrNull(ps, idx++, history.getChangePercent());
        setDoubleOrNull(ps, idx++, history.getAmplitude());
        setDoubleOrNull(ps, idx++, history.getTurnoverRate());
        
        // 均线系统分析
        setBooleanOrNull(ps, idx++, history.getIsMa5GoldenCross());
        setBooleanOrNull(ps, idx++, history.getIsMa5DeathCross());
        setBooleanOrNull(ps, idx++, history.getIsMa10GoldenCross());
        setBooleanOrNull(ps, idx++, history.getIsMa10DeathCross());
        setBooleanOrNull(ps, idx++, history.getIsMaBullish());
        setBooleanOrNull(ps, idx++, history.getIsMaBearish());
        
        // K线形态分析
        setIntegerOrNull(ps, idx++, history.getKlineType());
        setDoubleOrNull(ps, idx++, history.getUpperShadowRatio());
        setDoubleOrNull(ps, idx++, history.getLowerShadowRatio());
        setDoubleOrNull(ps, idx++, history.getBodyRatio());
        setBooleanOrNull(ps, idx++, history.getIsDoji());
        setBooleanOrNull(ps, idx++, history.getIsHammer());
        setBooleanOrNull(ps, idx++, history.getIsInvertedHammer());
        
        // 趋势分析
        setIntegerOrNull(ps, idx++, history.getConsecutiveRiseDays());
        setBooleanOrNull(ps, idx++, history.getIsBreakHigh());
        setBooleanOrNull(ps, idx++, history.getIsBreakLow());
        
        // 成交量分析
        setDoubleOrNull(ps, idx++, history.getVolumeRatio());
        setBooleanOrNull(ps, idx++, history.getIsVolumeSurge());
        setBooleanOrNull(ps, idx++, history.getIsVolumeShrink());
        setBooleanOrNull(ps, idx++, history.getIsPriceVolumeMatch());
        
        // 技术指标
        setDoubleOrNull(ps, idx++, history.getMacdDif());
        setDoubleOrNull(ps, idx++, history.getMacdDea());
        setDoubleOrNull(ps, idx++, history.getMacdBar());
        setBooleanOrNull(ps, idx++, history.getIsMacdGoldenCross());
        setBooleanOrNull(ps, idx++, history.getIsMacdDeathCross());
        setDoubleOrNull(ps, idx++, history.getRsi6());
        setDoubleOrNull(ps, idx++, history.getRsi12());
        setDoubleOrNull(ps, idx++, history.getRsi24());
        setBooleanOrNull(ps, idx++, history.getIsOverbought());
        setBooleanOrNull(ps, idx++, history.getIsOversold());
        setDoubleOrNull(ps, idx++, history.getBollUpper());
        setDoubleOrNull(ps, idx++, history.getBollMiddle());
        setDoubleOrNull(ps, idx++, history.getBollLower());
        setBooleanOrNull(ps, idx++, history.getIsTouchBollUpper());
        setBooleanOrNull(ps, idx++, history.getIsTouchBollLower());
    }
    
    /**
     * 设置Double类型参数，支持null值
     */
    private void setDoubleOrNull(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }
    
    /**
     * 设置Boolean类型参数，支持null值
     */
    private void setBooleanOrNull(PreparedStatement ps, int index, Boolean value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BOOLEAN);
        } else {
            ps.setBoolean(index, value);
        }
    }
    
    /**
     * 设置Integer类型参数，支持null值
     */
    private void setIntegerOrNull(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}

