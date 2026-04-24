# 🔍 API CALLS NOT WORKING - DIAGNOSTIC GUIDE

## Problem Analysis

Posts are not fetching even though:
- Backend is running on port 5000
- IP address was changed and updated

---

## 🔴 MOST LIKELY ISSUES

### Issue 1: Firebase Token Not Attached (MOST COMMON)
**Symptom:**
- Posts are not loading
- Logcat shows no "Firebase Token attached" message
- Backend shows 401 Unauthorized errors

**Solution:**
```
1. Check Logcat for: "Firebase Token attached to request"
   If NOT present → Token not being attached
   
2. Ensure Firebase is initialized:
   - google-services.json exists in app/src/
   - Firebase Authentication is working
   
3. Clear app data:
   Settings → Apps → DropSpot → Storage → Clear
   
4. Uninstall and reinstall app
   
5. Wait 3-5 seconds for Firebase to initialize after app opens
```

### Issue 2: No Posts in Firebase Database
**Symptom:**
- App runs without errors
- But no data appears
- Backend returns empty list

**Solution:**
```
1. Create a test post using the app:
   - Click + button
   - Fill post form
   - Click upload
   
2. Or add test data to Firebase Firestore:
   Collection: posts
   Add a document with:
   - id: "test123"
   - userId: "your_user_id"
   - title: "Test Post"
   - description: "Test description"
   - category: "Electronics"
   - condition: "New"
   - price: 100
   - latitude: 19.0760
   - longitude: 72.8777
   - isActive: true
   - createdAt: current timestamp
```

### Issue 3: Authentication Token Expired
**Symptom:**
- Was working before, now fails
- Logcat shows "Token expired"

**Solution:**
```
1. Restart the app
2. If still failing, sign out and sign back in
3. Wait for Firebase token refresh
```

### Issue 4: Query Not Matching Data
**Symptom:**
- Some posts visible, others not
- Backend filters out posts

**Reason:**
Backend filters by:
- isActive == true (only active posts shown)
- Not by current user (if myPostsOnly=false)
- Location distance (within 50km default)

**Solution:**
Ensure your test data has:
```
isActive: true
latitude: valid number (19-20 for Mumbai)
longitude: valid number (72-73 for Mumbai)
```

---

## 🔧 STEP-BY-STEP DEBUG PROCESS

### Step 1: Check Logcat Output
```
1. Android Studio → View → Tool Windows → Logcat
2. Clear previous logs (click trash icon)
3. Restart the app
4. Look for these messages:

✅ GOOD SIGNS:
   "Firebase Token attached to request"
   "D/Retrofit: POST http://192.168.38.40:5000/api/posts"
   "D/Retrofit: Response Code: 200"
   "Load posts successful"

❌ BAD SIGNS:
   "Error getting Firebase Token"
   "Connection refused"
   "HTTP 401 Unauthorized"
   "HTTP 404 Not Found"
```

### Step 2: Check Backend Logs
```
1. Look at the terminal where "npm start" is running
2. You should see:
   [TIMESTAMP] GET /api/posts 200 50ms
   
3. If you don't see requests:
   - App is not making API calls
   - Backend not receiving requests
   
4. If you see 401 errors:
   - Token not attached or invalid
   - Check Firebase token generation
```

### Step 3: Test Backend Directly
```
Use browser to test backend health:
http://192.168.38.40:5000

Expected response:
{"success":true,"message":"DropSpot API Server is running"}

If works → Backend is fine, issue is with app
If fails → Backend is down or IP is wrong
```

### Step 4: Verify Firebase Setup
```
1. In Android app, add logging:
   
   FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
   if (user != null) {
       Log.d("Firebase", "User: " + user.getUid());
   } else {
       Log.e("Firebase", "No user logged in!");
   }

2. Check Logcat for user ID
3. If "No user logged in" → Firebase auth broken
```

---

## 📋 VERIFICATION CHECKLIST

- [ ] Backend running (netstat -ano | findstr "5000")
- [ ] API URL correct (http://192.168.38.40:5000/api/)
- [ ] Firebase initialized (user logged in)
- [ ] Logcat shows "Firebase Token attached"
- [ ] Backend logs show incoming requests
- [ ] Posts exist in Firestore (or created via app)
- [ ] Posts have isActive: true
- [ ] User location is valid

---

## 🚀 QUICK FIX - TRY THIS FIRST

```
1. Kill app (close it completely)
2. Clear app data:
   Settings → Apps → DropSpot → Storage → Clear Cache & Data
3. Force stop app:
   Settings → Apps → DropSpot → Force Stop
4. Open app again
5. Wait 5 seconds for Firebase to initialize
6. Check Logcat for "Firebase Token attached"
7. Try creating a test post
8. Refresh to see if it loads
```

---

## 📝 EXPECTED BEHAVIOR

### When Working Correctly ✅
```
App opens
  ↓
Requests location permission
  ↓
Makes API call: GET /api/posts
  ↓
Backend receives request with auth token
  ↓
Backend queries Firestore for posts
  ↓
Backend returns posts JSON
  ↓
App displays posts in RecyclerView
  ↓
User sees list of posts
```

### When Not Working ❌
```
App opens
  ↓
No API call made OR
API call made but fails
  ↓
Backend doesn't receive request OR
Returns 401 Unauthorized
  ↓
App doesn't display posts
  ↓
User sees empty screen
```

---

## 🔐 FIREBASE TOKEN DEBUG

Add this code to HomeFragment.java to debug token:

```java
private void debugFirebaseToken() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user != null) {
        Log.d(TAG, "✅ User logged in: " + user.getUid());
        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = result.getToken();
            Log.d(TAG, "✅ Token obtained: " + token.substring(0, 20) + "...");
            Log.d(TAG, "Token length: " + token.length());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Failed to get token: " + e.getMessage());
        });
    } else {
        Log.e(TAG, "❌ NO USER LOGGED IN");
    }
}

// Call this in onCreateView:
debugFirebaseToken();
```

---

## 🛠️ FIX OPTIONS

### Option 1: Add Test Data to Firebase
```
Go to Firebase Console:
- Firestore Database
- Create collection: "posts"
- Add document with test post data
- Ensure isActive: true
- Restart app
```

### Option 2: Create Post via App
```
1. Click + button
2. Select "📦 Post Item"
3. Fill all fields
4. Take a photo or select from gallery
5. Click upload
6. Go back to home
7. Swipe to refresh
8. Post should appear
```

### Option 3: Check User Permissions
```
Firebase Firestore → Rules tab
Ensure rules allow authenticated users to read:

allow read, write: if request.auth != null;
```

---

## 📞 FINAL CHECKLIST

If posts still not loading after trying above:

1. ✅ Confirmed backend running?
2. ✅ Confirmed Firebase user logged in?
3. ✅ Confirmed "Firebase Token attached" in Logcat?
4. ✅ Confirmed posts exist in Firestore?
5. ✅ Confirmed firestore rules allow read?
6. ✅ Confirmed correct API URL with :5000?
7. ✅ Confirmed no 401/403 errors in Logcat?

If all checked → Problem is likely app logic or Firebase configuration

---

## 🎯 MOST COMMON FIX

**99% of the time, the issue is:**
```
Posts not loading = Firebase token not attached

FIX:
1. Make sure user is logged in
2. Check Logcat for "Firebase Token attached to request"
3. If not there, Firebase auth is broken
4. Sign out, sign in again
5. Rebuild and run app
```

Try this first! ⚡

