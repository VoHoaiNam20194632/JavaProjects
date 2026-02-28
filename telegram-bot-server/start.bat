@echo off
chcp 65001 >nul
title Telegram Bot Server

echo ============================================
echo   Telegram Bot Server - START
echo ============================================

:: Check if already running on port 8080
netstat -ano | findstr ":8080 .*LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo.
    echo [!] Server da dang chay tren port 8080
    echo [!] Dung stop.bat de tat truoc khi chay lai.
    echo.
    pause
    exit /b 1
)

cd /d %~dp0

:: Load .env file
if exist .env (
    echo [*] Loading .env ...
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        if not "%%a"=="" if not "%%b"=="" set "%%a=%%b"
    )
)

:: Build
echo.
echo [1/2] Building project...
call D:\Tools\apache-maven-3.9.8\bin\mvn.cmd clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [X] Build FAILED!
    pause
    exit /b 1
)
echo [OK] Build thanh cong.

:: Run
echo.
echo [2/2] Starting server...
echo [*] Nhan Ctrl+C de dung server.
echo ============================================
echo.

java -jar target\telegram-bot-server-1.0.0.jar
