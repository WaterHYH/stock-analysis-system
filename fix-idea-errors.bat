@echo off
chcp 65001 >nul
echo ============================================
echo IDEA 2025.3 项目错误修复脚本
echo ============================================
echo.

echo [1/5] 清理Maven缓存...
call mvnw.cmd clean
if %errorlevel% neq 0 (
    echo 清理失败,请检查Maven配置
    pause
    exit /b 1
)
echo.

echo [2/5] 下载所有Maven依赖...
call mvnw.cmd dependency:resolve
if %errorlevel% neq 0 (
    echo 依赖下载失败,请检查网络连接或Maven仓库配置
    pause
    exit /b 1
)
echo.

echo [3/5] 编译项目...
call mvnw.cmd compile
if %errorlevel% neq 0 (
    echo 编译失败,请检查代码错误
    pause
    exit /b 1
)
echo.

echo [4/5] 运行测试(可选)...
choice /C YN /M "是否运行测试?"
if %errorlevel% equ 1 (
    call mvnw.cmd test
    if %errorlevel% neq 0 (
        echo 测试失败,请检查测试代码
    )
) else (
    echo 跳过测试
)
echo.

echo [5/5] 打包项目...
call mvnw.cmd package -DskipTests
if %errorlevel% neq 0 (
    echo 打包失败,请检查配置
    pause
    exit /b 1
)
echo.

echo ============================================
echo 修复完成!
echo.
echo 接下来请在IDEA中执行以下操作:
echo 1. 点击 File -^> Invalidate Caches -^> Invalidate and Restart
echo 2. 重启后,点击 Maven 工具窗口中的刷新按钮
echo 3. 检查是否还有报错
echo ============================================
echo.
echo 按任意键退出...
pause >nul
