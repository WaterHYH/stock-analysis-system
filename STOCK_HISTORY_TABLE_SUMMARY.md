# stock_history 表结构说明

## 表创建完成 ✅

**创建时间**: 2025-11-13  
**数据库**: stock_db  
**字段数量**: 52个  
**索引数量**: 20个（含主键和唯一索引）

---

## 📋 表结构概览

### 基础字段 (9个)
- `id` - 主键，自增
- `symbol` - 股票代码（如sh600000）
- `code` - 纯数字代码（如600000）
- `trade_date` - 交易日期
- `open`, `high`, `low`, `close` - OHLC价格数据
- `volume` - 成交量

### 均线数据 (6个)
- `ma_price5`, `ma_price10`, `ma_price30` - 5/10/30日均价
- `ma_volume5`, `ma_volume10`, `ma_volume30` - 5/10/30日均成交量

### K线分析字段 (3个)
- `change_percent` - 涨跌幅
- `amplitude` - 振幅
- `turnover_rate` - 换手率

### 均线系统分析 (6个)
- `is_ma5_golden_cross`, `is_ma5_death_cross` - MA5金叉/死叉
- `is_ma10_golden_cross`, `is_ma10_death_cross` - MA10金叉/死叉
- `is_ma_bullish`, `is_ma_bearish` - 均线多头/空头排列

### K线形态分析 (7个)
- `kline_type` - K线类型（0:阴线, 1:阳线, 2:十字星）
- `upper_shadow_ratio`, `lower_shadow_ratio`, `body_ratio` - 影线和实体占比
- `is_doji`, `is_hammer`, `is_inverted_hammer` - 特殊K线形态

### 趋势分析 (3个)
- `consecutive_rise_days` - 连续上涨天数
- `is_break_high`, `is_break_low` - 突破前高/跌破前低

### 成交量分析 (4个)
- `volume_ratio` - 成交量相对5日均量比例
- `is_volume_surge`, `is_volume_shrink` - 放量/缩量
- `is_price_volume_match` - 量价配合

### 技术指标 (14个)
- **MACD**: `macd_dif`, `macd_dea`, `macd_bar`, `is_macd_golden_cross`, `is_macd_death_cross`
- **RSI**: `rsi6`, `rsi12`, `rsi24`, `is_overbought`, `is_oversold`
- **布林带**: `boll_upper`, `boll_middle`, `boll_lower`, `is_touch_boll_upper`, `is_touch_boll_lower`

---

## 🚀 索引设计（性能优化）

### 主键和唯一索引
1. **PRIMARY** - `id` (主键)
2. **uk_symbol_date** - `(symbol, trade_date)` (唯一索引，防重复)

### 基础查询索引
3. **idx_symbol** - `symbol` (单股票查询)
4. **idx_trade_date** - `trade_date` (按日期查询)
5. **idx_symbol_date_desc** - `(symbol, trade_date DESC)` (时间序列优化)
6. **idx_date_symbol** - `(trade_date, symbol)` (特定日期多股票查询)

### 技术分析筛选索引（覆盖索引优化）
7. **idx_ma5_golden_date** - `(is_ma5_golden_cross, trade_date, symbol)`
8. **idx_ma10_golden_date** - `(is_ma10_golden_cross, trade_date, symbol)`
9. **idx_macd_golden_date** - `(is_macd_golden_cross, trade_date, symbol)`
10. **idx_ma_bullish_date** - `(is_ma_bullish, trade_date, symbol)`
11. **idx_volume_surge_date** - `(is_volume_surge, trade_date, symbol)`
12. **idx_oversold_date** - `(is_oversold, trade_date, symbol)`
13. **idx_overbought_date** - `(is_overbought, trade_date, symbol)`
14. **idx_kline_type_date** - `(kline_type, trade_date, symbol)`
15. **idx_break_high_date** - `(is_break_high, trade_date, symbol)`
16. **idx_price_volume_match** - `(is_price_volume_match, trade_date, symbol)`
17. **idx_consecutive_rise** - `(consecutive_rise_days, trade_date, symbol)`

### 数值范围查询索引
18. **idx_change_percent** - `(change_percent, trade_date)`
19. **idx_close_date** - `(close, trade_date)`
20. **idx_volume_date** - `(volume, trade_date)`

---

## 📊 常见查询场景优化

### 1. 单股票时间序列查询
```sql
SELECT * FROM stock_history 
WHERE symbol = 'sh600000' 
ORDER BY trade_date DESC 
LIMIT 100;
```
**使用索引**: `idx_symbol_date_desc`

### 2. 金叉股票筛选
```sql
SELECT symbol, trade_date, close 
FROM stock_history 
WHERE is_ma5_golden_cross = 1 
  AND trade_date = '2025-11-13';
```
**使用索引**: `idx_ma5_golden_date` (覆盖索引)

### 3. 涨幅榜查询
```sql
SELECT symbol, trade_date, change_percent 
FROM stock_history 
WHERE trade_date = '2025-11-13' 
  AND change_percent > 5 
ORDER BY change_percent DESC 
LIMIT 20;
```
**使用索引**: `idx_change_percent`

### 4. 超卖股票筛选
```sql
SELECT symbol, trade_date, rsi6 
FROM stock_history 
WHERE is_oversold = 1 
  AND trade_date = '2025-11-13';
```
**使用索引**: `idx_oversold_date`

### 5. 放量突破筛选
```sql
SELECT symbol, trade_date 
FROM stock_history 
WHERE is_volume_surge = 1 
  AND is_break_high = 1 
  AND trade_date = '2025-11-13';
```
**使用索引**: `idx_volume_surge_date`, `idx_break_high_date`

---

## ⚡ 性能优化建议

1. **定期维护索引**
   ```sql
   ANALYZE TABLE stock_history;
   OPTIMIZE TABLE stock_history;
   ```

2. **监控索引使用情况**
   ```sql
   SHOW INDEX FROM stock_history;
   ```

3. **查询性能分析**
   ```sql
   EXPLAIN SELECT ... FROM stock_history WHERE ...;
   ```

4. **考虑分区策略**（数据量达到千万级时）
   - 按年份分区
   - 按月份分区
   - 范围分区

---

## 📝 注意事项

1. **唯一约束**: `(symbol, trade_date)` 确保同一股票同一天只有一条记录
2. **Boolean字段**: MySQL中BOOLEAN实际存储为TINYINT(1)
3. **索引维护**: 随着数据增长，定期检查索引碎片并优化
4. **查询优化**: 优先使用覆盖索引，减少回表查询
5. **批量插入**: 使用`rewriteBatchedStatements=true`提升批量操作性能

---

## 🔗 相关文件

- 表创建脚本: `recreate_stock_history_table.sql`
- 实体类: `src/main/java/com/example/stock/entity/StockHistory.java`
- 批量插入实现: `src/main/java/com/example/stock/repository/StockHistoryCustomRepositoryImpl.java`
