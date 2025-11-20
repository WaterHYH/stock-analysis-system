-- ========================================
-- 股票历史数据表优化脚本
-- 目标：减少磁盘空间占用，从2.8GB压缩到约1GB
-- 优化方式：调整字段类型，使用更紧凑的数据类型
-- ========================================

USE stock_db;

-- 备份表（可选，建议先备份）
-- CREATE TABLE stock_history_backup AS SELECT * FROM stock_history;

-- 优化表结构
7y6y8

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
-- 预期效果：
-- 1. 价格字段(7个DOUBLE): 56字节 -> 35字节，节省37%
-- 2. 均量字段(3个BIGINT): 24字节 -> 12字节，节省50%
-- 3. 布尔字段(约23个): 92字节 -> 23字节，节省75%
-- 4. 整体预计节省: 约60-70%的存储空间
-- 5. 从2.8GB压缩到约1GB左右
-- ========================================
