@echo off
REM API Connectivity Test Script
REM This script tests if the backend is accessible from your device IP

setlocal enabledelayedexpansion

cls
echo.
echo ======================================================
echo  DropSpot API Connectivity Test
echo ======================================================
echo.

REM Get local IP
for /f "tokens=2 delims=:" %%a in ('ipconfig^|find "IPv4"') do (
    set "ip=%%a"
    set "ip=!ip: =!"
)

echo Your Computer IP: %ip%
echo.
echo Backend Base URL should be:
echo   http://%ip%:5000/api/
echo.
echo ======================================================
echo.

REM Test basic connectivity
echo [TEST 1] Testing basic connectivity to backend...
curl -s http://%ip%:5000 >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED: Cannot reach backend at http://%ip%:5000
    echo.
    echo Make sure:
    echo   1. Backend is running (npm start)
    echo   2. Port 5000 is not blocked by firewall
    echo   3. You're using correct IP address
    echo.
) else (
    echo ✅ PASSED: Backend is reachable!
)

echo.

REM Test health endpoint
echo [TEST 2] Testing health endpoint...
curl -s http://%ip%:5000/health >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED: Health endpoint not responding
) else (
    echo ✅ PASSED: Health endpoint is working!
)

echo.
echo ======================================================
echo.

REM Display test results
echo Summary:
echo   Backend URL: http://%ip%:5000/api/
echo   Status: Check results above
echo.
echo Next Steps:
echo   1. Update ApiClient.java with: http://%ip%:5000/api/
echo   2. Rebuild Android app (Build ^> Rebuild Project)
echo   3. Clear app data on device (Settings ^> Apps)
echo   4. Reinstall app on device
echo   5. Check Logcat for "Firebase Token attached to request"
echo.

pause

