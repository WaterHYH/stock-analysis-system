# Superpowers

你已加载 superpowers 技能框架（14 个 skills）。

## 核心规则

1. **收到任务时，先检查是否有匹配的 skill** — 哪怕只有 1% 的可能性也要检查
2. **设计先于编码** — 收到功能需求时，先用 brainstorming skill 做需求分析
3. **测试先于实现** — 写代码前先写测试（TDD）
4. **验证先于完成** — 声称完成前必须运行验证命令
5. **修改代码后必须重新运行项目** — 任何 .java/.html/.properties 等源码改动后，必须重新 mvn compile + spring-boot:run 验证项目能正常启动和运行
6. **修改必须通过服务器验证才叫完成** — 本地运行成功后，必须 `mvn clean package -DskipTests` 打包，scp 推送到服务器 `root@120.76.43.179:/var/www/stock/app/`，远程重启并验证启动成功。如果服务器运行失败，必须继续修改直到服务器也运行成功。

## Skills 位置

项目 `.trae/skills/` 和全局 `~/.trae-cn/skills/`

## 如何使用

当任务匹配某个 skill 的触发条件时，使用 `Skill` 工具加载对应 skill 并严格遵循其流程。哪怕只有 1% 的可能性，也要调用 Skill 工具检查。

## 项目运行规则

**每次修改代码后必须重新编译并启动项目**，验证修改是否生效。

### 启动命令

1. 先杀掉占用 8080 端口的旧进程：
```powershell
$p = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -First 1; if ($p) { Stop-Process -Id $p.OwningProcess -Force }
```

2. 编译并启动（使用非阻塞模式）：
```powershell
mvn compile -q && mvn spring-boot:run
```

> 服务端口：`http://localhost:8080`
> 健康检查：`http://localhost:8080/test`
> 版本号自动递增（基于 git commit 计数），已内置在 stocks 页

### 验证命令

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/stocks" -UseBasicParsing -TimeoutSec 5
```

### 服务器部署与验证

本地运行成功后，必须推送到服务器验证：

SSH Key：`~/.ssh/stock_deploy_temp`（免密，已配置）

**1. 打包：**
```powershell
mvn clean package -DskipTests
```

**2. 上传到服务器：**
```powershell
scp -i $env:USERPROFILE\.ssh\stock_deploy_temp target\demo-0.0.1-SNAPSHOT.jar root@120.76.43.179:/var/www/stock/app/
```

**3. 服务器重启：** 先上传 restart.sh 脚本再执行（避免 PowerShell 引号转义问题）
```powershell
scp -i $env:USERPROFILE\.ssh\stock_deploy_temp restart.sh root@120.76.43.179:/var/www/stock/app/
ssh -i $env:USERPROFILE\.ssh\stock_deploy_temp root@120.76.43.179 "bash /var/www/stock/app/restart.sh"
```

`restart.sh` 内容：
```bash
#!/bin/bash
pkill -9 -f demo-0.0.1-SNAPSHOT.jar 2>/dev/null
sleep 2
cd /var/www/stock/app
nohup java -jar demo-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
sleep 12
grep -E "Started StockApplication|APPLICATION FAILED" app.log | tail -2
```

**4. 验证服务器启动成功：**
```powershell
ssh -i $env:USERPROFILE\.ssh\stock_deploy_temp root@120.76.43.179 "sleep 10; tail -30 /var/www/stock/app/app.log"
```

检查 `restart.sh` 输出中出现 `Started StockApplication` 且无 `APPLICATION FAILED TO START` 错误，则服务器运行成功。如果启动失败，根据错误日志继续修改代码。**

## 故障处理

当服务器部署失败时：
1. 检查服务器 jar 是否正确：`ssh ... "ls -la /var/www/stock/app/demo-0.0.1-SNAPSHOT.jar"`
2. 检查 jar 中关键类是否存在：`ssh ... "jar tf /var/www/stock/app/demo-0.0.1-SNAPSHOT.jar | grep StockMapper"`
3. 查看完整错误日志：`ssh ... "tail -50 /var/www/stock/app/app.log"`
4. 本地重新 `mvn clean compile` 验证后再打包部署


