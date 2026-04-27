# DropSpot - Marketplace Android App

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/Firebase-Enabled-orange.svg)](https://firebase.google.com)

A comprehensive marketplace Android application built with modern technologies, featuring real-time notifications, secure payments, and location-based item discovery.

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Setup & Installation](#setup--installation)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Firebase Configuration](#firebase-configuration)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

## 🎯 Overview

DropSpot is a full-featured marketplace application that connects buyers and sellers in a seamless, secure environment. Users can post items for sale, browse nearby listings, make requests, process payments, and track deliveries - all with real-time notifications and location-based discovery.

### Core Functionality

- **Item Posting**: Multi-image uploads with Cloudinary integration
- **Location Services**: GPS-based item discovery and proximity alerts
- **Request System**: Interest expression and negotiation workflow
- **Payment Processing**: Mock payment system with realistic simulation
- **Order Management**: Complete dispatch and delivery tracking
- **Real-time Notifications**: FCM-powered push notifications
- **User Management**: Firebase Authentication with profile management
- **Event System**: Community events and attendance tracking

## 🚀 Key Features

### Marketplace Core
- ✅ Browse items by category and location
- ✅ Advanced search with distance filtering
- ✅ Multi-image item posting (Gallery/Camera)
- ✅ Location-based item discovery
- ✅ Saved posts functionality
- ✅ Item view tracking and statistics

### User Interaction
- ✅ Request system for item interest
- ✅ Real-time request status updates
- ✅ User ratings and profiles
- ✅ Private messaging through requests
- ✅ Notification center with read/unread status

### Payment & Commerce
- ✅ Mock payment system (80% success rate)
- ✅ Payment simulation with loading states
- ✅ Order status tracking (Paid → Dispatched → Delivered)
- ✅ Delivery address collection
- ✅ Payment history and receipts

### Communication
- ✅ Firebase Cloud Messaging (FCM)
- ✅ Push notifications for all major events
- ✅ In-app notification management
- ✅ Real-time status updates

### Community Features
- ✅ Local events creation and management
- ✅ Event attendance tracking
- ✅ Community announcements
- ✅ Proximity-based event discovery

## 🏗️ Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                      │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Activities: Main, PostItem, ItemDetail, Payment    │    │
│  │  Fragments: Home, Profile, MyRequests, etc.         │    │
│  │  Services: FCM, Location, NotificationWorker        │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────┬───────────────────────────────────────────┘
                  │
           ┌──────┴──────┐
           │  REST API   │
           │ (Express.js)│
           └──────┬──────┘
                  │
┌─────────────────┴───────────────────────────────────────────┐
│                 Firebase Ecosystem                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ Firestore   │ │ Auth        │ │ Cloud       │           │
│  │ Database    │ │ Service     │ │ Messaging   │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **User Action** → Android App captures input
2. **API Request** → Retrofit sends authenticated request to backend
3. **Backend Processing** → Express.js validates and processes request
4. **Database Operations** → Firestore CRUD operations
5. **Notifications** → FCM sends push notifications
6. **Response** → Data flows back to Android app
7. **UI Update** → App updates interface with new data

### Security Architecture

- **Authentication**: Firebase ID tokens for all API requests
- **Authorization**: User-based permissions in Firestore rules
- **Data Validation**: Input sanitization on both client and server
- **Secure Storage**: Encrypted SharedPreferences for sensitive data

## 🛠️ Technology Stack

### Android App
- **Language**: Java
- **Architecture**: MVVM with Activities/Fragments
- **Networking**: Retrofit 2 + OkHttp
- **Authentication**: Firebase Auth
- **Database**: Local SQLite (for caching)
- **Image Processing**: Glide, Cloudinary
- **Location**: Google Play Services Location
- **Notifications**: Firebase Cloud Messaging
- **UI**: Material Design Components

### Backend Server
- **Runtime**: Node.js 18+
- **Framework**: Express.js
- **Database**: Firebase Firestore
- **Authentication**: Firebase Admin SDK
- **Notifications**: FCM HTTP v1 API
- **Image Storage**: Cloudinary
- **Deployment**: Render.com

### Infrastructure
- **Database**: Firebase Firestore (NoSQL)
- **Authentication**: Firebase Auth
- **File Storage**: Cloudinary CDN
- **Push Notifications**: Firebase Cloud Messaging
- **Hosting**: Render.com (Backend), Google Play (App)

## 📦 Setup & Installation

### Prerequisites

- **Android Development**:
  - Android Studio Arctic Fox or later
  - JDK 11+
  - Android SDK API 21+ (Android 5.0+)
  - Physical device or emulator

- **Backend Development**:
  - Node.js 18+
  - npm or yarn
  - Firebase project with Firestore enabled

### Android App Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/yourusername/dropspot.git
   cd dropspot
   ```

2. **Open in Android Studio**
   ```bash
   # Open the project in Android Studio
   studio.bat
   ```

3. **Configure Firebase**
   - Download `google-services.json` from Firebase Console
   - Place in `app/src/main/`
   - Update `ApiClient.java` with your backend URL

4. **Configure Cloudinary**
   - Update credentials in `PostItemActivity.java`
   - Set your cloud name and upload preset

5. **Build and Run**
   ```bash
   ./gradlew clean assembleDebug
   ./gradlew installDebug
   ```

### Backend Setup

1. **Install Dependencies**
   ```bash
   cd backend
   npm install
   ```

2. **Environment Configuration**
   ```bash
   cp .env.example .env
   # Edit .env with your Firebase credentials
   ```

3. **Firebase Setup**
   - Create Firebase project
   - Enable Firestore, Authentication, FCM
   - Generate service account key
   - Update `.env` with credentials

4. **Start Development Server**
   ```bash
   npm run dev  # With hot reload
   # or
   npm start    # Production mode
   ```

### Firebase Configuration

1. **Create Project**: Go to Firebase Console → Add Project
2. **Enable Services**:
   - Authentication (Email/Password, Google)
   - Firestore Database
   - Cloud Messaging
   - Storage (optional)

3. **Security Rules**: Update `firestore.rules` and deploy
4. **Service Account**: Generate key for backend authentication

## 📚 API Documentation

### Authentication

All API requests require Firebase ID token:

```
Authorization: Bearer <firebase_id_token>
Content-Type: application/json
```

### Base URL
```
Development: http://192.168.x.x:5000/api
Production: https://dropspot-xt4s.onrender.com/api
```

### Posts Endpoints

#### Create Post
```http
POST /api/posts
Content-Type: application/json

{
  "title": "iPhone 12",
  "description": "Excellent condition",
  "category": "Electronics",
  "condition": "excellent",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "images": ["url1", "url2"]
}
```

#### Get Posts
```http
GET /api/posts?category=Electronics&limit=20&latitude=40.7128&longitude=-74.0060&maxDistance=50
```

#### Update Post
```http
PUT /api/posts/{id}
{
  "title": "Updated Title",
  "description": "Updated description"
}
```

### Requests Endpoints

#### Create Request
```http
POST /api/requests
{
  "postId": "post-id",
  "message": "I'm interested in this item"
}
```

#### Update Request Status
```http
PUT /api/requests/{id}/status
{
  "status": "accepted"
}
```

#### Get Requests
```http
GET /api/requests?type=received  # or 'sent'
```

### Users Endpoints

#### Update Profile
```http
POST /api/users
{
  "name": "John Doe",
  "phone": "+1234567890",
  "bio": "Community member"
}
```

#### Get User Profile
```http
GET /api/users/{userId}
```

### Notifications Endpoints

#### Get Notifications
```http
GET /api/notifications/{userId}?limit=20&unreadOnly=false
```

#### Mark as Read
```http
PUT /api/notifications/{id}
```

### Events Endpoints

#### Create Event
```http
POST /api/events
{
  "title": "Community Cleanup",
  "description": "Let's clean up the park",
  "startDate": "2024-04-20T10:00:00Z",
  "endDate": "2024-04-20T12:00:00Z",
  "location": "Central Park",
  "latitude": 40.7829,
  "longitude": -73.9654
}
```

#### Join Event
```http
POST /api/events/{id}/join
```

### Payments Endpoints

#### Save Payment
```http
POST /api/payments
{
  "paymentId": "PAY_123456789",
  "postId": "post-id",
  "requesterId": "buyer-id",
  "ownerId": "seller-id",
  "amount": 299.99,
  "status": "success"
}
```

## 🗄️ Database Schema

### Firestore Collections

#### Users Collection
```json
{
  "uid": "user-id",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "photo": "https://...",
  "bio": "Community member",
  "location": "New York",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "rating": 4.5,
  "ratingCount": 12,
  "fcmToken": "fcm-token",
  "createdAt": "2024-01-15T10:00:00Z"
}
```

#### Posts Collection
```json
{
  "id": "post-id",
  "userId": "user-id",
  "title": "iPhone 12",
  "description": "Excellent condition",
  "category": "Electronics",
  "condition": "excellent",
  "price": 299.99,
  "latitude": 40.7128,
  "longitude": -74.0060,
  "images": ["url1", "url2"],
  "isActive": true,
  "requestCount": 3,
  "viewCount": 25,
  "createdAt": "2024-01-15T10:00:00Z"
}
```

#### Requests Collection
```json
{
  "id": "request-id",
  "postId": "post-id",
  "postOwnerId": "seller-id",
  "postTitle": "iPhone 12",
  "requesterId": "buyer-id",
  "requesterName": "Jane Doe",
  "message": "I'm interested",
  "status": "pending|accepted|rejected|dispatched|completed",
  "trackingNumber": "TRK-123456",
  "createdAt": "2024-01-15T10:00:00Z",
  "respondedAt": "2024-01-16T14:30:00Z"
}
```

#### Notifications Collection
```json
{
  "id": "notification-id",
  "userId": "user-id",
  "type": "new_request|request_accepted|order_dispatched",
  "title": "New Request",
  "message": "Someone requested your item",
  "relatedId": "request-id",
  "relatedType": "request",
  "read": false,
  "createdAt": "2024-01-15T10:00:00Z"
}
```

#### Payments Collection
```json
{
  "paymentId": "PAY_123456789",
  "postId": "post-id",
  "requesterId": "buyer-id",
  "ownerId": "seller-id",
  "amount": 299.99,
  "status": "success|failed",
  "deliveryAddress": "123 Main St, NY 10001",
  "createdAt": "2024-01-15T10:00:00Z"
}
```

#### Orders Collection
```json
{
  "id": "order-id",
  "paymentId": "PAY_123456789",
  "buyerId": "buyer-id",
  "sellerId": "seller-id",
  "itemTitle": "iPhone 12",
  "status": "paid|dispatched|delivered",
  "trackingNumber": "TRK-123456",
  "dispatchedAt": "2024-01-16T10:00:00Z",
  "deliveredAt": "2024-01-17T15:00:00Z"
}
```

#### Events Collection
```json
{
  "id": "event-id",
  "userId": "organizer-id",
  "title": "Community Cleanup",
  "description": "Let's clean up the park",
  "category": "Community",
  "startDate": "2024-04-20T10:00:00Z",
  "endDate": "2024-04-20T12:00:00Z",
  "location": "Central Park",
  "latitude": 40.7829,
  "longitude": -73.9654,
  "attendees": ["user1", "user2"],
  "attendeeCount": 2,
  "isActive": true
}
```

## 🔧 Firebase Configuration

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }

    // Anyone can read posts, owners can write
    match /posts/{postId} {
      allow read: if true;
      allow write: if request.auth.uid == resource.data.userId;
    }

    // Request permissions based on ownership
    match /requests/{requestId} {
      allow read: if request.auth.uid == resource.data.requesterId
                      || request.auth.uid == resource.data.postOwnerId;
      allow create: if request.auth.uid == request.resource.data.requesterId;
      allow write: if request.auth.uid == resource.data.postOwnerId;
    }

    // Payment permissions
    match /payments/{paymentId} {
      allow read, write: if request.auth.uid == resource.data.ownerId
                              || request.auth.uid == resource.data.requesterId;
    }

    // Notifications are private
    match /notifications/{notificationId} {
      allow read, write: if request.auth.uid == resource.data.userId;
    }

    // Events are public for reading
    match /events/{eventId} {
      allow read: if true;
      allow write: if request.auth.uid == resource.data.userId;
    }
  }
}
```

### FCM Configuration

1. **Server Key**: Get from Firebase Console → Project Settings → Cloud Messaging
2. **Client Setup**: Android app automatically registers for FCM tokens
3. **Notification Payload**: Structured with type, relatedId, and user data

## 🔍 Troubleshooting

### Common Android Issues

#### Build Failures
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug

# Check for dependency conflicts
./gradlew app:dependencies --configuration releaseRuntimeClasspath
```

#### Permission Denied Errors
- Check `AndroidManifest.xml` for required permissions
- Verify runtime permission requests in code
- Ensure Firebase Auth user is signed in

#### Network Connection Issues
- Verify backend server is running
- Check IP address in `ApiClient.java`
- Test connectivity with `ping` or `curl`

### Common Backend Issues

#### Firebase Connection
```bash
# Check .env file
cat backend/.env

# Test Firebase connection
node -e "const admin = require('firebase-admin'); console.log('Firebase connected');"
```

#### Port Already in Use
```bash
# Find process using port 5000
netstat -ano | findstr :5000

# Kill process
taskkill /PID <PID> /F
```

#### CORS Errors
- Check `FRONTEND_URL` in backend `.env`
- Verify device IP is in allowed origins
- Restart backend after configuration changes

### Database Issues

#### Firestore Permission Denied
- Deploy updated security rules
- Verify user authentication
- Check document ownership fields

#### Query Failures
- Ensure composite indexes exist for complex queries
- Check field names match schema
- Verify query parameters are correct

### Notification Issues

#### FCM Not Working
- Verify FCM token is stored in user document
- Check server key in backend configuration
- Test with Firebase Console notification composer

#### Missing Notifications
- Check device notification permissions
- Verify FCM token is current (regenerate if needed)
- Check backend logs for FCM errors

### Verification Commands

#### Android
```bash
# Check device connection
adb devices

# View app logs
adb logcat | findstr DropSpot

# Check app installation
adb shell pm list packages | findstr dropspot
```

#### Backend
```bash
# Check server status
curl http://localhost:5000

# View server logs
tail -f backend/logs/app.log

# Test API endpoint
curl -H "Authorization: Bearer <token>" http://localhost:5000/api/posts
```

## 💻 Development

### Android Development

#### Project Structure
```
app/src/main/java/com/example/dropspot/
├── activities/          # Activity classes
├── adapters/           # RecyclerView adapters
├── fragments/          # Fragment classes
├── models/            # Data model classes
├── services/          # Background services
├── utils/             # Utility classes
└── ApiClient.java     # Network client
```

#### Key Classes
- `MainActivity`: App entry point and navigation
- `PostItemActivity`: Item creation with image upload
- `ItemDetailActivity`: Item viewing and purchasing
- `PaymentActivity`: Mock payment processing
- `MyFirebaseMessagingService`: FCM message handling

#### Adding New Features
1. Create new Activity/Fragment
2. Add to `AndroidManifest.xml`
3. Update navigation in `MainActivity`
4. Add API endpoints in `ApiService.java`
5. Implement backend routes

### Backend Development

#### Project Structure
```
backend/
├── config/             # Firebase configuration
├── middleware/         # Auth and error handling
├── routes/            # API route handlers
├── utils/             # Helper functions
├── index.js           # Server entry point
└── package.json       # Dependencies
```

#### Key Files
- `index.js`: Express app setup and middleware
- `routes/posts.js`: Post CRUD operations
- `routes/requests.js`: Request management
- `config/firebase.js`: Firebase initialization
- `utils/notifications.js`: FCM notification helpers

#### Adding New Endpoints
1. Create route handler in `routes/`
2. Add validation and business logic
3. Update Firestore operations
4. Add to main `index.js`
5. Test with API client

### Testing Strategy

#### Unit Tests
```bash
# Android tests
./gradlew test

# Backend tests
cd backend && npm test
```

#### Integration Tests
- API endpoint testing with Postman
- Database operation verification
- End-to-end user flow testing

#### Manual Testing Checklist
- [ ] App installation and launch
- [ ] User registration/login
- [ ] Item posting with images
- [ ] Location permission and fetching
- [ ] Item browsing and filtering
- [ ] Request creation and management
- [ ] Payment flow simulation
- [ ] Notification delivery
- [ ] Order tracking workflow

## 🚀 Deployment

### Android App Deployment

#### Google Play Store
1. **Prepare Release Build**
   ```bash
   ./gradlew clean assembleRelease
   ```

2. **Generate Signed APK**
   - Create keystore
   - Configure signing in `build.gradle.kts`
   - Build signed APK

3. **Upload to Play Console**
   - Create app listing
   - Upload APK/AAB
   - Configure store listing
   - Publish

#### Alternative Distribution
- Firebase App Distribution
- Direct APK sharing
- Third-party app stores

### Backend Deployment

#### Render.com Deployment
1. **Prepare for Production**
   ```bash
   cd backend
   npm run build  # If using build step
   ```

2. **Environment Variables**
   - Set production Firebase credentials
   - Configure production database URL
   - Set NODE_ENV=production

3. **Deploy**
   - Connect GitHub repository
   - Configure build settings
   - Set environment variables
   - Deploy

#### Alternative Hosting
- Google Cloud Functions
- AWS Lambda
- Heroku
- DigitalOcean App Platform

### Production Checklist

#### Security
- [ ] HTTPS enabled
- [ ] Firebase security rules deployed
- [ ] API keys secured
- [ ] Input validation active
- [ ] Error messages sanitized

#### Performance
- [ ] Database indexes created
- [ ] Image optimization enabled
- [ ] Caching implemented
- [ ] CDN configured for assets

#### Monitoring
- [ ] Error logging configured
- [ ] Performance monitoring active
- [ ] User analytics enabled
- [ ] Backup strategy in place

## 🤝 Contributing

### Development Workflow
1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Code Standards
- Follow Java/Android coding conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Write unit tests for new features
- Update documentation

### Reporting Issues
- Use GitHub Issues for bug reports
- Include device information and Android version
- Provide steps to reproduce
- Attach logcat output for crashes
- Include screenshots for UI issues

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Firebase for backend services
- Google for Android platform
- Cloudinary for image hosting
- Material Design for UI components
- Open source community for libraries

## 📞 Support

For support and questions:
- 📧 Email: support@dropspot.com
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/dropspot/issues)
- 📖 Docs: [Documentation](https://github.com/yourusername/dropspot/wiki)

---

**DropSpot** - Connecting communities through local commerce. 🛒✨
