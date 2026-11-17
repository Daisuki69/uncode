@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
title QIEZKA - Build Release APK

:start_build
echo.
echo  ====================================================
echo   QIEZKA - Build Release APK
echo  ====================================================
echo.
echo  Starting Android Gradle build process...
echo.

:: Use 'call' so the script continues running after gradlew finishes
call "%~dp0android\gradlew.bat" -p "%~dp0android" assembleRelease

echo.
echo  ====================================================
echo   Build process completed.
echo  ====================================================
echo.

:ask_restart
set "RESTART_CHOICE="
set /p "RESTART_CHOICE=  Do you want to go again? [1 = Go Again, 2 = Exit]: "
if /i "!RESTART_CHOICE!"=="1" (
    cls
    goto :start_build
) else if /i "!RESTART_CHOICE!"=="2" (
    exit
) else (
    echo   [ERROR] Invalid choice. Please enter 1 or 2.
    goto :ask_restart
)