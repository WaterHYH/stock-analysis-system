-- 为stock_history表添加K线分析字段
-- 执行前请备份数据库

USE stock_db;

-- ==================== 涨跌幅和振幅 ====================
ALTER TABLE stock_history ADD COLUMN change_percent DOUBLE COMMENT '涨跌幅(相对前一交易日的涨跌百分比)' AFTER maVolume30;
ALTER TABLE stock_history ADD COLUMN amplitude DOUBLE COMMENT '振幅((最高价-最低价)/前收盘价*100)' AFTER change_percent;
ALTER TABLE stock_history ADD COLUMN turnover_rate DOUBLE COMMENT '换手率(成交量/流通股本*100)' AFTER amplitude;

-- ==================== 均线系统分析 ====================
ALTER TABLE stock_history ADD COLUMN is_ma5_golden_cross BOOLEAN COMMENT 'MA5金叉标志(MA5向上穿过MA10)' AFTER turnover_rate;
ALTER TABLE stock_history ADD COLUMN is_ma5_death_cross BOOLEAN COMMENT 'MA5死叉标志(MA5向下穿过MA10)' AFTER is_ma5_golden_cross;
ALTER TABLE stock_history ADD COLUMN is_ma10_golden_cross BOOLEAN COMMENT 'MA10金叉标志(MA10向上穿过MA30)' AFTER is_ma5_death_cross;
ALTER TABLE stock_history ADD COLUMN is_ma10_death_cross BOOLEAN COMMENT 'MA10死叉标志(MA10向下穿过MA30)' AFTER is_ma10_golden_cross;
ALTER TABLE stock_history ADD COLUMN is_ma_bullish BOOLEAN COMMENT '均线多头排列(MA5 > MA10 > MA30)' AFTER is_ma10_death_cross;
ALTER TABLE stock_history ADD COLUMN is_ma_bearish BOOLEAN COMMENT '均线空头排列(MA5 < MA10 < MA30)' AFTER is_ma_bullish;

-- ==================== K线形态分析 ====================
ALTER TABLE stock_history ADD COLUMN kline_type INT COMMENT 'K线类型(0:阴线, 1:阳线, 2:十字星)' AFTER is_ma_bearish;
ALTER TABLE stock_history ADD COLUMN upper_shadow_ratio DOUBLE COMMENT '上影线长度占比' AFTER kline_type;
ALTER TABLE stock_history ADD COLUMN lower_shadow_ratio DOUBLE COMMENT '下影线长度占比' AFTER upper_shadow_ratio;
ALTER TABLE stock_history ADD COLUMN body_ratio DOUBLE COMMENT '实体长度占比' AFTER lower_shadow_ratio;
ALTER TABLE stock_history ADD COLUMN is_doji BOOLEAN COMMENT '是否为十字星(实体长度占比 < 5%)' AFTER body_ratio;
ALTER TABLE stock_history ADD COLUMN is_hammer BOOLEAN COMMENT '是否为锤子线(下影线长、上影线短、实体小)' AFTER is_doji;
ALTER TABLE stock_history ADD COLUMN is_inverted_hammer BOOLEAN COMMENT '是否为倒锤子线(上影线长、下影线短、实体小)' AFTER is_hammer;

-- ==================== 趋势分析 ====================
ALTER TABLE stock_history ADD COLUMN consecutive_rise_days INT COMMENT '连续上涨天数(正数表示连涨,负数表示连跌)' AFTER is_inverted_hammer;
ALTER TABLE stock_history ADD COLUMN is_break_high BOOLEAN COMMENT '是否突破前高(收盘价 > 近N日最高价)' AFTER consecutive_rise_days;
ALTER TABLE stock_history ADD COLUMN is_break_low BOOLEAN COMMENT '是否跌破前低(收盘价 < 近N日最低价)' AFTER is_break_high;

-- ==================== 成交量分析 ====================
ALTER TABLE stock_history ADD COLUMN volume_ratio DOUBLE COMMENT '成交量相对5日均量比例(当日成交量/MA5成交量)' AFTER is_break_low;
ALTER TABLE stock_history ADD COLUMN is_volume_surge BOOLEAN COMMENT '是否放量(成交量 > 1.5倍5日均量)' AFTER volume_ratio;
ALTER TABLE stock_history ADD COLUMN is_volume_shrink BOOLEAN COMMENT '是否缩量(成交量 < 0.5倍5日均量)' AFTER is_volume_surge;
ALTER TABLE stock_history ADD COLUMN is_price_volume_match BOOLEAN COMMENT '量价配合(价涨量增或价跌量缩)' AFTER is_volume_shrink;

-- ==================== MACD指标 ====================
ALTER TABLE stock_history ADD COLUMN macd_dif DOUBLE COMMENT 'MACD DIF值' AFTER is_price_volume_match;
ALTER TABLE stock_history ADD COLUMN macd_dea DOUBLE COMMENT 'MACD DEA值' AFTER macd_dif;
ALTER TABLE stock_history ADD COLUMN macd_bar DOUBLE COMMENT 'MACD柱状图值' AFTER macd_dea;
ALTER TABLE stock_history ADD COLUMN is_macd_golden_cross BOOLEAN COMMENT 'MACD金叉(DIF向上穿过DEA)' AFTER macd_bar;
ALTER TABLE stock_history ADD COLUMN is_macd_death_cross BOOLEAN COMMENT 'MACD死叉(DIF向下穿过DEA)' AFTER is_macd_golden_cross;

-- ==================== RSI指标 ====================
ALTER TABLE stock_history ADD COLUMN rsi6 DOUBLE COMMENT 'RSI指标值(6日)' AFTER is_macd_death_cross;
ALTER TABLE stock_history ADD COLUMN rsi12 DOUBLE COMMENT 'RSI指标值(12日)' AFTER rsi6;
ALTER TABLE stock_history ADD COLUMN rsi24 DOUBLE COMMENT 'RSI指标值(24日)' AFTER rsi12;
ALTER TABLE stock_history ADD COLUMN is_overbought BOOLEAN COMMENT '是否超买(RSI6 > 80)' AFTER rsi24;
ALTER TABLE stock_history ADD COLUMN is_oversold BOOLEAN COMMENT '是否超卖(RSI6 < 20)' AFTER is_overbought;

-- ==================== 布林带指标 ====================
ALTER TABLE stock_history ADD COLUMN boll_upper DOUBLE COMMENT '布林带上轨' AFTER is_oversold;
ALTER TABLE stock_history ADD COLUMN boll_middle DOUBLE COMMENT '布林带中轨' AFTER boll_upper;
ALTER TABLE stock_history ADD COLUMN boll_lower DOUBLE COMMENT '布林带下轨' AFTER boll_middle;
ALTER TABLE stock_history ADD COLUMN is_touch_boll_upper BOOLEAN COMMENT '是否触及布林带上轨' AFTER boll_lower;
ALTER TABLE stock_history ADD COLUMN is_touch_boll_lower BOOLEAN COMMENT '是否触及布林带下轨' AFTER is_touch_boll_upper;

-- 创建索引以优化查询性能
CREATE INDEX idx_ma_golden_cross ON stock_history(is_ma5_golden_cross, is_ma10_golden_cross);
CREATE INDEX idx_ma_death_cross ON stock_history(is_ma5_death_cross, is_ma10_death_cross);
CREATE INDEX idx_macd_signal ON stock_history(is_macd_golden_cross, is_macd_death_cross);
CREATE INDEX idx_rsi ON stock_history(rsi6, rsi12);
CREATE INDEX idx_volume_signal ON stock_history(is_volume_surge, is_volume_shrink);
CREATE INDEX idx_kline_type ON stock_history(kline_type);
CREATE INDEX idx_trend ON stock_history(consecutive_rise_days, is_break_high, is_break_low);

-- 查看表结构
DESC stock_history;
