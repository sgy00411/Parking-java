@echo off
chcp 65001 >nul
echo ======================================
echo MySQL 数据库导出脚本
echo ======================================
echo.

REM 设置变量
set MYSQL_PATH="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
set DB_USER=Parking
set DB_PASSWORD=aA135135@@@
set DB_NAME=parking
set OUTPUT_FILE=parking_database.sql
set BACKUP_DIR=D:\停车场\quare_api\quaer_api

echo 数据库: %DB_NAME%
echo 用户名: %DB_USER%
echo 输出目录: %BACKUP_DIR%
echo 输出文件: %OUTPUT_FILE%
echo.

REM 检查 MySQL 是否存在
if not exist %MYSQL_PATH% (
    echo 错误: 未找到 MySQL，请检查路径：%MYSQL_PATH%
    pause
    exit /b 1
)

REM 切换到输出目录
cd /d "%BACKUP_DIR%"

REM 导出数据库
echo [1/2] 正在导出数据库...
%MYSQL_PATH% -u %DB_USER% -p%DB_PASSWORD% %DB_NAME% --single-transaction --routines --triggers --no-tablespaces > %OUTPUT_FILE% 2>&1

REM 检查是否成功
if %errorlevel% neq 0 (
    echo 错误: 数据库导出失败！
    pause
    exit /b 1
)

echo.
echo [2/2] 数据库导出成功！
echo 文件位置: %BACKUP_DIR%\%OUTPUT_FILE%

REM 显示文件大小
for %%A in (%OUTPUT_FILE%) do echo 文件大小: %%~zA 字节

echo.
echo ======================================
echo 导出完成！
echo ======================================
pause
