# 云服务器部署和日志监控指南

## 📋 目录
1. [快速部署](#快速部署)
2. [查看日志的多种方法](#查看日志的多种方法)
3. [监控脚本使用](#监控脚本使用)
4. [日志配置说明](#日志配置说明)
5. [常见问题排查](#常见问题排查)

---

## 🚀 快速部署

### 1. 服务器环境检查
```bash
# 检查Java版本
java -version

# 检查Maven安装
mvn -version

# 检查MySQL连接
mysql -h 120.76.43.179 -u stock_user -p123456 -e "SELECT 1"
```

### 2. 克隆项目并构建
```bash
# 克隆项目
git clone <your-repo-url> /opt/stock-system

# 进入项目目录
cd /opt/stock-system

# 构建项目
mvn clean package -DskipTests

# 或构建为Docker镜像
docker build -t stock-system:latest .
```

### 3. 在后台运行应用

#### 方式1: 使用nohup（最简单）
```bash
# 启动应用
cd /opt/stock-system
nohup java -jar target/stock-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8080 \
  > /var/log/stock/startup.log 2>&1 &

# 查看启动日志
tail -f /var/log/stock/startup.log

# 保存进程ID
echo $! > /var/run/stock.pid

# 停止应用
kill $(cat /var/run/stock.pid)
```

#### 方式2: 使用systemd服务（推荐）
```bash
# 创建systemd服务文件
sudo tee /etc/systemd/system/stock.service << EOF
[Unit]
Description=Stock System Service
After=network.target

[Service]
Type=simple
User=stock
WorkingDirectory=/opt/stock-system
Environment="JAVA_HOME=/usr/lib/jvm/java-17"
Environment="LOG_PATH=/var/log/stock"
ExecStart=/usr/lib/jvm/java-17/bin/java -jar /opt/stock-system/target/stock-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8080
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 启用服务
sudo systemctl enable stock

# 启动服务
sudo systemctl start stock

# 查看服务状态
sudo systemctl status stock

# 查看实时日志
sudo journalctl -u stock -f
```

#### 方式3: 使用Docker（容器化）
```bash
# 构建镜像
docker build -t stock-system:latest .

# 运行容器
docker run -d \
  --name stock-system \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /var/log/stock:/app/logs \
  stock-system:latest

# 查看容器日志
docker logs -f stock-system

# 进入容器
docker exec -it stock-system /bin/bash
```

---

## 📊 查看日志的多种方法

### 方法1: 使用监控脚本（最推荐）
```bash
# 给脚本添加执行权限
chmod +x /opt/stock-system/monitor_logs.sh

# 查看应用日志最新100行
./monitor_logs.sh app

# 查看最新50行
./monitor_logs.sh app -n 50

# 实时跟踪日志
./monitor_logs.sh follow app

# 查看所有错误
./monitor_logs.sh error

# 查看统计信息
./monitor_logs.sh stats

# 搜索包含"金叉"的日志
./monitor_logs.sh search -k "金叉"

# 查看应用运行状态
./monitor_logs.sh status
```

### 方法2: 直接使用tail命令
```bash
# 查看最新100行
tail -n 100 /var/log/stock/stock-app.log

# 实时跟踪（最常用）
tail -f /var/log/stock/stock-app.log

# 显示最后20行并自动刷新
tail -f /var/log/stock/stock-app.log -n 20

# 跳过前100行，显示后面的所有内容
tail -n +100 /var/log/stock/stock-app.log
```

### 方法3: 使用less进行翻页查看
```bash
# 打开日志文件
less /var/log/stock/stock-app.log

# 快捷键:
# - G: 跳到文件末尾
# - g: 跳到文件开头
# - /: 搜索
# - n: 下一个匹配
# - N: 上一个匹配
# - q: 退出
```

### 方法4: 使用grep搜索
```bash
# 搜索ERROR级别日志
grep "ERROR" /var/log/stock/stock-app.log

# 搜索包含"金叉"的日志
grep "金叉" /var/log/stock/stock-app.log

# 显示匹配前后各5行
grep -C 5 "ERROR" /var/log/stock/stock-app.log

# 统计错误数
grep -c "ERROR" /var/log/stock/stock-app.log

# 彩色输出
grep --color=auto "ERROR" /var/log/stock/stock-app.log
```

### 方法5: 使用watch实时监控
```bash
# 每2秒更新一次，显示日志最后50行
watch -n 2 'tail -n 50 /var/log/stock/stock-app.log'

# 每1秒刷新一次错误日志
watch -n 1 'tail -n 20 /var/log/stock/stock-error.log'
```

### 方法6: 实时日志搜索和统计
```bash
# 实时显示新添加的ERROR日志
tail -f /var/log/stock/stock-app.log | grep "ERROR"

# 实时显示包含特定关键字的日志
tail -f /var/log/stock/stock-app.log | grep "数据同步"

# 统计每种日志级别的数量（实时更新）
tail -f /var/log/stock/stock-app.log | awk '{print $5}' | sort | uniq -c
```

---

## 🔧 监控脚本使用详解

### 脚本功能列表

| 命令 | 说明 | 示例 |
|------|------|------|
| `app` | 查看应用日志 | `./monitor_logs.sh app -n 50` |
| `sync` | 查看同步日志 | `./monitor_logs.sh sync` |
| `kline` | 查看K线日志 | `./monitor_logs.sh kline` |
| `error` | 查看错误日志 | `./monitor_logs.sh error` |
| `follow` | 实时跟踪日志 | `./monitor_logs.sh follow app` |
| `status` | 查看应用运行状态 | `./monitor_logs.sh status` |
| `stats` | 显示日志统计信息 | `./monitor_logs.sh stats` |
| `search` | 搜索日志内容 | `./monitor_logs.sh search -k "金叉"` |

### 实用组合

```bash
# 快速健康检查
./monitor_logs.sh status

# 检查最近的错误
./monitor_logs.sh error -n 20

# 实时监控同步过程
./monitor_logs.sh follow sync

# 查找今天的所有错误
./monitor_logs.sh search -k "2025-11-13"

# 显示所有日志统计
./monitor_logs.sh stats

# 清理30天前的旧日志
./monitor_logs.sh clean
```

---

## 📝 日志配置说明

### 日志文件位置
```
/var/log/stock/
├── stock-app.log      # 主应用日志
├── stock-sync.log     # 数据同步日志
├── stock-kline.log    # K线分析日志
├── stock-error.log    # 错误日志
└── [日期].gz          # 压缩的历史日志
```

### 日志级别
- **ERROR** - 错误信息（应该立即处理）
- **WARN** - 警告信息（需要注意）
- **INFO** - 信息日志（正常运行）
- **DEBUG** - 调试信息（开发环境）

### 日志滚动策略
- **按大小滚动**: 单个文件最大10MB
- **按时间滚动**: 每天自动生成新文件
- **自动压缩**: 旧日志自动gzip压缩
- **自动删除**: 30天前的日志自动删除

### 日志格式
```
2025-11-13 14:30:25.123 [stock-sync-1] INFO com.example.stock.scheduler.StockHistorySyncScheduler - 开始同步股票历史数据...
```

---

## 🔍 常见问题排查

### 问题1: 找不到日志文件
**症状**: 日志文件不存在或为空

**排查步骤**:
```bash
# 检查日志目录是否存在
ls -la /var/log/stock/

# 如果不存在，创建目录
mkdir -p /var/log/stock
chmod 755 /var/log/stock

# 检查应用权限
ls -la /var/log/stock/stock-app.log

# 如果权限不足，修改权限
sudo chown -R stock:stock /var/log/stock
sudo chmod 755 /var/log/stock
```

### 问题2: 日志文件过大
**症状**: 日志文件占用大量磁盘空间

**解决方案**:
```bash
# 查看日志文件大小
du -sh /var/log/stock/*

# 手动清理旧日志
find /var/log/stock -name "*.log.gz" -mtime +30 -delete

# 或使用脚本清理
./monitor_logs.sh clean

# 压缩当前日志
gzip /var/log/stock/stock-app.log
```

### 问题3: 日志输出为乱码
**症状**: 日志中出现特殊字符或乱码

**解决方案**:
```bash
# 检查文件编码
file /var/log/stock/stock-app.log

# 使用iconv转换编码
iconv -f GBK -t UTF-8 /var/log/stock/stock-app.log > stock-app.log.utf8

# 或使用less进行查看
less -R /var/log/stock/stock-app.log
```

### 问题4: 应用无法启动
**症状**: 启动日志中显示错误

**排查步骤**:
```bash
# 查看启动日志
tail -f /var/log/stock/startup.log

# 检查端口是否被占用
netstat -tlnp | grep 8080
lsof -i :8080

# 杀死占用端口的进程
kill -9 <PID>

# 检查数据库连接
mysql -h 120.76.43.179 -u stock_user -p123456 -e "SELECT 1"

# 查看应用错误日志
tail -n 50 /var/log/stock/stock-error.log
```

### 问题5: 性能问题排查
**症状**: 应用响应缓慢或出现超时

**排查步骤**:
```bash
# 查看K线分析日志（耗时较长）
grep "耗时" /var/log/stock/stock-kline.log | tail -20

# 搜索慢查询
grep "耗时.*ms" /var/log/stock/stock-app.log | awk -F'耗时' '{print $2}' | sort -rn

# 查看资源使用情况
./monitor_logs.sh status

# 检查内存使用
free -h

# 检查CPU使用
top -b -n 1 | head -15
```

---

## 📌 快速参考

### SSH远程查看日志
```bash
# 登录服务器
ssh user@server-ip

# 查看日志
tail -f /var/log/stock/stock-app.log

# 搜索并统计错误
grep "ERROR" /var/log/stock/stock-app.log | wc -l

# 离线查看（下载到本地）
scp user@server-ip:/var/log/stock/stock-app.log ./
```

### 创建日志监控别名
```bash
# 在~/.bashrc中添加
alias stock-log='tail -f /var/log/stock/stock-app.log'
alias stock-error='tail -f /var/log/stock/stock-error.log'
alias stock-status='/opt/stock-system/monitor_logs.sh status'
alias stock-monitor='/opt/stock-system/monitor_logs.sh'

# 使别名生效
source ~/.bashrc
```

---

## 📞 获取帮助

```bash
# 查看脚本帮助
./monitor_logs.sh help

# 查看tail帮助
man tail

# 查看grep帮助
man grep

# 查看systemd帮助
man systemctl
```

---

**提示**: 建议定期检查日志，及时发现和解决问题！
