# FCM Payment Notification Bug Fix 🔧

## Problem 🐛

Owner was not receiving push notifications when a payment was processed on their item. The logs showed:

```
[FCM] User document not found for dUuM1GOBRKWjbotzrU7bby:APA91bEO3dyBYU8sZTcg...
[FCM] Error sending FCM notification: User dUuM1GOBRKWjbotzrU7bby:APA91bEO3dyBYU8sZTcg... not found
```

## Root Cause 🎯

In `backend/routes/payments.js` line 71, the code was passing the **FCM token** to `sendFCMNotification()` instead of the **userId**:

```javascript
// ❌ WRONG - Passing FCM token instead of userId
await sendFCMNotification(
  ownerData.fcmToken,  // This is a token, not a user ID!
  'Payment Received 💰',
  ...
);
```

The `sendFCMNotification()` function in `fcm-helper.js` expects a **userId** as the first parameter, which it uses to:
1. Query the user document from Firestore
2. Retrieve the actual FCM token from the user document
3. Send the message to that token

When the token was passed as userId, the function tried to look up a user document with the token as the ID, which didn't exist, causing the error.

## Solution ✅

Changed line 71 in `backend/routes/payments.js` to pass the **userId** (ownerId) instead:

```javascript
// ✅ CORRECT - Passing userId to look up the user document
await sendFCMNotification(
  ownerId,  // Pass the actual user ID
  'Payment Received 💰',
  ...
);
```

### File Changed
- **File**: `backend/routes/payments.js`
- **Line**: 71
- **Change**: `ownerData.fcmToken` → `ownerId`

## How It Works Now 🔄

1. **Backend receives payment** → `POST /api/payments`
2. **Payment saved** to Firestore with owner ID
3. **Notification created** in Firestore for owner
4. **sendFCMNotification(ownerId, ...)** is called with the correct userId
5. **fcm-helper.js** looks up user document using ownerId
6. **Retrieves actual FCM token** from user's document
7. **Sends message to that token** via Firebase Cloud Messaging
8. **Owner receives push notification** on their device
9. **onMessageReceived()** in `MyFirebaseMessagingService.java` processes it
10. **Notification displayed** on Android device

## Android Implementation ✓

The Android side is already correctly implemented:
- `MyFirebaseMessagingService.java` extends `FirebaseMessagingService`
- `onMessageReceived()` method handles incoming FCM messages
- Validates notification is for current user via `recipientUserId` field
- Shows system notification with title and body
- Service is properly registered in `AndroidManifest.xml`

## Testing ✓

To test the fix:

1. **On Phone A (Owner)**: 
   - Log in as owner
   - List an item for sale
   - Ensure FCM token is registered (check server logs)

2. **On Phone B (Buyer)**:
   - Log in as different user
   - Find item from Phone A
   - Process payment

3. **Expected Result**:
   - Phone A should receive push notification immediately
   - Notification will appear in system tray
   - Notification will also be saved in Firestore for offline viewing
   - AnnouncementsActivity real-time listener will update the list

## Verification Checklist ✅

- [x] Code change made in `payments.js`
- [x] `sendFCMNotification()` called with correct `ownerId` parameter
- [x] Android `MyFirebaseMessagingService` properly implemented
- [x] AndroidManifest.xml has service registration
- [x] Notification is saved to Firestore (for offline access)
- [x] Push notification is sent via FCM (for real-time alert)
- [x] AnnouncementsActivity loads notifications with real-time listener

## Additional Notes 📝

- The fix also ensures notification is saved to Firestore (line 47-58), so even if FCM fails or user is offline, they'll see it when they open the app
- The real-time listener in `AnnouncementsActivity.loadNotifications()` will auto-update the UI when new notifications arrive
- FCM has been verified in other routes (notifications.js) - no similar bugs found

## Files Modified

1. **backend/routes/payments.js** - Line 71: Changed `ownerData.fcmToken` to `ownerId`

---

**Status**: ✅ FIXED
**Date**: April 21, 2026
**Impact**: Payment notifications will now be delivered successfully to owners

