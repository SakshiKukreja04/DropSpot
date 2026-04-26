/**
 * PAYMENT FLOW VERIFICATION
 *
 * This documents the complete flow for payment → dispatch → delivery
 * with proper FCM notifications at each step
 */

/**
 * STEP 1: USER LOGS IN
 * ==================
 * Android: Login with Firebase
 * Android: Get FCM token via FirebaseMessaging.getInstance().getToken()
 * Android: Send token to backend via PUT /api/users/:userId
 *
 * Backend: Stores fcmToken in user document
 * Example:
 *   PUT /api/users/buyer123
 *   {
 *     fcmToken: "dUuM1GOBRKWjbotzrU7b..."
 *   }
 */

/**
 * STEP 2: REQUESTER (BUYER) MAKES PAYMENT
 * ======================================
 * Android PaymentActivity.java:
 *   1. User clicks "Pay Now"
 *   2. Validates card details & delivery address
 *   3. Simulates payment (2-second delay)
 *   4. Calls: POST /api/payments
 *      {
 *        paymentId: "PAY_1776713569324_5483",
 *        postId: "post123",
 *        requesterId: "buyer123",
 *        ownerId: "seller456",
 *        amount: 200.0,
 *        status: "success"
 *      }
 *
 * Backend: payments.js handles:
 *   ✅ Save payment record
 *   ✅ Update post status to "sold"
 *   ✅ Create notification record (for OWNER, not requester)
 *   ✅ Send FCM to owner (if online)
 *   ✅ Queue notification (if owner offline)
 *
 * Owner receives FCM notification:
 *   Title: "Payment Received 💰"
 *   Body: "Your item (test5) has been paid for!"
 */

/**
 * STEP 3: OWNER MARKS ITEM AS DISPATCHED
 * ====================================
 * Android PostedItemsAdapter/MyPostsFragment:
 *   1. Owner clicks "Dispatch" button on paid item
 *   2. Calls: POST /api/dispatch/mark-dispatched
 *      {
 *        paymentId: "PAY_1776713569324_5483",
 *        buyerId: "buyer123",
 *        sellerId: "seller456",
 *        itemTitle: "test5",
 *        trackingNumber: "TRK123456"
 *      }
 *
 * Backend: dispatch.js handles:
 *   ✅ Update payment status to "dispatched"
 *   ✅ Create notification record (for BUYER, not owner)
 *   ✅ Send FCM to buyer (if online)
 *
 * Buyer receives FCM notification:
 *   Title: "Order Shipped 🚚"
 *   Body: "Your item has been dispatched. Tracking: TRK123456"
 */

/**
 * STEP 4: BUYER CONFIRMS DELIVERY
 * ==============================
 * Android MyRequestsAdapter/MyRequestsFragment:
 *   1. Buyer clicks "Confirm Delivery" button
 *   2. Calls: POST /api/dispatch/mark-delivered
 *      {
 *        paymentId: "PAY_1776713569324_5483",
 *        buyerId: "buyer123",
 *        sellerId: "seller456",
 *        itemTitle: "test5"
 *      }
 *
 * Backend: dispatch.js handles:
 *   ✅ Update payment status to "delivered"
 *   ✅ Create notification record (for SELLER, not buyer)
 *   ✅ Send FCM to seller (if online)
 *
 * Seller receives FCM notification:
 *   Title: "Delivery Confirmed 📦"
 *   Body: "Buyer confirmed delivery of test5"
 */

/**
 * CRITICAL RULES FOR FCM NOTIFICATIONS
 * ==================================
 * 1. PAYMENT SUCCESS:
 *    - Receiver: OWNER (seller)
 *    - Sender: BUYER (requester)
 *    - Do NOT send to requester/buyer
 *
 * 2. ORDER DISPATCHED:
 *    - Receiver: BUYER (requester)
 *    - Sender: OWNER (seller)
 *    - Do NOT send to owner/seller
 *
 * 3. DELIVERY CONFIRMED:
 *    - Receiver: SELLER (owner)
 *    - Sender: BUYER (requester)
 *    - Do NOT send to buyer/requester
 *
 * 4. FCM Token Requirements:
 *    - Must be stored in Firestore at user.fcmToken
 *    - Updated on every login
 *    - Must match currently logged-in device
 *    - If not available, queue in pendingNotifications collection
 *    - Process pending on next login via /users/:userId/process-pending-notifications
 */

/**
 * NOTIFICATIONS FLOW IN ANNOUNCEMENTS/NOTIFICATIONS SECTION
 * ======================================================
 * When user opens Announcements:
 *   1. Fetch notifications: GET /api/notifications/:userId
 *   2. Sort by timestamp (latest first)
 *   3. Display all notification types:
 *      - PAYMENT_SUCCESS: "New Order Received 🎉"
 *      - ORDER_DISPATCHED: "Order Shipped 🚚"
 *      - DELIVERY_CONFIRMED: "Delivery Confirmed 📦"
 *      - EVENT_ATTEND: "User joined your event"
 *      - Any other type
 *
 * Only user's own notifications are shown
 * (verified via userId in notification record)
 */

export default {
  documentation: "Payment flow verification and FCM notification routing"
};

