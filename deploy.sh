#!/bin/bash

# ============================================
# 股票系统部署脚本
# 支持多种部署方式: nohup, systemd, docker
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
PROJECT_DIR="/opt/stock-system"
JAR_FILE="target/stock-0.0.1-SNAPSHOT.jar"
LOG_DIR="/var/log/stock"
PID_FILE="/var/run/stock.pid"
PORT=8080
PROFILE="prod"

# 打印信息
info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 帮助信息
show_help() {
    echo "=================================="
    echo "股票系统部署脚本"
    echo "=================================="
    echo ""
    echo "使用方式:"
    echo "  ./deploy.sh [命令] [选项]"
    echo ""
    echo "可用命令:"
    echo "  build       - 构建项目（Maven）"
    echo "  nohup       - 使用nohup后台运行"
    echo "  systemd     - 安装并运行systemd服务"
    echo "  docker      - 使用Docker部署"
    echo "  start       - 启动应用"
    echo "  stop        - 停止应用"
    echo "  restart     - 重启应用"
    echo "  status      - 查看应用状态"
    echo "  logs        - 查看应用日志"
    echo "  install     - 完整安装流程"
    echo "  help        - 显示帮助信息"
    echo ""
    echo "示例:"
    echo "  ./deploy.sh build                    # 构建项目"
    echo "  ./deploy.sh nohup                    # nohup方式启动"
    echo "  ./deploy.sh docker                   # Docker方式启动"
    echo "  ./deploy.sh install                  # 完整安装（推荐）"
    echo ""
}

# 检查环境
check_env() {
    info "检查环境..."
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        error "Java未安装，请先安装JDK 17+"
    fi
    success "Java版本: $(java -version 2>&1 | head -1)"
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        error "Maven未安装，请先安装Maven"
    fi
    success "Maven版本: $(mvn -version | head -1)"
}

# 创建日志目录
setup_log_dir() {
    info "创建日志目录..."
    
    sudo mkdir -p "$LOG_DIR"
    sudo chmod 755 "$LOG_DIR"
    success "日志目录: $LOG_DIR"
}

# 构建项目
build_project() {
    info "开始构建项目..."
    
    cd "$PROJECT_DIR"
    
    if [ ! -f "pom.xml" ]; then
        error "未找到pom.xml，请在项目根目录运行此脚本"
    fi
    
    mvn clean package -DskipTests || error "构建失败"
    
    if [ ! -f "$JAR_FILE" ]; then
        error "JAR文件生成失败"
    fi
    
    success "项目构建完成: $(ls -lh $JAR_FILE | awk '{print $9, $5}')"
}

# 使用nohup运行
run_nohup() {
    info "使用nohup方式启动应用..."
    
    build_project
    setup_log_dir
    
    cd "$PROJECT_DIR"
    
    # 停止已运行的实例
    if [ -f "$PID_FILE" ] && ps -p $(cat $PID_FILE) > /dev/null 2>&1; then
        warning "检测到应用已运行，将停止旧实例..."
        kill $(cat $PID_FILE) || true
        sleep 2
    fi
    
    # 启动应用
    nohup java -jar "$JAR_FILE" \
        --spring.profiles.active=$PROFILE \
        --server.port=$PORT \
        > "$LOG_DIR/startup.log" 2>&1 &
    
    local PID=$!
    echo $PID > "$PID_FILE"
    
    info "应用已启动，PID: $PID"
    info "等待应用启动..."
    sleep 5
    
    # 检查应用是否成功启动
    if curl -s http://localhost:$PORT > /dev/null 2>&1; then
        success "应用启动成功！"
        info "访问地址: http://localhost:$PORT"
        info "查看日志: tail -f $LOG_DIR/stock-app.log"
    else
        warning "应用未完全启动，查看启动日志..."
        tail -n 50 "$LOG_DIR/startup.log"
    fi
}

# 使用systemd运行
run_systemd() {
    info "安装systemd服务..."
    
    build_project
    setup_log_dir
    
    # 获取Java路径
    local JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    
    info "创建systemd服务文件..."
    
    cat > /tmp/stock.service << EOF
[Unit]
Description=Stock System Service
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$PROJECT_DIR
Environment="JAVA_HOME=$JAVA_HOME"
Environment="LOG_PATH=$LOG_DIR"
ExecStart=$JAVA_HOME/bin/java -jar $PROJECT_DIR/$JAR_FILE \
  --spring.profiles.active=$PROFILE \
  --server.port=$PORT
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
    
    info "复制服务文件..."
    sudo cp /tmp/stock.service /etc/systemd/system/stock.service
    sudo chmod 644 /etc/systemd/system/stock.service
    
    info "启用并启动服务..."
    sudo systemctl daemon-reload
    sudo systemctl enable stock
    sudo systemctl start stock
    
    info "等待服务启动..."
    sleep 5
    
    # 检查服务状态
    if sudo systemctl is-active --quiet stock; then
        success "systemd服务已启动！"
        info "查看服务状态: sudo systemctl status stock"
        info "实时日志: sudo journalctl -u stock -f"
    else
        error "服务启动失败"
    fi
}

# 使用Docker运行
run_docker() {
    info "准备使用Docker部署..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        error "Docker未安装，请先安装Docker"
    fi
    
    # 检查docker-compose
    if ! command -v docker-compose &> /dev/null; then
        error "Docker Compose未安装，请先安装Docker Compose"
    fi
    
    build_project
    
    cd "$PROJECT_DIR"
    
    info "拉取镜像和启动容器..."
    docker-compose up -d
    
    info "等待容器启动..."
    sleep 10
    
    # 检查容器状态
    if docker ps | grep -q stock-system; then
        success "Docker容器启动成功！"
        info "查看容器日志: docker logs -f stock-system"
        info "进入容器: docker exec -it stock-system bash"
        info "查看容器状态: docker-compose ps"
    else
        error "容器启动失败"
    fi
}

# 启动应用
start_app() {
    info "启动应用..."
    
    if [ -f "$PID_FILE" ] && ps -p $(cat $PID_FILE) > /dev/null 2>&1; then
        warning "应用已在运行中，PID: $(cat $PID_FILE)"
        return
    fi
    
    cd "$PROJECT_DIR"
    
    if [ ! -f "$JAR_FILE" ]; then
        error "JAR文件不存在，请先执行 ./deploy.sh build"
    fi
    
    nohup java -jar "$JAR_FILE" \
        --spring.profiles.active=$PROFILE \
        --server.port=$PORT \
        > "$LOG_DIR/startup.log" 2>&1 &
    
    echo $! > "$PID_FILE"
    success "应用已启动"
}

# 停止应用
stop_app() {
    info "停止应用..."
    
    if [ -f "$PID_FILE" ]; then
        local PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            sleep 2
            if ! ps -p $PID > /dev/null 2>&1; then
                rm -f "$PID_FILE"
                success "应用已停止"
            else
                warning "进程未响应，强制关闭..."
                kill -9 $PID
                rm -f "$PID_FILE"
                success "应用已强制停止"
            fi
        else
            warning "进程不存在"
            rm -f "$PID_FILE"
        fi
    else
        warning "未找到PID文件"
    fi
}

# 重启应用
restart_app() {
    stop_app
    sleep 2
    start_app
}

# 查看应用状态
show_status() {
    info "应用状态:"
    
    if [ -f "$PID_FILE" ] && ps -p $(cat $PID_FILE) > /dev/null 2>&1; then
        local PID=$(cat $PID_FILE)
        echo -e "${GREEN}✓ 应用正在运行${NC}"
        echo "  PID: $PID"
        ps aux | grep $PID | grep -v grep | awk '{print "  CPU: "$3"%, 内存: "$4"%"}'
        
        # 检查Web服务
        if curl -s http://localhost:$PORT > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓${NC} Web服务: http://localhost:$PORT"
        else
            echo -e "  ${RED}✗${NC} Web服务: 不可用"
        fi
    else
        echo -e "${RED}✗ 应用未运行${NC}"
    fi
    
    echo ""
    info "日志文件:"
    ls -lh "$LOG_DIR"/ 2>/dev/null || echo "日志目录不存在"
}

# 查看日志
show_logs() {
    if [ ! -f "$LOG_DIR/stock-app.log" ]; then
        error "日志文件不存在"
    fi
    
    info "实时监控日志 (按 Ctrl+C 停止)..."
    tail -f "$LOG_DIR/stock-app.log"
}

# 完整安装流程
full_install() {
    info "开始完整安装流程..."
    
    check_env
    
    echo ""
    echo "请选择部署方式:"
    echo "1. nohup (简单易用，适合开发)"
    echo "2. systemd (推荐用于生产环境)"
    echo "3. docker (容器化部署，推荐)"
    echo ""
    
    read -p "请输入选项 (1/2/3): " choice
    
    case $choice in
        1)
            run_nohup
            ;;
        2)
            run_systemd
            ;;
        3)
            run_docker
            ;;
        *)
            error "无效的选择"
            ;;
    esac
    
    echo ""
    success "安装完成！"
}

# 主程序
main() {
    case "${1:-help}" in
        build)
            check_env
            build_project
            ;;
        nohup)
            check_env
            run_nohup
            ;;
        systemd)
            check_env
            run_systemd
            ;;
        docker)
            run_docker
            ;;
        start)
            start_app
            ;;
        stop)
            stop_app
            ;;
        restart)
            restart_app
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs
            ;;
        install)
            full_install
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            echo -e "${RED}✗ 未知命令: $1${NC}"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

main "$@"
