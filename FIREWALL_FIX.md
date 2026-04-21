# 🔌 Backend Connection Troubleshooting - QUICK FIX

## 🎯 Most Likely Issue: FIREWALL BLOCKING PORT 5000

Your Windows Firewall is probably blocking the connection from your device to the backend.

---

## ⚡ FIX #1: Allow Port 5000 Through Firewall

```powershell
# Run as Administrator in PowerShell:
netsh advfirewall firewall add rule name="Allow Node.js Port 5000" dir=in action=allow protocol=tcp localport=5000 enable=yes
```

**After running this, restart backend:**
```bash
taskkill /F /IM node.exe
cd backend
npm start
```

---

## ⚡ FIX #2: Manual Firewall Configuration

**Windows Settings Method:**
1. Press `Win + R`, type: `firewall.cpl`
2. Click: "Allow an app through firewall"
3. Click: "Change settings"
4. Click: "Allow another app"
5. Browse to: `C:\Program Files\nodejs\node.exe`
6. Click "Add"
7. Make sure it's checked for "Private" networks
8. Click "OK"

---

## ✅ Verify Connection Works

**Run this script:**
```
Double-click: C:\Users\saksh\AndroidStudioProjects\DropSpot\test_connection.bat
```

**It will check:**
- ✅ Is Node.js running?
- ✅ Is port 5000 listening?
- ✅ What's your IP address?
- ✅ Is firewall allowing port 5000?

---

## 🧪 Manual Test

**Step 1: Make sure backend is running**
```bash
cd backend
npm start
```

Should see:
```
DropSpot API Backend Server running on http://0.0.0.0:5000
```

**Step 2: Test from PC terminal**
```bash
curl http://192.168.29.133:5000/health
```

Should return:
```json
{"success":true,"message":"API is healthy"}
```

**Step 3: Test from Device**

**If using Emulator:**
```bash
adb shell curl http://10.0.2.2:5000/health
```

**If using Physical Device:**
```bash
adb shell curl http://192.168.29.133:5000/health
```

Both should return the health JSON.

---

## 🔍 Verification Checklist

After applying the firewall fix:

- [ ] Firewall rule added for port 5000
- [ ] Node.js backend running (see "Server running" message)
- [ ] Port 5000 listening (`netstat -ano | findstr :5000` shows entry)
- [ ] Device/Emulator can reach backend (curl returns 200)
- [ ] App can connect and see backend data

---

## 📋 Current Configuration

**Computer IP**: 192.168.29.133 ✓  
**Backend Port**: 5000 ✓  
**ApiClient URL**: http://192.168.29.133:5000/api/ ✓  
**CORS Configured**: Yes ✓  
**Firebase Initialized**: Yes ✓  

**Only issue**: Likely firewall blocking the connection ⚠️

---

## 🚀 Quick Fix Steps (5 minutes)

1. **Add firewall rule:**
   ```powershell
   netsh advfirewall firewall add rule name="Allow Node.js Port 5000" dir=in action=allow protocol=tcp localport=5000 enable=yes
   ```

2. **Restart backend:**
   ```bash
   taskkill /F /IM node.exe
   cd backend
   npm start
   ```

3. **Rebuild and test app:**
   ```bash
   .\gradlew.bat clean assembleDebug
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

4. **Test endpoint:**
   ```bash
   adb shell curl http://192.168.29.133:5000/health
   ```

---

**Status**: 🔥 Most likely firewall issue - Apply fix above  
**Time**: ~2 minutes to fix


