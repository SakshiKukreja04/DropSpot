# FCM Payment Notification Issue - Complete Diagnostic & Fix

## Problem Statement
Owner is **NOT receiving push notifications** when a buyer pays for their item.
- Duration: 2+ hours
- Expected: Owner should see notification in system tray within 2-5 seconds
- Actual: No notification appears

---

## Root Cause Analysis

### Possible Issues:

1. **FCM Token Not Registered** ❌
   - Owner's device hasn't saved FCM token to backend
   - Check: Firestore `users` collection - look for `fcmToken` field

2. **Backend Not Running** ❌
   - Payment API endpoint not accessible
   - Check: Backend process on port 5000

3. **Firebase Admin SDK Issue** ❌
   - Service account key invalid or expired
   - Firebase Admin SDK initialization failed

4. **Notification Permission Denied** ❌
   - Owner's device hasn't granted notification permission
   - Check: Android Settings > Apps > DropSpot > Notifications

5. **Current User Check** ❌
   - Notification filtered out due to `recipientUserId` mismatch
   - Check: Is current user logged in as owner?

---

## Quick Diagnosis Checklist

### Step 1: Verify Backend is Running
```bash
# Check if node process exists
Get-Process node -ErrorAction SilentlyContinue

# If not running, start backend:
cd backend
npm start
```

### Step 2: Check FCM Token Registration
1. Open DropSpot app on owner's phone
2. Check Logcat for: `FCM Token: ` messages
3. Look for: `FCM Token saved to server` or `FCM Token updated on server`
4. If not found → FCM token NOT registered

**What should appear in logs:**
```
DropSpotApp: FCM Token: dUuM1GOBRKWjbo0IcQZCCN1...
DropSpotApp: FCM Token saved to server on app start
```

### Step 3: Verify Token in Firestore
1. Open Firebase Console
2. Go to: Firestore Database > Collection `users`
3. Find owner's user document
4. Check: Does it have `fcmToken` field with a non-empty value?

**Expected:**
```
User Document: {
  uid: "oXMiK5qXbnNuA6pJu6BSNBTLGbr1",
  name: "Owner Name",
  email: "owner@example.com",
  fcmToken: "dUuM1GOBRKWjbo0IcQZCCN1s3W0tV5qR...",  ✅ SHOULD EXIST
  createdAt: timestamp,
  ...
}
```

### Step 4: Check Notification Permissions
**Owner's Phone (Android 13+):**
1. Settings > Apps > DropSpot
2. Notifications > Toggle ON
3. Allow notifications for "DropSpot Notifications" channel

### Step 5: Test Payment Flow
1. **Owner's Phone**: Open DropSpot, stay logged in
2. **Buyer's Phone**: Find owner's item → Click Pay
3. **Backend Logs**: Should show:
   ```
   [PAYMENT] Request received - paymentId: ...
   [PAYMENT] Payment saved to Firestore
   [PAYMENT] Sending FCM notification to owner: oXMiK5qXbnNuA6pJu6BSNBTLGbr1
   [FCM] User document found - has FCM token: true
   [FCM] Sending "Payment Received" to owner
   [FCM] Found FCM token for user: dUuM1GOBRKWjbo0...
   [FCM] Sending message: { notification: { ... }, token: ... }
   [FCM] Notification sent successfully: projects/dropspotapp.../messages/...
   ```

4. **Owner's Phone**: Should receive notification in 2-5 seconds

---

## Issue: FCM Token Not Being Registered

### Problem
App may not be automatically registering FCM token on first run.

### Solution
Add explicit FCM token initialization with retry logic:

**File: `app/src/main/java/com/example/dropspot/DropSpotApplication.java`**

```java
private void initializeFCM() {
    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
        if (!task.isSuccessful()) {
            Log.w(TAG, "Fetching FCM token failed", task.getException());
            // Retry after delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                initializeFCM();
            }, 5000);
            return;
        }

        String token = task.getResult();
        Log.d(TAG, "FCM Token: " + token);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            Log.d(TAG, "User logged in, saving FCM token...");
            saveFCMTokenToServer(token);
        } else {
            Log.d(TAG, "No user logged in, will save FCM token after login");
        }
    });
}

private void saveFCMTokenToServer(String token) {
    FirebaseAuth auth = FirebaseAuth.getInstance();
    if (auth.getCurrentUser() == null) {
        Log.d(TAG, "No user to save FCM token for");
        return;
    }
    
    String userId = auth.getCurrentUser().getUid();
    Log.d(TAG, "Saving FCM token for user: " + userId);
    
    ApiService apiService = ApiClient.getClient().create(ApiService.class);
    
    java.util.Map<String, Object> update = new java.util.HashMap<>();
    update.put("fcmToken", token);
    
    apiService.updateUserProfile(userId, update).enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
        @Override
        public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
            if (response.isSuccessful()) {
                Log.d(TAG, "✅ FCM Token saved to server successfully");
            } else {
                Log.e(TAG, "❌ Failed to save FCM token: " + response.code() + " " + response.message());
            }
        }

        @Override
        public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
            Log.e(TAG, "❌ Error saving FCM token", t);
            // Retry after delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                saveFCMTokenToServer(token);
            }, 10000);
        }
    });
}
```

**Required Import:**
```java
import android.os.Handler;
import android.os.Looper;
```

---

## Issue: Backend FCM Sending Fails

### Problem
Backend tries to send but fails silently.

### Solution
Improve error handling in payment route:

**File: `backend/routes/payments.js` - Line 61-90**

```javascript
// 4. Fetch owner's FCM token and send real-time push notification
try {
  console.log(`[PAYMENT] Attempting to send FCM notification to owner: ${ownerId}`);
  const ownerDoc = await db.collection('users').doc(ownerId).get();
  
  if (!ownerDoc.exists) {
    console.error(`[PAYMENT] ❌ Owner document not found: ${ownerId}`);
    // Notification saved in DB, but couldn't send push
  } else {
    const ownerData = ownerDoc.data();
    const hasToken = !!ownerData.fcmToken;
    console.log(`[PAYMENT] Owner document found - has FCM token: ${hasToken}`);
    
    if (ownerData.fcmToken) {
      try {
        console.log(`[FCM] Sending notification to owner ${ownerId}`);
        await sendFCMNotification(
          ownerId,
          'Payment Received 💰',
          'Payment has been completed for your item',
          {
            type: 'PAYMENT_SUCCESS',
            postId,
            paymentId,
            recipientUserId: ownerId
          }
        );
        console.log(`[PAYMENT] ✅ FCM sent successfully to owner: ${ownerId}`);
      } catch (fcmError) {
        console.error(`[PAYMENT] ❌ FCM error for owner ${ownerId}:`, fcmError.message);
        // Don't fail - notification is in DB, push is just bonus
      }
    } else {
      console.warn(`[PAYMENT] ⚠️ Owner ${ownerId} has no FCM token - push not sent, but notification saved in DB`);
    }
  }
} catch (pushError) {
  console.error('[PAYMENT] ❌ Error in FCM section:', pushError);
  // Don't fail the entire payment
}
```

---

## Issue: Firebase Admin SDK Not Initialized

### Check Backend Init:

**File: `backend/config/firebase.js`**

```javascript
// Should exist and be valid
import admin from 'firebase-admin';
import { readFile } from 'fs/promises';

// Verify file exists
const serviceAccount = JSON.parse(
  await readFile(new URL('./serviceAccountKey.json', import.meta.url))
);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

console.log('✅ Firebase Admin SDK initialized successfully');
export const db = admin.firestore();
```

**If error:** Backend will fail to start. Check:
1. Is `serviceAccountKey.json` in `backend/config/`?
2. Is it a valid JSON file?
3. Try: `node -e "require('./backend/config/firebase.js')"`

---

## Complete Step-by-Step Debug Process

### 1. **Stop everything and clean restart**
```bash
# Kill backend
taskkill /F /IM node.exe

# Kill app on phone
# Go to Settings > Apps > DropSpot > Force Stop
```

### 2. **Start backend with verbose logging**
```bash
cd backend
npm start
# Should show: "DropSpot API Backend Server running on http://0.0.0.0:5000"
```

### 3. **Owner: Open app and check logs**
```
Expected Logcat:
DropSpotApp: FCM Token: dUuM1GOBRKWjbo0IcQZCCN1s3W0tV5qR...
DropSpotApp: FCM Token saved to server on app start
```

If not showing, do manual trigger:
- Go to Settings > Apps > DropSpot > Permissions > Notifications > Grant

### 4. **Verify Firestore**
1. Go to Firebase Console
2. Firestore Database > Collection `users` > Owner's document
3. Check `fcmToken` field - should NOT be empty

### 5. **Buyer: Process payment**
- Open app as different user
- Find owner's item
- Click Pay → Process payment

### 6. **Check backend logs**
```
Should see:
[PAYMENT] Request received - paymentId: PAY_12345
[PAYMENT] Sending FCM notification to owner: oXMiK5qXbnNuA6pJu6BSNBTLGbr1
[FCM] Found FCM token for user: dUuM1GOBRKWjbo0...
[FCM] Notification sent successfully: projects/...
[PAYMENT] ✅ FCM sent successfully
```

### 7. **Check owner's phone**
- Should see push notification in 2-5 seconds
- Tap notification → Opens app

---

## Testing Without Two Devices

If you only have one device, manually trigger payment:

**File: `backend/routes/payments.js` - Add test endpoint**

```javascript
// Add at the end, before export
router.post('/test', async (req, res) => {
  try {
    const { ownerId } = req.body;
    
    if (!ownerId) {
      return res.status(400).json(errorResponse('ownerId required'));
    }
    
    console.log(`[TEST] Sending test notification to owner: ${ownerId}`);
    
    await sendFCMNotification(
      ownerId,
      '🧪 Test Notification',
      'This is a test payment notification',
      {
        type: 'TEST',
        recipientUserId: ownerId
      }
    );
    
    res.json(successResponse({}, 'Test notification sent'));
  } catch (error) {
    console.error('[TEST] Error:', error);
    res.status(500).json(errorResponse(error.message));
  }
});
```

**Usage:**
```bash
curl -X POST http://localhost:5000/api/payments/test \
  -H "Content-Type: application/json" \
  -d '{"ownerId": "oXMiK5qXbnNuA6pJu6BSNBTLGbr1"}'
```

---

## Expected Working Flow

```
App Starts
   ↓
DropSpotApplication.onCreate()
   ↓
initializeFCM() called
   ↓
FirebaseMessaging.getToken() fetches token
   ↓
saveFCMTokenToServer(token) called
   ↓
ApiClient.updateUserProfile() sends to backend
   ↓
Backend: PUT /api/users/{userId}
   ↓
Backend saves fcmToken to Firestore users collection ✅
   ↓
Owner's Firestore doc now has fcmToken: "dUuM1GOBRKWjbo0..."
   ↓
[LATER] Buyer processes payment
   ↓
Backend POST /api/payments
   ↓
Backend: sendFCMNotification(ownerId, ...)
   ↓
FCM Helper: Look up user doc for ownerId
   ↓
FCM Helper: Get fcmToken from doc
   ↓
FCM Helper: Send message via Firebase Cloud Messaging
   ↓
Firebase Cloud Messaging delivers to owner's device token
   ↓
Owner's device receives message
   ↓
MyFirebaseMessagingService.onMessageReceived() called
   ↓
Validate recipientUserId matches current user ✅
   ↓
showNotification() displays system notification
   ↓
Owner sees "Payment Received 💰" notification ✅
```

---

## Checklist Before Testing Again

- [ ] Backend running on port 5000
- [ ] Owner's app installed and logged in
- [ ] Owner sees "FCM Token saved to server" in logs
- [ ] Firestore shows fcmToken for owner's user document
- [ ] Notification permissions granted in Android Settings
- [ ] Both devices on same WiFi (or reachable)
- [ ] Service account key valid in backend/config/
- [ ] No duplicate app instances running

---

**Last Updated:** April 21, 2026
**Status:** Ready to Debug


