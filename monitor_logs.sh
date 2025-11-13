#!/bin/bash

# ============================================
# 股票系统日志监控脚本集合
# 使用方式: ./monitor_logs.sh [command] [options]
# ============================================

# 日志文件路径配置
LOG_DIR="${LOG_DIR:-.}/logs"
APP_LOG="${LOG_DIR}/stock-app.log"
SYNC_LOG="${LOG_DIR}/stock-sync.log"
KLINE_LOG="${LOG_DIR}/stock-kline.log"
ERROR_LOG="${LOG_DIR}/stock-error.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 帮助信息
show_help() {
    echo "=================================="
    echo "股票系统日志监控工具"
    echo "=================================="
    echo ""
    echo "使用方式:"
    echo "  ./monitor_logs.sh [命令] [选项]"
    echo ""
    echo "可用命令:"
    echo "  app       - 查看应用日志"
    echo "  sync      - 查看数据同步日志"
    echo "  kline     - 查看K线分析日志"
    echo "  error     - 查看错误日志"
    echo "  tail      - 实时监控最新日志 (默认100行)"
    echo "  follow    - 跟踪日志实时输出 (类似tail -f)"
    echo "  status    - 查看应用运行状态"
    echo "  stats     - 显示日志统计信息"
    echo "  search    - 搜索日志内容"
    echo "  help      - 显示帮助信息"
    echo ""
    echo "选项:"
    echo "  -n, --lines NUM   - 显示行数 (默认100)"
    echo "  -k, --keyword     - 搜索关键字"
    echo "  -l, --level       - 日志级别 (ERROR, WARN, INFO, DEBUG)"
    echo ""
    echo "示例:"
    echo "  ./monitor_logs.sh app -n 50          # 查看最新50行应用日志"
    echo "  ./monitor_logs.sh follow app         # 实时跟踪应用日志"
    echo "  ./monitor_logs.sh error              # 查看所有错误日志"
    echo "  ./monitor_logs.sh search -k '金叉'  # 搜索包含'金叉'的日志"
    echo "  ./monitor_logs.sh stats              # 显示日志统计"
    echo ""
}

# 检查日志文件是否存在
check_log_file() {
    if [ ! -f "$1" ]; then
        echo -e "${RED}✗ 日志文件不存在: $1${NC}"
        return 1
    fi
    return 0
}

# 显示应用日志
show_app_log() {
    local lines=${1:-100}
    echo -e "${BLUE}═══ 应用日志 (最新${lines}行) ═══${NC}"
    check_log_file "$APP_LOG" && tail -n "$lines" "$APP_LOG" || echo "找不到应用日志"
}

# 显示同步日志
show_sync_log() {
    local lines=${1:-100}
    echo -e "${BLUE}═══ 数据同步日志 (最新${lines}行) ═══${NC}"
    check_log_file "$SYNC_LOG" && tail -n "$lines" "$SYNC_LOG" || echo "找不到同步日志"
}

# 显示K线日志
show_kline_log() {
    local lines=${1:-100}
    echo -e "${BLUE}═══ K线分析日志 (最新${lines}行) ═══${NC}"
    check_log_file "$KLINE_LOG" && tail -n "$lines" "$KLINE_LOG" || echo "找不到K线日志"
}

# 显示错误日志
show_error_log() {
    local lines=${1:-100}
    echo -e "${BLUE}═══ 错误日志 (最新${lines}行) ═══${NC}"
    check_log_file "$ERROR_LOG" && tail -n "$lines" "$ERROR_LOG" || echo "找不到错误日志"
}

# 实时跟踪日志
follow_log() {
    local log_file="$1"
    local log_name="$2"
    
    if check_log_file "$log_file"; then
        echo -e "${GREEN}✓ 开始实时监控 $log_name (按 Ctrl+C 停止)${NC}"
        echo "═══════════════════════════════════════════════"
        tail -f "$log_file"
    fi
}

# 查看应用运行状态
show_status() {
    echo -e "${BLUE}═══ 应用运行状态 ═══${NC}"
    
    # 检查进程
    if pgrep -f "stock" > /dev/null; then
        echo -e "${GREEN}✓ 应用进程运行中${NC}"
        pgrep -f "stock" | while read pid; do
            echo "  PID: $pid"
            ps aux | grep "$pid" | grep -v grep | awk '{print "  CPU: "$3"%, 内存: "$4"%"}'
            echo "  启动时间: $(ps -o lstart= -p $pid)"
        done
    else
        echo -e "${RED}✗ 应用进程未运行${NC}"
    fi
    
    echo ""
    echo -e "${BLUE}═══ 日志文件大小 ═══${NC}"
    
    [ -f "$APP_LOG" ] && echo "应用日志: $(du -h $APP_LOG | cut -f1)" || echo "应用日志: 不存在"
    [ -f "$SYNC_LOG" ] && echo "同步日志: $(du -h $SYNC_LOG | cut -f1)" || echo "同步日志: 不存在"
    [ -f "$KLINE_LOG" ] && echo "K线日志: $(du -h $KLINE_LOG | cut -f1)" || echo "K线日志: 不存在"
    [ -f "$ERROR_LOG" ] && echo "错误日志: $(du -h $ERROR_LOG | cut -f1)" || echo "错误日志: 不存在"
    
    echo ""
    echo -e "${BLUE}═══ 磁盘使用情况 ═══${NC}"
    df -h | head -2
}

# 显示日志统计
show_stats() {
    echo -e "${BLUE}═══ 日志统计信息 ═══${NC}"
    
    echo -e "\n${YELLOW}应用日志统计:${NC}"
    check_log_file "$APP_LOG" && grep -c "ERROR" "$APP_LOG" && echo "  错误数: $(grep -c 'ERROR' $APP_LOG)" || echo "  错误数: 0"
    check_log_file "$APP_LOG" && echo "  警告数: $(grep -c 'WARN' $APP_LOG)" || echo "  警告数: 0"
    
    echo -e "\n${YELLOW}同步日志统计:${NC}"
    check_log_file "$SYNC_LOG" && echo "  行数: $(wc -l < $SYNC_LOG)" || echo "  行数: 0"
    
    echo -e "\n${YELLOW}K线日志统计:${NC}"
    check_log_file "$KLINE_LOG" && echo "  行数: $(wc -l < $KLINE_LOG)" || echo "  行数: 0"
    
    echo -e "\n${YELLOW}错误日志统计:${NC}"
    check_log_file "$ERROR_LOG" && echo "  错误数: $(wc -l < $ERROR_LOG)" || echo "  错误数: 0"
    
    echo -e "\n${YELLOW}最近错误:${NC}"
    check_log_file "$ERROR_LOG" && tail -5 "$ERROR_LOG" | sed 's/^/  /'
}

# 搜索日志
search_log() {
    local keyword="$1"
    local level="$2"
    
    if [ -z "$keyword" ]; then
        echo -e "${RED}✗ 请指定搜索关键字${NC}"
        return 1
    fi
    
    echo -e "${BLUE}═══ 搜索结果 (关键字: $keyword) ═══${NC}"
    
    if [ -n "$level" ]; then
        echo -e "\n${YELLOW}应用日志:${NC}"
        check_log_file "$APP_LOG" && grep -i "$level.*$keyword" "$APP_LOG" || echo "未找到"
    else
        echo -e "\n${YELLOW}应用日志:${NC}"
        check_log_file "$APP_LOG" && grep -i "$keyword" "$APP_LOG" | head -20 || echo "未找到"
        
        echo -e "\n${YELLOW}同步日志:${NC}"
        check_log_file "$SYNC_LOG" && grep -i "$keyword" "$SYNC_LOG" | head -20 || echo "未找到"
        
        echo -e "\n${YELLOW}错误日志:${NC}"
        check_log_file "$ERROR_LOG" && grep -i "$keyword" "$ERROR_LOG" | head -20 || echo "未找到"
    fi
}

# 清理旧日志
cleanup_logs() {
    echo -e "${YELLOW}清理30天前的日志文件...${NC}"
    find "$LOG_DIR" -name "*.log.gz" -mtime +30 -delete
    echo -e "${GREEN}✓ 清理完成${NC}"
}

# 主程序
main() {
    case "$1" in
        app)
            lines=${3:-100}
            [ "$2" = "-n" ] || [ "$2" = "--lines" ] && lines="$3" || lines="$2"
            show_app_log "$lines"
            ;;
        sync)
            lines=${3:-100}
            [ "$2" = "-n" ] || [ "$2" = "--lines" ] && lines="$3" || lines="$2"
            show_sync_log "$lines"
            ;;
        kline)
            lines=${3:-100}
            [ "$2" = "-n" ] || [ "$2" = "--lines" ] && lines="$3" || lines="$2"
            show_kline_log "$lines"
            ;;
        error)
            lines=${3:-100}
            [ "$2" = "-n" ] || [ "$2" = "--lines" ] && lines="$3" || lines="$2"
            show_error_log "$lines"
            ;;
        tail)
            lines=${3:-100}
            [ "$2" = "-n" ] || [ "$2" = "--lines" ] && lines="$3" || lines="$2"
            show_app_log "$lines"
            ;;
        follow|tail-f)
            case "$2" in
                app) follow_log "$APP_LOG" "应用日志" ;;
                sync) follow_log "$SYNC_LOG" "同步日志" ;;
                kline) follow_log "$KLINE_LOG" "K线日志" ;;
                error) follow_log "$ERROR_LOG" "错误日志" ;;
                *) follow_log "$APP_LOG" "应用日志" ;;
            esac
            ;;
        status)
            show_status
            ;;
        stats)
            show_stats
            ;;
        search)
            search_log "$3" "$4"
            ;;
        clean)
            cleanup_logs
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

# 运行主程序
main "$@"
