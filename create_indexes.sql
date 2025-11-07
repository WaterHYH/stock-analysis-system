-- 为 stock_history 表创建性能优化索引

-- 索引1: 用于按 symbol 分组和查找最高价
CREATE INDEX IF NOT EXISTS idx_symbol_high ON stock_history(symbol, high DESC);

-- 索引2: 用于按 symbol 和日期排序获取最新记录  
CREATE INDEX IF NOT EXISTS idx_symbol_date ON stock_history(symbol, trade_date DESC);

-- 索引3: 组合索引优化聚合查询
CREATE INDEX IF NOT EXISTS idx_symbol_date_close ON stock_history(symbol, trade_date DESC, close);

-- 查看已创建的索引
SHOW INDEX FROM stock_history;
