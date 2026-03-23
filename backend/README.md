# DropSpot Backend API

A complete Firebase Firestore REST API backend for the DropSpot Android application built with Node.js, Express, and Firebase Admin SDK.

## Features

- ✅ Firebase Authentication integration (ID token verification)
- ✅ Firestore database operations
- ✅ RESTful API with Express.js
- ✅ Modular route structure
- ✅ Comprehensive error handling
- ✅ Request validation
- ✅ User profile management
- ✅ Posts management (CRUD)
- ✅ Request/offer system
- ✅ Saved posts functionality
- ✅ Events management
- ✅ Notifications system
- ✅ User ratings and statistics

## Project Structure

```
backend/
├── config/
│   └── firebase.js           # Firebase Admin SDK initialization
├── middleware/
│   └── auth.js               # Authentication & error handling middleware
├── routes/
│   ├── posts.js              # Posts endpoints
│   ├── requests.js           # Request/offer endpoints
│   ├── savedPosts.js         # Saved posts endpoints
│   ├── events.js             # Events endpoints
│   ├── notifications.js      # Notifications endpoints
│   └── users.js              # User profile endpoints
├── utils/
│   └── helpers.js            # Helper functions and utilities
├── .env.example              # Environment variables template
├── .gitignore                # Git ignore rules
├── package.json              # Dependencies and scripts
├── README.md                 # This file
└── index.js                  # Main application entry point
```

## Prerequisites

- Node.js 18+ or higher
- Firebase project with Firestore enabled
- Firebase Admin SDK service account credentials
- npm or yarn

## Installation

### 1. Clone and Setup

```bash
cd backend
npm install
```

### 2. Environment Configuration

Create a `.env` file in the backend directory:

```bash
cp .env.example .env
```

Edit `.env` with your Firebase credentials:

```env
PORT=5000
NODE_ENV=development
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY="your-private-key"
FIREBASE_CLIENT_EMAIL=your-client-email@project.iam.gserviceaccount.com
FIREBASE_DATABASE_URL=https://your-project.firebaseio.com
FRONTEND_URL=http://localhost:3000
```

**Getting Firebase Credentials:**
1. Go to Firebase Console → Project Settings
2. Service Accounts tab
3. Click "Generate New Private Key"
4. Download JSON file
5. Copy values to your `.env` file

### 3. Start the Server

```bash
# Development mode with auto-reload
npm run dev

# Production mode
npm start
```

Server will start on `http://localhost:5000`

## API Documentation

All API endpoints require Firebase Authentication token in the Authorization header:

```
Authorization: Bearer <firebase_id_token>
```

### Response Format

Success Response:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { }
}
```

Error Response:
```json
{
  "success": false,
  "message": "Error description",
  "error": "ERROR_CODE",
  "code": 400
}
```

---

## Posts Endpoints

### Create Post
```
POST /api/posts
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "iPhone 12",
  "description": "Excellent condition, rarely used",
  "category": "Electronics",
  "condition": "excellent",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "images": ["url1", "url2"]
}
```

### Get All Posts
```
GET /api/posts?category=Electronics&limit=20&offset=0&latitude=40.7128&longitude=-74.0060&maxDistance=50
Authorization: Bearer <token>
```

Query Parameters:
- `category` - Filter by category
- `userId` - Filter by user ID
- `limit` - Number of posts (default: 20)
- `offset` - Pagination offset (default: 0)
- `latitude` - User latitude (for distance calculation)
- `longitude` - User longitude (for distance calculation)
- `maxDistance` - Max distance in km (default: 50)

### Get Post Details
```
GET /api/posts/:id
Authorization: Bearer <token>
```

### Update Post
```
PUT /api/posts/:id
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "iPhone 12 Pro",
  "description": "Updated description",
  "condition": "new"
}
```

### Delete Post
```
DELETE /api/posts/:id
Authorization: Bearer <token>
```

### Increment View Count
```
PUT /api/posts/:id/view
Authorization: Bearer <token>
```

---

## Requests Endpoints

### Create Request
```
POST /api/requests
Content-Type: application/json
Authorization: Bearer <token>

{
  "postId": "post-id-here",
  "message": "I'm interested in this item"
}
```

### Get Requests
```
GET /api/requests?type=received
Authorization: Bearer <token>
```

Query Parameters:
- `type` - `received` (default) or `sent`

### Get Request Details
```
GET /api/requests/:id
Authorization: Bearer <token>
```

### Accept/Reject Request
```
PUT /api/requests/:id
Content-Type: application/json
Authorization: Bearer <token>

{
  "status": "accepted"
}
```

Status options: `accepted` or `rejected`

### Cancel Request
```
DELETE /api/requests/:id
Authorization: Bearer <token>
```

---

## Saved Posts Endpoints

### Save Post
```
POST /api/saved
Content-Type: application/json
Authorization: Bearer <token>

{
  "postId": "post-id-here"
}
```

### Get Saved Posts
```
GET /api/saved/:userId?limit=20&offset=0
Authorization: Bearer <token>
```

### Remove Saved Post
```
DELETE /api/saved/:postId
Authorization: Bearer <token>
```

### Check if Post is Saved
```
GET /api/saved/check/:postId
Authorization: Bearer <token>
```

---

## Events Endpoints

### Create Event
```
POST /api/events
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Community Cleanup",
  "description": "Let's clean up the park",
  "category": "Community",
  "startDate": "2024-04-20T10:00:00Z",
  "endDate": "2024-04-20T12:00:00Z",
  "location": "Central Park",
  "latitude": 40.7829,
  "longitude": -73.9654,
  "images": ["url1", "url2"]
}
```

### Get All Events
```
GET /api/events?category=Community&limit=20&offset=0&upcomingOnly=true
Authorization: Bearer <token>
```

### Get Event Details
```
GET /api/events/:id
Authorization: Bearer <token>
```

### Update Event
```
PUT /api/events/:id
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Updated title",
  "description": "Updated description"
}
```

### Delete Event
```
DELETE /api/events/:id
Authorization: Bearer <token>
```

### Join Event
```
POST /api/events/:id/join
Authorization: Bearer <token>
```

### Leave Event
```
DELETE /api/events/:id/leave
Authorization: Bearer <token>
```

---

## Notifications Endpoints

### Get Notifications
```
GET /api/notifications/:userId?limit=20&offset=0&unreadOnly=false
Authorization: Bearer <token>
```

### Mark as Read
```
PUT /api/notifications/:id
Authorization: Bearer <token>
```

### Mark Multiple as Read
```
PUT /api/notifications/batch/read
Content-Type: application/json
Authorization: Bearer <token>

{
  "notificationIds": ["id1", "id2", "id3"]
}
```

### Delete Notification
```
DELETE /api/notifications/:id
Authorization: Bearer <token>
```

### Delete Multiple Notifications
```
DELETE /api/notifications/batch/delete
Content-Type: application/json
Authorization: Bearer <token>

{
  "notificationIds": ["id1", "id2", "id3"]
}
```

### Get Unread Count
```
GET /api/notifications/:userId/unread-count
Authorization: Bearer <token>
```

---

## Users Endpoints

### Create/Update User Profile
```
POST /api/users
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "John Doe",
  "phone": "+1234567890",
  "bio": "Community member",
  "location": "New York",
  "latitude": 40.7128,
  "longitude": -74.0060
}
```

### Get User Profile
```
GET /api/users/:userId
Authorization: Bearer <token>
```

### Get User Posts
```
GET /api/users/:userId/posts?limit=10&offset=0
Authorization: Bearer <token>
```

### Get User Statistics
```
GET /api/users/:userId/stats
Authorization: Bearer <token>
```

### Rate User
```
PUT /api/users/:userId/rating
Content-Type: application/json
Authorization: Bearer <token>

{
  "rating": 5,
  "comment": "Great experience!"
}
```

---

## Firestore Collection Structure

### Users Collection
```json
{
  "uid": "user-id",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "bio": "Community member",
  "photo": "url",
  "location": "New York",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "rating": 25,
  "ratingCount": 5,
  "isVerified": false,
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-20T15:30:00Z"
}
```

### Posts Collection
```json
{
  "id": "post-id",
  "userId": "user-id",
  "title": "iPhone 12",
  "description": "Excellent condition",
  "category": "Electronics",
  "condition": "excellent",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "images": ["url1"],
  "postedAt": "2024-01-15T10:00:00Z",
  "requestCount": 3,
  "viewCount": 25,
  "isActive": true
}
```

### Requests Collection
```json
{
  "id": "request-id",
  "postId": "post-id",
  "postTitle": "iPhone 12",
  "postOwnerId": "owner-id",
  "requesterId": "requester-id",
  "requesterName": "Jane Doe",
  "requesterEmail": "jane@example.com",
  "message": "I'm interested",
  "status": "pending",
  "createdAt": "2024-01-15T10:00:00Z",
  "respondedAt": null
}
```

### Events Collection
```json
{
  "id": "event-id",
  "userId": "organizer-id",
  "title": "Community Cleanup",
  "description": "Let's clean the park",
  "category": "Community",
  "startDate": "2024-04-20T10:00:00Z",
  "endDate": "2024-04-20T12:00:00Z",
  "location": "Central Park",
  "latitude": 40.7829,
  "longitude": -73.9654,
  "attendees": ["user1", "user2"],
  "attendeeCount": 2,
  "isActive": true,
  "createdAt": "2024-01-15T10:00:00Z"
}
```

### Notifications Collection
```json
{
  "id": "notification-id",
  "userId": "user-id",
  "type": "new_request",
  "title": "New request for iPhone 12",
  "message": "Someone requested your item",
  "relatedId": "request-id",
  "relatedType": "request",
  "read": false,
  "readAt": null,
  "createdAt": "2024-01-15T10:00:00Z"
}
```

---

## Error Codes

| Code | Status | Meaning |
|------|--------|---------|
| `UNAUTHORIZED` | 401 | No auth token provided |
| `INVALID_TOKEN` | 401 | Invalid or expired token |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 400 | Input validation failed |
| `INVALID_INPUT` | 400 | Invalid input data |
| `INVALID_REQUEST` | 400 | Conflicting request |
| `DUPLICATE_REQUEST` | 400 | Duplicate submission |
| `SERVER_ERROR` | 500 | Internal server error |

---

## Development

### Enable Hot Reload
```bash
npm run dev
```

Uses `nodemon` for automatic server restart on file changes.

### Console Logs
The application includes comprehensive logging for debugging:
- Request logging
- Error logging
- Authentication logging

---

## Deployment

### Firebase Cloud Functions

Deploy as Cloud Function:

```bash
firebase deploy --only functions
```

Create `functions/index.js`:
```javascript
import functions from 'firebase-functions';
import app from './backend/index.js';

export const api = functions.https.onRequest(app);
```

### Docker Deployment

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
EXPOSE 5000
CMD ["node", "index.js"]
```

### Environment Variables (Production)
Ensure these are set in your deployment environment:
- `FIREBASE_PROJECT_ID`
- `FIREBASE_PRIVATE_KEY`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_DATABASE_URL`
- `PORT`
- `NODE_ENV=production`

---

## Security Considerations

1. **Authentication**: All endpoints require Firebase ID token verification
2. **Authorization**: User can only access their own data (with exceptions for public profiles)
3. **Validation**: Input validation on all endpoints
4. **Error Handling**: Sensitive error details not exposed to clients
5. **CORS**: Configured for specific origins only

---

## Performance Tips

1. **Indexing**: Create Firestore indexes for common queries
2. **Pagination**: Use limit and offset for large datasets
3. **Caching**: Implement caching for frequently accessed data
4. **Batch Operations**: Use batch writes for multiple updates

---

## Troubleshooting

### Firebase Connection Issues
- Verify `.env` file has correct credentials
- Check Firebase project is active
- Ensure Firestore database is created

### CORS Errors
- Check `FRONTEND_URL` in `.env`
- Verify origin is in CORS whitelist

### Authentication Failures
- Verify Firebase ID token is valid
- Check token hasn't expired
- Ensure proper Authorization header format

---

## License

MIT

---

## Support

For issues and questions, please refer to:
- [Firebase Documentation](https://firebase.google.com/docs)
- [Express.js Documentation](https://expressjs.com)
- [Firestore Documentation](https://firebase.google.com/docs/firestore)
