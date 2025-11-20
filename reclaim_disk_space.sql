-- 第一步：执行 OPTIMIZE TABLE 来真正回收磁盘空间
-- 这个操作会重新整理表的物理存储，可能需要较长时间
OPTIMIZE TABLE stock_db.stock_history;

-- 第二步：检查优化后的表大小
SELECT table_name, 
       ROUND(((data_length + index_length) / 1024 / 1024), 2) AS Size_MB,
       table_rows,
       data_free AS Free_Space_Bytes
FROM information_schema.tables 
WHERE table_schema='stock_db' AND table_name='stock_history';

-- 第三步：检查索引完整性
SHOW INDEXES FROM stock_db.stock_history;

-- 第四步：验证数据完整性（检查数据行数）
SELECT COUNT(*) AS total_rows FROM stock_db.stock_history;
