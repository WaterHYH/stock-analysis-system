-- 检查 stock_history 表是否存在
SHOW TABLES FROM stock_db LIKE 'stock_history%';

-- 检查表大小
SELECT table_name, 
       ROUND(((data_length + index_length) / 1024 / 1024), 2) AS Size_MB,
       table_rows
FROM information_schema.tables 
WHERE table_schema='stock_db' AND table_name LIKE 'stock_history%';

-- 检查是否有临时表残留
SELECT table_name FROM information_schema.tables 
WHERE table_schema='stock_db' AND table_name LIKE 'stock_history_new';

-- 查看表行数
SELECT COUNT(*) FROM stock_db.stock_history;
