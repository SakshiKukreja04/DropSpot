# ✅ DropSpot Setup Checklist

## 🗑️ Step 1: Cleanup - Remove FCM Files

In VS Code, delete these files:
- [ ] `app/src/main/java/com/example/dropspot/DropSpotMessagingService.java`
- [ ] `app/src/main/java/com/example/dropspot/FCMTokenManager.java`
- [ ] `app/src/main/java/com/example/dropspot/NotificationWorker.java`

**Status:** ✅ Code already cleaned in Java files

---

## 📱 Step 2: Phone Preparation

### Developer Mode
- [ ] Settings → About Phone
- [ ] Tap "Build Number" 7 times
- [ ] Settings → Developer Options
- [ ] Enable ✅ USB Debugging
- [ ] Enable ✅ USB Debugging (Security Settings)

### USB Connection
- [ ] Connect phone via USB cable
- [ ] Approve "USB Debugging" prompt on phone
- [ ] Test connection:
  ```powershell
  $env:PATH += ";C:\Android\sdk\platform-tools"
  adb devices
  # Should list your phone
  ```

---

## ⚙️ Step 3: Configuration

### Update API URL
- [ ] Open: `app/src/main/res/values/strings.xml`
- [ ] Find: `<string name="api_base_url">` 
- [ ] Replace with your computer's IP:
  ```xml
  <string name="api_base_url">http://192.168.X.X:5000</string>
  ```
- [ ] Save file

**Get your IP:**
```powershell
ipconfig  # Look for IPv4 Address
```

---

## 🚀 Step 4: Backend Service

### Start Backend (Keep running)
```powershell
cd C:\Users\saksh\AndroidStudioProjects\DropSpot\backend
npm install     # Only first time
npm start       # Runs on port 5000
```

**Window 1 Status:** ✅ Backend running
```
Server running on port 5000
```

---

## 🏗️ Step 5: Build & Run

### Option A: Use Batch Script (Easiest)
```powershell
cd C:\Users\saksh\AndroidStudioProjects\DropSpot
.\run-on-phone.bat
```

This will:
- [ ] Clean build folder
- [ ] Build APK
- [ ] Install on phone
- [ ] Launch app
- [ ] Show logs

---

### Option B: Manual Commands (Advanced)

**Terminal 2:**
```powershell
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

## 📊 Expected Output

### Terminal 1 (Backend)
```
> npm start

Server running on port 5000
```

### Terminal 2 (Build & Install)
```
BUILD SUCCESSFUL
installed: app-debug.apk
```

### Phone
```
DropSpot app opens with:
- Welcome screen (if first time)
- Login screen (if configured)
- App loads from your backend
```

---

## 🔧 Troubleshooting Quick Fixes

### "adb: command not found"
```powershell
$env:PATH += ";C:\Android\sdk\platform-tools"
adb devices
```

### Device not showing in "adb devices"
```powershell
adb kill-server
$env:PATH += ";C:\Android\sdk\platform-tools"
adb devices
# Approve on phone again
```

### Build fails
```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

### API connection fails
1. Check backend runs: `npm start` shows "Server running on port 5000"
2. Check IP: `ipconfig` matches strings.xml
3. Check firewall: Port 5000 must be open
4. Check phone logs: `adb logcat | findstr "DropSpot"`

---

## 📋 Verification Steps

- [ ] Backend running (Terminal 1): `npm start` → "Server running on port 5000"
- [ ] Phone connected: `adb devices` → Shows phone with "device" status
- [ ] Build successful: `.\gradlew.bat installDebug` → "BUILD SUCCESSFUL"
- [ ] App installed: `adb shell pm list packages | findstr dropspot`
- [ ] App launches: `adb shell am start -n com.example.dropspot/.MainActivity`
- [ ] Backend connected: App can fetch data without errors
- [ ] Logs visible: `adb logcat | findstr "DropSpot"` → Shows app messages

---

## 📱 Common Tasks

### View App Logs
```powershell
adb logcat | findstr "DropSpot"
```

### Restart App
```powershell
adb shell am start -n com.example.dropspot/.MainActivity
```

### Uninstall App
```powershell
adb uninstall com.example.dropspot
```

### Clear App Data
```powershell
adb shell pm clear com.example.dropspot
```

### Rebuild Completely
```powershell
.\gradlew.bat clean
.\gradlew.bat installDebug
adb shell am start -n com.example.dropspot/.MainActivity
```

---

## ✨ Ready to Go!

You now have:
- ✅ FCM code removed
- ✅ Phone setup guide
- ✅ Batch script for easy deployment
- ✅ Troubleshooting guide
- ✅ Command reference

**Next:** Run `.\run-on-phone.bat` and watch your app come to life! 🎉

---

**Need help?** Check:
1. [PHONE_SETUP_GUIDE.md](PHONE_SETUP_GUIDE.md) - Detailed setup
2. [Backend README](backend/README.md) - Backend API docs
3. Terminal output for specific errors
