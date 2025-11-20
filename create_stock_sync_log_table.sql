-- 创建股票同步日志表
-- 用于记录每个股票在每天是否已经同步过，避免重复获取

CREATE TABLE IF NOT EXISTS stock_sync_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    symbol VARCHAR(20) NOT NULL COMMENT '股票代码（如sh600000）',
    sync_date DATE NOT NULL COMMENT '同步日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    
    -- 复合唯一索引：保证同一股票同一天只有一条记录
    UNIQUE KEY uk_symbol_sync_date (symbol, sync_date),
    
    -- 普通索引：用于查询
    KEY idx_sync_date (sync_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✅ stock_sync_log 表创建成功' AS result;
