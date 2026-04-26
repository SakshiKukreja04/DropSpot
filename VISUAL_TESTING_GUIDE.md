# Visual Testing Guide - Step by Step

## Screen 1: Seller Posts Item

```
┌─────────────────────────────────┐
│ My Posts                        │
├─────────────────────────────────┤
│ [Refresh ↓]                     │
│                                 │
│ ┌──────────────────────┐        │
│ │  iPhone 13 (Image)   │        │
│ ├──────────────────────┤        │
│ │ Title: Apple iPhone  │        │
│ │ Category: Electronics│        │
│ │ Status: Active ✓     │        │
│ │                      │        │
│ │ [Click to detail]    │        │
│ └──────────────────────┘        │
│                                 │
│ (MORE POSTS...)                 │
└─────────────────────────────────┘

STATUS IN LOGS:
STATUS_CHECK: Post: Apple iPhone - Found 0 payments
STATUS_CHECK: No payments found - Checking requests...
```

---

## Screen 2: Buyer Completes Payment

```
┌─────────────────────────────────┐
│ Payment                         │
├─────────────────────────────────┤
│ Item: Apple iPhone              │
│ Price: ₹10,000                  │
│                                 │
│ Card Number: [4111....]         │
│ Expiry: [12/25]                 │
│ CVV: [123]                      │
│ Address: [123 Main St]          │
│                                 │
│ [Pay Now Button]                │
│                                 │
│ ✓ Payment Completed ✅          │
└─────────────────────────────────┘

LOGCAT SHOWS:
PAYMENT_DEBUG: Creating Payment Record
PAYMENT_DEBUG: Payment ID: PAY_1234567890
PAYMENT_DEBUG: Post ID: post_abc123
PAYMENT_DEBUG: Requester ID: buyer_id
PAYMENT_DEBUG: Owner ID: seller_id
PAYMENT_DEBUG: Status: paid
PAYMENT_DEBUG: ✅ Payment saved to Firestore!

FIRESTORE NOW HAS:
Collection: payments
Document ID: PAY_1234567890
Fields:
  ├─ postId: "post_abc123"
  ├─ ownerId: "seller_id"
  ├─ requesterId: "buyer_id"
  ├─ status: "paid" ← CRITICAL!
  ├─ paymentId: "PAY_1234567890"
  └─ amount: 10000
```

---

## Screen 3: Seller Checks "My Posts" (DISPATCH BUTTON SHOULD APPEAR HERE)

```
┌─────────────────────────────────┐
│ My Posts                        │
├─────────────────────────────────┤
│ [Refresh ↓]  ← PULL DOWN!      │
│                                 │
│ ┌──────────────────────┐        │
│ │  iPhone 13 (Image)   │        │
│ ├──────────────────────┤        │
│ │ Title: Apple iPhone  │        │
│ │ Category: Electronics│        │
│ │                      │        │
│ │ 💰 Paid             │        │ ← ORDER STATUS SHOWS
│ │ Ready to dispatch    │        │
│ │                      │        │
│ │ [Dispatch Order 🚚]  │ ← BUTTON APPEARS!
│ │                      │        │
│ └──────────────────────┘        │
│                                 │
│ (MORE POSTS...)                 │
└─────────────────────────────────┘

LOGCAT SHOWS:
STATUS_CHECK: ===============================================
STATUS_CHECK: Binding Post: Apple iPhone
STATUS_CHECK: Post ID: post_abc123
STATUS_CHECK: Post Owner ID: seller_id
STATUS_CHECK: Current User ID: seller_id
STATUS_CHECK: Is Owner? true ← IMPORTANT!
STATUS_CHECK: ===============================================
STATUS_CHECK: Payment query returned 1 documents
STATUS_CHECK: Post: Apple iPhone - Found 1 payments
STATUS_CHECK: Post: Apple iPhone - Payment Status: [paid]
STATUS_CHECK: BuyerId: buyer_id, PaymentId: PAY_1234567890
STATUS_CHECK: Normalized Status: [paid]
STATUS_CHECK: ✅ PAID - Showing Dispatch button for: Apple iPhone
```

**✅ If you see this: DISPATCH BUTTON SHOULD BE VISIBLE!**

---

## Screen 4: Seller Clicks Dispatch Button

```
┌─────────────────────────────────┐
│ Dispatch Order                  │
├─────────────────────────────────┤
│ Provide shipper details and     │
│ contact number                  │
│                                 │
│ [Enter shipper name]            │
│ Test Shipper Co.                │
│                                 │
│ [Phone/Tracking number]         │
│ 9876543210                      │
│                                 │
│       [Confirm Dispatch]        │
│       [    Cancel    ]          │
└─────────────────────────────────┘

THEN:
Toast: "Order dispatched! 🚀\nShipper: Test Shipper Co."

FIRESTORE UPDATES:
payments/PAY_1234567890:
  ├─ status: "dispatched" ← CHANGED!
  ├─ shipperName: "Test Shipper Co."
  ├─ trackingNumber: "9876543210"

requests/[request_id]:
  ├─ status: "dispatched" ← SYNCED!
  ├─ shipperName: "Test Shipper Co."
  ├─ trackingNumber: "9876543210"
```

---

## Screen 5: Seller Sees Updated Post Status

```
┌─────────────────────────────────┐
│ My Posts                        │
├─────────────────────────────────┤
│ [Refresh ↓]                     │
│                                 │
│ ┌──────────────────────┐        │
│ │  iPhone 13 (Image)   │        │
│ ├──────────────────────┤        │
│ │ Title: Apple iPhone  │        │
│ │ Category: Electronics│        │
│ │                      │        │
│ │ 📦 Order Dispatched! │        │
│ │ Shipper:             │        │
│ │   Test Shipper Co.   │        │
│ │ Tracking:            │        │
│ │   9876543210         │        │
│ │                      │        │
│ │ (Dispatch button     │        │
│ │  now hidden)         │        │
│ │                      │        │
│ └──────────────────────┘        │
│                                 │
│ (MORE POSTS...)                 │
└─────────────────────────────────┘

LOGCAT SHOWS:
STATUS_CHECK: ✅ DISPATCHED - Hiding Dispatch button for: Apple iPhone
```

---

## Screen 6: Buyer Sees Confirm Delivery Button (SHOULD APPEAR HERE)

```
┌─────────────────────────────────┐
│ My Requests                     │
├─────────────────────────────────┤
│ [Refresh ↓]  ← PULL DOWN!      │
│                                 │
│ ┌──────────────────────┐        │
│ │ Apple iPhone         │        │
│ │ Requested on: 2024.. │        │
│ │ Status: [DISPATCHED] │        │
│ ├──────────────────────┤        │
│ │ Message: Interested. │        │
│ │                      │        │
│ │ 📦 Order Dispatched! │        │
│ │                      │        │
│ │ Shipper:             │        │
│ │   Test Shipper Co.   │        │
│ │ Delivery Contact:    │        │
│ │   9876543210         │        │
│ │                      │        │
│ │ [Confirm Delivery ✅]│ ← BUTTON!
│ │                      │        │
│ └──────────────────────┘        │
│                                 │
│ (MORE REQUESTS...)              │
└─────────────────────────────────┘
```

**✅ If you see this: CONFIRM DELIVERY BUTTON SHOULD BE VISIBLE!**

---

## Screen 7: Buyer Clicks Confirm Delivery

```
Toast: "Delivery confirmed! ✅ Order Completed."

FIRESTORE UPDATES:
payments/PAY_1234567890:
  ├─ status: "delivered" ← FINAL!

requests/[request_id]:
  ├─ status: "completed" ← COMPLETED!

posts/post_abc123:
  ├─ isActive: false ← POST CLOSED!
  ├─ status: "sold"
```

---

## Screen 8: Order Complete

```
┌─────────────────────────────────┐
│ My Requests                     │
├─────────────────────────────────┤
│ [Refresh ↓]                     │
│                                 │
│ ┌──────────────────────┐        │
│ │ Apple iPhone         │        │
│ │ Requested on: 2024.. │        │
│ │ Status: [COMPLETED]  │        │
│ ├──────────────────────┤        │
│ │ Message: Interested. │        │
│ │                      │        │
│ │ ✅ Order Completed   │        │
│ │ Successfully!        │        │
│ │                      │        │
│ │ (Confirm button      │        │
│ │  now hidden)         │        │
│ │                      │        │
│ └──────────────────────┘        │
│                                 │
│ (MORE REQUESTS...)              │
└─────────────────────────────────┘
```

---

## Troubleshooting Visual Guide

### ❌ Problem: Button Doesn't Appear on Seller's Screen

#### Check 1: Are You the Owner?

```
Logcat shows:
STATUS_CHECK: Post Owner ID: seller_id
STATUS_CHECK: Current User ID: buyer_id
STATUS_CHECK: Is Owner? false

FIX: Log out, log in as the SELLER (who posted the item)
```

#### Check 2: Is Payment Created?

```
Logcat shows:
STATUS_CHECK: Payment query returned 0 documents

FIX: Check Firestore Console
  1. Go to Firebase Console
  2. Firestore Database
  3. Collection: payments
  4. Should have a document with your postId
  
If not there:
  - Check PAYMENT_DEBUG logs
  - Verify payment completed successfully
  - Check backend /payments endpoint working
```

#### Check 3: Is Status "paid"?

```
Logcat shows:
STATUS_CHECK: Payment Status: [null]
or
STATUS_CHECK: Payment Status: [something_else]

FIX: Go to Firebase Console
  1. Firestore → payments
  2. Find your payment
  3. Check status field = "paid" (lowercase)
  4. If missing, manually add: status: "paid"
```

#### Check 4: Did You Refresh?

```
Solution:
  1. Go to "My Posts" tab
  2. PULL DOWN (if RefreshLayout exists)
  3. Or: Kill app → Restart
  4. Or: Go back → Reopen "My Posts"
```

---

### ❌ Problem: Button Doesn't Appear on Buyer's Screen

#### Confirm Delivery Button Not Showing?

```
Logcat shows:
STATUS_CHECK: Payment Status: [accepted]
or other status not "dispatched"

FIX: Seller must complete dispatch first
  1. Seller goes to "My Posts"
  2. Seller clicks "Dispatch Order 🚚"
  3. Seller enters shipper details
  4. Dispatch status changes to "dispatched"
  
Then buyer can see "Confirm Delivery" button
```

---

## Success Indicators

### ✅ Dispatch Button Working
- [ ] Logcat shows: `Is Owner? true`
- [ ] Logcat shows: `✅ PAID - Showing Dispatch button`
- [ ] Button visible on "My Posts"
- [ ] Button clickable
- [ ] Dialog appears when clicked
- [ ] Can enter shipper details
- [ ] Dispatch succeeds

### ✅ Confirm Delivery Button Working
- [ ] Dispatch button was clicked and completed
- [ ] Logcat shows: `Payment Status: [dispatched]`
- [ ] Button visible on "My Requests"
- [ ] Button clickable
- [ ] Confirm delivery succeeds
- [ ] Status changes to "Completed"

---

## Testing Checklist

```
BEFORE TESTING:
☐ Rebuild project: ./gradlew clean build
☐ Deploy to device
☐ Open Logcat in Android Studio
☐ Have 2 test accounts ready

DURING TESTING:
☐ Watch Logcat for PAYMENT_DEBUG and STATUS_CHECK logs
☐ Verify each screen matches expected output
☐ Check Firebase Firestore for data updates
☐ Pull down to refresh after each major action
☐ Take screenshots if issues occur

AFTER TESTING:
☐ Complete order flow end-to-end
☐ Verify all status changes
☐ Confirm both buttons appeared
☐ Verify notifications sent (if FCM configured)
```

---

Good luck! Follow the screens and logs step by step, and the buttons should work! 🚀

