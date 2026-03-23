# DropSpot Backend - Setup Guide

## Quick Start

### 1. Prerequisites
- Node.js 18+ installed
- Firebase project created
- Service account key downloaded

### 2. Installation Steps

```bash
# Navigate to backend directory
cd backend

# Install dependencies
npm install

# Create .env file
cp .env.example .env

# Edit .env with your Firebase credentials
# Windows: notepad .env
# Mac/Linux: nano .env
```

### 3. Firebase Setup

**Get your Firebase credentials:**

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Click ⚙️ Settings → Project Settings
4. Go to "Service Accounts" tab
5. Click "Generate New Private Key"
6. Copy these values to `.env`:
   - `FIREBASE_PROJECT_ID` → project_id
   - `FIREBASE_PRIVATE_KEY` → private_key (keep quotes, replace \n with actual newlines)
   - `FIREBASE_CLIENT_EMAIL` → client_email
   - `FIREBASE_DATABASE_URL` → databaseURL

### 4. Start Backend

```bash
# Development (with hot reload)
npm run dev

# Production
npm start
```

You should see:
```
═══════════════════════════════════════════════════════════
  DropSpot API Backend Server
═══════════════════════════════════════════════════════════
  Environment: development
  Port: 5000
  Started: 2024-01-15T10:00:00.000Z
```

### 5. Test the API

```bash
# Test health endpoint (no auth required)
curl http://localhost:5000/health

# Response:
{
  "success": true,
  "message": "API is healthy"
}
```

---

## Firebase Authentication Setup

### Enable Authentication in Firebase Console

1. **Firebase Console** → Your Project
2. **Build** section → **Authentication**
3. Click **Get Started**
4. **Sign-in method** → **Google** → Enable
5. **Settings** → **Authorized domains**
6. Add your Android app package name (com.example.dropspot)

### Get Firebase ID Token from Android App

The Android app gets the token via Firebase Auth:

```java
Task<GetTokenResult> task = FirebaseAuth.getInstance().getCurrentUser().getIdToken(true);
task.addOnSuccessListener(result -> {
    String token = result.getToken();
    // Use this token in Authorization header
});
```

Then in API calls from Android:
```java
OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder()
    .url("http://your-api-url/api/posts")
    .addHeader("Authorization", "Bearer " + token)
    .build();
```

---

## Firestore Database Setup

### Create Collections

The backend automatically creates these collections when first accessed:

1. **users** - User profiles
2. **posts** - Item posts
3. **requests** - Item requests
4. **savedPosts** - Saved posts
5. **events** - Community events
6. **notifications** - User notifications

### Create Firestore Indexes (Optional but Recommended)

For better query performance, create these indexes in Firebase Console:

**Collection: requests**
- Fields: `postOwnerId` (Ascending), `createdAt` (Descending)
- Fields: `requesterId` (Ascending), `createdAt` (Descending)
- Fields: `postId` (Ascending), `status` (Ascending)

**Collection: posts**
- Fields: `category` (Ascending), `postedAt` (Descending)
- Fields: `userId` (Ascending), `isActive` (Ascending)

**Collection: events**
- Fields: `startDate` (Ascending), `isActive` (Ascending)
- Fields: `category` (Ascending), `startDate` (Ascending)

---

## Environment Variables Reference

```env
# Server Configuration
PORT=5000                                          # HTTP port
NODE_ENV=development                               # development or production

# Firebase Configuration
FIREBASE_PROJECT_ID=your-project-id               # From Firebase console
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n..."  # From downloaded JSON
FIREBASE_CLIENT_EMAIL=service-account@project.iam.gserviceaccount.com
FIREBASE_DATABASE_URL=https://your-project.firebaseio.com

# Frontend Configuration (CORS)
FRONTEND_URL=http://localhost:3000               # Frontend URL for CORS
```

---

## Directory Structure Explained

```
backend/
│
├── config/
│   └── firebase.js                    # Firebase Admin SDK setup
│                                       # Initializes Firestore and Auth
│
├── middleware/
│   └── auth.js                        # Authentication & error handling
│                                       # verifyToken middleware
│                                       # errorHandler middleware
│
├── routes/
│   ├── posts.js                       # GET, POST, PUT, DELETE posts
│   ├── requests.js                    # Request/offer management
│   ├── savedPosts.js                  # Save/unsave posts
│   ├── events.js                      # Event management
│   ├── notifications.js               # Notification management
│   └── users.js                       # User profiles & ratings
│
├── utils/
│   └── helpers.js                     # Utilities & validations
│                                       # generateId()
│                                       # validatePostData()
│                                       # calculateDistance()
│
├── index.js                           # Main Express app
│                                       # Server setup & routes
│                                       # Middleware setup
│
├── package.json                       # Dependencies
├── .env.example                       # Environment template
├── .env                               # Your config (create this)
├── .gitignore                         # Git ignore rules
├── README.md                          # Full documentation
├── SETUP.md                           # This file
└── API_REQUESTS.rest                 # Sample API requests
```

---

## Common Issues & Solutions

### Issue: "FIREBASE_PRIVATE_KEY is undefined"

**Solution:**
- Check `.env` file exists and is in backend directory
- Verify `FIREBASE_PRIVATE_KEY` is properly formatted
- Use quotes around the key: `FIREBASE_PRIVATE_KEY="-----BEGIN...-----"`
- Newlines should be `\n` in the string

### Issue: "Cannot connect to Firestore"

**Solution:**
- Verify Firebase project is active
- Check `FIREBASE_PROJECT_ID` matches your project
- Ensure Firestore database is created (not Realtime DB)
- Check network connectivity

### Issue: "401 Unauthorized" on API requests

**Solution:**
- Verify you're sending the Authorization header
- Format: `Authorization: Bearer <token>`
- Check token hasn't expired (valid for 1 hour)
- Regenerate token from Android app if needed

### Issue: CORS errors from Android app

**Solution:**
- CORS is not relevant for mobile apps
- Ensure API is properly exposed and reachable
- Check firewall/network settings
- Verify backend is running and accessible

### Issue: Port already in use

**Solution:**
```bash
# Change port in .env
PORT=5001

# Or kill existing process using port 5000
# Windows: netstat -ano | findstr :5000
# Mac/Linux: lsof -i :5000
```

---

## Monitoring & Debugging

### Enable Debug Logging

The backend logs all requests. Check console output:

```
[2024-01-15T10:00:00.000Z] POST /api/posts
[2024-01-15T10:00:01.234Z] GET /api/posts
```

### Check Firestore Data

1. Firebase Console → Firestore Database
2. Browse collections and documents
3. Verify data structure matches API responses

### Test Individual Endpoints

Use the provided `API_REQUESTS.rest` file:

1. Install VS Code REST Client extension
2. Open `API_REQUESTS.rest`
3. Set `@token` variable
4. Click "Send Request" on any endpoint

---

## Production Deployment

### Deploy to Firebase Cloud Functions

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase project
firebase init functions

# Copy backend code to functions/
# Update functions/index.js to export Express app

# Deploy
firebase deploy --only functions
```

### Deploy to Heroku

```bash
# Create Heroku app
heroku create dropspot-api

# Set environment variables
heroku config:set FIREBASE_PROJECT_ID=xxx
heroku config:set FIREBASE_PRIVATE_KEY=xxx

# Deploy
git push heroku main
```

### Deploy to AWS Lambda

Use serverless framework:
```bash
npm install -g serverless
serverless create --template aws-nodejs
# Configure and deploy
```

---

## API Rate Limiting (Optional)

Add rate limiting middleware:

```javascript
import rateLimit from 'express-rate-limit';

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});

app.use('/api/', limiter);
```

---

## Performance Tips

1. **Index Firestore queries** - Create indexes for frequently filtered queries
2. **Paginate results** - Use limit and offset
3. **Cache responses** - Implement caching for user profiles
4. **Use batch operations** - For multiple updates
5. **Monitor function execution** - Check Firebase logs

---

## Testing

### Manual Testing with cURL

```bash
# Health check
curl http://localhost:5000/health

# Get posts (with token)
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:5000/api/posts

# Create post
curl -X POST http://localhost:5000/api/posts \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"title":"Test","description":"Test post"}'
```

### Unit Testing

Create test files:
```bash
mkdir tests
touch tests/posts.test.js
```

---

## Support & Resources

- [Firebase Docs](https://firebase.google.com/docs)
- [Express.js Guide](https://expressjs.com/en/starter/basic-routing.html)
- [Firestore Rest API](https://firebase.google.com/docs/firestore/use-rest-api)
