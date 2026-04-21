# Quick Test Guide: FCM Payment Notification Fix ✅

## What Was Fixed
Owner now receives push notifications when a payment is processed on their item.

**Bug**: Backend was passing FCM token instead of userId to the notification function
**Fix**: Changed line 71 in `backend/routes/payments.js` to use `ownerId` instead of `ownerData.fcmToken`

---

## Quick Test (5 Minutes)

### Setup
- **Phone 1**: Owner's device (logged in as owner)
- **Phone 2**: Buyer's device (logged in as different user)
- **Backend**: Must be running on port 5000

### Test Steps

1. **Phone 1 (Owner)**
   - Open DropSpot app
   - Navigate to "My Items" or post a new item
   - Note: Owner must have internet connection and FCM token registered

2. **Phone 2 (Buyer)**
   - Open DropSpot app
   - Find an item from Phone 1
   - Tap on item and proceed to payment
   - Process payment (mock payment or real test payment)

3. **Check Phone 1 (Owner)**
   - ✅ **Should see push notification** within 2-5 seconds in system tray
   - Notification title: "Payment Received 💰"
   - Notification body: "Payment has been completed for your item"
   - Tap notification → Opens app → Can view in Announcements/Notifications

### What to Look For

**Success Indicators** ✅
```
Backend logs should show:
[PAYMENT] Sending FCM notification to owner: oXMiK5qXbnNuA6pJu6BSNBTLGbr1
[FCM] Found FCM token for user oXMiK5qXbnNuA6pJu6BSNBTLGbr1: dUuM1GOBRKWjbo...
[FCM] Notification sent successfully: projects/dropspotapp-b4dc8/messages/...
[PAYMENT] FCM sent successfully to owner: oXMiK5qXbnNuA6pJu6BSNBTLGbr1
```

Phone 1 should show:
- Push notification in system tray immediately
- Notification appears in Announcements activity
- Real-time update of notification list

**Failure Indicators** ❌ (Old behavior)
```
[FCM] User document not found for dUuM1GOBRKWjbo... (FCM token used as user ID)
[FCM] Error sending FCM notification: User ... not found
```

---

## Technical Details

### File Changed
`backend/routes/payments.js` - Line 71

### Before (❌ Bug)
```javascript
await sendFCMNotification(
  ownerData.fcmToken,  // ❌ WRONG: FCM token, not user ID
  'Payment Received 💰',
  ...
);
```

### After (✅ Fixed)
```javascript
await sendFCMNotification(
  ownerId,  // ✅ CORRECT: User ID to look up user document
  'Payment Received 💰',
  ...
);
```

### Why It Works
1. `sendFCMNotification()` expects userId as first parameter
2. Function looks up user document in Firestore using that userId
3. Extracts actual FCM token from user's `fcmToken` field
4. Sends message to that token via Firebase Cloud Messaging
5. User receives push notification on their device

---

## Backup: View Notifications in App

If push notification doesn't appear (device locked, app in background, etc.):

1. Open DropSpot app on Phone 1
2. Go to "Events & Announcements" tab
3. Check "Notifications" section
4. Should see new notification saved in Firestore (real-time listener)

---

## Expected Behavior Flow

```
Payment Submitted (Phone 2)
    ↓
Backend receives /api/payments request
    ↓
✅ Fixed: sendFCMNotification(ownerId, ...) with correct userId
    ↓
Backend looks up owner user document
    ↓
Backend gets owner's FCM token from document
    ↓
Backend sends FCM message to that token
    ↓
Firebase Cloud Messaging delivers message
    ↓
Owner's device receives message
    ↓
MyFirebaseMessagingService.onMessageReceived() called
    ↓
Validates recipient is current user
    ↓
Shows system notification
    ↓
Owner sees push notification alert ✅
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| No push notification on owner's device | Check if FCM token was registered (look for "FCM Token updated" in app logs) |
| Backend shows "User not found" error | This was the bug - verify you're running the latest code with the fix |
| Notification appears in app but not in system tray | Check notification permissions in Android settings |
| Notification shows but no sound/vibration | Check notification channel settings for "DropSpot Notifications" |

---

## Related Files
- `backend/routes/payments.js` - Payment processing (FIXED)
- `backend/utils/fcm-helper.js` - FCM sending function (correct)
- `app/src/main/java/com/example/dropspot/MyFirebaseMessagingService.java` - Receives notifications (correct)
- `app/src/main/java/com/example/dropspot/AnnouncementsActivity.java` - Displays notifications (correct)

---

**Status**: ✅ Ready to Test
**Last Updated**: April 21, 2026

