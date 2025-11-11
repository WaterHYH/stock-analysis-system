package com.example.stock.service;

import com.example.stock.entity.StockHistory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * K线技术分析服务
 * 提供各种技术指标的计算和K线形态识别
 */
@Service
@RequiredArgsConstructor
public class KLineAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(KLineAnalysisService.class);
    
    /**
     * 对单条历史数据进行技术分析
     * @param currentData 当前交易日数据
     * @param previousData 前一交易日数据(可能为null)
     * @param historyDataList 用于计算技术指标的历史数据列表(按日期降序,包含当前数据)
     */
    public void analyzeKLine(StockHistory currentData, StockHistory previousData, List<StockHistory> historyDataList) {
        if (currentData == null) {
            return;
        }
        
        try {
            // 1. 计算基础涨跌数据
            calculateBasicMetrics(currentData, previousData);
            
            // 2. 分析均线系统
            analyzeMaSystem(currentData, historyDataList);
            
            // 3. 分析K线形态
            analyzeKLinePattern(currentData);
            
            // 4. 分析趋势
            analyzeTrend(currentData, historyDataList);
            
            // 5. 分析成交量
            analyzeVolume(currentData);
            
            // 6. 计算MACD指标
            calculateMACD(currentData, historyDataList);
            
            // 7. 计算RSI指标
            calculateRSI(currentData, historyDataList);
            
            // 8. 计算布林带
            calculateBOLL(currentData, historyDataList);
            
        } catch (Exception e) {
            logger.error("分析K线数据失败: symbol={}, date={}, error={}", 
                currentData.getSymbol(), currentData.getDay(), e.getMessage());
        }
    }
    
    /**
     * 计算基础涨跌数据
     */
    private void calculateBasicMetrics(StockHistory current, StockHistory previous) {
        if (previous == null || previous.getClose() == 0) {
            current.setChangePercent(0.0);
            current.setAmplitude(0.0);
            return;
        }
        
        // 涨跌幅 = (当前收盘价 - 前收盘价) / 前收盘价 * 100
        double changePercent = (current.getClose() - previous.getClose()) / previous.getClose() * 100;
        current.setChangePercent(changePercent);
        
        // 振幅 = (最高价 - 最低价) / 前收盘价 * 100
        double amplitude = (current.getHigh() - current.getLow()) / previous.getClose() * 100;
        current.setAmplitude(amplitude);
    }
    
    /**
     * 分析均线系统
     */
    private void analyzeMaSystem(StockHistory current, List<StockHistory> historyList) {
        if (historyList == null || historyList.size() < 2) {
            return;
        }
        
        // 获取前一天的数据用于判断金叉死叉
        StockHistory previous = historyList.size() > 1 ? historyList.get(1) : null;
        
        if (previous != null) {
            // MA5金叉死叉判断
            if (current.getMaPrice5() > 0 && current.getMaPrice10() > 0 
                && previous.getMaPrice5() > 0 && previous.getMaPrice10() > 0) {
                
                // 金叉: 前一天MA5 <= MA10, 今天MA5 > MA10
                boolean isGoldenCross = previous.getMaPrice5() <= previous.getMaPrice10() 
                    && current.getMaPrice5() > current.getMaPrice10();
                current.setIsMa5GoldenCross(isGoldenCross);
                
                // 死叉: 前一天MA5 >= MA10, 今天MA5 < MA10
                boolean isDeathCross = previous.getMaPrice5() >= previous.getMaPrice10() 
                    && current.getMaPrice5() < current.getMaPrice10();
                current.setIsMa5DeathCross(isDeathCross);
            }
            
            // MA10金叉死叉判断
            if (current.getMaPrice10() > 0 && current.getMaPrice30() > 0 
                && previous.getMaPrice10() > 0 && previous.getMaPrice30() > 0) {
                
                boolean isGoldenCross = previous.getMaPrice10() <= previous.getMaPrice30() 
                    && current.getMaPrice10() > current.getMaPrice30();
                current.setIsMa10GoldenCross(isGoldenCross);
                
                boolean isDeathCross = previous.getMaPrice10() >= previous.getMaPrice30() 
                    && current.getMaPrice10() < current.getMaPrice30();
                current.setIsMa10DeathCross(isDeathCross);
            }
        }
        
        // 均线多头排列: MA5 > MA10 > MA30
        if (current.getMaPrice5() > 0 && current.getMaPrice10() > 0 && current.getMaPrice30() > 0) {
            boolean isBullish = current.getMaPrice5() > current.getMaPrice10() 
                && current.getMaPrice10() > current.getMaPrice30();
            current.setIsMaBullish(isBullish);
            
            // 均线空头排列: MA5 < MA10 < MA30
            boolean isBearish = current.getMaPrice5() < current.getMaPrice10() 
                && current.getMaPrice10() < current.getMaPrice30();
            current.setIsMaBearish(isBearish);
        }
    }
    
    /**
     * 分析K线形态
     */
    private void analyzeKLinePattern(StockHistory current) {
        double open = current.getOpen();
        double close = current.getClose();
        double high = current.getHigh();
        double low = current.getLow();
        
        if (high <= low) {
            return; // 数据异常
        }
        
        double range = high - low;
        double body = Math.abs(close - open);
        
        // K线类型: 0-阴线, 1-阳线, 2-十字星
        if (body / range < 0.05) {
            current.setKlineType(2); // 十字星
            current.setIsDoji(true);
        } else if (close > open) {
            current.setKlineType(1); // 阳线
        } else {
            current.setKlineType(0); // 阴线
        }
        
        // 计算上下影线比例
        double maxPrice = Math.max(open, close);
        double minPrice = Math.min(open, close);
        
        double upperShadow = high - maxPrice;
        double lowerShadow = minPrice - low;
        
        current.setUpperShadowRatio(upperShadow / range * 100);
        current.setLowerShadowRatio(lowerShadow / range * 100);
        current.setBodyRatio(body / range * 100);
        
        // 锤子线: 下影线长(>实体2倍), 上影线短(<实体), 实体小(<20%)
        boolean isHammer = lowerShadow > body * 2 
            && upperShadow < body 
            && current.getBodyRatio() < 20;
        current.setIsHammer(isHammer);
        
        // 倒锤子线: 上影线长(>实体2倍), 下影线短(<实体), 实体小(<20%)
        boolean isInvertedHammer = upperShadow > body * 2 
            && lowerShadow < body 
            && current.getBodyRatio() < 20;
        current.setIsInvertedHammer(isInvertedHammer);
    }
    
    /**
     * 分析趋势
     */
    private void analyzeTrend(StockHistory current, List<StockHistory> historyList) {
        if (historyList == null || historyList.size() < 2) {
            current.setConsecutiveRiseDays(0);
            return;
        }
        
        // 计算连续涨跌天数
        int consecutiveDays = 0;
        double currentClose = current.getClose();
        
        for (int i = 1; i < historyList.size(); i++) {
            StockHistory prev = historyList.get(i);
            StockHistory prevPrev = i + 1 < historyList.size() ? historyList.get(i + 1) : null;
            
            if (prevPrev == null) {
                break;
            }
            
            // 判断是上涨还是下跌
            boolean isRising = prev.getClose() > prevPrev.getClose();
            
            if (i == 1) {
                // 第一次比较,确定方向
                consecutiveDays = (currentClose > prev.getClose() ? 1 : -1);
                if ((consecutiveDays > 0 && isRising) || (consecutiveDays < 0 && !isRising)) {
                    consecutiveDays += (consecutiveDays > 0 ? 1 : -1);
                } else {
                    break;
                }
            } else {
                // 继续累计
                if ((consecutiveDays > 0 && isRising) || (consecutiveDays < 0 && !isRising)) {
                    consecutiveDays += (consecutiveDays > 0 ? 1 : -1);
                } else {
                    break;
                }
            }
        }
        
        current.setConsecutiveRiseDays(consecutiveDays);
        
        // 判断是否突破前高/跌破前低(以20日为周期)
        int checkDays = Math.min(20, historyList.size() - 1);
        if (checkDays > 0) {
            double maxHigh = current.getHigh();
            double minLow = current.getLow();
            
            for (int i = 1; i <= checkDays; i++) {
                StockHistory history = historyList.get(i);
                maxHigh = Math.max(maxHigh, history.getHigh());
                minLow = Math.min(minLow, history.getLow());
            }
            
            current.setIsBreakHigh(currentClose > maxHigh);
            current.setIsBreakLow(currentClose < minLow);
        }
    }
    
    /**
     * 分析成交量
     */
    private void analyzeVolume(StockHistory current) {
        if (current.getMaVolume5() == 0) {
            return;
        }
        
        // 量比 = 当日成交量 / MA5成交量
        double volumeRatio = (double) current.getVolume() / current.getMaVolume5();
        current.setVolumeRatio(volumeRatio);
        
        // 放量: 成交量 > 1.5倍MA5
        current.setIsVolumeSurge(volumeRatio > 1.5);
        
        // 缩量: 成交量 < 0.5倍MA5
        current.setIsVolumeShrink(volumeRatio < 0.5);
        
        // 量价配合: (涨幅>0且放量) 或 (跌幅<0且缩量)
        Double changePercent = current.getChangePercent();
        if (changePercent != null) {
            boolean match = (changePercent > 0 && volumeRatio > 1.2) 
                || (changePercent < 0 && volumeRatio < 0.8);
            current.setIsPriceVolumeMatch(match);
        }
    }
    
    /**
     * 计算MACD指标
     * MACD = DIF - DEA
     * DIF = EMA(12) - EMA(26)
     * DEA = EMA(DIF, 9)
     */
    private void calculateMACD(StockHistory current, List<StockHistory> historyList) {
        if (historyList == null || historyList.size() < 26) {
            return; // 数据不足
        }
        
        // 计算EMA12和EMA26
        double ema12 = calculateEMA(historyList, 12);
        double ema26 = calculateEMA(historyList, 26);
        
        // DIF = EMA12 - EMA26
        double dif = ema12 - ema26;
        current.setMacdDif(dif);
        
        // 计算DEA (DIF的9日EMA)
        double dea = calculateDEAFromHistory(historyList, 9);
        current.setMacdDea(dea);
        
        // MACD柱状图 = 2 * (DIF - DEA)
        double bar = 2 * (dif - dea);
        current.setMacdBar(bar);
        
        // 判断金叉死叉
        if (historyList.size() > 1) {
            StockHistory previous = historyList.get(1);
            if (previous.getMacdDif() != null && previous.getMacdDea() != null) {
                // 金叉: 前一天DIF <= DEA, 今天DIF > DEA
                boolean isGoldenCross = previous.getMacdDif() <= previous.getMacdDea() 
                    && dif > dea;
                current.setIsMacdGoldenCross(isGoldenCross);
                
                // 死叉: 前一天DIF >= DEA, 今天DIF < DEA
                boolean isDeathCross = previous.getMacdDif() >= previous.getMacdDea() 
                    && dif < dea;
                current.setIsMacdDeathCross(isDeathCross);
            }
        }
    }
    
    /**
     * 计算EMA (指数移动平均)
     */
    private double calculateEMA(List<StockHistory> historyList, int period) {
        if (historyList.size() < period) {
            return 0;
        }
        
        // EMA(t) = (2/(N+1)) * Price(t) + (1 - 2/(N+1)) * EMA(t-1)
        double multiplier = 2.0 / (period + 1);
        
        // 初始EMA为前N天的简单平均
        double ema = 0;
        for (int i = period - 1; i >= 0; i--) {
            ema += historyList.get(i).getClose();
        }
        ema /= period;
        
        // 从第N+1天开始计算EMA
        for (int i = period; i < historyList.size() && i < 100; i++) {
            double close = historyList.get(i).getClose();
            ema = (close - ema) * multiplier + ema;
        }
        
        // 最后计算当天的EMA
        double currentClose = historyList.get(0).getClose();
        ema = (currentClose - ema) * multiplier + ema;
        
        return ema;
    }
    
    /**
     * 计算DEA (DIF的EMA)
     */
    private double calculateDEAFromHistory(List<StockHistory> historyList, int period) {
        if (historyList.size() < period) {
            return 0;
        }
        
        double multiplier = 2.0 / (period + 1);
        
        // 找到有DIF值的历史数据
        int validCount = 0;
        double dea = 0;
        
        for (int i = historyList.size() - 1; i >= 0 && validCount < period; i--) {
            Double dif = historyList.get(i).getMacdDif();
            if (dif != null) {
                dea += dif;
                validCount++;
            }
        }
        
        if (validCount == 0) {
            return 0;
        }
        
        dea /= validCount;
        
        // 继续计算EMA
        for (int i = Math.min(period, historyList.size() - 1); i >= 0; i--) {
            Double dif = historyList.get(i).getMacdDif();
            if (dif != null) {
                dea = (dif - dea) * multiplier + dea;
            }
        }
        
        return dea;
    }
    
    /**
     * 计算RSI指标
     * RSI = 100 - (100 / (1 + RS))
     * RS = 平均涨幅 / 平均跌幅
     */
    private void calculateRSI(StockHistory current, List<StockHistory> historyList) {
        // 计算RSI6
        current.setRsi6(calculateRSIForPeriod(historyList, 6));
        
        // 计算RSI12
        current.setRsi12(calculateRSIForPeriod(historyList, 12));
        
        // 计算RSI24
        current.setRsi24(calculateRSIForPeriod(historyList, 24));
        
        // 判断超买超卖
        Double rsi6 = current.getRsi6();
        if (rsi6 != null) {
            current.setIsOverbought(rsi6 > 80);
            current.setIsOversold(rsi6 < 20);
        }
    }
    
    /**
     * 计算指定周期的RSI
     */
    private Double calculateRSIForPeriod(List<StockHistory> historyList, int period) {
        if (historyList == null || historyList.size() < period + 1) {
            return null;
        }
        
        double sumGain = 0;
        double sumLoss = 0;
        
        for (int i = 0; i < period; i++) {
            StockHistory current = historyList.get(i);
            StockHistory previous = historyList.get(i + 1);
            
            double change = current.getClose() - previous.getClose();
            if (change > 0) {
                sumGain += change;
            } else {
                sumLoss += Math.abs(change);
            }
        }
        
        if (sumLoss == 0) {
            return 100.0; // 全部上涨
        }
        
        double avgGain = sumGain / period;
        double avgLoss = sumLoss / period;
        double rs = avgGain / avgLoss;
        double rsi = 100 - (100 / (1 + rs));
        
        return rsi;
    }
    
    /**
     * 计算布林带
     * 中轨 = N日移动平均线
     * 上轨 = 中轨 + K × N日标准差
     * 下轨 = 中轨 - K × N日标准差
     */
    private void calculateBOLL(StockHistory current, List<StockHistory> historyList) {
        int period = 20; // 布林带周期通常为20日
        double k = 2.0;   // 标准差倍数
        
        if (historyList == null || historyList.size() < period) {
            return;
        }
        
        // 计算中轨(20日均线)
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += historyList.get(i).getClose();
        }
        double middle = sum / period;
        current.setBollMiddle(middle);
        
        // 计算标准差
        double variance = 0;
        for (int i = 0; i < period; i++) {
            double diff = historyList.get(i).getClose() - middle;
            variance += diff * diff;
        }
        double stdDev = Math.sqrt(variance / period);
        
        // 计算上下轨
        double upper = middle + k * stdDev;
        double lower = middle - k * stdDev;
        
        current.setBollUpper(upper);
        current.setBollLower(lower);
        
        // 判断是否触及布林带
        double currentClose = current.getClose();
        current.setIsTouchBollUpper(currentClose >= upper * 0.99); // 允许1%误差
        current.setIsTouchBollLower(currentClose <= lower * 1.01);
    }
}
