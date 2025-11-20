SELECT table_name, ROUND(((data_length + index_length) / 1024 / 1024), 2) AS Size_MB FROM information_schema.tables WHERE table_schema='stock_db' AND table_name='stock_history';
