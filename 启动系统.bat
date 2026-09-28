@echo off
setlocal
title DIP AI Tutor Launcher

echo ============================================
echo   DIP AI Tutor - Start All Services
echo ============================================
echo.

echo [1/3] Starting Redis ...
start "Redis" /min "D:\Redis\Redis-x64-5.0.14.1\redis-server.exe" "D:\Redis\Redis-x64-5.0.14.1\redis.windows.conf"
timeout /t 2 /nobreak >nul

echo [2/3] Starting Backend (port 8088) ...
start "Backend-8088" /min cmd /c "cd /d D:\A-project\DIP\backend && java -jar target\ai-tutor-backend-1.0.0.jar > backend.log 2>&1"
echo     Backend logs -> backend\backend.log

echo [3/3] Starting Frontend (port 5173) ...
start "Frontend-5173" /min cmd /c "cd /d D:\A-project\DIP\frontpage && npm run dev"

echo.
echo ============================================
echo   STARTED OK
echo   Frontend : http://localhost:5173
echo   Backend  : http://localhost:8088
echo.
echo   If backend failed, check backend\backend.log
echo.
echo   Accounts:
echo     Teacher : teacher / teacher123
echo     Student : student ID / same ID
echo ============================================
echo.
pause
