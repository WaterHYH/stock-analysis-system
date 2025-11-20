-- 检查 stock_history 表的字段类型
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'stock_db' 
  AND TABLE_NAME = 'stock_history'
  AND COLUMN_NAME IN ('open', 'high', 'low', 'close', 'ma_price5', 'ma_price10', 'ma_price30', 
                      'ma_volume5', 'ma_volume10', 'ma_volume30', 'kline_type', 'consecutive_rise_days')
ORDER BY ORDINAL_POSITION;
