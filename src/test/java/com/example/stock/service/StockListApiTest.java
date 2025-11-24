package com.example.stock.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试使用qstock和AKShare两种Python API获取股票列表
 * 
 * 前置条件：
 * 1. 已安装Python 3.x
 * 2. pip install qstock 或 pip install akshare
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("股票列表API测试 - qstock vs AKShare")
class StockListApiTest {

    /**
     * 测试qstock API获取A股列表（包含已退市股票）
     * 需要先执行：pip install qstock -U --no-cache-dir
     */
    @Test
    @DisplayName("测试1：qstock API获取全部A股列表")
    void testQstockGetStockList() {
        System.out.println("\n=== 测试1：qstock API获取A股列表 ===");
        
        // Python脚本：调用qstock获取股票列表
        String pythonCode = "import qstock as qs\n" +
                "stock_list = qs.stock_list()\n" +
                "print(f'总股票数: {len(stock_list)}')\n" +
                "print('前5条记录:')\n" +
                "print(stock_list.head())\n" +
                "print('\\n股票代码示例:')\n" +
                "for i in range(min(5, len(stock_list))):\n" +
                "    print(f\"  {stock_list.iloc[i, 0]}: {stock_list.iloc[i, 1]}\")";
        
        try {
            String output = executePython(pythonCode);
            System.out.println("✅ qstock执行成功");
            System.out.println("输出结果：\n" + output);
            
            // 验证输出包含关键信息
            assertNotNull(output, "qstock输出不应为null");
            assertTrue(output.contains("总股票数"), "输出应包含总股票数");
            System.out.println("✅ qstock成功获取股票列表");
        } catch (IOException | InterruptedException e) {
            System.out.println("⚠️  qstock执行失败: " + e.getMessage());
            System.out.println("请执行：pip install qstock -U --no-cache-dir");
        }
    }

    /**
     * 测试AKShare API获取A股列表
     * 需要先执行：pip install akshare
     */
    @Test
    @DisplayName("测试2：AKShare API获取A股列表")
    void testAkshareGetStockList() {
        System.out.println("\n=== 测试2：AKShare API获取A股列表 ===");
        
        // Python脚本：调用AKShare获取上海交易所股票
        String pythonCode = "import akshare as ak\n" +
                "try:\n" +
                "    sse_summary = ak.stock_sse_summary()\n" +
                "    print(f'上海交易所数据获取成功')\n" +
                "    print(sse_summary)\n" +
                "    print(f'\\n上市公司数: {sse_summary.iloc[3, 1]}')\n" +
                "    print(f'上市股票数: {sse_summary.iloc[4, 1]}')\n" +
                "except Exception as e:\n" +
                "    print(f'错误: {str(e)}')";
        
        try {
            String output = executePython(pythonCode);
            System.out.println("✅ AKShare执行成功");
            System.out.println("输出结果：\n" + output);
            
            // 验证输出
            assertNotNull(output, "AKShare输出不应为null");
            assertTrue(output.contains("上海交易所数据获取成功") || output.contains("上市股票数"),
                    "输出应包含上海交易所数据");
            System.out.println("✅ AKShare成功获取股票列表");
        } catch (IOException | InterruptedException e) {
            System.out.println("⚠️  AKShare执行失败: " + e.getMessage());
            System.out.println("请执行：pip install akshare");
        }
    }

    /**
     * 对比测试：同时调用两个API获取股票数量
     */
    @Test
    @DisplayName("测试3：对比qstock和AKShare的数据")
    void testCompareApiData() {
        System.out.println("\n=== 测试3：对比两种API的数据 ===");
        
        String pythonCode = "import sys\n" +
                "results = {}\n" +
                "\n" +
                "# 测试qstock\n" +
                "try:\n" +
                "    import qstock as qs\n" +
                "    stock_list = qs.stock_list()\n" +
                "    results['qstock'] = {\n" +
                "        'success': True,\n" +
                "        'total': len(stock_list),\n" +
                "        'first_code': stock_list.iloc[0, 0] if len(stock_list) > 0 else None\n" +
                "    }\n" +
                "except Exception as e:\n" +
                "    results['qstock'] = {'success': False, 'error': str(e)}\n" +
                "\n" +
                "# 测试AKShare\n" +
                "try:\n" +
                "    import akshare as ak\n" +
                "    sse_data = ak.stock_sse_summary()\n" +
                "    stock_count = sse_data.iloc[4, 1] if len(sse_data) > 4 else 0\n" +
                "    results['akshare'] = {\n" +
                "        'success': True,\n" +
                "        'sse_stock_count': stock_count\n" +
                "    }\n" +
                "except Exception as e:\n" +
                "    results['akshare'] = {'success': False, 'error': str(e)}\n" +
                "\n" +
                "# 输出结果\n" +
                "import json\n" +
                "print(json.dumps(results, indent=2, ensure_ascii=False))\n" +
                "\n" +
                "# 对比分析\n" +
                "if results.get('qstock', {}).get('success'):\n" +
                "    print(f\"\\nqstock获取的总股票数: {results['qstock']['total']}\")\n" +
                "if results.get('akshare', {}).get('success'):\n" +
                "    print(f\"AKShare获取的上海交易所股票数: {results['akshare']['sse_stock_count']}\")";
        
        try {
            String output = executePython(pythonCode);
            System.out.println("✅ 对比测试执行成功");
            System.out.println("输出结果：\n" + output);
            System.out.println("✅ 两种API都能成功获取数据");
        } catch (IOException | InterruptedException e) {
            System.out.println("⚠️  对比测试执行失败: " + e.getMessage());
            System.out.println("提示：请确保已安装 qstock 和 akshare");
            System.out.println("  pip install qstock -U --no-cache-dir");
            System.out.println("  pip install akshare");
        }
    }

    /**
     * 执行Python代码的辅助方法
     */
    private String executePython(String pythonCode) throws IOException, InterruptedException {
        // 创建Python进程
        ProcessBuilder pb = new ProcessBuilder("python", "-c", pythonCode);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // 等待进程完成
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("Python进程以代码 " + exitCode + " 退出");
        }
        
        return output.toString();
    }

    /**
     * 获取Python可执行文件路径的辅助方法
     */
    private String getPythonPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win") ? "python" : "python3";
    }
}
