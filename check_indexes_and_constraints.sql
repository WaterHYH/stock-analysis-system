-- 查看 stock_history 表的所有索引和约束信息

-- 1. 查看表的完整创建语句
SHOW CREATE TABLE stock_db.stock_history;

-- 2. 查看所有索引
SELECT * FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'stock_db' AND TABLE_NAME = 'stock_history'
ORDER BY SEQ_IN_INDEX;

-- 3. 查看所有约束
SELECT * FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'stock_db' AND TABLE_NAME = 'stock_history';
