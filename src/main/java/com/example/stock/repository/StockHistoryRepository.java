package com.example.stock.repository;

import com.example.stock.entity.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 股票历史数据仓库接口
 * 提供对StockHistory实体的数据访问操作
 */
@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> , StockHistoryCustomRepository{

    /**
     * 根据股票代码查询所有历史记录
     * @param symbol 股票代码
     * @return 股票历史数据列表
     */
    // 方法1：根据symbol查询所有历史记录（自动生成查询）
    List<StockHistory> findBySymbol(String symbol);

    /**
     * 根据股票代码批量删除历史记录
     * @param symbol 股票代码
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM StockHistory sh WHERE sh.symbol = :symbol")
    int deleteBySymbol(@Param("symbol") String symbol);

    /**
     * 插入股票历史数据
     * 使用原生SQL实现insert操作，避免重复数据
     * @param entity 股票历史实体对象
     * @return 插入的记录数
     */
    @Modifying(clearAutomatically = false)
    @Query(nativeQuery = true, value = """
            INSERT INTO stock_history
                (symbol, code, trade_date, open, high, low, close, volume,
                 ma_price5, ma_price10, ma_price30, ma_volume5, ma_volume10, ma_volume30) VALUES
                (:#{#entity.symbol}, :#{#entity.code}, :#{#entity.day},
                 :#{#entity.open}, :#{#entity.high}, :#{#entity.low}, :#{#entity.close},
                 :#{#entity.volume}, :#{#entity.maPrice5}, :#{#entity.maPrice10},
                 :#{#entity.maPrice30}, :#{#entity.maVolume5}, :#{#entity.maVolume10}, :#{#entity.maVolume30})
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertStockHistory(@Param("entity") StockHistory stockHistory);

    /**
     * 按股票代码模糊查询 + 分页（无总数统计）
     * @param symbol 股票代码
     * @param pageable 分页参数
     * @return 股票历史实体分页结果
     */
    @Query("SELECT sh FROM StockHistory sh WHERE sh.symbol LIKE %:symbol% ORDER BY sh.day DESC")
    Page<StockHistory> findBySymbolContainingWithoutCount(@Param("symbol") String symbol, Pageable pageable);

    /**
     * 查询所有历史数据 + 分页（无总数统计）
     * @param pageable 分页参数
     * @return 股票历史实体分页结果
     */
    @Query("SELECT sh FROM StockHistory sh ORDER BY sh.day DESC")
    Page<StockHistory> findAllWithoutCount(Pageable pageable);

    /**
     * 按股票代码模糊查询 + 分页
     * @param symbol 股票代码
     * @param pageable 分页参数
     * @return 股票历史实体分页结果
     */
    Page<StockHistory> findBySymbolContaining(String symbol, Pageable pageable);

    /**
     * 使用数据库聚合查询：筛选低于历史最高值25%以上的股票（默认参数）
     * 返回: symbol, 历史最高价, 最新价格
     * 优化: 单次查询完成，避免N+1查询问题
     * 优化版本3: 使用简单的GROUP BY和子查询，直接在数据库层过滤
     */
    @Query(value = """
        SELECT 
            t1.symbol,
            t1.max_high,
            t2.current_price
        FROM (
            SELECT symbol, MAX(high) as max_high
            FROM stock_history
            GROUP BY symbol
        ) t1
        INNER JOIN (
            SELECT sh.symbol, sh.close as current_price
            FROM stock_history sh
            INNER JOIN (
                SELECT symbol, MAX(trade_date) as latest_date
                FROM stock_history
                GROUP BY symbol
            ) latest ON sh.symbol = latest.symbol AND sh.trade_date = latest.latest_date
        ) t2 ON t1.symbol = t2.symbol
        WHERE t2.current_price < (t1.max_high * 0.75)
        """, nativeQuery = true)
    List<Map<String, Object>> findStocksBelowHistoricalHigh();

    /**
     * 使用数据库聚合查询：筛选低于历史最高值指定百分比的股票（带参数）
     * @param startDate 开始日期（YYYY-MM-DD格式字符串），只统计此日期之后的历史数据
     * @param dropPercentage 跌幅百分比阈值（如输入25表示低于最高价25%）
     * @return 符合条件的股票列表，包含symbol, max_high, current_price
     */
    @Query(value = """
        SELECT 
            t1.symbol,
            t1.max_high,
            t2.current_price
        FROM (
            SELECT symbol, MAX(high) as max_high
            FROM stock_history
            WHERE trade_date >= :startDate
            GROUP BY symbol
        ) t1
        INNER JOIN (
            SELECT sh.symbol, sh.close as current_price
            FROM stock_history sh
            INNER JOIN (
                SELECT symbol, MAX(trade_date) as latest_date
                FROM stock_history
                WHERE trade_date >= :startDate
                GROUP BY symbol
            ) latest ON sh.symbol = latest.symbol AND sh.trade_date = latest.latest_date
        ) t2 ON t1.symbol = t2.symbol
        WHERE t2.current_price < (t1.max_high * (1 - :dropPercentage / 100))
        """, nativeQuery = true)
    List<Map<String, Object>> findStocksBelowHistoricalHighWithParams(
            @Param("startDate") String startDate,
            @Param("dropPercentage") Double dropPercentage
    );

    /**
     * 数据库聚合查询：找出均线金叉的股票
     * 条件：5日均线上穿10日均线（昨天5日均线<=10日均线，今天5日均线>10日均线）
     * 返回：symbol, current_price
     * 优化：使用自连接和索引优化，避免多重子查询的性能问题
     */
    @Query(value = """
        SELECT DISTINCT
            t1.symbol,
            t1.close as current_price
        FROM stock_history t1
        INNER JOIN (
            SELECT symbol, MAX(id) as max_id
            FROM stock_history
            GROUP BY symbol
        ) latest ON t1.symbol = latest.symbol AND t1.id = latest.max_id
        LEFT JOIN stock_history t2 ON t1.symbol = t2.symbol AND t2.id = (
            SELECT MAX(id) FROM stock_history 
            WHERE symbol = t1.symbol AND id < t1.id
        )
        WHERE t1.ma_price5 > t1.ma_price10
            AND t1.ma_price5 IS NOT NULL 
            AND t1.ma_price10 IS NOT NULL
            AND t2.ma_price5 <= t2.ma_price10
        """, nativeQuery = true)
    List<Map<String, Object>> findGoldenCrossStocksOptimized();

    /**
     * 获取最近两个交易日的所有股票数据
     * 优化方案：先获取最近两个交易日的数据，然后通过代码过滤出金叉股票
     * @param latestDate 最新交易日
     * @param previousDate 前一个交易日
     * @return 股票历史数据列表
     */
    @Query("SELECT sh FROM StockHistory sh WHERE sh.day IN (:latestDate, :previousDate) ORDER BY sh.symbol, sh.day DESC")
    List<StockHistory> findLatestTwoDaysData(@Param("latestDate") LocalDate latestDate, @Param("previousDate") LocalDate previousDate);

    /**
     * 查询指定日期之前的最近一个交易日
     * @param currentDate 当前日期
     * @return 前一个交易日，如果不存在则返回null
     */
    @Query(value = """
        SELECT MAX(trade_date) 
        FROM stock_history 
        WHERE trade_date < :currentDate
        """, nativeQuery = true)
    LocalDate findPreviousTradeDate(@Param("currentDate") LocalDate currentDate);

    /**
     * 查询指定股票在指定日期之前的最近一条交易数据
     * @param symbol 股票代码
     * @param currentDate 当前日期
     * @return 前一个交易日的股票数据，如果不存在则返回null
     */
    @Query("SELECT sh FROM StockHistory sh WHERE sh.symbol = :symbol AND sh.day < :currentDate ORDER BY sh.day DESC LIMIT 1")
    StockHistory findPreviousDayDataForStock(@Param("symbol") String symbol, @Param("currentDate") LocalDate currentDate);

    /**
     * 创建必要的索引以优化查询性能
     * 索引1: symbol + trade_date (用于按股票代码和日期排序)
     * 索引2: symbol + high (用于聚合查询最高价)
     */
    @Modifying
    @Transactional
    @Query(value = """
        CREATE INDEX IF NOT EXISTS idx_symbol_date ON stock_history(symbol, trade_date DESC);
        CREATE INDEX IF NOT EXISTS idx_symbol_high ON stock_history(symbol, high DESC);
        """, nativeQuery = true)
    void createIndexes();

    /**
     * 获取所有股票代码
     * @return 股票代码列表
     */
    @Query("SELECT DISTINCT sh.symbol FROM StockHistory sh")
    List<String> findAllSymbols();

    /**
     * 根据股票代码查询所有历史记录，按日期升序排列
     * @param symbol 股票代码
     * @return 股票历史数据列表
     */
    List<StockHistory> findBySymbolOrderByDayAsc(String symbol);
}