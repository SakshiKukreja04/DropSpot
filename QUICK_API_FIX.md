# 🚀 QUICK FIX: API Connectivity After IP Change

## 📋 What Was Changed

✅ **ApiClient.java** - Updated BASE_URL to include port 5000
- Before: `http://192.168.38.40/api/`
- After: `http://192.168.38.40:5000/api/`

✅ **backend/index.js** - Added CORS whitelist for device IP and emulator
- Device IP: `http://192.168.38.40:*`
- Emulator: `http://10.0.2.2:*`

---

## 🔧 STEP-BY-STEP TO GET API WORKING

### **STEP 1: Start the Backend Server**

**Option A: Using Batch Script (Recommended)**
```
Double-click: C:\Users\saksh\AndroidStudioProjects\DropSpot\start-backend.bat
```

**Option B: Manual**
```powershell
cd C:\Users\saksh\AndroidStudioProjects\DropSpot\backend
npm start
```

⚠️ **Keep this terminal OPEN** while developing!

**Expected Output:**
```
DropSpot API Backend Server running on port 5000
```

---

### **STEP 2: Test Backend Connectivity**

**Option A: Using Test Script (Recommended)**
```
Double-click: C:\Users\saksh\AndroidStudioProjects\DropSpot\test-api-connectivity.bat
```

**Option B: Manual Test**
```powershell
# Should return JSON response
curl http://192.168.38.40:5000

# Should return: {"success":true,"message":"DropSpot API Server is running"}
```

✅ If you see the response, backend is working!

---

### **STEP 3: Rebuild Android App**

1. **Open Android Studio**
2. **File → Sync Now**
   - Waits for Gradle to sync
3. **Build → Clean Project**
   - Removes old build artifacts
4. **Build → Rebuild Project**
   - Rebuilds entire project
5. **Wait for build to complete**
   - Check bottom status bar for "Build successful"

---

### **STEP 4: Run App on Device**

1. **Connect Android device via USB**
2. **Enable USB Debugging:**
   - Settings → Developer Options → USB Debugging → ON
3. **Select Device:**
   - Android Studio: Top toolbar → Select device
4. **Click Run (green play button)**
   - Or press `Shift + F10`

---

### **STEP 5: Monitor App Logs (Logcat)**

1. **View → Tool Windows → Logcat**
2. **Search for: `ApiClient`**
3. **Look for these messages:**
   ```
   D/ApiClient: Firebase Token attached to request  ✅ GOOD
   ```

4. **If you see errors:**
   ```
   E/ApiClient: Error getting Firebase Token  ❌ Check Firebase
   E/Retrofit: HTTP 401                        ❌ Auth issue
   E/Retrofit: Connection refused               ❌ Backend not running
   ```

---

## 🐛 TROUBLESHOOTING

### Problem: "Connection refused" / "Cannot reach server"

**Check List:**
1. ✅ Backend running? (`npm start` visible in terminal)
2. ✅ Correct IP? (Run `test-api-connectivity.bat`)
3. ✅ Port 5000? (URL should have `:5000`)
4. ✅ Firewall? (Windows Firewall blocking port 5000?)

**Fix:**
```powershell
# Check if backend is listening
netstat -ano | findstr "5000"

# Kill and restart if needed
taskkill /PID <PID_NUMBER> /F
cd backend && npm start
```

---

### Problem: "No FCM token found" Errors

**Cause:** App not initialized properly with Firebase
**Fix:**
1. Make sure Firebase is set up (google-services.json present)
2. Clear app data: Settings → Apps → DropSpot → Storage → Clear
3. Uninstall and reinstall app
4. Wait for app to fully initialize before making API calls

---

### Problem: Still getting errors after fix

**Step 1: Verify API is actually being called**
```
1. Open Logcat
2. Filter for: "Retrofit" or "OkHttp"
3. Look for actual HTTP requests
4. Check if response codes appear (200, 401, etc.)
```

**Step 2: Check backend logs**
```
In the backend terminal, look for:
[TIMESTAMP] POST /api/posts
[TIMESTAMP] GET /api/requests
etc.

If you see requests = API is being called ✅
If you don't see requests = App is not making calls ❌
```

**Step 3: Clear app cache**
```powershell
adb shell pm clear com.example.dropspot
```

---

## 📱 EMULATOR vs PHYSICAL DEVICE

### **For Physical Device:**
```java
// ApiClient.java
private static final String BASE_URL = "http://192.168.38.40:5000/api/";
```

### **For Android Emulator:**
```java
// ApiClient.java
private static final String BASE_URL = "http://10.0.2.2:5000/api/";
```

---

## ✅ VERIFICATION CHECKLIST

After completing all steps, verify:

- [ ] Backend is running (`npm start` terminal visible)
- [ ] ApiClient.java has correct URL with `:5000` port
- [ ] Build is successful (no red errors)
- [ ] App installed on device
- [ ] Device has internet (can open browser)
- [ ] Logcat shows "Firebase Token attached"
- [ ] Backend logs show incoming requests
- [ ] No "Connection refused" errors

---

## 🎯 WHAT SHOULD HAPPEN

### Before Fix ❌
```
API calls not being made
Network errors in Logcat
No HTTP requests in backend logs
```

### After Fix ✅
```
Logcat shows: "Firebase Token attached to request"
Backend logs show: "POST /api/posts", "GET /api/requests"
Data loads in app
No connection errors
```

---

## 📞 STILL HAVING ISSUES?

1. **Check the detailed guide:** `API_CONNECTIVITY_FIX.md`
2. **Verify IP address:** Run `test-api-connectivity.bat`
3. **Check backend status:** Look at backend terminal for errors
4. **Review Logcat:** View → Tool Windows → Logcat
5. **Force rebuild:** Build → Clean → Rebuild

---

**Once you confirm backend is running and API calls are working, all features should function properly!** 🎉

