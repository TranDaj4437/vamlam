@echo off
title Build JCraft Wheel + NoDash + CS2 BHop v3.3
echo Dang build file .jar cho Forge 1.20.1...
gradle build --no-daemon
if %errorlevel% neq 0 (
    echo.
    echo [LOI] Chua cai Gradle tren may hoac co loi build. Ban co the upload thu muc src len GitHub Actions nhu cu!
    pause
    exit /b 1
)
echo.
echo [THANH CONG] File .jar da nam trong thu muc build\libs\
explorer build\libs
pause
