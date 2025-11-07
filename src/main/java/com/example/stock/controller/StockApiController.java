package com.example.stock.controller;

import com.example.stock.service.StockDataFetchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 股票数据API控制器
 * 提供股票数据同步相关的REST API接口
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockApiController {
    private final StockDataFetchService dataFetchService;

    /**
     * 手动触发数据同步接口
     * 调用此接口将从新浪财经API获取最新的股票数据并保存到数据库
     * @return 包含同步结果的JSON响应
     */
    @GetMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncStockData() {
        int count = dataFetchService.fetchAndSaveStockData();
        Map<String, Object> response = new HashMap<>();
        if (count > 0) {
            response.put("success", true);
            response.put("message", "同步成功，更新 " + count + " 条记录");
        } else {
            response.put("success", false);
            response.put("message", "同步失败");
        }
        return ResponseEntity.ok(response);
    }
}
