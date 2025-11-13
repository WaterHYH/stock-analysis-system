-- ============================================
-- 股票历史数据表重建脚本
-- 包含完整的字段定义和高性能索引设计
-- 适用于千万级数据量的股票分析场景
-- ============================================

-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS `stock_history`;

-- 创建stock_history表
CREATE TABLE `stock_history` (
  -- 主键
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 基础字段
  `symbol` VARCHAR(20) NOT NULL COMMENT '股票代码（如sh600000）',
  `code` VARCHAR(10) NOT NULL COMMENT '纯数字代码（如600000）',
  `trade_date` DATE NOT NULL COMMENT '交易日期',
  `open` DOUBLE NOT NULL DEFAULT 0 COMMENT '开盘价',
  `high` DOUBLE NOT NULL DEFAULT 0 COMMENT '最高价',
  `low` DOUBLE NOT NULL DEFAULT 0 COMMENT '最低价',
  `close` DOUBLE NOT NULL DEFAULT 0 COMMENT '收盘价',
  `volume` BIGINT NOT NULL DEFAULT 0 COMMENT '成交量',
  
  -- 均线数据
  `ma_price5` DOUBLE DEFAULT NULL COMMENT '5日均价',
  `ma_price10` DOUBLE DEFAULT NULL COMMENT '10日均价',
  `ma_price30` DOUBLE DEFAULT NULL COMMENT '30日均价',
  `ma_volume5` BIGINT DEFAULT NULL COMMENT '5日均成交量',
  `ma_volume10` BIGINT DEFAULT NULL COMMENT '10日均成交量',
  `ma_volume30` BIGINT DEFAULT NULL COMMENT '30日均成交量',
  
  -- K线分析字段
  `change_percent` DOUBLE DEFAULT NULL COMMENT '涨跌幅（相对前一交易日）',
  `amplitude` DOUBLE DEFAULT NULL COMMENT '振幅',
  `turnover_rate` DOUBLE DEFAULT NULL COMMENT '换手率',
  
  -- 均线系统分析
  `is_ma5_golden_cross` BOOLEAN DEFAULT NULL COMMENT 'MA5金叉标志',
  `is_ma5_death_cross` BOOLEAN DEFAULT NULL COMMENT 'MA5死叉标志',
  `is_ma10_golden_cross` BOOLEAN DEFAULT NULL COMMENT 'MA10金叉标志',
  `is_ma10_death_cross` BOOLEAN DEFAULT NULL COMMENT 'MA10死叉标志',
  `is_ma_bullish` BOOLEAN DEFAULT NULL COMMENT '均线多头排列',
  `is_ma_bearish` BOOLEAN DEFAULT NULL COMMENT '均线空头排列',
  
  -- K线形态分析
  `kline_type` INT DEFAULT NULL COMMENT 'K线类型（0:阴线, 1:阳线, 2:十字星）',
  `upper_shadow_ratio` DOUBLE DEFAULT NULL COMMENT '上影线长度占比',
  `lower_shadow_ratio` DOUBLE DEFAULT NULL COMMENT '下影线长度占比',
  `body_ratio` DOUBLE DEFAULT NULL COMMENT '实体长度占比',
  `is_doji` BOOLEAN DEFAULT NULL COMMENT '是否为十字星',
  `is_hammer` BOOLEAN DEFAULT NULL COMMENT '是否为锤子线',
  `is_inverted_hammer` BOOLEAN DEFAULT NULL COMMENT '是否为倒锤子线',
  
  -- 趋势分析
  `consecutive_rise_days` INT DEFAULT NULL COMMENT '连续上涨天数',
  `is_break_high` BOOLEAN DEFAULT NULL COMMENT '是否突破前高',
  `is_break_low` BOOLEAN DEFAULT NULL COMMENT '是否跌破前低',
  
  -- 成交量分析
  `volume_ratio` DOUBLE DEFAULT NULL COMMENT '成交量相对5日均量比例',
  `is_volume_surge` BOOLEAN DEFAULT NULL COMMENT '是否放量',
  `is_volume_shrink` BOOLEAN DEFAULT NULL COMMENT '是否缩量',
  `is_price_volume_match` BOOLEAN DEFAULT NULL COMMENT '量价配合',
  
  -- 技术指标
  `macd_dif` DOUBLE DEFAULT NULL COMMENT 'MACD DIF值',
  `macd_dea` DOUBLE DEFAULT NULL COMMENT 'MACD DEA值',
  `macd_bar` DOUBLE DEFAULT NULL COMMENT 'MACD柱状图值',
  `is_macd_golden_cross` BOOLEAN DEFAULT NULL COMMENT 'MACD金叉',
  `is_macd_death_cross` BOOLEAN DEFAULT NULL COMMENT 'MACD死叉',
  `rsi6` DOUBLE DEFAULT NULL COMMENT 'RSI指标值（6日）',
  `rsi12` DOUBLE DEFAULT NULL COMMENT 'RSI指标值（12日）',
  `rsi24` DOUBLE DEFAULT NULL COMMENT 'RSI指标值（24日）',
  `is_overbought` BOOLEAN DEFAULT NULL COMMENT '是否超买',
  `is_oversold` BOOLEAN DEFAULT NULL COMMENT '是否超卖',
  `boll_upper` DOUBLE DEFAULT NULL COMMENT '布林带上轨',
  `boll_middle` DOUBLE DEFAULT NULL COMMENT '布林带中轨',
  `boll_lower` DOUBLE DEFAULT NULL COMMENT '布林带下轨',
  `is_touch_boll_upper` BOOLEAN DEFAULT NULL COMMENT '是否触及布林带上轨',
  `is_touch_boll_lower` BOOLEAN DEFAULT NULL COMMENT '是否触及布林带下轨',
  
  -- 主键约束
  PRIMARY KEY (`id`),
  
  -- 唯一约束（避免重复数据）
  UNIQUE KEY `uk_symbol_date` (`symbol`, `trade_date`)
  
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票历史数据表';

-- ============================================
-- 索引设计说明
-- ============================================
-- 基于常见的股票分析场景设计索引，包括：
-- 1. 单股票时间序列查询
-- 2. 多股票特定日期查询
-- 3. 金叉/死叉筛选
-- 4. 技术指标筛选
-- 5. 成交量分析
-- 6. K线形态识别
-- ============================================

-- 1. 股票代码查询索引（最常用）
CREATE INDEX `idx_symbol` ON `stock_history` (`symbol`);

-- 2. 交易日期索引（按日期查询所有股票）
CREATE INDEX `idx_trade_date` ON `stock_history` (`trade_date`);

-- 3. 股票代码+日期复合索引（时间序列查询优化）
CREATE INDEX `idx_symbol_date_desc` ON `stock_history` (`symbol`, `trade_date` DESC);

-- 4. 日期+股票代码复合索引（特定日期多股票查询）
CREATE INDEX `idx_date_symbol` ON `stock_history` (`trade_date`, `symbol`);

-- 5. MA5金叉筛选索引（覆盖索引优化）
CREATE INDEX `idx_ma5_golden_date` ON `stock_history` (`is_ma5_golden_cross`, `trade_date`, `symbol`);

-- 6. MA10金叉筛选索引
CREATE INDEX `idx_ma10_golden_date` ON `stock_history` (`is_ma10_golden_cross`, `trade_date`, `symbol`);

-- 7. MACD金叉筛选索引
CREATE INDEX `idx_macd_golden_date` ON `stock_history` (`is_macd_golden_cross`, `trade_date`, `symbol`);

-- 8. 均线多头排列筛选索引
CREATE INDEX `idx_ma_bullish_date` ON `stock_history` (`is_ma_bullish`, `trade_date`, `symbol`);

-- 9. 放量筛选索引
CREATE INDEX `idx_volume_surge_date` ON `stock_history` (`is_volume_surge`, `trade_date`, `symbol`);

-- 10. 涨跌幅范围查询索引
CREATE INDEX `idx_change_percent` ON `stock_history` (`change_percent`, `trade_date`);

-- 11. RSI超卖筛选索引
CREATE INDEX `idx_oversold_date` ON `stock_history` (`is_oversold`, `trade_date`, `symbol`);

-- 12. RSI超买筛选索引
CREATE INDEX `idx_overbought_date` ON `stock_history` (`is_overbought`, `trade_date`, `symbol`);

-- 13. K线类型筛选索引
CREATE INDEX `idx_kline_type_date` ON `stock_history` (`kline_type`, `trade_date`, `symbol`);

-- 14. 突破前高筛选索引
CREATE INDEX `idx_break_high_date` ON `stock_history` (`is_break_high`, `trade_date`, `symbol`);

-- 15. 量价配合筛选索引
CREATE INDEX `idx_price_volume_match` ON `stock_history` (`is_price_volume_match`, `trade_date`, `symbol`);

-- 16. 连续上涨天数筛选索引
CREATE INDEX `idx_consecutive_rise` ON `stock_history` (`consecutive_rise_days`, `trade_date`, `symbol`);

-- 17. 收盘价范围查询索引（价格区间筛选）
CREATE INDEX `idx_close_date` ON `stock_history` (`close`, `trade_date`);

-- 18. 成交量查询索引
CREATE INDEX `idx_volume_date` ON `stock_history` (`volume`, `trade_date`);

-- ============================================
-- 执行完成提示
-- ============================================
SELECT 'stock_history表创建完成！' AS Status,
       (SELECT COUNT(*) FROM information_schema.statistics 
        WHERE table_schema = 'stock_db' 
        AND table_name = 'stock_history') AS '索引数量';

-- 显示所有索引
SHOW INDEX FROM `stock_history`;
