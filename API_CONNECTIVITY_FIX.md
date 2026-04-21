# 🔧 API Connectivity Troubleshooting Guide

## Problem
After changing the IP address in `ApiClient.java`, REST API calls are not being made.

**Current URL:** `http://192.168.38.40/api/`

---

## ✅ STEP-BY-STEP DIAGNOSTIC CHECKLIST

### Step 1: Verify Backend is Running
```powershell
# Check if backend is listening on port 5000
netstat -ano | findstr "5000"

# If not running, start the backend:
cd C:\Users\saksh\AndroidStudioProjects\DropSpot\backend
npm start

# You should see:
# DropSpot API Backend Server running on port 5000
```

**IMPORTANT:** Keep this terminal open while developing!

---

### Step 2: Test API Connectivity from Device/Emulator

**For Android Emulator (use 10.0.2.2):**
```
http://10.0.2.2:5000/api/
```

**For Physical Device (use your computer IP):**
```
http://192.168.38.40:5000/api/
```

⚠️ **NOTE:** Make sure you're using `:5000` port in the URL!

---

### Step 3: Verify Network Connectivity
```powershell
# 1. Find your actual computer IP
ipconfig

# 2. Check if the device can ping your computer
# (If ICMP is blocked, ping may fail but HTTP should work)

# 3. Verify the backend is accessible
curl http://192.168.38.40:5000
# Should return: {"success":true,"message":"DropSpot API Server is running"}

# 4. Test with health endpoint
curl http://192.168.38.40:5000/health
# Should return: {"success":true,"message":"API is healthy"}
```

---

### Step 4: Check Android Manifest Permissions

Verify you have **Internet permission** in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.NETWORK_STATE" />
```

---

### Step 5: Fix ApiClient.java

**Current Issue:** The URL might be missing the port number!

**CORRECT URLS:**

```java
// For Physical Device:
private static final String BASE_URL = "http://192.168.38.40:5000/api/";

// For Emulator (if using emulator instead of device):
private static final String BASE_URL = "http://10.0.2.2:5000/api/";
```

---

## 🔴 COMMON ISSUES & FIXES

### Issue 1: "Connection Refused" or "Cannot reach server"
**Cause:** Backend is not running or IP is wrong
**Fix:**
1. Ensure `npm start` is running in backend folder
2. Verify IP using `ipconfig` command
3. Check port 5000 is not blocked by firewall

### Issue 2: URL shows correct IP but no API calls
**Cause:** Port 5000 is missing from URL
**Fix:**
```java
// ❌ WRONG
private static final String BASE_URL = "http://192.168.38.40/api/";

// ✅ CORRECT
private static final String BASE_URL = "http://192.168.38.40:5000/api/";
```

### Issue 3: "No FCM token found" errors
**Cause:** Backend is not receiving FCM token from app
**Fix:**
1. Ensure `FirebaseMessagingService` is working
2. Check FCM token is saved to Firebase Firestore in users collection
3. Restart app after Firebase setup

### Issue 4: CORS errors
**Cause:** Backend CORS config doesn't include device IP
**Fix:**
Update `backend/index.js`:
```javascript
const corsOptions = {
  origin: [
    'http://localhost:3000',
    'http://localhost:5000',
    'http://localhost:8080',
    'http://192.168.38.40:*',  // Add this
    process.env.FRONTEND_URL,
  ].filter(Boolean),
  // ... rest
};
```

### Issue 5: AndroidStudio shows red errors but API works
**Cause:** Project hasn't been built/synced after URL change
**Fix:**
1. File → Sync Now
2. Build → Clean Project
3. Build → Rebuild Project
4. Run App again

---

## 📋 VERIFICATION CHECKLIST

- [ ] Backend is running (`npm start` in terminal)
- [ ] ApiClient.java has correct IP:PORT (e.g., `http://192.168.38.40:5000/api/`)
- [ ] AndroidManifest.xml has `<uses-permission android:name="android.permission.INTERNET" />`
- [ ] Device/Emulator can reach backend (test with curl)
- [ ] Project is synced and rebuilt (File → Sync Now)
- [ ] Firebase authentication is working
- [ ] App logs show "Firebase Token attached to request"
- [ ] No firewall blocking port 5000

---

## 📱 Testing API Calls

### Method 1: Using Android Logcat
1. Open Android Studio → View → Tool Windows → Logcat
2. Search for "ApiClient" tag
3. Look for: "Firebase Token attached to request" ✅
4. Look for API response logs ✅

### Method 2: Using REST Client (VSCode)
Create file: `backend/API_REQUESTS.rest`
```
### Test health endpoint
GET http://192.168.38.40:5000/health

### Get posts (requires auth token)
GET http://192.168.38.40:5000/api/posts
Authorization: Bearer YOUR_FIREBASE_TOKEN
```

### Method 3: Using curl
```powershell
# Test basic connectivity
curl http://192.168.38.40:5000

# Test with auth header
curl -H "Authorization: Bearer YOUR_TOKEN" http://192.168.38.40:5000/api/posts
```

---

## 🚀 NEXT STEPS AFTER FIX

1. **Rebuild app** with updated URL
2. **Clear app data** on device (optional but recommended)
3. **Reinstall** app on device
4. **Check Logcat** for confirmation messages
5. **Test one API call** (e.g., fetching posts)
6. **Monitor backend logs** for incoming requests

---

## 💡 QUICK REFERENCE

| Issue | Check |
|-------|-------|
| API not called | Is `npm start` running? |
| Connection refused | Correct IP:PORT in BaseUrl? |
| 404 errors | Is endpoint URL correct? |
| 401 errors | Is Firebase token being attached? |
| CORS error | Is IP in CORS whitelist? |
| No response | Is backend listening? |

---

## 📞 DEBUGGING COMMANDS

```powershell
# Kill any process on port 5000
netstat -ano | findstr "5000"
taskkill /PID <PID_NUMBER> /F

# Reinstall dependencies
cd backend
rm -r node_modules
npm install
npm start

# Check backend logs
npm start  # Output should show all requests
```

---

**Make sure the backend is running before testing the app!**

