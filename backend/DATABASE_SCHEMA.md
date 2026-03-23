# Firestore Database Schema & Queries

## Collection Structure

### Users Collection

```
users/
├── {userId}/
│   ├── uid: string                    // Firebase UID
│   ├── name: string                   // User's display name
│   ├── email: string                  // User's email
│   ├── phone: string                  // User's phone number
│   ├── bio: string                    // User's bio/description
│   ├── photo: string                  // Profile photo URL
│   ├── location: string               // Location description
│   ├── latitude: number               // User's latitude
│   ├── longitude: number              // User's longitude
│   ├── rating: number                 // Total rating points
│   ├── ratingCount: number            // Number of ratings
│   ├── isVerified: boolean            // Verification status
│   ├── createdAt: timestamp           // Account creation date
│   ├── updatedAt: timestamp           // Last update date
│   │
│   ├── posts/                         // User's posts subcollection
│   │   └── {postId}/
│   │       ├── postId: string
│   │       ├── createdAt: timestamp
│   │
│   ├── savedPosts/                    // User's saved posts
│   │   └── {postId}/
│   │       ├── id: string
│   │       ├── postId: string
│   │       ├── postTitle: string
│   │       ├── postCategory: string
│   │       ├── postOwnerId: string
│   │       ├── savedAt: timestamp
│   │
│   ├── events/                        // User's events
│   │   └── {eventId}/
│   │       ├── eventId: string
│   │       ├── joinedAt: timestamp
│   │       ├── isCreator: boolean
│   │
│   └── ratings/                       // Ratings from other users
│       └── {raterId}/
│           ├── rating: number (1-5)
│           ├── comment: string
│           ├── ratedBy: string
│           ├── ratedAt: timestamp
```

### Posts Collection

```
posts/
├── {postId}/
│   ├── id: string                     // Post ID (UUID)
│   ├── userId: string                 // Post owner's UID
│   ├── title: string                  // Item title
│   ├── description: string            // Item description
│   ├── category: string               // Category (Electronics, Furniture, etc.)
│   ├── condition: string              // (excellent, good, fair, poor)
│   ├── latitude: number               // Posting location latitude
│   ├── longitude: number              // Posting location longitude
│   ├── images: array<string>          // Image URLs
│   ├── postedAt: timestamp            // When posted
│   ├── updatedAt: timestamp           // Last update
│   ├── requestCount: number           // Number of requests
│   ├── viewCount: number              // Number of views
│   ├── isActive: boolean              // Active/deleted status
│   └── acceptedRequestId: string      // ID of accepted request (if any)
```

Indexes:
- Composite: `userId` (Asc) + `isActive` (Asc)
- Composite: `category` (Asc) + `postedAt` (Desc)
- Single: `isActive` (Asc)

### Requests Collection

```
requests/
├── {requestId}/
│   ├── id: string                     // Request ID
│   ├── postId: string                 // Referenced post ID
│   ├── postTitle: string              // Post title (cached)
│   ├── postOwnerId: string            // Post owner's UID
│   ├── requesterId: string            // Who requested (UID)
│   ├── requesterName: string          // Requester's name (cached)
│   ├── requesterEmail: string         // Requester's email (cached)
│   ├── requesterPhoto: string         // Requester's photo URL
│   ├── message: string                // Request message
│   ├── status: string                 // pending, accepted, rejected, rejected_auto
│   ├── createdAt: timestamp           // When requested
│   ├── respondedAt: timestamp         // When owner responded
│   └── respondedBy: string            // Owner's UID
```

Indexes:
- Composite: `postOwnerId` (Asc) + `createdAt` (Desc)
- Composite: `requesterId` (Asc) + `createdAt` (Desc)
- Composite: `postId` (Asc) + `status` (Asc)

### Saved Posts Collection

```
savedPosts/
├── {saveId}/
│   ├── id: string                     // Save ID
│   ├── postId: string                 // Saved post ID
│   ├── postTitle: string              // Post title (cached)
│   ├── postCategory: string           // Post category (cached)
│   ├── postOwnerId: string            // Post owner's UID
│   ├── userId: string                 // User who saved it
│   └── savedAt: timestamp             // When saved
```

Indexes:
- Composite: `userId` (Asc) + `savedAt` (Desc)
- Single: `postId` (Asc)

### Events Collection

```
events/
├── {eventId}/
│   ├── id: string                     // Event ID
│   ├── userId: string                 // Organizer's UID
│   ├── title: string                  // Event title
│   ├── description: string            // Event description
│   ├── category: string               // Event category
│   ├── startDate: timestamp           // Event start time
│   ├── endDate: timestamp             // Event end time
│   ├── location: string               // Event location
│   ├── latitude: number               // Event latitude
│   ├── longitude: number              // Event longitude
│   ├── images: array<string>          // Event images
│   ├── attendees: array<string>       // Array of attendee UIDs
│   ├── attendeeCount: number          // Number of attendees
│   ├── isActive: boolean              // Active/deleted
│   ├── createdAt: timestamp           // When created
│   └── updatedAt: timestamp           // Last update
```

Indexes:
- Composite: `startDate` (Asc) + `isActive` (Asc)
- Composite: `category` (Asc) + `startDate` (Asc)

### Notifications Collection

```
notifications/
├── {notificationId}/
│   ├── id: string                     // Notification ID
│   ├── userId: string                 // Recipient's UID
│   ├── type: string                   // new_request, request_accepted, request_rejected, etc.
│   ├── title: string                  // Notification title
│   ├── message: string                // Notification message
│   ├── relatedId: string              // ID of related document (post, request, event)
│   ├── relatedType: string            // Type of related document
│   ├── read: boolean                  // Read status
│   ├── readAt: timestamp              // When read
│   └── createdAt: timestamp           // When created
```

Indexes:
- Composite: `userId` (Asc) + `read` (Asc)
- Composite: `userId` (Asc) + `createdAt` (Desc)

---

## Common Queries

### Get User's Active Posts

```javascript
db.collection('posts')
  .where('userId', '==', userId)
  .where('isActive', '==', true)
  .orderBy('postedAt', 'desc')
  .limit(20)
```

### Get Posts by Category

```javascript
db.collection('posts')
  .where('category', '==', 'Electronics')
  .where('isActive', '==', true)
  .orderBy('postedAt', 'desc')
  .limit(20)
```

### Get Requests Received by User

```javascript
db.collection('requests')
  .where('postOwnerId', '==', userId)
  .orderBy('createdAt', 'desc')
```

### Get Requests Sent by User

```javascript
db.collection('requests')
  .where('requesterId', '==', userId)
  .orderBy('createdAt', 'desc')
```

### Get Pending Requests for a Post

```javascript
db.collection('requests')
  .where('postId', '==', postId)
  .where('status', '==', 'pending')
```

### Get User's Saved Posts

```javascript
db.collection('users')
  .doc(userId)
  .collection('savedPosts')
  .orderBy('savedAt', 'desc')
```

### Get Upcoming Events

```javascript
db.collection('events')
  .where('isActive', '==', true)
  .where('startDate', '>=', now)
  .orderBy('startDate', 'asc')
```

### Get User's Unread Notifications

```javascript
db.collection('notifications')
  .where('userId', '==', userId)
  .where('read', '==', false)
  .orderBy('createdAt', 'desc')
```

### Get User's Recent Notifications

```javascript
db.collection('notifications')
  .where('userId', '==', userId)
  .orderBy('createdAt', 'desc')
  .limit(20)
```

---

## Data Access Patterns

### Pattern 1: Get Post with Owner Info

```javascript
async function getPostWithOwner(postId) {
  const postDoc = await db.collection('posts').doc(postId).get();
  const post = postDoc.data();
  
  const userDoc = await db.collection('users').doc(post.userId).get();
  const user = userDoc.data();
  
  return {
    ...post,
    owner: {
      uid: user.uid,
      name: user.name,
      photo: user.photo,
      rating: user.rating / user.ratingCount
    }
  };
}
```

### Pattern 2: Get User's Statistics

```javascript
async function getUserStats(userId) {
  const postCount = (await db.collection('posts')
    .where('userId', '==', userId)
    .where('isActive', '==', true)
    .count()
    .get()).data().count;
  
  const requestCount = (await db.collection('requests')
    .where('postOwnerId', '==', userId)
    .count()
    .get()).data().count;
  
  const acceptedCount = (await db.collection('requests')
    .where('postOwnerId', '==', userId)
    .where('status', '==', 'accepted')
    .count()
    .get()).data().count;
  
  return {
    postsCount: postCount,
    requestsCount: requestCount,
    acceptedCount: acceptedCount
  };
}
```

### Pattern 3: Create Post with Notifications

```javascript
async function createPostWithNotifications(userId, post) {
  const batch = db.batch();
  
  // Add post
  const postRef = db.collection('posts').doc(postId);
  batch.set(postRef, post);
  
  // Add to user's posts
  const userPostRef = db.collection('users')
    .doc(userId)
    .collection('posts')
    .doc(postId);
  batch.set(userPostRef, { postId, createdAt: now });
  
  // Notify followers (if implemented)
  const followersQuery = await db.collection('followers')
    .where('followingId', '==', userId)
    .get();
  
  followersQuery.forEach(doc => {
    const notifRef = db.collection('notifications').doc();
    batch.set(notifRef, {
      userId: doc.data().followerId,
      type: 'new_post',
      message: `${user.name} posted a new item`,
      relatedId: postId,
      relatedType: 'post',
      createdAt: now
    });
  });
  
  await batch.commit();
}
```

### Pattern 4: Handle Request Response

```javascript
async function respondToRequest(requestId, status, userId) {
  const batch = db.batch();
  
  const requestDoc = await db.collection('requests').doc(requestId).get();
  const request = requestDoc.data();
  
  // Update request status
  batch.update(db.collection('requests').doc(requestId), {
    status,
    respondedAt: now,
    respondedBy: userId
  });
  
  if (status === 'accepted') {
    // Deactivate post
    batch.update(db.collection('posts').doc(request.postId), {
      isActive: false,
      acceptedRequestId: requestId
    });
    
    // Reject other pending requests
    const otherRequests = await db.collection('requests')
      .where('postId', '==', request.postId)
      .where('status', '==', 'pending')
      .get();
    
    otherRequests.forEach(doc => {
      if (doc.id !== requestId) {
        batch.update(doc.ref, {
          status: 'rejected_auto',
          respondedAt: now,
          respondedBy: userId
        });
      }
    });
  }
  
  // Create notification for requester
  batch.set(db.collection('notifications').doc(), {
    userId: request.requesterId,
    type: `request_${status}`,
    message: status === 'accepted' 
      ? 'Your request was accepted!'
      : 'Your request was rejected',
    relatedId: requestId,
    relatedType: 'request',
    read: false,
    createdAt: now
  });
  
  await batch.commit();
}
```

---

## Performance Optimization

### 1. Collection Grouping
For queries across all user's posts in subcollections:
```javascript
db.collectionGroup('posts')
  .where('userId', '==', userId)
  .limit(20)
```

### 2. Data Caching
Cache frequently accessed data:
- User profiles
- Posts metadata
- Notification counts

### 3. Pagination
Always use limit and offset for lists:
```javascript
// Good
.limit(20).offset(0) // First page
.limit(20).offset(20) // Second page

// Bad
.limit(1000) // Expensive
```

### 4. Reduce ReadCount
Use denormalization for expensive counts:
```javascript
// Store on post document
post.requestCount = 5
post.viewCount = 100

// Instead of querying each time
db.collection('requests').where('postId', '==', postId).count()
```

---

## Security Rules

**firestore.rules:**
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Authenticated users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    
    // Anyone can read posts
    match /posts/{postId} {
      allow read: if true;
      allow write: if request.auth.uid == resource.data.userId;
    }
    
    // Users can create requests
    match /requests/{requestId} {
      allow read: if request.auth.uid == resource.data.requesterId 
                     || request.auth.uid == resource.data.postOwnerId;
      allow create: if request.auth.uid == request.resource.data.requesterId;
      allow write: if request.auth.uid == resource.data.postOwnerId;
    }
  }
}
```

---

## Monitoring & Analytics

### Track Performance
- Monitor query execution times
- Set up Cloud Monitoring alerts
- Use Firestore capacity metrics

### Common Issues
- N+1 query problem (fetch posts, then fetch owner for each)
- Unbounded queries (missing where clauses)
- Inefficient pagination (large offsets are expensive)
