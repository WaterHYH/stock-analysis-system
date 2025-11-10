package com.example.stock.service.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.TypeReference;
import com.example.stock.dto.StockDTO;
import com.example.stock.dto.StockHistoryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.HttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 新浪股票数据客户端
 * 用于从新浪财经API获取股票实时数据和历史数据
 */
@Component
public class SinaStockClient {
    private static final Logger logger = LoggerFactory.getLogger(SinaStockClient.class);
    private final RestTemplate restTemplate;
    // 新浪财经沪深A股列表接口地址
    // 📊 API_BASE_URL（基础接口 - 灵活分页）
    // 用途：支持自定义参数的基础接口，用于获取股票实时行情数据
    // 特点：高度可定制，支持分页，可自定义每页数量和排序方式
    // 使用场景：批量分页获取股票数据，需要自定义每页数量，需要按特定字段排序
    // 调用方法：fetchStocksByPage() 使用 buildFullUrl() 构建完整URL
    private static final String API_BASE_URL = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData";
    // 📈 API_URL（快速接口 - 固定参数）
    // 用途：快速获取少量股票实时行情数据
    // 特点：参数固定，无法定制，只获取第1页30条数据
    // 固定参数：page=1(第一页), num=30(每页30条), node=hs_a(沪深A股), sort=code(按代码排序), asc=1(升序)
    // 使用场景：快速测试接口可用性，获取少量示例数据，不需要遍历所有股票
    // 调用方法：fetchAllStocks() 直接使用此URL获取数据
    private static final String API_URL = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page=1&num=30&node=hs_a&sort=code&asc=1&_s_r_a=init";

    // 📉 API_HISTORY_URL（历史数据接口）
    // 用途：获取单只股票的历史K线数据
    // 特点：针对单只股票，可指定时间范围和数据量，获取历史时间序列数据
    // 数据类型：开高低收、成交量、均线等历史交易数据
    // 使用场景：技术分析、历史数据回溯、构建股票历史数据库
    // 调用方法：getStockHistory() 使用此URL并添加symbol、scale、datalen、end_date参数
    // 完整示例：https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sz000001&scale=240&datalen=70000&end_date=20250405
    private static final String API_HISTORY_URL = "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData";

    /**
     * 构造函数，通过依赖注入获取RestTemplate实例
     * @param restTemplate RestTemplate实例
     */
    public SinaStockClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 分页获取股票数据
     * @param page 页码
     * @return 包含股票代码、名称、交易所的DTO集合
     */
    public StockDTO[] fetchStocksByPage(int page) {
        String url = buildFullUrl(page);
        return restTemplate.getForObject(url, StockDTO[].class);
    }

    /**
     * 分页获取股票数据（自定义页面大小）
     * @param page 页码
     * @param pageSize 页面大小
     * @return 包含股票代码、名称、交易所的DTO集合
     */
    public StockDTO[] fetchStocksByPage(int page, int pageSize) {
        String url = buildFullUrl(page, pageSize);
        return restTemplate.getForObject(url, StockDTO[].class);
    }

    /**
     * 构建完整的URL
     * @param page 页码
     * @return 完整的URL字符串
     */
    private String buildFullUrl(int page) {
        Map<String, String> params = buildParams(page);
        StringBuilder urlBuilder = new StringBuilder(API_BASE_URL);
        urlBuilder.append('?');
        params.forEach((key, value) -> urlBuilder.append(key).append('=').append(value).append('&'));
        // 删除最后一个多余的'&'
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '&') {
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }
        return urlBuilder.toString();
    }

    /**
     * 构建完整的URL（自定义页面大小）
     * @param page 页码
     * @param pageSize 页面大小
     * @return 完整的URL字符串
     */
    private String buildFullUrl(int page, int pageSize) {
        Map<String, String> params = buildParams(page, pageSize);
        StringBuilder urlBuilder = new StringBuilder(API_BASE_URL);
        urlBuilder.append('?');
        params.forEach((key, value) -> urlBuilder.append(key).append('=').append(value).append('&'));
        // 删除最后一个多余的'&'
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '&') {
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }
        return urlBuilder.toString();
    }

    /**
     * 分页获取全部A股数据（沪深两市）
     * @return 包含股票代码、名称、交易所的DTO集合
     * @throws IOException 网络请求异常
     */
    public StockDTO[] fetchAllStocks() {
        // 调用 API 获取 JSON 数据
        return restTemplate.getForObject(API_URL, StockDTO[].class);
        // 将数组转换为 List
        //return Arrays.asList(stocks);
    }

    /**
     * 构建接口请求参数
     * @param page 当前页码
     * @return 包含分页、排序等参数的Map
     */
    private Map<String, String> buildParams(int page) {
        return buildParams(page, 100); // 默认页面大小为100
    }

    /**
     * 构建接口请求参数（自定义页面大小）
     * @param page 当前页码
     * @param pageSize 页面大小
     * @return 包含分页、排序等参数的Map
     */
    private Map<String, String> buildParams(int page, int pageSize) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("num", String.valueOf(pageSize)); // 控制每页数据量‌:ml-citation{ref="4,5" data="citationList"}
        params.put("node", "hs_a");      // 固定参数：沪深A股‌:ml-citation{ref="4,5" data="citationList"}
        params.put("sort", "code");    // 按代码排序
        params.put("asc", "1");          // 升序排列
        // 可以考虑添加时间戳等防止缓存的参数，如果需要的话
        return params;
    }

    /**
     * 获取单只股票历史数据（日线）
     * @param symbol 股票代码（如sh600000）
     * @return 股票历史数据列表
     */
    // 获取单只股票历史数据（日线）
    public List<StockHistoryDTO> getStockHistory(String symbol) {
        // 从symbol中提取code（去除前缀）
        String code = symbol.substring(2);
        
        String endDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        // 构造历史数据API请求URL，包含以下参数：
        // symbol: 股票代码（如sh600000、sz000001）
        // scale: K线周期，240表示日线（其他可选值：5/15/30/60分钟）
        // datalen: 数据长度，70000表示获取尽可能多的历史数据
        // end_date: 结束日期，格式为yyyyMMdd
        String url = String.format("%s?symbol=%s&scale=240&datalen=70000&end_date=%s",
                API_HISTORY_URL, symbol, endDate);

        // 1. 获取原始 JSON 响应
        String jsonResponse = restTemplate.getForObject(url, String.class);

        // 2. 处理无效 symbol 返回的 "null" 字符串
        if ("null".equals(jsonResponse.trim())) {
            return Collections.emptyList();
        }

        try {
            // 3. 使用 Fastjson 解析 JSON
            List<StockHistoryDTO> dtos = JSON.parseObject(
                    jsonResponse,
                    new TypeReference<List<StockHistoryDTO>>() {}
            );

            // 4. 手动注入 symbol 和 code（假设原始数据不包含这些字段）
            if (dtos != null) {
                for (StockHistoryDTO dto : dtos) {
                    dto.setSymbol(symbol);
                    dto.setCode(code);
                }
                return dtos;
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            // 5. 处理 JSON 解析异常
            logger.error("股票数据解析失败: symbol={}, code={}, response={}", symbol, code, jsonResponse);
            return Collections.emptyList();
        }
    }


    /**
     * 解析JSON数据为DTO对象
     * @param jsonArray 原始JSON数组
     * @return 清洗后的股票数据集合
     */
    /*private List<StockDTO> parseStockData(JSONArray jsonArray) {
        return jsonArray.stream()
                .map(obj -> (JSONObject) obj)
                .map(item -> new StockDTO(
                        item.getString("symbol"),
                        item.getString("name"),
                        // 根据股票代码判断交易所（6开头为沪市）‌:ml-citation{ref="4" data="citationList"}
                        item.getString("symbol").startsWith("6") ? "sh" : "sz"
                )).collect(Collectors.toList());
    }*/
}



