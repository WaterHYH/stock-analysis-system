# 日志查看快速参考卡片

## 🚀 最快方式（推荐）

### 使用监控脚本
```bash
# 查看应用日志最新100行
./monitor_logs.sh app

# 实时跟踪日志
./monitor_logs.sh follow app

# 查看所有错误
./monitor_logs.sh error

# 查看运行状态
./monitor_logs.sh status
```

---

## 📊 日志查看命令速查表

### 最常用的命令

| 目的 | 命令 | 说明 |
|------|------|------|
| 查看最新100行 | `tail -n 100 /var/log/stock/stock-app.log` | 最常用 |
| 实时跟踪 | `tail -f /var/log/stock/stock-app.log` | 监控实时日志 |
| 实时显示ERROR | `tail -f /var/log/stock/stock-app.log \| grep ERROR` | 只看错误 |
| 搜索关键字 | `grep "金叉" /var/log/stock/stock-app.log` | 搜索内容 |
| 统计错误数 | `grep -c ERROR /var/log/stock/stock-app.log` | 计数 |
| 查看最后50行并翻页 | `less /var/log/stock/stock-app.log` | 精确查看 |
| 分页查看 | `cat /var/log/stock/stock-app.log \| less` | 翻页浏览 |

---

## 🔥 最实用的三个命令

### 1️⃣ 实时监控（最重要）
```bash
tail -f /var/log/stock/stock-app.log
```
按 `Ctrl+C` 停止

### 2️⃣ 快速查看最后50行
```bash
tail -n 50 /var/log/stock/stock-app.log
```

### 3️⃣ 搜索错误
```bash
grep "ERROR" /var/log/stock/stock-app.log | tail -20
```

---

## 📁 日志文件位置一览

```
/var/log/stock/
├── stock-app.log      ← 主应用日志（看这个！）
├── stock-error.log    ← 错误日志
├── stock-sync.log     ← 数据同步日志
├── stock-kline.log    ← K线分析日志
└── 2025-11-13.*       ← 历史日志
```

---

## 🎯 常见场景解决方案

### 场景1: 应用刚启动，想看启动日志
```bash
tail -f /var/log/stock/stock-app.log
# 然后查看是否有ERROR信息
```

### 场景2: 金叉数据同步进度
```bash
tail -f /var/log/stock/stock-sync.log | grep "金叉"
```

### 场景3: K线分析耗时查询
```bash
grep "耗时" /var/log/stock/stock-kline.log | tail -10
```

### 场景4: 查找最近的错误
```bash
tail -100 /var/log/stock/stock-error.log
```

### 场景5: 统计今天的错误数
```bash
grep "2025-11-13.*ERROR" /var/log/stock/stock-app.log | wc -l
```

---

## 💡 高效技巧

### 彩色输出搜索结果
```bash
grep --color=auto "ERROR" /var/log/stock/stock-app.log
```

### 显示匹配行前后5行
```bash
grep -C 5 "数据库连接错误" /var/log/stock/stock-app.log
```

### 统计各日志级别数量
```bash
echo "ERROR: $(grep -c ERROR /var/log/stock/stock-app.log)"
echo "WARN:  $(grep -c WARN /var/log/stock/stock-app.log)"
echo "INFO:  $(grep -c INFO /var/log/stock/stock-app.log)"
```

### 每2秒刷新一次日志
```bash
watch -n 2 'tail -n 30 /var/log/stock/stock-app.log'
```

### 下载日志到本地
```bash
scp user@server:/var/log/stock/stock-app.log ./
```

---

## 🚨 快速故障排查

### 应用是否正在运行？
```bash
ps aux | grep java
```

### 8080端口是否被占用？
```bash
lsof -i :8080
```

### 数据库是否连接正常？
```bash
grep "连接" /var/log/stock/stock-app.log | tail -5
```

### 最近发生了什么错误？
```bash
tail -20 /var/log/stock/stock-error.log
```

---

## 📞 保存快速别名

编辑 `~/.bashrc`，添加以下行：

```bash
# Stock System Aliases
alias stock-log='tail -f /var/log/stock/stock-app.log'
alias stock-error='tail -n 50 /var/log/stock/stock-error.log'
alias stock-tail100='tail -n 100 /var/log/stock/stock-app.log'
alias stock-grep='grep -r'
alias stock-du='du -sh /var/log/stock/*'
```

然后运行：
```bash
source ~/.bashrc
```

现在可以直接用：
```bash
stock-log           # 实时监控日志
stock-error         # 查看错误
stock-tail100       # 查看最新100行
stock-du            # 查看日志大小
```

---

## ⏱️ 根据时间查看日志

### 查看特定时间的日志
```bash
# 查看14:30:00之后的日志
grep "14:3[0-9]" /var/log/stock/stock-app.log

# 查看某个小时的日志
grep "14:" /var/log/stock/stock-app.log

# 查看某一天的日志
grep "2025-11-13" /var/log/stock/stock-app.log
```

---

## 🧹 日志清理

### 查看日志大小
```bash
du -sh /var/log/stock/
```

### 删除7天前的压缩日志
```bash
find /var/log/stock -name "*.log.gz" -mtime +7 -delete
```

### 清空某个日志文件
```bash
> /var/log/stock/stock-app.log
```

---

## 📌 记住这三个命令就够了！

```bash
# 实时监控（最重要）
tail -f /var/log/stock/stock-app.log

# 查看最后N行
tail -n 50 /var/log/stock/stock-app.log

# 搜索关键字
grep "关键字" /var/log/stock/stock-app.log
```

**提示**: 大部分情况下，用第一个命令就能解决90%的问题！
