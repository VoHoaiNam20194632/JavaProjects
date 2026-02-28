@echo off
chcp 65001 >nul

echo ============================================
echo   Telegram Bot Server - STOP
echo ============================================

set found=0

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 .*LISTENING"') do (
    echo.
    echo [*] Tim thay server tren port 8080, PID=%%a
    taskkill /PID %%a /F >nul 2>&1
    echo [OK] Da tat server, PID=%%a
    set found=1
)

if "%found%"=="0" (
    echo.
    echo [!] Khong tim thay server dang chay tren port 8080.
)

echo.
pause
