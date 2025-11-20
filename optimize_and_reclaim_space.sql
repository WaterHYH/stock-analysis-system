-- 回收 stock_history 表的磁盘空间
-- 注意：此操作会锁表，请在非业务时间执行

OPTIMIZE TABLE stock_db.stock_history;
