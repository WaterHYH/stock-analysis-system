-- 通过重建表的方式来优化 stock_history 表
-- 这个方法比单纯的 OPTIMIZE TABLE 更有效，因为它会：
-- 1. 创建一个新表，完整保留旧表的所有索引和约束
-- 2. 优化关键字段的类型
-- 3. 将数据从旧表复制到新表
-- 4. 删除旧表并重命名新表
-- 预计可以节省 50-70% 的磁盘空间

-- 第一步：创建优化后的新表结构（保留所有索引和约束）
CREATE TABLE stock_history_new LIKE stock_history;

-- 第二步：修改新表的关键字段类型（优化版本）
-- 注意：使用数据库实际的所有下划线命名字段名
ALTER TABLE stock_history_new
    MODIFY COLUMN open DECIMAL(8,3),
    MODIFY COLUMN high DECIMAL(8,3),
    MODIFY COLUMN low DECIMAL(8,3),
    MODIFY COLUMN close DECIMAL(8,3),
    MODIFY COLUMN ma_price5 DECIMAL(8,3),
    MODIFY COLUMN ma_price10 DECIMAL(8,3),
    MODIFY COLUMN ma_price30 DECIMAL(8,3),
    MODIFY COLUMN change_percent DECIMAL(6,2),
    MODIFY COLUMN amplitude DECIMAL(6,2),
    MODIFY COLUMN turnover_rate DECIMAL(6,2),
    MODIFY COLUMN kline_type TINYINT,
    MODIFY COLUMN upper_shadow_ratio DECIMAL(6,2),
    MODIFY COLUMN lower_shadow_ratio DECIMAL(6,2),
    MODIFY COLUMN body_ratio DECIMAL(6,2),
    MODIFY COLUMN consecutive_rise_days SMALLINT,
    MODIFY COLUMN volume_ratio DECIMAL(6,2),
    MODIFY COLUMN macd_dif DECIMAL(8,4),
    MODIFY COLUMN macd_dea DECIMAL(8,4),
    MODIFY COLUMN macd_bar DECIMAL(8,4),
    MODIFY COLUMN rsi6 DECIMAL(6,2),
    MODIFY COLUMN rsi12 DECIMAL(6,2),
    MODIFY COLUMN rsi24 DECIMAL(6,2),
    MODIFY COLUMN boll_upper DECIMAL(8,3),
    MODIFY COLUMN boll_middle DECIMAL(8,3),
    MODIFY COLUMN boll_lower DECIMAL(8,3);

-- 第三步：将数据从旧表复制到新表（这会花费一些时间）
INSERT INTO stock_history_new SELECT * FROM stock_history;

-- 第四步：查看数据是否复制完成
SELECT COUNT(*) AS row_count FROM stock_history_new;

-- 第五步：删除旧表（会自动删除其关联的索引和约束）
DROP TABLE stock_history;

-- 第六步：重命名新表
RENAME TABLE stock_history_new TO stock_history;

-- 第七步：验证表大小和索引
SELECT table_name, 
       ROUND(((data_length + index_length) / 1024 / 1024), 2) AS Size_MB
FROM information_schema.tables 
WHERE table_schema='stock_db' AND table_name='stock_history';

-- 第八步：验证索引是否完整
SHOW INDEXES FROM stock_db.stock_history;
