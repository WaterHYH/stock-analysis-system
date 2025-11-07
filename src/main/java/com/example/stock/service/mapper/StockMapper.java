package com.example.stock.service.mapper;

import com.example.stock.dto.StockDTO;
import com.example.stock.dto.StockHistoryDTO;
import com.example.stock.entity.Stock;
import com.example.stock.entity.StockHistory;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 股票数据映射器接口
 * 使用MapStruct实现DTO与实体类之间的自动映射
 */
@Mapper(componentModel = "spring")
public interface StockMapper {
    /**
     * 将StockDTO映射为Stock
     * @param dto 股票数据传输对象
     * @return 股票实体对象
     */
    Stock dtoToEntity(StockDTO dto);

    /**
     * 将StockHistoryDTO映射为StockHistory
     * @param dto 股票历史数据传输对象
     * @return 股票历史实体对象
     */
    StockHistory toStockHistory(StockHistoryDTO dto);

    /**
     * 将StockHistoryDTO列表映射为StockHistory列表
     * @param dtos 股票历史数据传输对象列表
     * @return 股票历史实体对象列表
     */
    List<StockHistory> toStockHistoryList(List<StockHistoryDTO> dtos);
}

