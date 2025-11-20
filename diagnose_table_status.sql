-- 诊断脚本：检查表的当前状态

-- 1. 检查表大小
SELECT table_name, 
       ROUND(((data_length + index_length) / 1024 / 1024), 2) AS Size_MB,
       table_rows,
       data_free AS Free_Space_Bytes
FROM information_schema.tables 
WHERE table_schema='stock_db' AND table_name='stock_history';

-- 2. 检查关键字段的类型
SELECT COLUMN_NAME, COLUMN_TYPE 
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'stock_db' 
  AND TABLE_NAME = 'stock_history'
  AND COLUMN_NAME IN ('open', 'high', 'low', 'close', 'ma_price5', 'ma_price10', 'ma_price30', 
                      'ma_volume5', 'ma_volume10', 'ma_volume30', 'kline_type', 'consecutive_rise_days')
ORDER BY ORDINAL_POSITION;

-- 3. 检查是否有正在运行的ALTER操作
SHOW PROCESSLIST;
