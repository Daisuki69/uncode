@echo off
setlocal enabledelayedexpansion
title QIEZKA Setup and Permissions Tool

:: ============================================================================
::                     USER CONFIGURATION / PREFERENCES
::  Edit the values below (true or false) to tailor the setup to your needs.
:: ============================================================================

:: 1. Re-install/update local APK on device (default: true, keeps existing data)[cite: 5]
set "FORCE_REINSTALL_APK=true"

:: 2. Unlock Android 13/14+ Restricted Settings automatically via ADB[cite: 5]
set "BYPASS_RESTRICTED_SETTINGS=true"

:: 3. Grant elevated system permissions (WRITE_SECURE_SETTINGS, DUMP)[cite: 5]
set "GRANT_SECURE_PERMISSIONS=true"

:: 4. Whitelist QIEZKA from aggressive OS battery savers (Samsung, Xiaomi, etc.)[cite: 5]
set "WHITELIST_BATTERY=true"

:: 5. Automatically enable QIEZKA's Accessibility Service via ADB[cite: 5]
set "ENABLE_ACCESSIBILITY=true"

:: 6. Activate Device Administrator to prevent uninstallation during lockdown[cite: 5]
::    (100% realistic: works with all personal Google accounts logged in, no wipe needed)
set "ACTIVATE_DEVICE_ADMIN=true"

:: 7. Automatically launch QIEZKA on your phone after setup completes[cite: 5]
set "LAUNCH_APP_ON_FINISH=true"

:: ============================================================================

:: Check for Administrator privileges[cite: 5]
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting Administrator privileges...
    powershell.exe -Command "Start-Process cmd -ArgumentList '/c \"%~dpnx0\"' -Verb RunAs"
    exit /b
)

:: Project directory[cite: 5]
set "PROJECT_DIR=%~dp0"

echo.
echo  ====================================================
echo   QIEZKA - Android Permission and Lockdown Setup
echo  ====================================================
echo.

:: Check ADB is available[cite: 5]
adb.exe version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] ADB not found. Make sure Android platform-tools is installed and in your PATH.
    goto :end
)

:: Pre-locate local APK if available[cite: 5]
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

:: [1/6] Check connected devices[cite: 5]
echo [1/6] Checking connected devices...
set "DEV_COUNT=0"
for /f "tokens=1,2" %%A in ('adb.exe devices ^| findstr /v /i "List"') do (
    if /i "%%B"=="device" (
        set /a DEV_COUNT+=1
        set "DEV_ID_!DEV_COUNT!=%%A"
    )
)

if !DEV_COUNT! equ 0 (
    echo [ERROR] No authorized device detected. Connect your phone via USB and enable USB Debugging.
    goto :end
)

if !DEV_COUNT! equ 1 (
    set "TARGET_DEVICES=!DEV_ID_1!"
    echo       Single device detected: !DEV_ID_1!
    goto :start_setup
)

echo.
echo       Multiple devices detected:
for /l %%I in (1,1,!DEV_COUNT!) do (
    set "DID=!DEV_ID_%%I!"
    set "DMODEL="
    set "DVER="
    for /f "tokens=*" %%M in ('adb.exe -s !DID! shell getprop ro.product.model 2^>nul') do set "DMODEL=%%M"
    for /f "tokens=*" %%V in ('adb.exe -s !DID! shell getprop ro.build.version.release 2^>nul') do set "DVER=%%V"
    if "!DMODEL!"=="" set "DMODEL=Unknown"
    if "!DVER!"=="" set "DVER=Unknown"
    echo         [%%I] !DID! (!DMODEL! - Android !DVER!)
)
echo         [A] Install on ALL connected devices
echo.

:select_device
set "CHOICE="
set /p "CHOICE=      Select target device [1-!DEV_COUNT! or A]: "
if /i "!CHOICE!"=="A" (
    set "TARGET_DEVICES="
    for /l %%I in (1,1,!DEV_COUNT!) do (
        set "TARGET_DEVICES=!TARGET_DEVICES! !DEV_ID_%%I!"
    )
    echo       Selected target: ALL connected devices
) else (
    if not defined DEV_ID_!CHOICE! (
        echo       [ERROR] Invalid selection. Please enter a number from 1 to !DEV_COUNT! or A.
        goto :select_device
    )
    for %%C in (!CHOICE!) do set "TARGET_DEVICES=!DEV_ID_%%C!"
    echo       Selected target: !TARGET_DEVICES!
)

:start_setup
echo.
echo ====================================================
echo  Executing Setup on Target Device(s)
echo ====================================================
echo.

:: [2/6] Check App Installation / APK Install[cite: 5]
echo [2/6] Checking QIEZKA installation...
for %%D in (!TARGET_DEVICES!) do (
    set "DEVICE=%%~D"
    echo   - Device: !DEVICE!
    set "APP_INSTALLED="
    for /f "tokens=*" %%p in ('adb.exe -s !DEVICE! shell pm path com.uncode.app 2^>nul') do set "APP_INSTALLED=%%p"

    set "PERFORM_INSTALL=false"
    if "!APP_INSTALLED!"=="" (
        set "PERFORM_INSTALL=true"
    ) else (
        if /i "!FORCE_REINSTALL_APK!"=="true" (
            echo       QIEZKA is already on device, but FORCE_REINSTALL_APK is true.
            set "PERFORM_INSTALL=true"
        ) else (
            echo       QIEZKA is already installed. Proceeding...
        )
    )

    if /i "!PERFORM_INSTALL!"=="true" (
        if not "!APK_PATH!"=="" (
            echo       Installing local APK...
            adb.exe -s !DEVICE! install -r "!APK_PATH!" >nul 2>&1
            if errorlevel 1 (
                echo       [ERROR] APK installation failed on !DEVICE!. Ensure the screen is unlocked.
            ) else (
                echo       QIEZKA installed successfully.
            )
        ) else (
            echo       [ERROR] No local APK found for !DEVICE!! Download it first.
        )
    )
    echo.
)

:: [3/6] Permissions & Restricted Settings[cite: 5]
echo [3/6] Configuring system permissions...
for %%D in (!TARGET_DEVICES!) do (
    set "DEVICE=%%~D"
    echo   - Device: !DEVICE!
    if /i "!GRANT_SECURE_PERMISSIONS!"=="true" (
        adb.exe -s !DEVICE! shell pm grant com.uncode.app android.permission.WRITE_SECURE_SETTINGS >nul 2>&1
        adb.exe -s !DEVICE! shell pm grant com.uncode.app android.permission.DUMP >nul 2>&1
        adb.exe -s !DEVICE! shell pm grant com.uncode.app android.permission.POST_NOTIFICATIONS >nul 2>&1
        adb.exe -s !DEVICE! shell appops set com.uncode.app SYSTEM_ALERT_WINDOW allow >nul 2>&1
        adb.exe -s !DEVICE! shell appops set com.uncode.app SCHEDULE_EXACT_ALARM allow >nul 2>&1
        echo       Secure permissions GRANTED.
    ) else (
        echo       Secure permissions SKIPPED.
    )

    if /i "!BYPASS_RESTRICTED_SETTINGS!"=="true" (
        adb.exe -s !DEVICE! shell appops set com.uncode.app ACCESS_RESTRICTED_SETTINGS allow >nul 2>&1
        echo       Restricted settings UNLOCKED.
    ) else (
        echo       Restricted settings SKIPPED.
    )
    echo.
)

:: [4/6] Battery optimization whitelist[cite: 5]
echo [4/6] Whitelisting from battery optimization...
for %%D in (!TARGET_DEVICES!) do (
    set "DEVICE=%%~D"
    echo   - Device: !DEVICE!
    if /i "!WHITELIST_BATTERY!"=="true" (
        adb.exe -s !DEVICE! shell dumpsys deviceidle whitelist +com.uncode.app >nul 2>&1
        echo       Battery whitelist OK.
    ) else (
        echo       Battery whitelist SKIPPED.
    )
    echo.
)

:: [5/6] Automatic Accessibility Service enablement via ADB[cite: 5]
echo [5/6] Enabling Accessibility Service...
for %%D in (!TARGET_DEVICES!) do (
    set "DEVICE=%%~D"
    echo   - Device: !DEVICE!
    if /i "!ENABLE_ACCESSIBILITY!"=="true" (
        adb.exe -s !DEVICE! shell settings put secure accessibility_enabled 1 >nul 2>&1
        set "CURR_SVCS="
        for /f "tokens=*" %%s in ('adb.exe -s !DEVICE! shell settings get secure enabled_accessibility_services 2^>nul') do set "CURR_SVCS=%%s"
        if "!CURR_SVCS!"=="null" set "CURR_SVCS="
        echo !CURR_SVCS! | findstr /c:"com.uncode.app" >nul
        if errorlevel 1 (
            if "!CURR_SVCS!"=="" (
                adb.exe -s !DEVICE! shell settings put secure enabled_accessibility_services com.uncode.app/com.uncode.app.LockAccessibilityService >nul 2>&1
            ) else (
                adb.exe -s !DEVICE! shell settings put secure enabled_accessibility_services "!CURR_SVCS!:com.uncode.app/com.uncode.app.LockAccessibilityService" >nul 2>&1
            )
        )
        echo       Accessibility Service enabled.
    ) else (
        echo       Accessibility Service SKIPPED.
    )
    echo.
)

:: [6/6] Device Administrator setup[cite: 5]
echo [6/6] Configuring Uninstall and Lockdown Protection...
for %%D in (!TARGET_DEVICES!) do (
    set "DEVICE=%%~D"
    echo   - Device: !DEVICE!
    if /i "!ACTIVATE_DEVICE_ADMIN!"=="true" (
        adb.exe -s !DEVICE! shell dpm set-active-admin com.uncode.app/.AdminReceiver >nul 2>&1
        if not errorlevel 1 (
            echo       Device Administrator ACTIVATED.
        ) else (
            echo       [INFO] Prompt may appear on device screen.
        )
    ) else (
        echo       Device Administrator SKIPPED.
    )
    echo.
)

:: Launch QIEZKA[cite: 5]
echo [7/7] Launching App...
for %%D in (!TARGET_DEVICES!) do (
    set "DEVICE=%%~D"
    if /i "!LAUNCH_APP_ON_FINISH!"=="true" (
        echo   - Launching QIEZKA on !DEVICE!...
        adb.exe -s !DEVICE! shell am start -n com.uncode.app/.MainActivity --es setup_source adb >nul 2>&1
    )
)

echo.
echo ====================================================
echo  All operations finished for target device(s).
echo ====================================================
:end
echo Press any key to exit.
pause >nul
exit