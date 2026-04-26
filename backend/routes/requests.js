import express from 'express';
import { db, FieldValue } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';
import { sendPushNotification } from '../utils/notifications.js';
import { sendFCMNotification } from '../utils/fcm-helper.js';

const router = express.Router();

/**
 * Handle Request Status Update
 * Ensures status is strictly one of: "pending", "accepted", "rejected"
 */
const handleStatusUpdate = async (id, status, userId, res) => {
  const normalizedStatus = status ? status.toLowerCase().trim() : "";

  // 1. Enforce strict status values
  const allowedStatuses = ['pending', 'accepted', 'rejected'];
  if (!allowedStatuses.includes(normalizedStatus)) {
    console.error(`[STATUS_UPDATE] Invalid status: ${status}`);
    return res.status(400).json(errorResponse('Invalid status. Allowed: pending, accepted, rejected', 'INVALID_INPUT', 400));
  }

  // 4. Add logging: FINAL STATUS
  console.log("FINAL STATUS:", normalizedStatus);

  try {
    const requestRef = db.collection('requests').doc(id);
    const requestDoc = await requestRef.get();

    if (!requestDoc.exists) {
      console.error(`[STATUS_UPDATE] ERROR: Request ${id} not found`);
      return res.status(404).json(errorResponse('Request not found', 'NOT_FOUND', 404));
    }

    const requestData = requestDoc.data();
    if (requestData.postOwnerId !== userId) {
      console.error(`[STATUS_UPDATE] ERROR: Unauthorized. Owner=${requestData.postOwnerId}, Actor=${userId}`);
      return res.status(403).json(errorResponse('Unauthorized', 'FORBIDDEN', 403));
    }

    const timestamp = getCurrentTimestamp();
    const batch = db.batch();

    // 3. Ensure Firestore always stores exact match: status = "accepted"
    batch.update(requestRef, {
      status: normalizedStatus,
      respondedAt: timestamp,
      respondedBy: userId,
      updatedAt: timestamp
    });

    if (normalizedStatus === 'accepted') {
      const postRef = db.collection('posts').doc(requestData.postId);
      batch.update(postRef, {
        acceptedRequestId: id,
        updatedAt: timestamp
      });

      // Reject all other pending requests for the same post
      const othersSnapshot = await db.collection('requests')
        .where('postId', '==', requestData.postId)
        .where('status', '==', 'pending')
        .get();

      othersSnapshot.forEach(oDoc => {
        if (oDoc.id !== id) {
          // 2. Replace: "rejected_auto" → "rejected"
          batch.update(oDoc.ref, {
            status: 'rejected',
            respondedAt: timestamp,
            respondedBy: userId
          });
        }
      });
    }

    await batch.commit();
    console.log(`[STATUS_UPDATE] SUCCESS: committed status ${normalizedStatus} for request ${id}`);

    // Fetch updated data for notification and response
    const updatedDoc = await requestRef.get();
    const updatedRequest = updatedDoc.data();

    // Ensure ID fields are consistent
    if (!updatedRequest.requestId) updatedRequest.requestId = id;
    if (!updatedRequest.id) updatedRequest.id = id;

    // Notification Logic
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: updatedRequest.requesterId,
      type: `request_${normalizedStatus}`,
      title: normalizedStatus === 'accepted' ? 'Request Accepted' : 'Request Rejected',
      message: normalizedStatus === 'accepted' ? 'Your request has been accepted. Proceed to payment.' : `Your request for "${updatedRequest.postTitle}" was not accepted.`,
      relatedId: id,
      relatedType: 'request',
      read: false,
      createdAt: timestamp,
      timestamp: new Date()
    });

    try {
      const requesterDoc = await db.collection('users').doc(updatedRequest.requesterId).get();
      if (requesterDoc.exists && requesterDoc.data().fcmToken) {
        await sendPushNotification(
          requesterDoc.data().fcmToken,
          normalizedStatus === 'accepted' ? 'Request Accepted ✅' : 'Request Rejected ❌',
          normalizedStatus === 'accepted' ? 'Your request has been accepted. Proceed to payment.' : `Your request for "${updatedRequest.postTitle}" was ${normalizedStatus}.`,
          {
            type: 'request_update',
            requestId: id,
            status: normalizedStatus,
            postId: updatedRequest.postId,
            recipientUserId: updatedRequest.requesterId
          }
        );
      }
    } catch (pushError) {
      console.error('[FCM] Error sending push:', pushError);
    }

    return res.status(200).json(successResponse(updatedRequest, `Request ${normalizedStatus}`));
  } catch (error) {
    console.error(`[STATUS_UPDATE] CRITICAL ERROR:`, error);
    return res.status(500).json(errorResponse('Server update failed', 'SERVER_ERROR', 500));
  }
};

/**
 * GET /api/requests
 * UI RULE: Do NOT hide requests if post.isActive = false (Standard fetch returns all)
 */
router.get('/', async (req, res, next) => {
  try {
    const { type } = req.query;
    const userId = req.user.uid;
    let query = db.collection('requests');

    if (type === 'my_sent') {
      query = query.where('requesterId', '==', userId);
    } else {
      query = query.where('postOwnerId', '==', userId);
    }

    const snapshot = await query.get();
    const requests = [];
    snapshot.forEach(doc => {
      const data = doc.data();
      if (!data.requestId) data.requestId = doc.id;
      if (!data.id) data.id = doc.id;
      requests.push(data);
    });

    res.status(200).json(successResponse(requests, 'Requests fetched'));
  } catch (error) { next(error); }
});

router.post('/', async (req, res, next) => {
  try {
    const { postId, message } = req.body;
    const userId = req.user.uid;

    const postDoc = await db.collection('posts').doc(postId).get();
    if (!postDoc.exists) return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));

    const post = postDoc.data();
    const requestId = generateId();
    const timestamp = getCurrentTimestamp();
    const userDoc = await db.collection('users').doc(userId).get();
    const userData = userDoc.exists ? userDoc.data() : {};

    const request = {
      id: requestId,
      requestId,
      postId,
      postOwnerId: post.userId,
      postTitle: post.title,
      postPrice: post.price || 0,
      requesterId: userId,
      requesterName: userData.name || userData.displayName || 'Anonymous',
      requesterPhoto: userData.photo || userData.photoURL || '',
      requesterEmail: userData.email || '',
      message,
      status: 'pending',
      createdAt: timestamp,
      respondedAt: null
    };

    await db.collection('requests').doc(requestId).set(request);
    await db.collection('posts').doc(postId).update({ requestCount: FieldValue.increment(1) });

    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: post.userId,
      type: 'new_request',
      title: 'New Request 📬',
      message: `${request.requesterName} is interested in "${post.title}"`,
      relatedId: requestId,
      relatedType: 'request',
      read: false,
      createdAt: timestamp,
      timestamp: new Date()
    });

    try {
      const ownerDoc = await db.collection('users').doc(post.userId).get();
      if (ownerDoc.exists && ownerDoc.data().fcmToken) {
        await sendPushNotification(
          ownerDoc.data().fcmToken,
          'New Request 📬',
          `${request.requesterName} is interested in "${post.title}"`,
          { type: 'new_request', requestId, postId, recipientUserId: post.userId }
        );
      }
    } catch (pushError) {
      console.error('[FCM] Error sending push:', pushError);
    }

    res.status(201).json(successResponse(request, 'Request sent'));
  } catch (error) { next(error); }
});

router.put('/:id/status', async (req, res, next) => {
  try {
    await handleStatusUpdate(req.params.id, req.body.status, req.user.uid, res);
  } catch (error) { next(error); }
});

router.post('/:id/dispatch', async (req, res) => {
  try {
    const requestId = req.params.id;
    const userId = req.user.uid;
    const { trackingNumber } = req.body;

    const doc = await db.collection('requests').doc(requestId).get();
    if (!doc.exists) return res.status(404).json(errorResponse('Request not found', 'NOT_FOUND', 404));

    const request = doc.data();
    if (request.postOwnerId !== userId) return res.status(403).json(errorResponse('Unauthorized', 'FORBIDDEN', 403));

    await db.collection('requests').doc(requestId).update({
      status: 'dispatched',
      trackingNumber: trackingNumber || 'N/A',
      dispatchedAt: getCurrentTimestamp()
    });

    await sendFCMNotification(
      request.requesterId,
      'Order Dispatched 📦',
      `Your order for "${request.postTitle}" has been dispatched.`,
      { type: 'order_dispatched', requestId, postId: request.postId }
    );

    res.status(200).json(successResponse(null, 'Order dispatched'));
  } catch (error) {
    res.status(500).json(errorResponse('Dispatch failed', 'SERVER_ERROR', 500));
  }
});

router.post('/:id/confirm-delivery', async (req, res) => {
  try {
    const requestId = req.params.id;
    const userId = req.user.uid;

    const doc = await db.collection('requests').doc(requestId).get();
    if (!doc.exists) return res.status(404).json(errorResponse('Request not found', 'NOT_FOUND', 404));

    const request = doc.data();
    if (request.requesterId !== userId) return res.status(403).json(errorResponse('Unauthorized', 'FORBIDDEN', 403));

    await db.collection('requests').doc(requestId).update({
      status: 'completed',
      deliveredAt: getCurrentTimestamp()
    });

    await sendFCMNotification(
      request.postOwnerId,
      'Order Completed ✅',
      `Buyer confirmed delivery for "${request.postTitle}".`,
      { type: 'order_completed', requestId, postId: request.postId }
    );

    res.status(200).json(successResponse(null, 'Delivery confirmed'));
  } catch (error) {
    res.status(500).json(errorResponse('Confirmation failed', 'SERVER_ERROR', 500));
  }
});

export default router;
