import express from 'express';
import { db, FieldValue } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';

const router = express.Router();

/**
 * Handle Request Status Update (Shared Logic)
 */
const handleStatusUpdate = async (id, status, userId, res) => {
  console.log(`[STATUS_UPDATE] Request: ${id}, New Status: ${status}, By User: ${userId}`);

  try {
    const doc = await db.collection('requests').doc(id).get();
    if (!doc.exists) {
      console.error(`[STATUS_UPDATE] Request ${id} NOT FOUND`);
      return res.status(404).json(errorResponse('Request not found', 'NOT_FOUND', 404));
    }

    const request = doc.data();
    if (request.postOwnerId !== userId) {
      console.error(`[STATUS_UPDATE] Unauthorized: User ${userId} is not owner ${request.postOwnerId}`);
      return res.status(403).json(errorResponse('Unauthorized', 'FORBIDDEN', 403));
    }

    const timestamp = getCurrentTimestamp();
    const batch = db.batch();

    // 1. Update request status
    batch.update(db.collection('requests').doc(id), {
      status,
      respondedAt: timestamp,
      respondedBy: userId
    });

    if (status === 'accepted') {
      // 2. Mark post as inactive
      batch.update(db.collection('posts').doc(request.postId), {
        isActive: false,
        acceptedRequestId: id,
        updatedAt: timestamp
      });

      // 3. Auto-reject others
      const others = await db.collection('requests')
        .where('postId', '==', request.postId)
        .where('status', '==', 'pending')
        .get();

      others.forEach(oDoc => {
        if (oDoc.id !== id) {
          batch.update(oDoc.ref, { status: 'rejected_auto', respondedAt: timestamp, respondedBy: userId });
        }
      });
    }

    await batch.commit();
    console.log(`[STATUS_UPDATE] SUCCESS: Request ${id} is now ${status}`);

    // Notification creation
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: request.requesterId,
      type: `request_${status}`,
      title: status === 'accepted' ? 'Request Accepted' : 'Request Rejected',
      message: `Your request for "${request.postTitle}" has been ${status}.`,
      relatedId: id,
      relatedType: 'request',
      read: false,
      createdAt: timestamp
    });

    return res.status(200).json(successResponse(null, `Request ${status}`));
  } catch (error) {
    console.error(`[STATUS_UPDATE] CRITICAL ERROR:`, error);
    return res.status(500).json(errorResponse('Server update failed', 'SERVER_ERROR', 500));
  }
};

/**
 * POST /requests - Create a new request
 */
router.post('/', async (req, res, next) => {
  try {
    const { postId, message } = req.body;
    const userId = req.user.uid;

    if (!postId || !message) return res.status(400).json(errorResponse('Post ID and message are required', 'VALIDATION_ERROR', 400));

    const postDoc = await db.collection('posts').doc(postId).get();
    if (!postDoc.exists) return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));

    const post = postDoc.data();
    if (post.userId === userId) return res.status(400).json(errorResponse('Cannot request your own post', 'INVALID_REQUEST', 400));

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

    res.status(201).json(successResponse(request, 'Request sent'));
  } catch (error) { next(error); }
});

/**
 * GET /requests - Fetch requests
 */
router.get('/', async (req, res, next) => {
  try {
    const { type } = req.query;
    const userId = req.user.uid;
    let query = db.collection('requests');
    if (type === 'my_sent') query = query.where('requesterId', '==', userId);
    else query = query.where('postOwnerId', '==', userId);

    const snapshot = await query.get();
    const requests = [];
    snapshot.forEach(doc => requests.push(doc.data()));
    requests.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    res.status(200).json(successResponse(requests, 'Requests fetched'));
  } catch (error) { next(error); }
});

/**
 * PUT /requests/:id/status
 */
router.put('/:id/status', async (req, res, next) => {
  try {
    await handleStatusUpdate(req.params.id, req.body.status, req.user.uid, res);
  } catch (error) { next(error); }
});

router.put('/:id', async (req, res, next) => {
  try {
    await handleStatusUpdate(req.params.id, req.body.status, req.user.uid, res);
  } catch (error) { next(error); }
});

export default router;
