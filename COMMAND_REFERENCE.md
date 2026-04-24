# 🎯 Quick Command Reference

## Build & Test Commands

### Clean Build
```powershell
cd C:\Users\saksh\AndroidStudioProjects\DropSpot
.\gradlew.bat clean
.\gradlew.bat build -x test
```

### Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```

### Build Release APK
```powershell
.\gradlew.bat assembleRelease
```

## ADB Commands

### Connect & Install
```powershell
# Check connected devices
adb devices

# Install APK
adb install app\build\outputs\apk\debug\app-debug.apk

# Uninstall app
adb uninstall com.example.dropspot

# Clear app cache
adb shell pm clear com.example.dropspot
```

### Run & Debug
```powershell
# Launch app
adb shell am start -n com.example.dropspot/.WelcomeActivity

# View logs
adb logcat

# Filter logs
adb logcat | findstr "DropSpot"
adb logcat | findstr "Firebase"
adb logcat | findstr "FCM"

# Save logs to file
adb logcat > logs.txt

# Clear logcat
adb logcat -c
```

## Gradle Tasks

### View Build Tasks
```powershell
.\gradlew.bat tasks
```

### Build Variants
```powershell
# Debug
.\gradlew.bat assembleDebug

# Release
.\gradlew.bat assembleRelease

# All
.\gradlew.bat assemble
```

## Testing

### Run All Tests
```powershell
.\gradlew.bat test
```

### Run Instrumented Tests
```powershell
.\gradlew.bat connectedAndroidTest
```

## File Locations

### APK Output
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

### Build Logs
```
app/build.log
final_build.log
```

### Source Code
```
app/src/main/java/com/example/dropspot/
```

### Resources
```
app/src/main/res/layout/
app/src/main/res/drawable/
```

### Backend
```
backend/routes/
backend/utils/
backend/config/
```

## Gradle Properties

### View Current Configuration
```powershell
cat gradle.properties
```

### View Build Configuration
```powershell
cat app/build.gradle.kts
```

## Firebase CLI

### Deploy Backend
```powershell
cd backend
firebase deploy
```

### View Logs
```powershell
firebase functions:log
```

### Test Locally
```powershell
firebase emulators:start
```

## Common Issues & Fixes

### Build Fails
```powershell
# Clean cache
.\gradlew.bat clean

# Rebuild
.\gradlew.bat build -x test --stacktrace
```

### APK Install Fails
```powershell
# Uninstall old version
adb uninstall com.example.dropspot

# Reinstall
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Device Not Found
```powershell
# Restart ADB
adb kill-server
adb start-server
adb devices
```

### App Crashes
```powershell
# View crash logs
adb logcat | findstr "ERROR\|Exception\|crash"

# Save full log
adb logcat > crash_log.txt
```

## Useful Workflows

### Complete Build & Install
```powershell
cd C:\Users\saksh\AndroidStudioProjects\DropSpot
.\gradlew.bat clean
.\gradlew.bat assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.dropspot/.WelcomeActivity
```

### Build & Monitor Logs
```powershell
.\gradlew.bat assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.dropspot/.WelcomeActivity
adb logcat | findstr "DropSpot\|FCM\|Firebase"
```

### Clean Install & Test
```powershell
adb uninstall com.example.dropspot
adb shell pm clear com.example.dropspot
.\gradlew.bat clean
.\gradlew.bat assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.dropspot/.WelcomeActivity
```

## Backend Commands

### Start Backend (Node.js)
```powershell
cd C:\Users\saksh\AndroidStudioProjects\DropSpot\backend
npm install
npm start
```

### Check Backend Status
```powershell
npm status
```

### View Backend Logs
```powershell
npm logs
```

## Documentation Files

All documentation saved in:
```
C:\Users\saksh\AndroidStudioProjects\DropSpot\
```

Files:
- FCM_NOTIFICATIONS_IMPLEMENTATION.md
- DISPATCH_FCM_GUIDE.md
- BUILD_STATUS.md
- RUN_ON_DEVICE.md
- FINAL_SUMMARY.md

---

**Ready to test!** Use these commands to build, install, and debug your app.

