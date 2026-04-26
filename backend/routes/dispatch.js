import express from 'express';
import { db } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';
import { sendPushNotification } from '../utils/notifications.js';

const router = express.Router();

/**
 * POST /dispatch/mark-dispatched
 * Mark order as dispatched and notify buyer
 *
 * Body: { paymentId, buyerId, sellerId, itemTitle, trackingNumber, shipperName }
 * NOTE: trackingNumber is used to store the phone number of the delivery person.
 */
router.post('/mark-dispatched', async (req, res, next) => {
  try {
    const { paymentId, buyerId, sellerId, itemTitle, trackingNumber, shipperName } = req.body;
    const userId = req.user.uid;

    // Validate request
    if (!paymentId || !buyerId || !sellerId || !itemTitle || !trackingNumber) {
      return res.status(400).json(errorResponse('Missing dispatch details', 'VALIDATION_ERROR', 400));
    }

    // Authorization: Only seller can mark their own orders as dispatched
    if (userId !== sellerId) {
      return res.status(403).json(errorResponse('Unauthorized', 'FORBIDDEN', 403));
    }

    console.log(`[DISPATCH] Marking order as dispatched - Payment: ${paymentId}, Buyer: ${buyerId}, Seller: ${sellerId}, Shipper: ${shipperName || 'N/A'}`);

    const timestamp = getCurrentTimestamp();

    // 1. Update payment/order status to DISPATCHED
    const paymentDoc = await db.collection('payments').doc(paymentId).get();
    if (!paymentDoc.exists) {
      console.error(`[DISPATCH] Payment ${paymentId} not found`);
      return res.status(404).json(errorResponse('Payment not found', 'NOT_FOUND', 404));
    }

    const batch = db.batch();

    // Update payment document
    batch.update(db.collection('payments').doc(paymentId), {
      status: 'dispatched',
      trackingNumber: trackingNumber,
      shipperName: shipperName || 'N/A',
      dispatchedAt: timestamp,
      updatedAt: timestamp
    });

    // 2. Sync status to Requests collection so buyer sees it in "My Requests"
    let requestDoc = null;

    const requestsSnapshot = await db.collection('requests')
      .where('paymentId', '==', paymentId)
      .limit(1)
      .get();

    if (!requestsSnapshot.empty) {
      requestDoc = requestsSnapshot.docs[0];
    } else {
      console.log("⚠️ No request found via paymentId, trying fallback...");

      const fallbackSnapshot = await db.collection('requests')
        .where('requesterId', '==', buyerId)
        .where('postOwnerId', '==', sellerId)
        .limit(1)
        .get();

      if (!fallbackSnapshot.empty) {
        requestDoc = fallbackSnapshot.docs[0];
      }
    }

    if (requestDoc) {
      batch.update(requestDoc.ref, {
        status: 'dispatched',
        trackingNumber: trackingNumber,
        shipperName: shipperName || 'N/A',
        updatedAt: timestamp
      });
    }

    await batch.commit();

    // 3. Create notification record for buyer
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: buyerId,
      message: `Your item "${itemTitle}" has been dispatched.\n\nShipper: ${shipperName || 'N/A'}\nTracking: ${trackingNumber}`,
      type: 'ORDER_DISPATCHED',
      shipperName: shipperName || 'N/A',
      title: 'Order Dispatched 🚚',
      trackingNumber: trackingNumber,
      itemTitle: itemTitle,
      relatedId: paymentId,
      relatedType: 'payment',
      read: false,
      createdAt: timestamp,
      timestamp: new Date()
    });

    // 4. Send FCM Push Notification
    try {
      const buyerDoc = await db.collection('users').doc(buyerId).get();
      if (buyerDoc.exists) {
        const buyerData = buyerDoc.data();
        if (buyerData.fcmToken) {
          await sendPushNotification(
            buyerData.fcmToken,
            'Order Dispatched 🚚',
            `Your item "${itemTitle}" has been dispatched by ${shipperName || 'delivery partner'}.\nTracking: ${trackingNumber}`,
            {
              shipperName: shipperName || 'N/A',
              type: 'order_dispatched',
              paymentId,
              trackingNumber,
              itemTitle,
              recipientUserId: buyerId
            }
          );
        }
      }
    } catch (pushError) {
      console.error('[FCM] Error sending dispatch notification:', pushError);
    }

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

    const batch = db.batch();

    batch.update(db.collection('payments').doc(paymentId), {
      status: 'delivered',
      deliveredAt: timestamp,
      updatedAt: timestamp
    });

    // 2. Sync status to Requests collection
    let requestDoc = null;

    const requestsSnapshot = await db.collection('requests')
      .where('paymentId', '==', paymentId)
      .limit(1)
      .get();

    if (!requestsSnapshot.empty) {
      requestDoc = requestsSnapshot.docs[0];
    } else {
      console.log("⚠️ No request found via paymentId, trying fallback...");

      const fallbackSnapshot = await db.collection('requests')
        .where('requesterId', '==', buyerId)
        .where('postOwnerId', '==', sellerId)
        .limit(1)
        .get();

      if (!fallbackSnapshot.empty) {
        requestDoc = fallbackSnapshot.docs[0];
      }
    }

    if (requestDoc) {
      batch.update(requestDoc.ref, {
        status: 'completed',
        updatedAt: timestamp
      });
    }

    await batch.commit();

    // 3. Close the post after successful delivery
    const paymentDocAfterDelivery = await db.collection('payments').doc(paymentId).get();
    if (paymentDocAfterDelivery.exists) {
      const paymentData = paymentDocAfterDelivery.data();
      await db.collection('posts').doc(paymentData.postId).update({
        status: 'sold',
        isActive: false,
        updatedAt: timestamp
      });
    }

    // 4. Create notification record for seller
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: sellerId,
      type: 'DELIVERY_CONFIRMED',
      title: 'Delivery Confirmed 📦',
      message: `Buyer confirmed delivery of "${itemTitle}". Order is complete.`,
      itemTitle: itemTitle,
      relatedId: paymentId,
      relatedType: 'payment',
      read: false,
      createdAt: timestamp,
      timestamp: new Date()
    });

    // 5. Send FCM Push Notification
    try {
      const sellerDoc = await db.collection('users').doc(sellerId).get();
      if (sellerDoc.exists) {
        const sellerData = sellerDoc.data();
        if (sellerData.fcmToken) {
          await sendPushNotification(
            sellerData.fcmToken,
            'Delivery Confirmed 📦',
            `Buyer confirmed delivery of "${itemTitle}"`,
            {
              type: 'order_delivered',
              paymentId,
              itemTitle,
              recipientUserId: sellerId
            }
          );
        }
      }
    } catch (pushError) {
      console.error('[FCM] Error sending delivery notification:', pushError);
    }

    res.status(200).json(successResponse({ status: 'delivered' }, 'Order marked as delivered successfully'));
  } catch (error) {
    console.error('[DELIVERY] ERROR:', error);
    next(error);
  }
});

export default router;
