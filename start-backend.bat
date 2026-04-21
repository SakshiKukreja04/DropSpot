@echo off
REM This batch file starts the DropSpot backend server
REM Keep this window open while developing!

cls
echo.
echo ======================================================
echo  DropSpot Backend Server Starter
echo ======================================================
echo.
echo Starting backend on port 5000...
echo Make sure your Android app is configured with:
echo   http://192.168.38.40:5000/api/
echo.
echo If using Emulator instead, use:
echo   http://10.0.2.2:5000/api/
echo.
echo Keep this window open while developing!
echo Press Ctrl+C to stop the server
echo.
echo ======================================================
echo.

cd /d "%~dp0\backend"
npm start

pause

