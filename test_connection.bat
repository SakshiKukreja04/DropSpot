@echo off
REM Backend Connection Test Script

echo ========================================
echo Testing Backend Connectivity
echo ========================================

echo.
echo 1. Checking if Node process is running...
tasklist | findstr "node.exe"
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Node.exe NOT running - Backend is DOWN
    echo Start backend: cd backend ^&^& npm start
) else (
    echo ✅ Node.exe is running
)

echo.
echo 2. Checking port 5000...
netstat -ano | findstr :5000
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Port 5000 NOT listening
) else (
    echo ✅ Port 5000 is listening
)

echo.
echo 3. Your IP Address:
ipconfig | findstr "192.168"

echo.
echo 4. Firewall Check:
netsh advfirewall firewall show rule name="*5000*" | findstr "Rule"
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️ No firewall rule found for port 5000
    echo Add with: netsh advfirewall firewall add rule name="Allow Node 5000" dir=in action=allow protocol=tcp localport=5000
)

echo.
echo ========================================
pause

