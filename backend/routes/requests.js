import express from 'express';
import { db, FieldValue } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';

const router = express.Router();

/**
 * POST /requests - Create a new request for a post
 */
router.post('/', async (req, res, next) => {
  try {
    const { postId, message } = req.body;
    const userId = req.user.uid;

    if (!postId || !message) {
      return res.status(400).json(errorResponse('Post ID and message are required', 'VALIDATION_ERROR', 400));
    }

    const postDoc = await db.collection('posts').doc(postId).get();
    if (!postDoc.exists) return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));

    const post = postDoc.data();
    if (post.userId === userId) return res.status(400).json(errorResponse('Cannot request your own post', 'INVALID_REQUEST', 400));
    if (!post.isActive) return res.status(400).json(errorResponse('Post is no longer active', 'INVALID_REQUEST', 400));

    // Prevent duplicate requests
    const existing = await db.collection('requests')
      .where('postId', '==', postId)
      .where('requesterId', '==', userId)
      .get();
    if (!existing.empty) return res.status(400).json(errorResponse('Duplicate request', 'DUPLICATE_REQUEST', 400));

    const requestId = generateId();
    const timestamp = getCurrentTimestamp();
    const request = {
      requestId,
      postId,
      postOwnerId: post.userId,
      requesterId: userId,
      message,
      status: 'pending',
      createdAt: timestamp,
      respondedAt: null
    };

    await db.collection('requests').doc(requestId).set(request);
    await db.collection('posts').doc(postId).update({ requestCount: FieldValue.increment(1) });

    // Trigger notification
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: post.userId,
      type: 'request_received',
      title: 'New Request',
      message: `Someone requested your item: ${post.title}`,
      relatedId: requestId,
      relatedType: 'request',
      read: false,
      createdAt: timestamp
    });

    res.status(201).json(successResponse(request, 'Request sent'));
  } catch (error) {
    next(error);
  }
});

/**
 * GET /requests - Get requests with strictly defined filtering
 */
router.get('/', async (req, res, next) => {
  try {
    const { type } = req.query; // 'sent' or 'received'
    const userId = req.user.uid;

    let query = db.collection('requests');
    if (type === 'sent') {
      query = query.where('requesterId', '==', userId);
    } else {
      query = query.where('postOwnerId', '==', userId);
    }

    const snapshot = await query.orderBy('createdAt', 'desc').get();
    const requests = [];
    snapshot.forEach(doc => requests.push(doc.data()));

    res.status(200).json(successResponse({ requests, count: requests.length }, 'Requests fetched'));
  } catch (error) {
    next(error);
  }
});

/**
 * PUT /requests/:id - Accept or reject a request
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const { status } = req.body; // 'accepted' | 'rejected'
    const userId = req.user.uid;

    if (!['accepted', 'rejected'].includes(status)) return res.status(400).json(errorResponse('Invalid status', 'INVALID_INPUT', 400));

    const doc = await db.collection('requests').doc(id).get();
    if (!doc.exists) return res.status(404).json(errorResponse('Request not found', 'NOT_FOUND', 404));

    const request = doc.data();
    if (request.postOwnerId !== userId) return res.status(403).json(errorResponse('Unauthorized', 'FORBIDDEN', 403));
    if (request.status !== 'pending') return res.status(400).json(errorResponse('Already processed', 'INVALID_REQUEST', 400));

    const timestamp = getCurrentTimestamp();
    const batch = db.batch();

    batch.update(db.collection('requests').doc(id), { status, respondedAt: timestamp });

    if (status === 'accepted') {
      // mark post as isActive = false
      batch.update(db.collection('posts').doc(request.postId), { isActive: false, updatedAt: timestamp });

      // auto-reject other pending requests
      const others = await db.collection('requests')
        .where('postId', '==', request.postId)
        .where('status', '==', 'pending')
        .get();

      others.forEach(oDoc => {
        if (oDoc.id !== id) {
          batch.update(oDoc.ref, { status: 'rejected', respondedAt: timestamp });
        }
      });
    }

    await batch.commit();

    // Trigger notification for requester
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      notificationId,
      userId: request.requesterId,
      type: `request_${status}`,
      title: status === 'accepted' ? 'Request Accepted' : 'Request Rejected',
      message: `Your request for an item has been ${status}.`,
      relatedId: id,
      relatedType: 'request',
      read: false,
      createdAt: timestamp
    });

    res.status(200).json(successResponse(null, `Request ${status}`));
  } catch (error) {
    next(error);
  }
});

export default router;
