@echo off
setlocal
title DIP AI Tutor Stopper

echo ============================================
echo   DIP AI Tutor - Stop All Services
echo ============================================
echo.

echo Stopping Backend (8088) ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8088" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo Stopping Frontend (5173) ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo Stopping Redis (6379) ...
"D:\Redis\Redis-x64-5.0.14.1\redis-cli.exe" shutdown >nul 2>&1

echo.
echo All services stopped.
pause
