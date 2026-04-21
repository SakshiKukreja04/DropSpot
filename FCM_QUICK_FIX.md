# ⚡ FCM Payment Notification - QUICK FIX COMMANDS

**Run these commands to get FCM notifications working**

---

## 🔧 Step 1: Rebuild Android App

### Clean Build
```bash
cd C:\Users\saksh\AndroidStudioProjects\DropSpot
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

### Alternative - Use Android Studio
```
Menu > Build > Clean Project
Menu > Build > Rebuild Project
```

---

## 📱 Step 2: Reinstall App

### Via ADB
```bash
adb uninstall com.example.dropspot
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Via Emulator
- Drag APK to emulator window

---

## 🚀 Step 3: Start Backend

```bash
cd backend
npm start
```

**Should print:**
```
DropSpot API Backend Server running on http://0.0.0.0:5000
```

---

## ✅ Step 4: Verify FCM Token

### Check Logcat
```
In Android Studio:
- Bottom panel > Logcat
- Filter box: "DropSpotApp"
- Look for: "FCM Token saved to server"
```

### Check Firestore
```
1. https://console.firebase.google.com
2. Firestore > Collection: users
3. Find owner's document
4. Should have field: fcmToken with value
```

---

## 🧪 Step 5: Test Payment

### Manual Test (Single Device)
```bash
curl -X POST http://localhost:5000/api/payments/test-fcm \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"YOUR_USER_ID_HERE\"}"
```

### Get Your User ID
```
Open app > Profile > Look in Logcat for "Current user: "
Or in Firestore > users collection > find your document
```

---

## 🔍 Step 6: Check Logs

### Backend Logs (Windows)
```bash
# Terminal shows all logs in real-time
npm start
```

### App Logs (Logcat)
```
Filter: "DropSpotApp"   - Shows FCM token registration
Filter: "MyFirebaseMsgService" - Shows notification reception
```

---

## ⚠️ If NOT Working

### 1. Backend Not Running
```bash
# Restart backend
cd backend
npm start

# Should see: "DropSpot API Backend Server running on http://0.0.0.0:5000"
```

### 2. No FCM Token Saved
```bash
# Check app logs for errors
# Logcat filter: "DropSpotApp"

# If failing, reinstall:
adb uninstall com.example.dropspot
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 3. Firestore Missing fcmToken
```bash
# Log out and log back in
Settings > Profile > Logout
Then login again
```

### 4. Still No Notification
```bash
# Check notification permissions
Android Settings > Apps > DropSpot > Notifications > Toggle ON

# Check notification channel
Android Settings > Apps > DropSpot > Notifications > DropSpot Notifications > IMPORTANCE_HIGH
```

---

## 📊 Expected Success Logs

### App Start
```
✅ DropSpotApp: ✅ DropSpot Application created
✅ DropSpotApp: 🔄 Initializing FCM token...
✅ DropSpotApp: ✅ FCM Token obtained: dUuM1GOBRKWjbo0...
✅ DropSpotApp: 💾 Saving FCM token for user: oXMiK5qXbnNuA6pJu6BSNBTLGbr1
✅ DropSpotApp: ✅ FCM Token saved to server on app start
```

### Payment Processing
```
✅ [PAYMENT] 📢 Attempting to send FCM notification to owner: oXMiK5qXbnNuA6pJu6BSNBTLGbr1
✅ [PAYMENT] ✅ Owner document found - has FCM token: true
✅ [FCM] 🚀 Attempting to send notification to user: oXMiK5qXbnNuA6pJu6BSNBTLGbr1
✅ [FCM] ✅ Found FCM token for user: dUuM1GOBRKWjbo0...
✅ [FCM] ✅ Notification sent successfully!
✅ [PAYMENT] ✅✅ FCM notification sent successfully to owner
```

### Notification Reception
```
✅ MyFirebaseMsgService: 📬 Message received from: projects/...
✅ MyFirebaseMsgService: 👤 Current user: oXMiK5qXbnNuA6pJu6BSNBTLGbr1
✅ MyFirebaseMsgService: ✅ Notification is for current user
✅ MyFirebaseMsgService: 🎯 Creating and displaying notification
✅ MyFirebaseMsgService: ✅ Notification displayed
```

---

## 🎯 One-Liner Quick Test

```bash
# Build + test in one go
cd C:\Users\saksh\AndroidStudioProjects\DropSpot; .\gradlew.bat clean assembleDebug; adb uninstall com.example.dropspot; adb install app\build\outputs\apk\debug\app-debug.apk; cd backend; npm start
```

---

## 📝 Files Modified

- ✅ `backend/utils/fcm-helper.js`
- ✅ `backend/routes/payments.js`
- ✅ `app/src/main/java/com/example/dropspot/DropSpotApplication.java`
- ✅ `app/src/main/java/com/example/dropspot/MyFirebaseMessagingService.java`

---

**📚 Full Documentation**: See `FCM_FIXES_APPLIED.md` and `FCM_PAYMENT_NOTIFICATION_DIAGNOSTIC.md`


