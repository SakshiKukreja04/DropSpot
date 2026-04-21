# Quick Testing Guide - Notifications

## Setup
- 2 phones/emulators with DropSpot app installed
- Phone A = Seller (User 1), Phone B = Requester/Buyer (User 2)
- Both must have FCM tokens (check: Firebase Console → users collection → fcmToken field)

## Test 1: Request Notification (Request new item)
**Setup**: Seller has posted an item

**Steps**:
1. On Phone B (Buyer): Open app, find Seller's item
2. Click "Request Item" button
3. Send request

**Expected**:
- Phone B: "Request sent!" message ✅
- Phone A: FCM notification "New Request 📬" appears ✅
- Phone A: Announcements shows notification ✅

**If NOT working**:
- Check backend logs for: `[REQUEST_CREATE]`
- Verify FCM token in Firebase for Seller
- Check Firestore notifications collection

---

## Test 2: Payment Notification (Complete payment)
**Setup**: Seller accepted request from Buyer

**Steps**:
1. On Phone B (Buyer): Go to request, click "Proceed to Payment"
2. Complete mock payment
3. Click "Confirm Payment"

**Expected**:
- Phone B: "Payment completed!" message ✅
- Phone A: FCM notification "Payment Received 💰" appears ✅
- Phone A: Announcements shows notification ✅

**If NOT working**:
- Check backend logs for: `[PAYMENT_SUCCESS]`
- Verify FCM token in Firebase for Owner/Seller
- Check Firestore notifications collection

---

## Test 3: Dispatch Notification (Mark as dispatched)
**Setup**: Payment completed

**Steps**:
1. On Phone A (Seller): Go to "My Posted Items"
2. Find the sold item with pending delivery
3. Click "Dispatch" button
4. Enter tracking number (e.g., "TRACK12345")
5. Click confirm

**Expected**:
- Phone A: "Order dispatched!" message ✅
- Phone B: FCM notification "Order Shipped 🚚" appears ✅
- Phone B: Announcements shows notification ✅

**If NOT working**:
- Check backend logs for: `[DISPATCH]`
- Verify FCM token in Firebase for Buyer
- Verify payment record exists with correct buyerId

---

## Test 4: Delivery Notification (Confirm delivery)
**Setup**: Order dispatched with tracking number

**Steps**:
1. On Phone B (Buyer): Go to "My Purchases" or tracking
2. Find dispatched order with tracking number
3. Click "Confirm Delivery" button
4. Click confirm

**Expected**:
- Phone B: "Order marked as delivered!" message ✅
- Phone A: FCM notification "Delivery Confirmed 📦" appears ✅
- Phone A: Announcements shows notification ✅

**If NOT working**:
- Check backend logs for: `[DELIVERY]`
- Verify FCM token in Firebase for Seller
- Verify payment record has correct sellerId

---

## Debug Commands

### Check Backend Logs
```
Look for tags:
[REQUEST_CREATE] - request created
[REQUEST_STATUS] - request accepted/rejected
[PAYMENT_SUCCESS] - payment notification sent
[DISPATCH] - dispatch notification sent
[DELIVERY] - delivery notification sent
```

### Check Firebase Notifications Collection
```
Go to: Firebase Console → Firestore → notifications
Look for:
- type: "new_request" → userId should be owner
- type: "payment_success" → userId should be owner
- type: "order_dispatched" → userId should be buyer
- type: "order_delivered" → userId should be seller
```

### Check FCM Tokens
```
Go to: Firebase Console → Firestore → users
For each user, verify:
- fcmToken field exists
- fcmToken is not empty
- fcmToken format looks valid
```

---

## Common Issues & Fixes

### Issue: "No notification appears"
**Checks**:
1. Is FCM token saved? Check Firebase users collection
2. Is backend API running? Check console for [DISPATCH] tags
3. Is notification in Firestore? Check notifications collection
4. Did app get installed after backend changes? Reinstall

**Fix**:
```
1. Rebuild backend: npm start
2. Reinstall app: ./gradlew installDebug
3. Clear app cache
4. Test again
```

### Issue: "Wrong user sees notification"
**Cause**: recipientUserId mismatch
**Check**: 
- Verify Firestore notification has correct userId
- Check FCM data includes recipientUserId
- Verify MyFirebaseMessagingService has validation

### Issue: "Notification in Firestore but no FCM"
**Cause**: FCM token not available
**Expected**: App stores to Firestore anyway (user sees when opening app)
**Not an error**: This is intentional fallback behavior

---

## All Tests Passed Criteria ✅

Test 1 ✅ Request notifications work
Test 2 ✅ Payment notifications work
Test 3 ✅ Dispatch notifications work
Test 4 ✅ Delivery notifications work
All notifications visible in Announcements section

