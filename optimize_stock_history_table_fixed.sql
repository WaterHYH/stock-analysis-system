-- ========================================
-- 股票历史数据表优化脚本（修正版）
-- 目标：减少磁盘空间占用，从2.8GB压缩到约1GB
-- 优化方式：调整字段类型，使用更紧凑的数据类型
-- ========================================

USE stock_db;

-- 优化表结构 - 修正版本
ALTER TABLE stock_history
    MODIFY COLUMN open DECIMAL(8,3),
    MODIFY COLUMN high DECIMAL(8,3),
    MODIFY COLUMN low DECIMAL(8,3),
    MODIFY COLUMN close DECIMAL(8,3),
    MODIFY COLUMN ma_price5 DECIMAL(8,3),
    MODIFY COLUMN ma_price10 DECIMAL(8,3),
    MODIFY COLUMN ma_price30 DECIMAL(8,3),
    MODIFY COLUMN ma_volume5 BIGINT,
    MODIFY COLUMN ma_volume10 BIGINT,
    MODIFY COLUMN ma_volume30 BIGINT,
    MODIFY COLUMN change_percent DECIMAL(6,2),
    MODIFY COLUMN amplitude DECIMAL(6,2),
    MODIFY COLUMN turnover_rate DECIMAL(6,2),
    MODIFY COLUMN is_ma5_golden_cross TINYINT(1),
    MODIFY COLUMN is_ma5_death_cross TINYINT(1),
    MODIFY COLUMN is_ma10_golden_cross TINYINT(1),
    MODIFY COLUMN is_ma10_death_cross TINYINT(1),
    MODIFY COLUMN is_ma_bullish TINYINT(1),
    MODIFY COLUMN is_ma_bearish TINYINT(1),
    MODIFY COLUMN kline_type TINYINT,
    MODIFY COLUMN upper_shadow_ratio DECIMAL(5,2),
    MODIFY COLUMN lower_shadow_ratio DECIMAL(5,2),
    MODIFY COLUMN body_ratio DECIMAL(5,2),
    MODIFY COLUMN is_doji TINYINT(1),
    MODIFY COLUMN is_hammer TINYINT(1),
    MODIFY COLUMN is_inverted_hammer TINYINT(1),
    MODIFY COLUMN consecutive_rise_days SMALLINT,
    MODIFY COLUMN is_break_high TINYINT(1),
    MODIFY COLUMN is_break_low TINYINT(1),
    MODIFY COLUMN volume_ratio DECIMAL(6,2),
    MODIFY COLUMN is_volume_surge TINYINT(1),
    MODIFY COLUMN is_volume_shrink TINYINT(1),
    MODIFY COLUMN is_price_volume_match TINYINT(1),
    MODIFY COLUMN macd_dif DECIMAL(10,4),
    MODIFY COLUMN macd_dea DECIMAL(10,4),
    MODIFY COLUMN macd_bar DECIMAL(10,4),
    MODIFY COLUMN is_macd_golden_cross TINYINT(1),
    MODIFY COLUMN is_macd_death_cross TINYINT(1),
    MODIFY COLUMN rsi6 DECIMAL(5,2),
    MODIFY COLUMN rsi12 DECIMAL(5,2),
    MODIFY COLUMN rsi24 DECIMAL(5,2),
    MODIFY COLUMN is_overbought TINYINT(1),
    MODIFY COLUMN is_oversold TINYINT(1),
    MODIFY COLUMN boll_upper DECIMAL(8,3),
    MODIFY COLUMN boll_middle DECIMAL(8,3),
    MODIFY COLUMN boll_lower DECIMAL(8,3),
    MODIFY COLUMN is_touch_boll_upper TINYINT(1),
    MODIFY COLUMN is_touch_boll_lower TINYINT(1);

-- 优化表，回收空间
OPTIMIZE TABLE stock_history;

-- 查看优化后的表大小
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)',
    table_rows AS 'Rows'
FROM information_schema.tables 
WHERE table_schema = 'stock_db' 
AND table_name = 'stock_history';

-- ========================================
-- 关键修正：
-- 均量字段(ma_volume5, ma_volume10, ma_volume30)
-- 保持使用 BIGINT，因为数据范围超过INT的最大值
-- ========================================
