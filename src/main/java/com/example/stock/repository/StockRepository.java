package com.example.stock.repository;

import com.example.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 股票数据仓库接口
 * 提供对Stock实体的数据访问操作
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * 根据股票代码查询股票信息
     * @param symbol 股票代码
     * @return 股票实体对象
     */
    Stock findBySymbol(String symbol);

    /**
     * 插入或更新股票数据
     * 使用原生SQL实现upsert操作，避免重复数据
     * @param entity 股票实体对象
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
        INSERT INTO stock (
            symbol, code, name, trade_price, price_change, change_percent,
            bid_price, ask_price, previous_close, open_price, high_price,
            low_price, volume, amount, last_trade_time, pe_ratio, pb_ratio,
            market_cap, circulating_market_cap, turnover_rate, created_at
        ) VALUES (
            :#{#entity.symbol},
            :#{#entity.code},
            :#{#entity.name},
            :#{#entity.tradePrice},
            :#{#entity.priceChange},
            :#{#entity.changePercent},
            :#{#entity.bidPrice},
            :#{#entity.askPrice},
            :#{#entity.previousClose},
            :#{#entity.openPrice},
            :#{#entity.highPrice},
            :#{#entity.lowPrice},
            :#{#entity.volume},
            :#{#entity.amount},
            :#{#entity.lastTradeTime},
            :#{#entity.peRatio},
            :#{#entity.pbRatio},
            :#{#entity.marketCap},
            :#{#entity.circulatingMarketCap},
            :#{#entity.turnoverRate},
            NOW()
        )
        ON DUPLICATE KEY UPDATE
            trade_price = VALUES(trade_price),
            price_change = VALUES(price_change),
            change_percent = VALUES(change_percent),
            bid_price = VALUES(bid_price),
            ask_price = VALUES(ask_price),
            previous_close = VALUES(previous_close),
            open_price = VALUES(open_price),
            high_price = VALUES(high_price),
            low_price = VALUES(low_price),
            volume = VALUES(volume),
            amount = VALUES(amount),
            last_trade_time = VALUES(last_trade_time),
            pe_ratio = VALUES(pe_ratio),
            pb_ratio = VALUES(pb_ratio),
            market_cap = VALUES(market_cap),
            circulating_market_cap = VALUES(circulating_market_cap),
            turnover_rate = VALUES(turnover_rate)
        """)
    void upsertStock(@Param("entity") Stock entity);

    /**
     * 按股票代码模糊查询 + 分页
     * @param symbol 股票代码
     * @param pageable 分页参数
     * @return 股票实体分页结果
     */
    Page<Stock> findBySymbolContaining(String symbol, Pageable pageable);

    /**
     * 批量查询指定代码的股票
     * @param symbols 股票代码列表
     * @return 股票实体列表
     */
    @Query("SELECT s FROM Stock s WHERE s.symbol IN :symbols")
    java.util.List<Stock> findBySymbolIn(@Param("symbols") java.util.List<String> symbols);
}

