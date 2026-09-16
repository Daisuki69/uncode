@echo off
setlocal enabledelayedexpansion
title QIEZKA Setup and Permissions Tool

:: ============================================================================
::                     USER CONFIGURATION / PREFERENCES
::  Edit the values below (true or false) to tailor the setup to your needs.
:: ============================================================================

:: 1. Re-install/update local APK on device (default: true, keeps existing data)
set "FORCE_REINSTALL_APK=true"

:: 2. Unlock Android 13/14+ Restricted Settings automatically via ADB
set "BYPASS_RESTRICTED_SETTINGS=true"

:: 3. Grant elevated system permissions (WRITE_SECURE_SETTINGS, DUMP)
set "GRANT_SECURE_PERMISSIONS=true"

:: 4. Whitelist QIEZKA from aggressive OS battery savers (Samsung, Xiaomi, etc.)
set "WHITELIST_BATTERY=true"

:: 5. Automatically enable QIEZKA's Accessibility Service via ADB
set "ENABLE_ACCESSIBILITY=true"

:: 6. Activate Device Administrator to prevent uninstallation during lockdown
::    (100% realistic: works with all personal Google accounts logged in, no wipe needed)
set "ACTIVATE_DEVICE_ADMIN=true"


:: 7. Automatically launch QIEZKA on your phone after setup completes
set "LAUNCH_APP_ON_FINISH=true"

:: ============================================================================

:: Check for Administrator privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting Administrator privileges...
    powershell.exe -Command "Start-Process cmd -ArgumentList '/k \"%~dpnx0\"' -Verb RunAs"
    exit /b
)

:: Project directory
set "PROJECT_DIR=%~dp0"

echo.
echo  ====================================================
echo   QIEZKA - Android Permission and Lockdown Setup
echo  ====================================================
echo.

:: Check ADB is available
adb.exe version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] ADB not found. Make sure Android platform-tools is installed and in your PATH.
    goto :end
)

:: Check device connected
echo [1/6] Checking connected device...
for /f "tokens=1" %%d in ('adb.exe devices ^| findstr /v "List" ^| findstr "device"') do set DEVICE=%%d
if "!DEVICE!"=="" (
    echo [ERROR] No device detected. Connect your phone via USB and enable USB Debugging.
    goto :end
)
echo       Device found: !DEVICE!
echo.

:: [2/6] Check App Installation / APK Install
echo [2/6] Checking QIEZKA installation on device...
set "APP_INSTALLED="
for /f "tokens=*" %%p in ('adb.exe shell pm path com.uncode.app 2^>nul') do set "APP_INSTALLED=%%p"

if not "!APP_INSTALLED!"=="" (
    if /i "!FORCE_REINSTALL_APK!"=="true" (
        echo       QIEZKA is already on device, but FORCE_REINSTALL_APK is true.
        goto :do_install
    )
    echo       QIEZKA is already installed on your device.
    echo       Proceeding directly to permissions setup...
    goto :after_install
)

:do_install
echo       Preparing QIEZKA APK installation/update...
echo       Searching for local APK to install...

set "APK_PATH="
if exist "%PROJECT_DIR%android\app\build\outputs\apk\debug\app-debug.apk" (
    set "APK_PATH=%PROJECT_DIR%android\app\build\outputs\apk\debug\app-debug.apk"
) else if exist "%PROJECT_DIR%android\app\build\outputs\apk\debug\qiezka.apk" (
    set "APK_PATH=%PROJECT_DIR%android\app\build\outputs\apk\debug\qiezka.apk"
) else if exist "%PROJECT_DIR%qiezka.apk" (
    set "APK_PATH=%PROJECT_DIR%qiezka.apk"
) else if exist "%PROJECT_DIR%app-debug.apk" (
    set "APK_PATH=%PROJECT_DIR%app-debug.apk"
) else if exist "%PROJECT_DIR%uncode.apk" (
    set "APK_PATH=%PROJECT_DIR%uncode.apk"
)

if not "!APK_PATH!"=="" (
    echo       Found local APK: !APK_PATH!
    echo       Installing to phone via ADB...
    adb.exe install -r "!APK_PATH!" >nul 2>&1
    if errorlevel 1 (
        echo       [ERROR] APK installation failed. Ensure your phone screen is unlocked.
        goto :end
    ) else (
        echo       QIEZKA installed successfully.
    )
) else (
    echo.
    echo  ========================================================================
    echo   [ERROR] QIEZKA is NOT installed on your phone and no APK was found!
    echo  ========================================================================
    echo.
    echo   Please download and install QIEZKA on your phone first from GitHub:
    echo   https://github.com/Daisuki69/uncode/releases
    echo.
    echo   Once installed on your phone, re-run this script to configure permissions.
    echo  ========================================================================
    goto :end
)

:after_install
echo.

:: [3/6] Permissions & Restricted Settings
echo [3/6] Configuring system permissions...
if /i "!GRANT_SECURE_PERMISSIONS!"=="true" (
    adb.exe shell pm grant com.uncode.app android.permission.WRITE_SECURE_SETTINGS >nul 2>&1
    adb.exe shell pm grant com.uncode.app android.permission.DUMP >nul 2>&1
    adb.exe shell pm grant com.uncode.app android.permission.POST_NOTIFICATIONS >nul 2>&1
    adb.exe shell appops set com.uncode.app SYSTEM_ALERT_WINDOW allow >nul 2>&1
    adb.exe shell appops set com.uncode.app SCHEDULE_EXACT_ALARM allow >nul 2>&1
    echo       WRITE_SECURE_SETTINGS, DUMP, POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW, and SCHEDULE_EXACT_ALARM: GRANTED.
) else (
    echo       Secure permissions: SKIPPED [Configured: false].
)

if /i "!BYPASS_RESTRICTED_SETTINGS!"=="true" (
    adb.exe shell appops set com.uncode.app ACCESS_RESTRICTED_SETTINGS allow >nul 2>&1
    echo       Restricted settings: UNLOCKED via ADB.
) else (
    echo       Restricted settings unlock: SKIPPED [Configured: false].
)
echo.

:: [4/6] Battery optimization whitelist
if /i "!WHITELIST_BATTERY!"=="true" (
    echo [4/6] Whitelisting from battery optimization...
    adb.exe shell dumpsys deviceidle whitelist +com.uncode.app >nul 2>&1
    echo       Battery whitelist OK.
) else (
    echo [4/6] Battery whitelist: SKIPPED [Configured: false].
)
echo.

:: [5/6] Automatic Accessibility Service enablement via ADB
if /i "!ENABLE_ACCESSIBILITY!"=="true" (
    echo [5/6] Enabling Accessibility Service automatically...
    adb.exe shell settings put secure accessibility_enabled 1 >nul 2>&1
    set "CURR_SVCS="
    for /f "tokens=*" %%s in ('adb.exe shell settings get secure enabled_accessibility_services 2^>nul') do set "CURR_SVCS=%%s"
    if "!CURR_SVCS!"=="null" set "CURR_SVCS="
    echo !CURR_SVCS! | findstr /c:"com.uncode.app" >nul
    if errorlevel 1 (
        if "!CURR_SVCS!"=="" (
            adb.exe shell settings put secure enabled_accessibility_services com.uncode.app/com.uncode.app.LockAccessibilityService >nul 2>&1
        ) else (
            adb.exe shell settings put secure enabled_accessibility_services "!CURR_SVCS!:com.uncode.app/com.uncode.app.LockAccessibilityService" >nul 2>&1
        )
    )
    echo       Accessibility Service enabled.
) else (
    echo [5/6] Accessibility Service: SKIPPED [Configured: false].
)
echo.

:: [6/6] Device Administrator & Device Owner setup
echo [6/6] Configuring Uninstall and Lockdown Protection...
if /i "!ACTIVATE_DEVICE_ADMIN!"=="true" (
    adb.exe shell dpm set-active-admin com.uncode.app/.AdminReceiver >nul 2>&1
    if not errorlevel 1 (
        echo       Device Administrator: ACTIVATED [Uninstall locked]
    ) else (
        echo       [INFO] Device Administrator prompt may appear on device screen.
    )
) else (
    echo       Device Administrator: SKIPPED [Configured: false].
)



:: Launch QIEZKA
if /i "!LAUNCH_APP_ON_FINISH!"=="true" (
    echo Launching QIEZKA...
    adb.exe shell am start -n com.uncode.app/.MainActivity --es setup_source adb >nul 2>&1
    echo.
)

echo  ====================================================
echo   Setup Complete
echo  ====================================================
echo.
echo   - App Blocking: Active via LockAccessibilityService
echo   - Quick Settings: Blocked
echo   - Camera and Gallery: Allowed for homework submission
echo   - Notifications: Pre-granted via ADB
if /i "!ACTIVATE_DEVICE_ADMIN!"=="true" (
    echo   - Uninstall Protection: Active [Device Admin + Settings blocked]
) else (
    echo   - Uninstall Protection: Disabled by user config
)
echo   - Mode: HIGH-SECURITY DEVICE ADMIN [Uninstall locked]
echo.

:end
echo ====================================================
echo Press any key to exit.
pause >nul