# 🔍 Backend-Frontend Connection Diagnostic

**Current Status**: Checking connection issues  
**IP Address**: 192.168.29.133  
**Port**: 5000  
**Date**: April 21, 2026

---

## ⚠️ Possible Connection Issues

### 1. **Firewall Blocking Port 5000**
Your Windows Firewall might be blocking the connection.

**Fix:**
```powershell
# Allow Node.js through firewall
netsh advfirewall firewall add rule name="Node.js Port 5000" dir=in action=allow program="C:\Program Files\nodejs\node.exe" localport=5000 protocol=tcp

# OR manually:
# Settings > Privacy & Security > Windows Defender Firewall > Allow an app through firewall
# Find "Node.js" or add "C:\Program Files\nodejs\node.exe"
```

### 2. **Backend NOT Listening on All Interfaces**
Check if backend is actually running and listening.

**Current Setup (CORRECT ✓):**
```javascript
app.listen(PORT, '0.0.0.0', () => { ... })
```

**Verify it's listening:**
```powershell
netstat -ano | findstr :5000
```

Should show:
```
TCP    0.0.0.0:5000           0.0.0.0:0              LISTENING       [PID]
```

---

## 🔧 CORS Configuration

**Current Backend CORS (backend/index.js line 30-44):**
```javascript
const corsOptions = {
  origin: [
    'http://localhost:3000',
    'http://localhost:4000',
    'http://localhost:5000',
    'http://localhost:8080',
    'http://10.0.2.2:*',           // Emulator
    'http://192.168.29.133:*',     // ✓ Correct
    'http://192.168.38.40:*',      // Old device
    process.env.FRONTEND_URL,
  ].filter(Boolean),
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
};
```

**Status**: ✅ CORS is correctly configured for 192.168.29.133

---

## 📱 App Configuration

**Current ApiClient (ApiClient.java):**
```java
private static final String BASE_URL = "http://192.168.29.133:5000/api/";
```

**Status**: ✅ Correct IP and port

---

## 🚀 Network Connectivity Checks

### Check 1: Can Device Reach Backend?

**On Device/Emulator Terminal:**
```bash
# Android Emulator
adb shell ping 192.168.29.133

# Physical Device (terminal app)
ping 192.168.29.133
```

**Expected**: Successful pings (no timeouts)

### Check 2: Is Backend Actually Running?

**On Your PC:**
```powershell
# Check if process is listening
netstat -ano | findstr :5000

# Or check processes
Get-Process node
```

**Expected**: 
```
node.exe process running with PID
TCP port 5000 in LISTENING state
```

### Check 3: Backend Responding?

**From Your PC Terminal:**
```powershell
curl http://192.168.29.133:5000/health
```

**Expected Response:**
```json
{"success":true,"message":"API is healthy"}
```

---

## 🎯 Most Common Issues

### Issue 1: Windows Firewall Blocking
**Symptom**: Connection timeout when app tries to reach backend  
**Solution**: 
- Open Windows Defender Firewall
- Allow Node.js inbound connections on port 5000
- OR disable firewall temporarily for testing

### Issue 2: Wrong IP Address
**Symptom**: App logs show connection refused  
**Solution**:
- Your actual IP: **192.168.29.133** ✓ (already correct in ApiClient)
- Verify device is on SAME WiFi network
- Check: `ipconfig` on PC, compare with device WiFi name

### Issue 3: Backend Not Running
**Symptom**: App logs show "Connection refused"  
**Solution**:
- Terminal should show: `DropSpot API Backend Server running on http://0.0.0.0:5000`
- If not, start: `cd backend && npm start`

### Issue 4: Firebase Admin SDK Issues
**Symptom**: Backend starts but can't send requests (auth error)  
**Check**: Backend logs for Firebase initialization errors
**Solution**:
- Verify `backend/config/serviceAccountKey.json` exists
- Verify Firebase credentials are valid

---

## 🧪 Step-by-Step Debugging

### Step 1: Verify Backend is Running
```bash
cd backend
npm start
```

**Expected Output:**
```
DropSpot API Backend Server running on http://0.0.0.0:5000
Access from device: http://192.168.29.133:5000
```

### Step 2: Test from Another Terminal
```powershell
# While backend is running, open new terminal
curl http://192.168.29.133:5000/health
```

**Expected Output:**
```
{"success":true,"message":"API is healthy"}
```

### Step 3: Check Firewall
```powershell
# Check inbound rules for port 5000
netsh advfirewall firewall show rule name="*5000*"

# If empty, add rule:
netsh advfirewall firewall add rule name="Allow Node 5000" dir=in action=allow protocol=tcp localport=5000
```

### Step 4: Device Connectivity
**On Device:**
```bash
adb shell ping -c 4 192.168.29.133
```

**Expected**: All pings successful (no 100% loss)

### Step 5: App Logs
```
Filter Logcat: "ApiClient"
Look for connection errors
```

---

## 📋 Connection Flow

```
App (Device)
   ↓
  Calls: http://192.168.29.133:5000/api/posts
   ↓
Network requests to your PC IP
   ↓
Windows Firewall (might block here ⚠️)
   ↓
Node.js Express Server (listening on 0.0.0.0:5000)
   ↓
Route handler processes request
   ↓
Response sent back to app
```

---

## ✅ Quick Verification Checklist

- [ ] Backend running (`npm start` shows "Server running on")
- [ ] Port 5000 in LISTENING state (`netstat -ano | findstr :5000`)
- [ ] Firewall allows inbound on port 5000
- [ ] ApiClient has correct IP: `192.168.29.133`
- [ ] Device/Emulator can ping PC (`adb shell ping 192.168.29.133`)
- [ ] `/health` endpoint responds (test with curl)
- [ ] No Firebase initialization errors in backend logs
- [ ] Device and PC on same WiFi network

---

## 🔧 Quick Fixes

### Fix Firewall
```powershell
# Add Node.js to firewall whitelist
netsh advfirewall firewall add rule name="Allow Node" dir=in action=allow program="C:\Program Files\nodejs\node.exe" enable=yes
```

### Restart Backend
```bash
taskkill /F /IM node.exe
cd backend
npm start
```

### Check All Listening Ports
```powershell
netstat -ano | findstr "LISTENING" | findstr "5000"
```

### Get Your IP
```powershell
ipconfig | Select-String "192.168"
```

---

**Status**: 🔍 Diagnostic Reference  
**Next**: Check firewall and verify backend is listening


