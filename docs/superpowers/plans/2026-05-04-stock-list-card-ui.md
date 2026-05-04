# 股票列表卡片UI优化 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `/stocks` 页面从 20 列白底表格改造为深色主题卡片风格，精选 8 个核心字段，红涨绿跌直觉化展示，支持快速翻页跳转。

**Architecture:** 仅修改 `list.html` 一个文件，纯 CSS + Thymeleaf 模板，无 JS 依赖，无后端改动。复用现有 `StockWebController` 提供的 `stocks`(Page)、`symbol`、`version`、`commitId` 四个模型属性。

**Tech Stack:** Thymeleaf, CSS3, Spring Boot 3.4.3

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/resources/templates/stocks/list.html` | **重写** | 全部卡片布局 CSS + HTML 模板 |
| 其他文件 | 不变 | Controller/Service/Entity 无改动 |

---

### Task 1: 重写 list.html — 完整卡片布局

**Files:**
- Modify: `src/main/resources/templates/stocks/list.html` (完整重写)

**说明：** 一次替换整个文件即可完成，以下是完整文件内容。

- [ ] **Step 1: 用以下完整内容替换 list.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>股票行情</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            background: #0f0f1a;
            color: #e0e0e0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
            min-height: 100vh;
            padding-bottom: 100px;
        }

        /* ====== 顶部搜索栏 ====== */
        .top-bar {
            position: sticky;
            top: 0;
            z-index: 100;
            background: #13132b;
            border-bottom: 1px solid #1e1e3a;
            padding: 12px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            flex-wrap: wrap;
        }

        .top-bar form {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .top-bar input[type="text"] {
            background: #1a1a35;
            border: 1px solid #2a2a50;
            color: #e0e0e0;
            padding: 8px 14px;
            border-radius: 6px;
            font-size: 14px;
            width: 220px;
            outline: none;
            transition: border-color 0.2s;
        }

        .top-bar input[type="text"]:focus {
            border-color: #e94560;
        }

        .top-bar input[type="text"]::placeholder {
            color: #666;
        }

        .btn-search {
            background: #e94560;
            color: white;
            border: none;
            padding: 8px 18px;
            border-radius: 6px;
            font-size: 14px;
            cursor: pointer;
            transition: background 0.2s;
        }

        .btn-search:hover {
            background: #d63850;
        }

        .top-bar .meta-info {
            font-size: 12px;
            color: #888;
        }

        /* ====== 卡片列表容器 ====== */
        .card-list {
            max-width: 820px;
            margin: 16px auto;
            padding: 0 16px;
        }

        /* ====== 单张卡片 ====== */
        .stock-card {
            display: flex;
            align-items: stretch;
            background: #1a1a2e;
            border-radius: 10px;
            margin-bottom: 10px;
            overflow: hidden;
            transition: transform 0.15s, box-shadow 0.15s;
            cursor: default;
        }

        .stock-card:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 20px rgba(233, 69, 96, 0.08);
        }

        /* 左边条 */
        .card-stripe {
            width: 4px;
            flex-shrink: 0;
            border-radius: 10px 0 0 10px;
        }

        .card-stripe.up   { background: #e94560; }
        .card-stripe.down { background: #4caf50; }
        .card-stripe.flat { background: #555; }

        /* 卡片主体 */
        .card-body {
            flex: 1;
            padding: 14px 18px;
            min-width: 0;
        }

        /* 顶行：代码 + 名称 + 市场标签 + 时间 */
        .card-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 10px;
        }

        .card-symbol {
            font-size: 14px;
            font-weight: 600;
            color: #e0e0e0;
            letter-spacing: 0.5px;
        }

        .card-name {
            font-size: 13px;
            color: #a0a0b0;
        }

        .market-tag {
            font-size: 10px;
            padding: 2px 6px;
            border-radius: 3px;
            font-weight: 500;
        }

        .market-tag.sh {
            background: #2a1a1a;
            color: #e94560;
        }

        .market-tag.sz {
            background: #1a2a1a;
            color: #4caf50;
        }

        .card-time {
            margin-left: auto;
            font-size: 11px;
            color: #666;
        }

        /* 核心行：大号价格 + 涨跌幅标签 + 涨跌额 */
        .card-price-row {
            display: flex;
            align-items: baseline;
            gap: 14px;
            margin-bottom: 8px;
        }

        .card-price {
            font-size: 26px;
            font-weight: 700;
            line-height: 1;
        }

        .card-price.up   { color: #e94560; }
        .card-price.down { color: #4caf50; }
        .card-price.flat { color: #ccc; }

        .card-change-badge {
            font-size: 14px;
            font-weight: 600;
            padding: 3px 10px;
            border-radius: 5px;
            line-height: 1;
        }

        .card-change-badge.up {
            background: rgba(233, 69, 96, 0.15);
            color: #e94560;
        }

        .card-change-badge.down {
            background: rgba(76, 175, 80, 0.15);
            color: #4caf50;
        }

        .card-change-badge.flat {
            background: rgba(136, 136, 136, 0.10);
            color: #888;
        }

        .card-change-amount {
            font-size: 13px;
        }

        .card-change-amount.up   { color: #e94560; }
        .card-change-amount.down { color: #4caf50; }
        .card-change-amount.flat { color: #888; }

        /* 辅助行 */
        .card-meta-row {
            display: flex;
            gap: 24px;
            font-size: 12px;
            color: #888;
        }

        .card-meta-row span strong {
            color: #b0b0b0;
            font-weight: 500;
            margin-right: 4px;
        }

        /* ====== 分页区域 ====== */
        .pagination {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: #13132b;
            border-top: 1px solid #1e1e3a;
            padding: 12px 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
            z-index: 100;
            flex-wrap: wrap;
        }

        .pagination a,
        .pagination span.pg-num {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 34px;
            height: 34px;
            padding: 0 10px;
            border-radius: 6px;
            font-size: 13px;
            text-decoration: none;
            color: #a0a0b0;
            background: #1a1a35;
            transition: all 0.15s;
            flex-shrink: 0;
        }

        .pagination a:hover {
            background: #2a2a50;
            color: #e0e0e0;
        }

        .pagination a.current,
        .pagination span.current {
            background: #e94560;
            color: white;
            font-weight: 600;
        }

        .pagination a.disabled {
            opacity: 0.35;
            pointer-events: none;
        }

        .jump-box {
            display: flex;
            align-items: center;
            gap: 4px;
            margin-left: 12px;
            font-size: 12px;
            color: #888;
        }

        .jump-box input[type="number"] {
            background: #1a1a35;
            border: 1px solid #2a2a50;
            color: #e0e0e0;
            padding: 6px 8px;
            border-radius: 5px;
            width: 50px;
            font-size: 13px;
            text-align: center;
            outline: none;
        }

        .jump-box input[type="number"]:focus {
            border-color: #e94560;
        }

        .jump-box .btn-go {
            background: #2a2a50;
            color: #e0e0e0;
            border: none;
            padding: 6px 12px;
            border-radius: 5px;
            font-size: 13px;
            cursor: pointer;
            transition: background 0.15s;
        }

        .jump-box .btn-go:hover {
            background: #e94560;
        }

        .page-size-info {
            margin-left: 12px;
            font-size: 12px;
            color: #666;
        }

        /* ====== 版本号 ====== */
        .version-footer {
            position: fixed;
            bottom: 52px;
            right: 16px;
            padding: 4px 12px;
            background: rgba(255, 255, 255, 0.04);
            border-radius: 4px 4px 0 0;
            font-size: 11px;
            color: #555;
            z-index: 99;
        }

        .version-sep {
            margin: 0 6px;
            color: #444;
        }

        /* ====== 响应式 ====== */
        @media (max-width: 768px) {
            .card-list {
                padding: 0 8px;
                margin: 10px auto;
            }

            .card-body {
                padding: 12px 14px;
            }

            .card-price {
                font-size: 22px;
            }

            .card-meta-row {
                gap: 14px;
                flex-wrap: wrap;
            }

            .top-bar {
                padding: 10px 12px;
            }

            .top-bar input[type="text"] {
                width: 140px;
                font-size: 13px;
            }

            .pagination {
                padding: 10px 12px;
                gap: 2px;
            }

            .pagination a,
            .pagination span.pg-num {
                min-width: 28px;
                height: 30px;
                font-size: 12px;
                padding: 0 6px;
            }

            .jump-box {
                margin-left: 6px;
            }

            .jump-box input[type="number"] {
                width: 40px;
                font-size: 12px;
            }

            .version-footer {
                bottom: 48px;
                right: 8px;
            }
        }
    </style>
</head>
<body>

<div class="top-bar">
    <form th:action="@{/stocks}" method="get">
        <input type="text" name="symbol" placeholder="🔍 搜索代码/名称" th:value="${symbol}">
        <button type="submit" class="btn-search">搜索</button>
    </form>
    <span class="meta-info">
        <span th:if="${stocks != null}">共 <span th:text="${stocks.totalElements}">0</span> 只</span>
        <span th:if="${stocks != null && !stocks.content.isEmpty()}">
            | 更新于 <span th:text="${#temporals.format(stocks.content[0].createdAt, 'yyyy-MM-dd HH:mm')}">--</span>
        </span>
    </span>
</div>

<div class="card-list">

    <!-- 空结果 -->
    <div th:if="${stocks == null || stocks.content.isEmpty()}" style="text-align:center;padding:60px;color:#666">
        <p style="font-size:16px">暂无数据</p>
        <p style="font-size:13px;margin-top:8px">尝试其他搜索条件</p>
    </div>

    <div th:each="stock : ${stocks.content}"
         class="stock-card">

        <!-- 涨跌判断 -->
        <th:block th:with="
            chg=${stock.changePercent != null ? stock.changePercent.doubleValue() : 0.0},
            dir=${chg > 0 ? 'up' : (chg < 0 ? 'down' : 'flat')}">

            <!-- 左边条 -->
            <div th:class="'card-stripe ' + ${dir}"></div>

            <div class="card-body">
                <!-- 顶行 -->
                <div class="card-header">
                    <span class="card-symbol" th:text="${stock.symbol}">000001</span>
                    <span class="card-name" th:text="${stock.name}">示例股票</span>
                    <span th:class="'market-tag ' + (${stock.symbol != null && stock.symbol.startsWith('sz')} ? 'sz' : 'sh')"
                          th:text="${stock.symbol != null && stock.symbol.startsWith('sz')} ? '深' : '沪'">沪</span>
                    <span class="card-time"
                          th:text="${stock.lastTradeTime != null ? #temporals.format(stock.lastTradeTime, 'HH:mm:ss') : '--'}">
                        15:00:00</span>
                </div>

                <!-- 核心行 -->
                <div class="card-price-row">
                    <span th:class="'card-price ' + ${dir}"
                          th:text="${stock.tradePrice != null ? #numbers.formatDecimal(stock.tradePrice, 1, 2) : '--'}">
                        12.34</span>

                    <span th:class="'card-change-badge ' + ${dir}"
                          th:text="${stock.changePercent != null ? #numbers.formatDecimal(stock.changePercent, 1, 2) + '%' : '--'}">
                        1.23%</span>

                    <span th:class="'card-change-amount ' + ${dir}"
                          th:text="${stock.priceChange != null ? (stock.priceChange.doubleValue() >= 0 ? '+' : '') + #numbers.formatDecimal(stock.priceChange, 1, 2) : '--'}">
                        +0.12</span>
                </div>

                <!-- 辅助行 -->
                <div class="card-meta-row">
                    <span><strong>成交量</strong> <span th:text="${stock.volume != null ? #numbers.formatInteger(stock.volume, 0, 'COMMA') : '--'}">0</span></span>
                    <span><strong>换手率</strong> <span th:text="${stock.turnoverRate != null ? #numbers.formatDecimal(stock.turnoverRate, 1, 2) + '%' : '--'}">0.00%</span></span>
                    <span><strong>市盈率</strong> <span th:text="${stock.peRatio != null ? #numbers.formatDecimal(stock.peRatio, 1, 2) : '--'}">0.00</span></span>
                </div>
            </div>
        </th:block>
    </div>

</div>

<!-- 分页 -->
<div class="pagination" th:if="${stocks != null && stocks.totalPages > 1}">
    <a th:classappend="${stocks.first} ? 'disabled'"
       th:href="${stocks.first} ? '#' : @{/stocks(page=0, size=${stocks.size}, symbol=${symbol})}">首页</a>
    <a th:classappend="${stocks.first} ? 'disabled'"
       th:href="${stocks.first} ? '#' : @{/stocks(page=${stocks.number - 1}, size=${stocks.size}, symbol=${symbol})}">上页</a>

    <span th:each="i : ${#numbers.sequence(0, stocks.totalPages - 1)}">
        <a th:href="@{/stocks(page=${i}, size=${stocks.size}, symbol=${symbol})}"
           th:text="${i + 1}"
           th:classappend="${i == stocks.number} ? 'current'"></a>
    </span>

    <a th:classappend="${stocks.last} ? 'disabled'"
       th:href="${stocks.last} ? '#' : @{/stocks(page=${stocks.number + 1}, size=${stocks.size}, symbol=${symbol})}">下页</a>
    <a th:classappend="${stocks.last} ? 'disabled'"
       th:href="${stocks.last} ? '#' : @{/stocks(page=${stocks.totalPages - 1}, size=${stocks.size}, symbol=${symbol})}">末页</a>

    <span class="jump-box">
        跳至
        <input type="number" id="jumpPage" min="1" th:max="${stocks.totalPages}" th:value="${stocks.number + 1}">
        页
        <button class="btn-go" onclick="jumpToPage()">GO</button>
    </span>

    <span class="page-size-info">每页 <span th:text="${stocks.size}">30</span> 条</span>
</div>

<div class="version-footer" th:if="${version != null}">
    <span th:text="${version}">v1</span>
    <span class="version-sep">|</span>
    <span th:text="${commitId}">abc1234</span>
</div>

<script>
    function jumpToPage() {
        var input = document.getElementById('jumpPage');
        var page = parseInt(input.value);
        var max = parseInt(input.max);
        if (isNaN(page) || page < 1) page = 1;
        if (page > max) page = max;

        var params = new URLSearchParams(window.location.search);
        params.set('page', page - 1);
        window.location.search = params.toString();
    }
</script>

</body>
</html>
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 停止旧进程，重启项目**

Run:
```
$p = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -First 1; if ($p) { Stop-Process -Id $p.OwningProcess -Force; Start-Sleep -Seconds 2 }
mvn spring-boot:run
```
Expected: Spring Boot 启动成功，`http-nio-8080` 监听

- [ ] **Step 4: 验证页面渲染**

Run: `curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/stocks`
Expected: `200`

Run: `curl.exe -s http://localhost:8080/stocks | Select-String "stock-card|card-price-row|jumpPage"`
Expected: 四个 CSS 类名都能匹配到

- [ ] **Step 5: 验证版本号显示**

Run: `curl.exe -s http://localhost:8080/stocks | Select-String "v\d+\|"`
Expected: 匹配到如 `v50|` 格式

- [ ] **Step 6: 提交**

```bash
git add src/main/resources/templates/stocks/list.html
git commit -m "feat: 股票列表页卡片UI全面优化" -m "- 方案B: 深色主题 + 卡片风 + 8核心字段" -m "- 红涨绿跌左边条 + 涨跌色块标签" -m "- 新增快速翻页跳转输入框" -m "- 响应式适配PC/平板/手机" -m "- 版本号保留"
```

- [ ] **Step 7: 推送**

Run: `git push origin main`
Expected: Push 成功
