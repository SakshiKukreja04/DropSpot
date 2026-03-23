import express from 'express';
import { db } from '../config/firebase.js';
import {
  generateId,
  getCurrentTimestamp,
  validateRequestData,
  successResponse,
  errorResponse,
} from '../utils/helpers.js';

const router = express.Router();

/**
 * POST /requests - Create a new request for a post
 */
router.post('/', async (req, res, next) => {
  try {
    const { postId, message } = req.body;
    const userId = req.user.uid;

    // Validate input
    const validation = validateRequestData(req.body);
    if (!validation.isValid) {
      return res.status(400).json(errorResponse('Validation failed', 'VALIDATION_ERROR', 400, validation.errors));
    }

    // Check if post exists
    const postDoc = await db.collection('posts').doc(postId).get();
    if (!postDoc.exists) {
      return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));
    }

    const post = postDoc.data();

    // Check if user is the post owner
    if (post.userId === userId) {
      return res.status(400).json(errorResponse('Cannot request your own post', 'INVALID_REQUEST', 400));
    }

    // Check for duplicate request
    const existingRequest = await db
      .collection('requests')
      .where('postId', '==', postId)
      .where('requesterId', '==', userId)
      .where('status', '!=', 'rejected')
      .get();

    if (!existingRequest.empty) {
      return res.status(400).json(errorResponse('You have already requested this item', 'DUPLICATE_REQUEST', 400));
    }

    // Create request
    const requestId = generateId();
    const request = {
      id: requestId,
      postId,
      postTitle: post.title,
      postOwnerId: post.userId,
      requesterId: userId,
      requesterName: req.user.name,
      requesterEmail: req.user.email,
      requesterPhoto: req.user.picture,
      message,
      status: 'pending', // pending, accepted, rejected
      createdAt: getCurrentTimestamp(),
      respondedAt: null,
      respondedBy: null,
    };

    // Save request
    await db.collection('requests').doc(requestId).set(request);

    // Add to post owner's requests
    await db.collection('users').doc(post.userId).collection('requests').doc(requestId).set({
      requestId,
      postId,
      requesterId: userId,
      createdAt: getCurrentTimestamp(),
    });

    // Increment request count on post
    await db.collection('posts').doc(postId).update({
      requestCount: (post.requestCount || 0) + 1,
    });

    // Create notification for post owner
    const notificationId = generateId();
    await db.collection('notifications').doc(notificationId).set({
      id: notificationId,
      userId: post.userId,
      type: 'new_request',
      title: `New request for "${post.title}"`,
      message: `${req.user.name} requested your item`,
      relatedId: requestId,
      relatedType: 'request',
      read: false,
      createdAt: getCurrentTimestamp(),
    });

    res.status(201).json(successResponse(request, 'Request created successfully'));
  } catch (error) {
    console.error('Error creating request:', error);
    next(error);
  }
});

/**
 * GET /requests - Get user's requests (received and sent)
 */
router.get('/', async (req, res, next) => {
  try {
    const { type = 'received' } = req.query;
    const userId = req.user.uid;

    let query;
    if (type === 'sent') {
      query = db.collection('requests').where('requesterId', '==', userId);
    } else {
      query = db.collection('requests').where('postOwnerId', '==', userId);
    }

    const snapshot = await query.orderBy('createdAt', 'desc').get();

    const requests = [];
    snapshot.forEach((doc) => {
      requests.push(doc.data());
    });

    res.status(200).json(successResponse({ requests, count: requests.length }, 'Requests retrieved successfully'));
  } catch (error) {
    console.error('Error getting requests:', error);
    next(error);
  }
});

/**
 * GET /requests/:id - Get request details
 */
router.get('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('requests').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Request not found', 'NOT_FOUND', 404));
    }

    const request = doc.data();

    // Check authorization
    if (request.postOwnerId !== userId && request.requesterId !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to view this request', 'FORBIDDEN', 403));
    }

    res.status(200).json(successResponse(request, 'Request retrieved successfully'));
  } catch (error) {
    console.error('Error getting request:', error);
    next(error);
  }
});

/**
 * PUT /requests/:id - Accept or reject a request
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const { status } = req.body; // 'accepted' or 'rejected'
    const userId = req.user.uid;

    // Validate status
    if (!['accepted', 'rejected'].includes(status)) {
      return res.status(400).json(errorResponse('Invalid status', 'INVALID_STATUS', 400));
    }

    const doc = await db.collection('requests').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Request not found', 'NOT_FOUND', 404));
    }

    const request = doc.data();

    // Check authorization (only post owner can accept/reject)
    if (request.postOwnerId !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to update this request', 'FORBIDDEN', 403));
    }

    // Check if already responded
    if (request.status !== 'pending') {
      return res.status(400).json(errorResponse('Request already responded to', 'ALREADY_RESPONDED', 400));
    }

    // Update request
    await db.collection('requests').doc(id).update({
      status,
      respondedAt: getCurrentTimestamp(),
      respondedBy: userId,
    });

    // If accepted, update post to inactive
    if (status === 'accepted') {
      await db.collection('posts').doc(request.postId).update({
        isActive: false,
        acceptedRequestId: id,
      });

      // Reject all other pending requests for this post
      const otherRequests = await db
        .collection('requests')
        .where('postId', '==', request.postId)
        .where('status', '==', 'pending')
        .get();

      otherRequests.forEach(async (doc) => {
        if (doc.id !== id) {
          await db.collection('requests').doc(doc.id).update({
            status: 'rejected_auto',
            respondedAt: getCurrentTimestamp(),
            respondedBy: userId,
          });
        }
      });
    }

    // Create notification for requester
    const notificationId = generateId();
    const notificationTitle = status === 'accepted' ? 'Request accepted!' : 'Request rejected';
    const notificationMessage =
      status === 'accepted'
        ? `Your request for "${request.postTitle}" was accepted by the owner!`
        : `Your request for "${request.postTitle}" was rejected`;

    await db.collection('notifications').doc(notificationId).set({
      id: notificationId,
      userId: request.requesterId,
      type: `request_${status}`,
      title: notificationTitle,
      message: notificationMessage,
      relatedId: id,
      relatedType: 'request',
      read: false,
      createdAt: getCurrentTimestamp(),
    });

    const updatedRequest = { ...request, status, respondedAt: getCurrentTimestamp(), respondedBy: userId };

    res.status(200).json(successResponse(updatedRequest, `Request ${status} successfully`));
  } catch (error) {
    console.error('Error updating request:', error);
    next(error);
  }
});

/**
 * DELETE /requests/:id - Cancel a request
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('requests').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Request not found', 'NOT_FOUND', 404));
    }

    const request = doc.data();

    // Check authorization (only requester can cancel pending request)
    if (request.requesterId !== userId || request.status !== 'pending') {
      return res.status(403).json(errorResponse('Cannot cancel this request', 'FORBIDDEN', 403));
    }

    // Delete request
    await db.collection('requests').doc(id).delete();

    // Update request count on post
    const postDoc = await db.collection('posts').doc(request.postId).get();
    if (postDoc.exists) {
      const post = postDoc.data();
      await db.collection('posts').doc(request.postId).update({
        requestCount: Math.max(0, (post.requestCount || 1) - 1),
      });
    }

    res.status(200).json(successResponse(null, 'Request cancelled successfully'));
  } catch (error) {
    console.error('Error deleting request:', error);
    next(error);
  }
});

export default router;
