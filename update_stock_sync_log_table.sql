DROP TABLE IF EXISTS stock_sync_log;

CREATE TABLE stock_sync_log (
    symbol VARCHAR(20) PRIMARY KEY,
    sync_date DATE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
