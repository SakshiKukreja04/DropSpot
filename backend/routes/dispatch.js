import express from 'express';
import { db } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';
import { sendPushNotification } from '../utils/notifications.js';

const router = express.Router();

/**
 * POST /dispatch/mark-dispatched
 * Mark order as dispatched and notify buyer
 *
 * Body: { paymentId, buyerId, sellerId, itemTitle, trackingNumber }
 */
router.post('/mark-dispatched', async (req, res, next) => {
  try {
    const { paymentId, buyerId, sellerId, itemTitle, trackingNumber } = req.body;
    const userId = req.user.uid;

    // Validate request
    if (!paymentId || !buyerId || !sellerId || !itemTitle || !trackingNumber) {
      return res.status(400).json(errorResponse('Missing dispatch details', 'VALIDATION_ERROR', 400));
    }

    // Authorization: Only seller can mark their own orders as dispatched
    if (userId !== sellerId) {
      return res.status(403).json(errorResponse('Only seller can dispatch this order', 'FORBIDDEN', 403));
    }

    console.log(`[DISPATCH] Marking order as dispatched - Payment: ${paymentId}, Buyer: ${buyerId}, Seller: ${sellerId}`);

    const timestamp = getCurrentTimestamp();

    // 1. Update payment/order status to DISPATCHED
    const paymentDoc = await db.collection('payments').doc(paymentId).get();
    if (!paymentDoc.exists) {
      console.error(`[DISPATCH] Payment ${paymentId} not found`);
      return res.status(404).json(errorResponse('Payment not found', 'NOT_FOUND', 404));
    }

    await db.collection('payments').doc(paymentId).update({
      status: 'dispatched',
      trackingNumber: trackingNumber,
      dispatchedAt: timestamp,
      updatedAt: timestamp
    });

    // 2. Create notification record in database (BUYER gets this notification)
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: buyerId, // CRITICAL: Notification goes to BUYER, not seller
      type: 'ORDER_DISPATCHED', // Match frontend enum
      title: 'Order Shipped 🚚',
      message: `Your item "${itemTitle}" has been dispatched by the seller`,
      trackingNumber: trackingNumber,
      itemTitle: itemTitle,
      relatedId: paymentId,
      relatedType: 'payment',
      read: false,
      createdAt: timestamp,
      timestamp: new Date() // Add timestamp field for sorting
    });
    console.log(`[DISPATCH] Notification saved - userId: ${buyerId}, notificationId: ${notificationId}`);

    // 3. Fetch buyer's FCM token and send real-time push notification
    try {
      const buyerDoc = await db.collection('users').doc(buyerId).get();
      if (buyerDoc.exists) {
        const buyerData = buyerDoc.data();
        if (buyerData.fcmToken) {
          console.log(`[DISPATCH] Sending dispatch notification to buyer: ${buyerId}`);
          await sendPushNotification(
            buyerData.fcmToken,
            'Order Shipped 🚚',
            `Your item "${itemTitle}" has been dispatched\nTracking: ${trackingNumber}`,
            {
              type: 'order_dispatched',
              paymentId,
              trackingNumber,
              itemTitle,
              recipientUserId: buyerId // CRITICAL: Frontend validates this matches current user
            }
          );
        }
      }
    } catch (pushError) {
      console.error('[FCM] Error sending dispatch notification:', pushError);
    }

    console.log(`[DISPATCH] SUCCESS: Order ${paymentId} marked as dispatched`);
    res.status(200).json(successResponse({ status: 'dispatched', trackingNumber }, 'Order dispatched successfully'));
  } catch (error) {
    console.error('[DISPATCH] ERROR:', error);
    next(error);
  }
});

/**
 * POST /dispatch/mark-delivered
 * Mark order as delivered and notify seller
 *
 * Body: { paymentId, buyerId, sellerId, itemTitle }
 */
router.post('/mark-delivered', async (req, res, next) => {
  try {
    const { paymentId, buyerId, sellerId, itemTitle } = req.body;
    const userId = req.user.uid;

    // Validate request
    if (!paymentId || !buyerId || !sellerId || !itemTitle) {
      return res.status(400).json(errorResponse('Missing delivery details', 'VALIDATION_ERROR', 400));
    }

    // Authorization: Only buyer can confirm delivery
    if (userId !== buyerId) {
      return res.status(403).json(errorResponse('Only buyer can confirm delivery', 'FORBIDDEN', 403));
    }

    console.log(`[DELIVERY] Marking order as delivered - Payment: ${paymentId}, Buyer: ${buyerId}, Seller: ${sellerId}`);

    const timestamp = getCurrentTimestamp();

    // 1. Update payment/order status to DELIVERED
    const paymentDoc = await db.collection('payments').doc(paymentId).get();
    if (!paymentDoc.exists) {
      console.error(`[DELIVERY] Payment ${paymentId} not found`);
      return res.status(404).json(errorResponse('Payment not found', 'NOT_FOUND', 404));
    }

    await db.collection('payments').doc(paymentId).update({
      status: 'delivered',
      deliveredAt: timestamp,
      updatedAt: timestamp
    });

    // 2. Create notification record in database (SELLER gets this notification)
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: sellerId, // CRITICAL: Notification goes to SELLER, not buyer
      type: 'DELIVERY_CONFIRMED', // Match frontend enum
      title: 'Delivery Confirmed 📦',
      message: `Buyer confirmed delivery of "${itemTitle}"`,
      itemTitle: itemTitle,
      relatedId: paymentId,
      relatedType: 'payment',
      read: false,
      createdAt: timestamp,
      timestamp: new Date() // Add timestamp field for sorting
    });
    console.log(`[DELIVERY] Notification saved - userId: ${sellerId}, notificationId: ${notificationId}`);

    // 3. Fetch seller's FCM token and send real-time push notification
    try {
      const sellerDoc = await db.collection('users').doc(sellerId).get();
      if (sellerDoc.exists) {
        const sellerData = sellerDoc.data();
        if (sellerData.fcmToken) {
          console.log(`[DELIVERY] Sending delivery notification to seller: ${sellerId}`);
          await sendPushNotification(
            sellerData.fcmToken,
            'Delivery Confirmed 📦',
            `Buyer confirmed delivery of "${itemTitle}"`,
            {
              type: 'order_delivered',
              paymentId,
              itemTitle,
              recipientUserId: sellerId // CRITICAL: Frontend validates this matches current user
            }
          );
        }
      }
    } catch (pushError) {
      console.error('[FCM] Error sending delivery notification:', pushError);
    }

    console.log(`[DELIVERY] SUCCESS: Order ${paymentId} marked as delivered`);
    res.status(200).json(successResponse({ status: 'delivered' }, 'Order marked as delivered successfully'));
  } catch (error) {
    console.error('[DELIVERY] ERROR:', error);
    next(error);
  }
});

export default router;



