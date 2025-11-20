-- 增量式优化 stock_history 表
-- 这个方法逐步修改字段类型，避免一次性重建表导致的问题
-- 每次修改后检查表状态，确保操作成功

-- 第一批：修改价格相关字段（open, high, low, close）
-- 这些字段从 DOUBLE 改为 DECIMAL(8,3)，节省空间
ALTER TABLE stock_history 
    MODIFY COLUMN open DECIMAL(8,3),
    MODIFY COLUMN high DECIMAL(8,3),
    MODIFY COLUMN low DECIMAL(8,3),
    MODIFY COLUMN close DECIMAL(8,3);

-- 验证第一批修改
SELECT COUNT(*) as '修改后行数' FROM stock_history;

-- 第二批：修改均价字段
ALTER TABLE stock_history 
    MODIFY COLUMN ma_price5 DECIMAL(8,3),
    MODIFY COLUMN ma_price10 DECIMAL(8,3),
    MODIFY COLUMN ma_price30 DECIMAL(8,3);

-- 验证第二批修改
SELECT COUNT(*) as '修改后行数' FROM stock_history;

-- 第三批：修改百分比和比值字段
ALTER TABLE stock_history 
    MODIFY COLUMN change_percent DECIMAL(6,2),
    MODIFY COLUMN amplitude DECIMAL(6,2),
    MODIFY COLUMN turnover_rate DECIMAL(6,2);

-- 验证第三批修改
SELECT COUNT(*) as '修改后行数' FROM stock_history;

-- 第四批：修改K线相关字段
ALTER TABLE stock_history 
    MODIFY COLUMN kline_type TINYINT,
    MODIFY COLUMN upper_shadow_ratio DECIMAL(6,2),
    MODIFY COLUMN lower_shadow_ratio DECIMAL(6,2),
    MODIFY COLUMN body_ratio DECIMAL(6,2);

-- 验证第四批修改
SELECT COUNT(*) as '修改后行数' FROM stock_history;

-- 第五批：修改趋势字段
ALTER TABLE stock_history 
    MODIFY COLUMN consecutive_rise_days SMALLINT,
    MODIFY COLUMN volume_ratio DECIMAL(6,2);

-- 验证第五批修改
SELECT COUNT(*) as '修改后行数' FROM stock_history;

-- 第六批：修改技术指标字段
ALTER TABLE stock_history 
    MODIFY COLUMN macd_dif DECIMAL(8,4),
    MODIFY COLUMN macd_dea DECIMAL(8,4),
    MODIFY COLUMN macd_bar DECIMAL(8,4),
    MODIFY COLUMN rsi6 DECIMAL(6,2),
    MODIFY COLUMN rsi12 DECIMAL(6,2),
    MODIFY COLUMN rsi24 DECIMAL(6,2);

-- 验证第六批修改
SELECT COUNT(*) as '修改后行数' FROM stock_history;

-- 第七批：修改布林带字段
ALTER TABLE stock_history 
    MODIFY COLUMN boll_upper DECIMAL(8,3),
    MODIFY COLUMN boll_middle DECIMAL(8,3),
    MODIFY COLUMN boll_lower DECIMAL(8,3);

-- 验证第七批修改
SELECT COUNT(*) as '修改后行数' FROM stock_history;

-- 最后：查看表大小和索引
SELECT table_name, 
       ROUND(((data_length + index_length) / 1024 / 1024), 2) AS Size_MB,
       table_rows
FROM information_schema.tables 
WHERE table_schema='stock_db' AND table_name='stock_history';

SHOW INDEXES FROM stock_db.stock_history;
