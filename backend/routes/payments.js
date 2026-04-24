import express from 'express';
import { db } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';
import { sendFCMNotification } from '../utils/fcm-helper.js';

const router = express.Router();

/**
 * POST /payments - Save payment and notify owner
 */
router.post('/', async (req, res, next) => {
  try {
    const { paymentId, postId, requesterId, ownerId, amount, status } = req.body;

    console.log(`[PAYMENT] Request received - paymentId: ${paymentId}, requesterId: ${requesterId}, ownerId: ${ownerId}`);

    if (!paymentId || !postId || !ownerId || !amount) {
      return res.status(400).json(errorResponse('Missing payment details', 'VALIDATION_ERROR', 400));
    }

    const timestamp = getCurrentTimestamp();

    // 1. Save payment in Firestore
    const paymentData = {
      paymentId,
      postId,
      requesterId,
      ownerId,
      amount,
      status,
      createdAt: timestamp
    };

    await db.collection('payments').doc(paymentId).set(paymentData);
    console.log(`[PAYMENT] Payment saved to Firestore: ${paymentId}`);

    // 2. Update post status to completed
    await db.collection('posts').doc(postId).update({
      paymentStatus: 'completed',
      status: 'sold',
      updatedAt: timestamp
    });
    console.log(`[PAYMENT] Post status updated to sold: ${postId}`);

    // 3. Create notification record in database (OWNER gets this notification)
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: ownerId, // CRITICAL: Notification goes to OWNER, not requester
      type: 'PAYMENT_SUCCESS', // Match frontend enum
      title: 'Payment Received 💰',
      message: 'Payment has been completed for your item',
      relatedId: paymentId,
      relatedType: 'payment',
      read: false,
      createdAt: timestamp,
      timestamp: new Date() // Add timestamp field for sorting
    });
    console.log(`[PAYMENT] Notification saved to Firestore - userId: ${ownerId}, notificationId: ${notificationId}`);

    // 4. Fetch owner's FCM token and send real-time push notification
    console.log(`[PAYMENT] 📢 Attempting to send FCM notification to owner: ${ownerId}`);
    try {
      const ownerDoc = await db.collection('users').doc(ownerId).get();
      if (!ownerDoc.exists) {
        console.error(`[PAYMENT] ❌ Owner document not found in Firestore: ${ownerId}`);
      } else {
        const ownerData = ownerDoc.data();
        const hasFCMToken = !!ownerData.fcmToken;
        console.log(`[PAYMENT] ✅ Owner document found - has FCM token: ${hasFCMToken}`);

        if (ownerData.fcmToken && ownerData.fcmToken.trim() !== '') {
          try {
            console.log(`[PAYMENT] 📤 Calling sendFCMNotification for owner ${ownerId}`);
            await sendFCMNotification(
              ownerId,
              'Payment Received 💰',
              'Payment has been completed for your item',
              {
                type: 'PAYMENT_SUCCESS',
                postId,
                paymentId,
                recipientUserId: ownerId
              }
            );
            console.log(`[PAYMENT] ✅✅ FCM notification sent successfully to owner: ${ownerId}`);
          } catch (fcmError) {
            console.error(`[PAYMENT] ❌ FCM sending failed for owner ${ownerId}:`, fcmError.message);
            console.error(`[PAYMENT] Full FCM error:`, fcmError);
            // Don't fail the payment - notification is already saved in DB
          }
        } else {
           console.warn(`[PAYMENT] ⚠️  Owner ${ownerId} has no valid FCM token - push not sent`);
           console.log(`[PAYMENT] ⏳ Queuing notification for later delivery when owner logs in`);

           // Queue notification to be sent later
           try {
             const queueId = generateId();
             await db.collection('pendingNotifications').doc(queueId).set({
               queueId,
               userId: ownerId,
               title: 'Payment Received 💰',
               body: 'Payment has been completed for your item',
               data: {
                 type: 'PAYMENT_SUCCESS',
                 postId,
                 paymentId,
                 recipientUserId: ownerId
               },
               status: 'pending',
               createdAt: timestamp,
               retryCount: 0,
               maxRetries: 5
             });
             console.log(`[PAYMENT] ✅ Notification queued for ${ownerId} - Queue ID: ${queueId}`);
           } catch (queueError) {
             console.error(`[PAYMENT] ❌ Error queuing notification:`, queueError.message);
           }
        }
      }
    } catch (pushError) {
      console.error('[PAYMENT] ❌ Unexpected error in FCM section:', pushError.message);
      console.error('[PAYMENT] This error will not fail the payment', pushError);
      // Don't fail the entire payment request
    }

    console.log(`[PAYMENT] SUCCESS: Payment processed - ${paymentId}, notification sent to owner: ${ownerId}`);
    res.status(201).json(successResponse(paymentData, 'Payment processed successfully'));
  } catch (error) {
    console.error('[PAYMENT] ERROR:', error);
    next(error);
  }
});

export default router;
