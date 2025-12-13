@echo off
chcp 65001 >nul
echo ======================================
echo 停车场后端项目 Git 推送脚本
echo ======================================
echo.

REM 检查是否有修改
git status

echo.
echo 是否继续提交? (按任意键继续，Ctrl+C 取消)
pause >nul

REM 添加所有修改
echo.
echo [1/4] 添加文件到 Git...
git add .

REM 查看将要提交的内容
echo.
echo [2/4] 查看待提交文件...
git status

REM 输入提交信息
echo.
set /p commit_msg="[3/4] 请输入提交信息: "

REM 如果没有输入提交信息，使用默认信息
if "%commit_msg%"=="" (
    set commit_msg=Update: 更新代码
)

REM 提交
git commit -m "%commit_msg%"

REM 推送到 GitHub
echo.
echo [4/4] 推送到 GitHub...
git push origin main

echo.
echo ======================================
echo 推送完成！
echo ======================================
pause
