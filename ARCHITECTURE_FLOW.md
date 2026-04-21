# 🔄 API COMMUNICATION FLOW - VISUAL GUIDE

## ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────┐
│                      ANDROID APP (Your Phone)                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  MainActivity / PostDetailActivity / etc.                    │   │
│  │                                                              │   │
│  │  → Calls: ApiService.createPost(post)                       │   │
│  │  → Calls: ApiService.getPosts()                             │   │
│  │  → Calls: ApiService.sendNotification()                     │   │
│  └─────────────────────┬───────────────────────────────────────┘   │
│                        │                                             │
│  ┌─────────────────────┼───────────────────────────────────────┐   │
│  │           ApiClient.java                                     │   │
│  │  ┌──────────────────────────────────────────────────────┐   │   │
│  │  │  BASE_URL = "http://192.168.38.40:5000/api/"         │   │   │
│  │  │             ^^^^^^^^^^^^^^^^^^^^^^   ^^^^             │   │   │
│  │  │             Your Computer IP      Port 5000           │   │   │
│  │  │                                                      │   │   │
│  │  │  Creates Retrofit client with:                        │   │   │
│  │  │  • HttpLoggingInterceptor (logs requests)            │   │   │
│  │  │  • Firebase Token Interceptor (auth)                 │   │   │
│  │  │  • CORS headers                                      │   │   │
│  │  └──────────────────────────────────────────────────────┘   │   │
│  └─────────────────────┬───────────────────────────────────────┘   │
│                        │                                             │
│      ┌─────────────────┴──────────────────┐                        │
│      │ RETROFIT HTTP REQUEST              │                        │
│      │ • Headers: Authorization, Content  │                        │
│      │ • Timeout: 15 seconds              │                        │
│      └─────────────────┬──────────────────┘                        │
│                        │                                             │
└────────────────────────┼─────────────────────────────────────────────┘
                         │
                    📡 NETWORK 📡
                    (WiFi / Mobile Data)
                         │
┌────────────────────────┼─────────────────────────────────────────────┐
│                        │                                              │
│  ┌─────────────────────┼──────────────────────────────────────────┐  │
│  │  BACKEND SERVER (Your Computer)                              │  │
│  │  Running on: Port 5000                                        │  │
│  │  IP: 192.168.38.40                                            │  │
│  │                                                               │  │
│  │  ┌─────────────────────────────────────────────────────────┐ │  │
│  │  │ Express.js Server (backend/index.js)                   │ │  │
│  │  │                                                         │ │  │
│  │  │ CORS Configuration:                                     │ │  │
│  │  │ ✅ 'http://10.0.2.2:*'        (Emulator)              │ │  │
│  │  │ ✅ 'http://192.168.38.40:*'   (Your Device)           │ │  │
│  │  │                                                         │ │  │
│  │  │ Routes:                                                 │ │  │
│  │  │ /api/posts         → PostsRouter                       │ │  │
│  │  │ /api/requests      → RequestsRouter                    │ │  │
│  │  │ /api/events        → EventsRouter                      │ │  │
│  │  │ /api/notifications → NotificationsRouter               │ │  │
│  │  │ /api/payments      → PaymentsRouter                    │ │  │
│  │  │ /api/users         → UsersRouter                       │ │  │
│  │  │ /api/saved         → SavedPostsRouter                  │ │  │
│  │  └─────────────────────────────────────────────────────────┘ │  │
│  │                           │                                   │  │
│  │  ┌────────────────────────┼──────────────────────────────┐   │  │
│  │  │                        │                              │   │  │
│  │  ▼                        ▼                              ▼   │  │
│  │ ┌─────────────┐  ┌─────────────────┐  ┌──────────────┐     │  │
│  │ │  Firebase   │  │ Firebase Admin  │  │ Email/Notify │     │  │
│  │ │  Firestore  │  │ SDK             │  │ Service      │     │  │
│  │ │             │  │                 │  │              │     │  │
│  │ │ Database:   │  │ Sends:          │  │ Sends alerts │     │  │
│  │ │ • posts     │  │ • FCM tokens    │  │ • FCM notif  │     │  │
│  │ │ • requests  │  │ • Cloud msgs    │  │ • Emails     │     │  │
│  │ │ • events    │  │                 │  │              │     │  │
│  │ │ • users     │  └─────────────────┘  └──────────────┘     │  │
│  │ │ • notif     │                                              │  │
│  │ └─────────────┘                                              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  Run: npm start                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## REQUEST-RESPONSE CYCLE

### Step 1: Android App Makes Request
```
App Code:
  ApiService apiService = ApiClient.getClient().create(ApiService.class);
  Call<ApiResponse<Post>> call = apiService.createPost(post);
  call.enqueue(new Callback<ApiResponse<Post>>() {
    @Override
    public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
      // Handle success
    }
  });
```

### Step 2: ApiClient Prepares Request
```
1. Create HTTP request with URL:
   POST http://192.168.38.40:5000/api/posts
   
2. Add headers:
   Authorization: Bearer <FIREBASE_TOKEN>
   Content-Type: application/json
   
3. Add body:
   {
     "title": "Old Phone",
     "description": "...",
     ...
   }
```

### Step 3: Send Over Network
```
Request travels over WiFi/Mobile Data
from Phone (192.168.x.x) → to Computer (192.168.38.40:5000)
```

### Step 4: Backend Receives Request
```
Express.js catches POST /api/posts
1. CORS middleware checks if origin is allowed ✅
2. Middleware checks Authentication token ✅
3. PostsRouter handles the request
4. Firebase Firestore saves the post ✅
```

### Step 5: Backend Sends Response
```
Response (JSON):
{
  "success": true,
  "data": {
    "id": "post123",
    "title": "Old Phone",
    ...
  },
  "message": "Post created successfully"
}

HTTP Status: 200 OK
```

### Step 6: Android App Receives Response
```
Retrofit intercepts response
Parses JSON into Post object
Calls onResponse() callback
App updates UI with new post ✅
```

---

## DATA FLOW FOR KEY FEATURES

### 📝 Creating a Post
```
User clicks "Create Post"
    ↓
User fills form (title, description, etc.)
    ↓
User clicks "Upload"
    ↓
App calls: ApiService.createPost(post)
    ↓
Request: POST http://192.168.38.40:5000/api/posts
    ↓
Backend: Saves to Firebase Firestore
    ↓
Response: { success: true, data: post }
    ↓
App: Shows "Post created!" toast
    ↓
App: Refreshes post list
```

### 🔔 Receiving Notifications
```
Firebase Admin SDK (Backend)
    ↓
Queries Firestore for FCM tokens
    ↓
Sends FCM message to device
    ↓
Android receives via FirebaseMessagingService
    ↓
Shows notification in system tray
    ↓
User taps notification
    ↓
App opens relevant screen
```

### 💳 Payment Flow
```
User clicks "Proceed to Payment"
    ↓
App opens PaymentActivity
    ↓
User enters dummy card details
    ↓
User clicks "Pay Now"
    ↓
Simulated payment (2 sec loading)
    ↓
Random: 80% Success, 20% Failure
    ↓
If Success:
  → Request status: "ACCEPTED"
  → Payment saved: POST /api/payments
  → Owner notified (FCM)
  → Buyer notified (FCM)
  → Navigate to "My Requests"
```

---

## IMPORTANT CONNECTION POINTS

### ✅ Correct Configuration
```
Device IP:  192.168.38.40
Port:       5000
API Base:   http://192.168.38.40:5000/api/
Status:     ✅ Connected
```

### ❌ Common Issues & Fixes
```
Issue: Connection Refused
  → Check: Is "npm start" running?
  → Fix: Start backend with start-backend.bat

Issue: CORS Error
  → Check: Is device IP in CORS whitelist?
  → Fix: Backend already updated

Issue: 401 Unauthorized
  → Check: Is Firebase token attached?
  → Fix: Check Logcat for "Firebase Token attached"

Issue: Timeout Error
  → Check: Is network reachable?
  → Fix: Run test-api-connectivity.bat
```

---

## VERIFICATION COMMANDS

### Check Backend Running
```powershell
netstat -ano | findstr "5000"
# Should show: Node.js process listening on 5000
```

### Check Connectivity
```powershell
curl http://192.168.38.40:5000
# Should return: {"success":true,"message":"DropSpot API Server is running"}
```

### Check Logs
```
Android Logcat: View → Tool Windows → Logcat
Filter: "ApiClient" or "Retrofit"
Look for: "Firebase Token attached to request"

Backend Console: Check npm start terminal
Look for: "[TIMESTAMP] POST /api/posts"
```

---

## SUMMARY

| Component | Status | Port |
|-----------|--------|------|
| Android App | 📱 Running | Device-specific |
| Backend Server | 🖥️ Running | 5000 |
| Firebase Firestore | ☁️ Cloud | Remote |
| FCM Notifications | 📨 Cloud | Remote |
| Network | 📡 WiFi | Connected |

**ALL SYSTEMS: ✅ OPERATIONAL** 🚀

---

*This diagram helps understand the complete flow of how Android app communicates with backend server.*

