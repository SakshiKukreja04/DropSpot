# PowerShell script to manage DropSpot development environment
# This script helps with starting backend, testing API, and managing the development setup

param(
    [string]$command = "menu"
)

$projectRoot = "C:\Users\saksh\AndroidStudioProjects\DropSpot"
$backendPath = "$projectRoot\backend"

# Function to display menu
function Show-Menu {
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  DropSpot Development Environment Manager      ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Start Backend Server (npm start)"
    Write-Host "2. Stop Backend Server"
    Write-Host "3. Test API Connectivity"
    Write-Host "4. Check IP Address"
    Write-Host "5. Show Current Configuration"
    Write-Host "6. Restart Backend"
    Write-Host "7. Install Backend Dependencies"
    Write-Host "8. View Backend Logs"
    Write-Host "9. Exit"
    Write-Host ""
}

# Start Backend
function Start-Backend {
    Write-Host "Starting DropSpot Backend Server..." -ForegroundColor Green
    Write-Host "📍 Backend running on: http://192.168.38.40:5000" -ForegroundColor Yellow
    Write-Host "Keep this window open while developing!" -ForegroundColor Yellow
    Write-Host ""

    Set-Location $backendPath
    npm start
}

# Stop Backend
function Stop-Backend {
    Write-Host "Stopping backend server..." -ForegroundColor Red
    Get-Process node -ErrorAction SilentlyContinue | Stop-Process -Force
    Write-Host "✅ Backend stopped" -ForegroundColor Green
}

# Test API
function Test-API {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  API Connectivity Test" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""

    $baseUrl = "http://192.168.38.40:5000"

    Write-Host "Testing: $baseUrl" -ForegroundColor Yellow
    Write-Host ""

    # Test 1: Basic connection
    Write-Host "[TEST 1] Basic connectivity..." -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri $baseUrl -TimeoutSec 5 -ErrorAction Stop
        Write-Host "✅ SUCCESS: Backend is reachable!" -ForegroundColor Green
        Write-Host "   Response: $($response.StatusCode)" -ForegroundColor Green
    } catch {
        Write-Host "❌ FAILED: Cannot reach backend" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }

    Write-Host ""

    # Test 2: Health check
    Write-Host "[TEST 2] Health endpoint..." -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/health" -TimeoutSec 5 -ErrorAction Stop
        Write-Host "✅ SUCCESS: Health check passed!" -ForegroundColor Green
        $body = $response.Content | ConvertFrom-Json
        Write-Host "   Message: $($body.message)" -ForegroundColor Green
    } catch {
        Write-Host "❌ FAILED: Health check failed" -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
}

# Get IP Address
function Get-IPAddress {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  Your Computer Network Information" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""

    $ipConfig = ipconfig | Select-String "IPv4"
    Write-Host $ipConfig

    Write-Host ""
    Write-Host "Use the IPv4 address in ApiClient.java:" -ForegroundColor Yellow
    Write-Host 'private static final String BASE_URL = "http://<YOUR_IP>:5000/api/";' -ForegroundColor Cyan

    Write-Host ""
    Write-Host "For Emulator use: http://10.0.2.2:5000/api/" -ForegroundColor Yellow
    Write-Host ""
}

# Show Configuration
function Show-Config {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  Current Configuration" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""

    # Check ApiClient.java
    Write-Host "1️⃣  ApiClient.java Configuration:" -ForegroundColor Yellow
    $apiClientPath = "$projectRoot\app\src\main\java\com\example\dropspot\ApiClient.java"
    if (Test-Path $apiClientPath) {
        $content = Get-Content $apiClientPath | Select-String 'BASE_URL'
        Write-Host "   $content" -ForegroundColor Cyan
    }

    Write-Host ""

    # Check Backend Port
    Write-Host "2️⃣  Backend Configuration:" -ForegroundColor Yellow
    $envPath = "$backendPath\.env"
    if (Test-Path $envPath) {
        Write-Host "   .env file exists ✅" -ForegroundColor Green
        $port = Get-Content $envPath | Select-String 'PORT'
        if ($port) {
            Write-Host "   $port" -ForegroundColor Cyan
        } else {
            Write-Host "   PORT not specified (default: 5000)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   No .env file (using defaults)" -ForegroundColor Yellow
    }

    Write-Host ""
}

# Install Dependencies
function Install-Dependencies {
    Write-Host "Installing backend dependencies..." -ForegroundColor Green
    Set-Location $backendPath
    npm install
    Write-Host "✅ Dependencies installed" -ForegroundColor Green
}

# View Logs
function View-Logs {
    Write-Host ""
    Write-Host "Backend is now running. Backend logs will appear below:" -ForegroundColor Yellow
    Write-Host "To stop, press Ctrl+C" -ForegroundColor Yellow
    Write-Host ""

    Start-Backend
}

# Process command or show interactive menu
if ($command -eq "menu") {
    Show-Menu
    $choice = Read-Host "Enter your choice (1-9)"

    switch ($choice) {
        "1" { Start-Backend }
        "2" { Stop-Backend }
        "3" { Test-API }
        "4" { Get-IPAddress }
        "5" { Show-Config }
        "6" { Stop-Backend; Start-Sleep -Seconds 2; Start-Backend }
        "7" { Install-Dependencies }
        "8" { View-Logs }
        "9" { Write-Host "Goodbye!"; exit }
        default { Write-Host "Invalid choice" -ForegroundColor Red }
    }
} else {
    # Run command directly
    switch ($command.ToLower()) {
        "start" { Start-Backend }
        "stop" { Stop-Backend }
        "test" { Test-API }
        "ip" { Get-IPAddress }
        "config" { Show-Config }
        "restart" { Stop-Backend; Start-Sleep -Seconds 2; Start-Backend }
        "install" { Install-Dependencies }
        "logs" { View-Logs }
        default {
            Write-Host "Unknown command: $command" -ForegroundColor Red
            Write-Host "Available commands: start, stop, test, ip, config, restart, install, logs" -ForegroundColor Yellow
        }
    }
}

