# Dispatch & Delivery Notifications Implementation Guide

## Overview 🚀
Complete implementation of FCM notifications for order dispatch (seller to buyer) and delivery confirmation (buyer to seller) with proper recipient validation and backend API integration.

## What Was Fixed? ✅

### Problem
- Dispatch and delivery notifications were being sent but not properly validated for recipient
- No database records in notifications collection for tracking
- No recipientUserId in FCM data payload for frontend validation
- All operations were client-side Firestore writes without backend coordination

### Solution
- **Backend**: Created `/api/dispatch/mark-dispatched` and `/api/dispatch/mark-delivered` endpoints
- **Backend**: Both endpoints save notifications to Firestore with correct `userId` (recipient's ID)
- **Backend**: Both endpoints send FCM with `recipientUserId` in data payload for validation
- **Frontend**: Updated `DispatchTrackingHelper` to call backend APIs instead of direct Firestore writes
- **Frontend**: Added proper API request/response objects for dispatch and delivery

## Architecture 🏗️

```
DISPATCH FLOW:
==============
Seller clicks "Dispatch" button
    ↓
PostedItemsAdapter calls DispatchTrackingHelper.markOrderAsDispatched()
    ↓
DispatchTrackingHelper calls sendDispatchNotification()
    ↓
Frontend calls Backend: POST /api/dispatch/mark-dispatched
    ↓
Backend:
  1. Validates seller is order owner
  2. Updates payment status to "dispatched"
  3. Saves notification to Firebase with userId = buyerId
  4. Sends FCM to buyer with recipientUserId = buyerId
    ↓
Buyer receives notification ✅ (Only buyer sees it)


DELIVERY FLOW:
==============
Buyer clicks "Confirm Delivery" button
    ↓
Buyer's Activity calls DispatchTrackingHelper.markOrderAsDelivered()
    ↓
DispatchTrackingHelper calls sendDeliveryConfirmedNotification()
    ↓
Frontend calls Backend: POST /api/dispatch/mark-delivered
    ↓
Backend:
  1. Validates buyer is requester
  2. Updates payment status to "delivered"
  3. Saves notification to Firebase with userId = sellerId
  4. Sends FCM to seller with recipientUserId = sellerId
    ↓
Seller receives notification ✅ (Only seller sees it)
```

## Implementation Details 📋

### Backend Files Created/Modified

**File: `backend/routes/dispatch.js` (NEW)**
- `POST /dispatch/mark-dispatched` - Dispatch order, notify buyer
- `POST /dispatch/mark-delivered` - Confirm delivery, notify seller

Key features:
- Authorization validation (seller/buyer only)
- Database notification save with correct `userId`
- FCM notification with `recipientUserId` validation
- Comprehensive logging with [DISPATCH] and [DELIVERY] tags

**File: `backend/index.js` (MODIFIED)**
- Added `import dispatchRouter from './routes/dispatch.js'`
- Added `app.use('/api/dispatch', dispatchRouter)`

### Frontend Files Modified

**File: `app/src/main/java/com/example/dropspot/ApiService.java`**
- Added `@POST("dispatch/mark-dispatched")` endpoint
- Added `@POST("dispatch/mark-delivered")` endpoint
- Added `DispatchRequest` class with fields: paymentId, buyerId, sellerId, itemTitle, trackingNumber
- Added `DeliveryRequest` class with fields: paymentId, buyerId, sellerId, itemTitle

**File: `app/src/main/java/com/example/dropspot/DispatchTrackingHelper.java`**
- `sendDispatchNotification()` - Now calls backend API via `markOrderDispatched()`
- `sendDeliveryConfirmedNotification()` - Now calls backend API via `markOrderDelivered()`
- `markOrderAsDispatched()` - Delegates to `sendDispatchNotification()`
- `markOrderAsDelivered()` - Delegates to `sendDeliveryConfirmedNotification()`
- Removed old direct Firestore writes
- Removed old `triggerFcmDispatchNotification()` and `triggerFcmDeliveryNotification()` methods

## Data Structure 🗃️

### Notification Document in Firestore (dispatch)
```json
{
  "notificationId": "unique-id",
  "userId": "buyerId",              // ← CRITICAL: Recipient is buyer
  "type": "order_dispatched",
  "title": "Order Shipped 🚚",
  "message": "Your item \"iPhone 12\" has been dispatched by the seller",
  "trackingNumber": "TRACK123456",
  "itemTitle": "iPhone 12",
  "relatedId": "paymentId",
  "relatedType": "payment",
  "read": false,
  "createdAt": "2026-04-20T10:30:00Z"
}
```

### Notification Document in Firestore (delivery)
```json
{
  "notificationId": "unique-id",
  "userId": "sellerId",             // ← CRITICAL: Recipient is seller
  "type": "order_delivered",
  "title": "Delivery Confirmed 📦",
  "message": "Buyer confirmed delivery of \"iPhone 12\"",
  "itemTitle": "iPhone 12",
  "relatedId": "paymentId",
  "relatedType": "payment",
  "read": false,
  "createdAt": "2026-04-20T10:35:00Z"
}
```

### FCM Data Payload
```javascript
{
  type: "order_dispatched" or "order_delivered",
  paymentId: "payment-id",
  trackingNumber: "TRACK123456",     // Only for dispatch
  itemTitle: "iPhone 12",
  recipientUserId: "user-id"         // ← CRITICAL: Frontend validates
}
```

## Testing Guide 🧪

### Test Case 1: Dispatch Notification (Seller → Buyer)
**Setup:**
- Two users: Seller (User A) and Buyer (User B)
- Payment completed for Item X
- Both users have app installed with FCM enabled

**Steps:**
1. Login as Seller (User A)
2. Go to "My Items" / "Posted Items"
3. Find the item with a pending delivery
4. Click "Dispatch" button
5. Enter tracking number (e.g., "TRACK12345")
6. Confirm

**Expected Result:**
- ✅ Seller sees "Order dispatched!" toast
- ✅ Buyer receives FCM notification: "Order Shipped 🚚"
- ✅ Notification appears in Buyer's Announcements section
- ✅ In Firebase Console, notifications collection has document with:
  - `userId: buyerId`
  - `type: "order_dispatched"`
  - `recipientUserId: buyerId` (in data payload)

**If NOT working:**
1. Check backend logs for: `[DISPATCH] Sending dispatch notification to buyer: [buyerId]`
2. Verify Firebase notification was saved with `userId = buyerId`
3. Check FCM token exists for buyer: `users > buyerId > fcmToken`

### Test Case 2: Delivery Notification (Buyer → Seller)
**Setup:**
- Same as above, but order is dispatched first
- Buyer has received the item

**Steps:**
1. Login as Buyer (User B)
2. Go to "My Purchases" or "Order Tracking"
3. Find the dispatched order
4. Click "Confirm Delivery" button
5. Confirm

**Expected Result:**
- ✅ Buyer sees "Order marked as delivered!" toast
- ✅ Seller receives FCM notification: "Delivery Confirmed 📦"
- ✅ Notification appears in Seller's Announcements section
- ✅ In Firebase Console, notifications collection has document with:
  - `userId: sellerId`
  - `type: "order_delivered"`
  - `recipientUserId: sellerId` (in data payload)

**If NOT working:**
1. Check backend logs for: `[DELIVERY] Sending delivery notification to seller: [sellerId]`
2. Verify Firebase notification was saved with `userId = sellerId`
3. Check FCM token exists for seller: `users > sellerId > fcmToken`

## Defense-in-Depth Protection 🛡️

### Level 1: Database (Backend)
- Notifications saved with `userId` = recipient only
- Query `notifications.where('userId', '==', currentUser.uid)` filters correctly
- Only recipient sees their notifications

### Level 2: FCM Data Payload
- `recipientUserId` sent in FCM data
- Frontend knows intended recipient

### Level 3: Frontend Validation (MyFirebaseMessagingService)
```java
String recipientUserId = remoteMessage.getData().get("recipientUserId");
FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

if (recipientUserId != null && !recipientUserId.isEmpty()) {
    if (currentUser == null || !currentUser.getUid().equals(recipientUserId)) {
        return; // Don't show - this is for another user
    }
}
```

## Debugging Checklist 🔧

1. **Verify Backend Routes Registered**
   ```
   Check: backend/index.js has dispatchRouter imported and registered
   ```

2. **Check Backend Logs (when dispatch/delivery attempted)**
   ```
   Look for: [DISPATCH] Sending dispatch notification to buyer: [buyerId]
   or: [DELIVERY] Sending delivery notification to seller: [sellerId]
   ```

3. **Verify Firestore Notifications**
   ```
   Go to: Firebase Console → Firestore → notifications collection
   Find documents with type: "order_dispatched" or "order_delivered"
   Verify: userId = recipient's UID
   ```

4. **Check FCM Tokens**
   ```
   Go to: Firebase Console → Firestore → users collection
   Verify: Both buyer and seller have fcmToken populated
   ```

5. **Frontend API Calls**
   ```
   Check: DispatchTrackingHelper calls markOrderDispatched/markOrderDelivered
   Verify: ApiService has DispatchRequest and DeliveryRequest classes
   ```

6. **Rebuild & Test**
   ```
   Android Studio: Build → Clean Project → Rebuild Project
   Uninstall app, reinstall from device
   Test again
   ```

## Files Changed Summary ✏️

### Backend
- ✅ `backend/routes/dispatch.js` - NEW
- ✅ `backend/index.js` - MODIFIED (added dispatch routes)

### Frontend
- ✅ `app/src/main/java/com/example/dropspot/ApiService.java` - MODIFIED
- ✅ `app/src/main/java/com/example/dropspot/DispatchTrackingHelper.java` - MODIFIED

## Related Documentation
- See `NOTIFICATION_FIX_QUICK_GUIDE.md` for request notification pattern
- See `PAYMENT_NOTIFICATION_FIX.md` for payment notification pattern
- Both use the same recipientUserId validation approach

## FAQ ❓

**Q: Why are we using backend API instead of direct Firestore?**
A: Backend API provides centralized notification management, FCM token lookup, authorization validation, and audit logging. Direct Firestore writes bypass security checks.

**Q: What if FCM token not found for recipient?**
A: Notification is still saved to Firestore database. When user opens app next time, they see it in Announcements. FCM is best-effort real-time delivery.

**Q: Can seller see buyer's delivery notification?**
A: No - notification is saved with `userId = sellerId` only. Frontend recipientUserId validation adds additional layer.

**Q: How long are notifications stored?**
A: Indefinitely in Firestore (until user deletes). You can add TTL (Time To Live) rules in production.

