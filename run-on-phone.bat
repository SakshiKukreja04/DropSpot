@echo off
REM ==========================================
REM DropSpot Quick Start - Run on Phone
REM ==========================================

echo.
echo [1] Setting up PATH for ADB...
set PATH=%PATH%;C:\Android\sdk\platform-tools
setx PATH "%PATH%"

echo [2] Checking for connected devices...
adb devices
echo.

echo [3] Building APK...
cd /d C:\Users\saksh\AndroidStudioProjects\DropSpot
call gradlew.bat clean assembleDebug

if errorlevel 1 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo [4] Installing on connected device...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if errorlevel 1 (
    echo [ERROR] Installation failed!
    echo Make sure:
    echo - Phone is connected via USB
    echo - USB Debugging is enabled
    echo - You approved the connection on phone
    pause
    exit /b 1
)

echo [5] Launching app...
adb shell am start -n com.example.dropspot/.MainActivity

echo.
echo ==========================================
echo ✓ App installed and launched!
echo ==========================================
echo.
echo Monitoring logs (Press Ctrl+C to stop):
adb logcat | findstr "DropSpot"

pause
