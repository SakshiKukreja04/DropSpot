import express from 'express';
import { db } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';
import { sendFCMNotification } from '../utils/fcm-helper.js';

const router = express.Router();

/**
 * POST /payments - Save payment and notify owner + requester
 * Ensures the transition to "paid" status is correctly handled for both owner and requester.
 */
router.post('/', async (req, res, next) => {
  try {
    const { paymentId, postId, requesterId, ownerId, amount } = req.body;

    console.log(`[PAYMENT] Processing success for paymentId: ${paymentId}, postId: ${postId}`);

    if (!paymentId || !postId || !ownerId || !amount) {
      return res.status(400).json(errorResponse('Missing payment details', 'VALIDATION_ERROR', 400));
    }

    const timestamp = getCurrentTimestamp();

    // 1. Save payment in Firestore with status "paid"
    const paymentData = {
      paymentId,
      postId,
      requesterId,
      ownerId,
      amount,
      status: 'paid', // BACKEND REQUIREMENT: status = "paid"
      createdAt: timestamp,
      updatedAt: timestamp
    };

    await db.collection('payments').doc(paymentId).set(paymentData);

    // 2. Update post status
    await db.collection('posts').doc(postId).update({
      paymentStatus: 'paid',
      updatedAt: timestamp
    });

    // 3. Update the specific Request document to 'paid'
    // This status is what triggers the Dispatch button on the owner side
    const requestsSnapshot = await db.collection('requests')
      .where('postId', '==', postId)
      .where('requesterId', '==', requesterId)
      .where('status', '==', 'accepted')
      .limit(1)
      .get();

    if (!requestsSnapshot.empty) {
      const requestRef = requestsSnapshot.docs[0].ref;
      await requestRef.update({
        status: 'paid', // Exact match required by frontend
        paymentId: paymentId,
        paymentStatus: 'success',
        updatedAt: timestamp
      });
      console.log(`[PAYMENT] Request ${requestRef.id} status updated to: "paid"`);
    } else {
      // Fallback search without requesterId if perfect match fails
      const fallbackSnapshot = await db.collection('requests')
        .where('postId', '==', postId)
        .where('status', '==', 'accepted')
        .limit(1)
        .get();

      if (!fallbackSnapshot.empty) {
        await fallbackSnapshot.docs[0].ref.update({
          status: 'paid',
          paymentId: paymentId,
          paymentStatus: 'success',
          updatedAt: timestamp
        });
        console.log(`[PAYMENT] Fallback: Request ${fallbackSnapshot.docs[0].id} updated to "paid"`);
      }
    }

    // 4. Create Notification for Owner
    try {
      await db.collection('notifications').add({
        userId: ownerId,
        type: 'PAYMENT_RECEIVED',
        title: 'Payment Received 💰',
        message: `Your item has been paid for. Please dispatch the order.`,
        postId: postId,
        paymentId: paymentId,
        read: false,
        createdAt: timestamp,
        timestamp: new Date()
      });

      // 5. Send FCM Push Notification to Owner
      await sendFCMNotification(
        ownerId,
        'Payment Received 💰',
        'Your item has been paid for. Please dispatch the order.',
        {
          type: 'PAYMENT_RECEIVED',
          postId,
          paymentId,
          recipientUserId: ownerId
        }
      );
    } catch (err) {
      console.error('[PAYMENT] Error sending notification to owner:', err.message);
    }

    res.status(201).json(successResponse(paymentData, 'Payment processed successfully and status set to paid'));
  } catch (error) {
    console.error('[PAYMENT] ERROR:', error);
    next(error);
  }
});

export default router;
