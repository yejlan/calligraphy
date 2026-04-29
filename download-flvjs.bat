@echo off
echo 正在下载 FLV.js 库...
echo.

REM 创建目录
if not exist "src\main\resources\static\js" mkdir "src\main\resources\static\js"

REM 尝试下载FLV.js
echo 尝试从 jsDelivr CDN 下载...
curl -L -o "src\main\resources\static\js\flv.min.js" "https://cdn.jsdelivr.net/npm/flv.js@1.6.2/dist/flv.min.js"

if %errorlevel% neq 0 (
    echo jsDelivr CDN 下载失败，尝试从 unpkg CDN 下载...
    curl -L -o "src\main\resources\static\js\flv.min.js" "https://unpkg.com/flv.js@1.6.2/dist/flv.min.js"
)

if %errorlevel% neq 0 (
    echo 所有CDN下载失败，请检查网络连接
    echo 您可以手动下载 FLV.js 并放置到 src\main\resources\static\js\flv.min.js
    pause
    exit /b 1
)

echo.
echo FLV.js 下载成功！
echo 文件位置: src\main\resources\static\js\flv.min.js
echo.
pause
