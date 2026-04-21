import express from 'express';
import { db } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';
import { sendFCMNotification } from '../utils/fcm-helper.js';

const router = express.Router();

/**
 * GET /notifications/:userId - Get user's notifications
 */
router.get('/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { limit = 20, offset = 0, unreadOnly = false } = req.query;

    console.log(`[NOTIFICATIONS] Fetching notifications for user: ${userId}`);

    // Check authorization
    if (req.user.uid !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to view these notifications', 'FORBIDDEN', 403));
    }

    let query = db.collection('notifications').where('userId', '==', userId);

    // Filter unread only if requested
    if (unreadOnly === 'true') {
      query = query.where('read', '==', false);
    }

    // Get all notifications without orderBy (to avoid composite index requirement)
    // Sort them in JavaScript instead
    const snapshot = await query.limit(parseInt(limit) + parseInt(offset) + 100).get();

    let notifications = [];
    snapshot.forEach((doc) => {
      const data = doc.data();
      notifications.push({
        id: doc.id,
        notificationId: data.notificationId,
        userId: data.userId,
        title: data.title,
        message: data.message,
        type: data.type,
        read: data.read,
        isRead: data.read,
        createdAt: data.createdAt,
        timestamp: data.timestamp || new Date(data.createdAt),
        relatedId: data.relatedId,
        relatedType: data.relatedType,
        trackingNumber: data.trackingNumber,
        itemTitle: data.itemTitle
      });
    });

    // Sort by timestamp in JavaScript (descending)
    notifications.sort((a, b) => {
      const timeA = a.timestamp instanceof Date ? a.timestamp.getTime() : new Date(a.timestamp).getTime();
      const timeB = b.timestamp instanceof Date ? b.timestamp.getTime() : new Date(b.timestamp).getTime();
      return timeB - timeA;
    });

    console.log(`[NOTIFICATIONS] Found ${notifications.length} notifications for user: ${userId}`);

    // Apply offset and limit
    notifications = notifications.slice(parseInt(offset), parseInt(offset) + parseInt(limit));

    res.status(200).json(
      successResponse({ notifications, count: notifications.length }, 'Notifications retrieved successfully')
    );
  } catch (error) {
    console.error('[NOTIFICATIONS] Error getting notifications:', error);
    next(error);
  }
});

/**
 * PUT /notifications/:id - Mark notification as read
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('notifications').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Notification not found', 'NOT_FOUND', 404));
    }

    const notification = doc.data();

    // Check authorization
    if (notification.userId !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to update this notification', 'FORBIDDEN', 403));
    }

    // Mark as read
    await db.collection('notifications').doc(id).update({
      read: true,
      readAt: getCurrentTimestamp(),
    });

    const updatedNotification = { ...notification, read: true, readAt: getCurrentTimestamp() };

    res.status(200).json(successResponse(updatedNotification, 'Notification marked as read'));
  } catch (error) {
    console.error('Error updating notification:', error);
    next(error);
  }
});

/**
 * PUT /notifications/batch/read - Mark multiple notifications as read
 */
router.put('/batch/read', async (req, res, next) => {
  try {
    const { notificationIds } = req.body;
    const userId = req.user.uid;

    if (!Array.isArray(notificationIds) || notificationIds.length === 0) {
      return res.status(400).json(errorResponse('Notification IDs are required', 'INVALID_INPUT', 400));
    }

    const batch = db.batch();
    const notificationSnapshots = await db.collection('notifications').where('id', 'in', notificationIds).get();

    notificationSnapshots.forEach((doc) => {
      const notification = doc.data();

      // Check authorization
      if (notification.userId !== userId) {
        throw new Error('Unauthorized');
      }

      batch.update(doc.ref, {
        read: true,
        readAt: getCurrentTimestamp(),
      });
    });

    await batch.commit();

    res.status(200).json(successResponse(null, 'Notifications marked as read'));
  } catch (error) {
    if (error.message === 'Unauthorized') {
      return res.status(403).json(errorResponse('Unauthorized operation', 'FORBIDDEN', 403));
    }
    console.error('Error updating notifications:', error);
    next(error);
  }
});

/**
 * DELETE /notifications/:id - Delete notification
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('notifications').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Notification not found', 'NOT_FOUND', 404));
    }

    const notification = doc.data();

    // Check authorization
    if (notification.userId !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to delete this notification', 'FORBIDDEN', 403));
    }

    // Delete notification
    await db.collection('notifications').doc(id).delete();

    res.status(200).json(successResponse(null, 'Notification deleted successfully'));
  } catch (error) {
    console.error('Error deleting notification:', error);
    next(error);
  }
});

/**
 * DELETE /notifications/batch/delete - Delete multiple notifications
 */
router.delete('/batch/delete', async (req, res, next) => {
  try {
    const { notificationIds } = req.body;
    const userId = req.user.uid;

    if (!Array.isArray(notificationIds) || notificationIds.length === 0) {
      return res.status(400).json(errorResponse('Notification IDs are required', 'INVALID_INPUT', 400));
    }

    const batch = db.batch();
    const notificationSnapshots = await db.collection('notifications').where('id', 'in', notificationIds).get();

    notificationSnapshots.forEach((doc) => {
      const notification = doc.data();

      // Check authorization
      if (notification.userId !== userId) {
        throw new Error('Unauthorized');
      }

      batch.delete(doc.ref);
    });

    await batch.commit();

    res.status(200).json(successResponse(null, 'Notifications deleted successfully'));
  } catch (error) {
    if (error.message === 'Unauthorized') {
      return res.status(403).json(errorResponse('Unauthorized operation', 'FORBIDDEN', 403));
    }
    console.error('Error deleting notifications:', error);
    next(error);
  }
});

/**
 * GET /notifications/:userId/unread-count - Get unread notification count
 */
router.get('/:userId/unread-count', async (req, res, next) => {
  try {
    const { userId } = req.params;

    // Check authorization
    if (req.user.uid !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to view this data', 'FORBIDDEN', 403));
    }

    const snapshot = await db.collection('notifications').where('userId', '==', userId).where('read', '==', false).get();

    const unreadCount = snapshot.size;

    res.status(200).json(successResponse({ unreadCount }, 'Unread count retrieved successfully'));
  } catch (error) {
    console.error('Error getting unread count:', error);
    next(error);
  }
});

/**
 * POST /notifications/send-fcm - Send FCM notification to a user
 */
router.post('/send-fcm', async (req, res, next) => {
  try {
    const { userId, title, body, type, postId, trackingNumber, itemTitle } = req.body;

    console.log('[Notifications] Received send-fcm request:', {
      userId,
      title,
      body,
      type,
      postId,
      trackingNumber,
      itemTitle,
    });

    if (!userId || !title || !body) {
      console.error('[Notifications] Missing required fields');
      return res.status(400).json(errorResponse('userId, title, and body are required', 'INVALID_INPUT', 400));
    }

    try {
      console.log('[Notifications] Attempting to send FCM notification');
      // Send FCM notification
      const result = await sendFCMNotification(userId, title, body, {
        recipientUserId: userId,
        type: type || 'INFO',
        postId: postId || '',
        trackingNumber: trackingNumber || '',
        itemTitle: itemTitle || '',
        timestamp: new Date().toISOString(),
      });

      console.log('[Notifications] FCM sent successfully, now saving to Firestore');
      // Save to Firestore for persistence
      await db.collection('notifications').add({
        userId: userId,
        receiverId: userId,
        senderId: 'system',
        title: title,
        body: body,
        type: type || 'INFO',
        postId: postId || null,
        trackingNumber: trackingNumber || null,
        itemTitle: itemTitle || null,
        read: false,
        timestamp: getCurrentTimestamp(),
      });

      console.log('[Notifications] Notification saved to Firestore');
      res.status(200).json(successResponse(result, 'FCM notification sent successfully'));
    } catch (fcmError) {
      console.error('[Notifications] FCM error:', fcmError.message);
      // If FCM token not found, still save to Firestore and return success
      if (fcmError.message.includes('No FCM token found')) {
        console.log(`[Notifications] FCM token not available for user ${userId}, saving to Firestore only`);

        // Save to Firestore anyway
        await db.collection('notifications').add({
          userId: userId,
          receiverId: userId,
          senderId: 'system',
          title: title,
          body: body,
          type: type || 'INFO',
          postId: postId || null,
          trackingNumber: trackingNumber || null,
          itemTitle: itemTitle || null,
          read: false,
          timestamp: getCurrentTimestamp(),
        });

        return res.status(200).json(successResponse(
          { saved: true, fcmSent: false },
          'Notification saved to Firestore (FCM token not available - user may not have app installed or FCM not initialized)'
        ));
      }

      throw fcmError;
    }
  } catch (error) {
    console.error('[Notifications] Error in send-fcm endpoint:', error);
    // Always save to Firestore, even on error
    try {
      const { userId, title, body, type, postId, trackingNumber, itemTitle } = req.body;
      if (userId && title && body) {
        await db.collection('notifications').add({
          userId: userId,
          receiverId: userId,
          senderId: 'system',
          title: title,
          body: body,
          type: type || 'INFO',
          postId: postId || null,
          trackingNumber: trackingNumber || null,
          itemTitle: itemTitle || null,
          read: false,
          timestamp: getCurrentTimestamp(),
          error: error.message,
        });

        return res.status(200).json(successResponse(
          { saved: true, fcmSent: false, error: error.message },
          'Notification saved to Firestore (FCM delivery failed)'
        ));
      }
    } catch (saveError) {
      console.error('[Notifications] Failed to save to Firestore:', saveError);
    }

    next(error);
  }
});

export default router;
