# DropSpot - Phone Setup & Deployment Guide

## Step 1: Delete FCM-Related Files

Since Firebase Cloud Messaging has been removed, delete these files from your project:

```
app/src/main/java/com/example/dropspot/
├── DropSpotMessagingService.java        ❌ DELETE
├── FCMTokenManager.java                 ❌ DELETE
└── NotificationWorker.java              ❌ DELETE
```

**To delete in VS Code:**
1. Open file explorer on the left sidebar
2. Navigate to each file above
3. Right-click and select "Delete"

---

## Step 2: Enable Developer Mode on Your Phone

1. **Settings → About Phone**
2. Find "Build Number" field
3. Tap 7 times until "Developer mode enabled" message appears
4. Go back and enter **Settings → Developer Options**
5. Enable:
   - ✅ USB Debugging
   - ✅ USB Debugging (Security Settings)
   - ✅ Install via USB

---

## Step 3: Install Android SDK Command-line Tools

### Option A: Using Android Studio (Recommended)
1. Open Android Studio
2. **Tools → SDK Manager**
3. Install:
   - Android SDK Build-Tools (36.0.0)
   - Android Platform (API 36)
   - Android Emulator (optional)

### Option B: Manual Installation
```powershell
# Create SDK directory
mkdir C:\Android\sdk
mkdir C:\Android\sdk\platform-tools
mkdir C:\Android\sdk\build-tools

# Download from https://developer.android.com/studio/releases/cmdline-tools
# Extract platform-tools to C:\Android\sdk\platform-tools
```

---

## Step 4: Connect Your Phone via USB

1. **Plug your phone into your computer** with USB cable
2. **On your phone:** Tap "Allow" when prompted for USB Debugging
3. **Verify connection in PowerShell:**

```powershell
# Add platform-tools to PATH (if not already done)
$env:PATH += ";C:\Android\sdk\platform-tools"

# List connected devices
adb devices

# Output should show:
# List of attached devices
# XXXXXXXX    device
```

If it shows "unauthorized", disconnect and reconnect, then approve again on phone.

---

## Step 5: Configure Your Backend API URL

The app needs to connect to your backend. Update the API base URL:

**File:** `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="api_base_url">http://<YOUR_IP>:5000</string>
    <!-- Replace <YOUR_IP> with your computer's IP address -->
</resources>
```

**To find your computer's IP:**
```powershell
# Windows
ipconfig

# Look for "IPv4 Address" (e.g., 192.168.1.100)
```

---

## Step 6: Start Your Backend Server

The app won't work without the backend running.

```powershell
# Navigate to backend folder
cd C:\Users\saksh\AndroidStudioProjects\DropSpot\backend

# Install dependencies (first time only)
npm install

# Start server
npm start

# You should see:
# Server running on port 5000
```

---

## Step 7: Build and Run on Phone

### Option A: Using VS Code with Flutter/Dart Extension
1. Open VS Code
2. Install **Flutter** extension (if not already)
3. Open Command Palette (Ctrl + Shift + P)
4. Run: `Flutter: Select Device`
5. Select your phone

*Note: This requires Flutter setup. Since you're using native Android, use Option B.*

### Option B: Using Android Studio in VS Code

1. **Open integrated terminal in VS Code:** Ctrl + `
2. **Navigate to project:**
```powershell
cd C:\Users\saksh\AndroidStudioProjects\DropSpot
```

3. **Build APK:**
```powershell
# For debug build
.\gradlew.bat assembleDebug

# Output: app\build\outputs\apk\debug\app-debug.apk
```

4. **Install on device:**
```powershell
# Ensure adb is in PATH
$env:PATH += ";C:\Android\sdk\platform-tools"

# Install app
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Success output:
# Success
```

5. **Run the app on phone:**
```powershell
adb shell am start -n com.example.dropspot/.MainActivity
```

---

## Step 8: Using Gradle Wrapper (Recommended)

The project includes **gradlew** which handles Gradle automatically:

```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Build and install on device
.\gradlew.bat installDebug

# Run on device
adb shell am start -n com.example.dropspot/.MainActivity
```

---

## Step 9: View Real-time Logs

Monitor your app's logs while running:

```powershell
# Show all logs
adb logcat

# Filter by app tag
adb logcat | findstr "DropSpot"

# Filter by level (E=Error, W=Warning, I=Info, D=Debug)
adb logcat *:W
```

---

## Complete Workflow: End-to-End

```powershell
# 1. Terminal 1: Start backend
cd C:\Users\saksh\AndroidStudioProjects\DropSpot\backend
npm start

# 2. Terminal 2: Build and deploy
cd C:\Users\saksh\AndroidStudioProjects\DropSpot
$env:PATH += ";C:\Android\sdk\platform-tools"

# Build and install
.\gradlew.bat installDebug

# Launch app
adb shell am start -n com.example.dropspot/.MainActivity

# View logs
adb logcat | findstr "DropSpot"
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "adb: command not found" | Add platform-tools to PATH: `$env:PATH += ";C:\Android\sdk\platform-tools"` |
| "device not found/offline" | 1. Disconnect & reconnect USB 2. Restart adb: `adb kill-server` then `adb devices` |
| "Permission denied" | Enable USB Debugging in Developer Options again |
| "API connection failed" | 1. Check backend is running 2. Verify correct IP in strings.xml 3. Check firewall allows port 5000 |
| "Gradle build failed" | 1. Delete build folder: `.\gradlew.bat clean` 2. Rebuild: `.\gradlew.bat assembleDebug` |
| "Firebase initialization error" | Check google-services.json is present in app folder |

---

## Firebase Setup (For Reference)

Your Firebase configuration is already set up in:
- `app/google-services.json` - Contains Firebase credentials
- `backend/config/firebase.js` - Backend Firebase configuration

If you need to reconfigure Firebase:
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Download google-services.json
3. Place in `app/` folder
4. Rebuild the app

---

## Quick Reference Commands

```powershell
# List all connected devices
adb devices

# Reboot device
adb reboot

# Clear app data
adb shell pm clear com.example.dropspot

# Uninstall app
adb uninstall com.example.dropspot

# Push file to device
adb push C:\local\file /sdcard/file

# Pull file from device
adb pull /sdcard/file C:\local\

# Open app shell
adb shell

# Check storage
adb shell df
```

---

## Next Steps

1. ✅ Delete FCM files (DropSpotMessagingService.java, FCMTokenManager.java, NotificationWorker.java)
2. ✅ Enable Developer Mode on phone
3. ✅ Connect phone via USB
4. ✅ Update API URL in strings.xml
5. ✅ Start backend server (npm start)
6. ✅ Build and install: `.\gradlew.bat installDebug`
7. ✅ Launch app: `adb shell am start -n com.example.dropspot/.MainActivity`
8. ✅ Monitor logs: `adb logcat | findstr "DropSpot"`

---

**Happy coding! 🚀**
