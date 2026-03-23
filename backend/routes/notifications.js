import express from 'express';
import { db } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';

const router = express.Router();

/**
 * GET /notifications/:userId - Get user's notifications
 */
router.get('/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { limit = 20, offset = 0, unreadOnly = false } = req.query;

    // Check authorization
    if (req.user.uid !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to view these notifications', 'FORBIDDEN', 403));
    }

    let query = db.collection('notifications').where('userId', '==', userId);

    // Filter unread only if requested
    if (unreadOnly === 'true') {
      query = query.where('read', '==', false);
    }

    const snapshot = await query.orderBy('createdAt', 'desc').limit(parseInt(limit) + parseInt(offset)).get();

    let notifications = [];
    snapshot.forEach((doc) => {
      notifications.push(doc.data());
    });

    // Apply offset
    notifications = notifications.slice(parseInt(offset), parseInt(offset) + parseInt(limit));

    res.status(200).json(
      successResponse({ notifications, count: notifications.length }, 'Notifications retrieved successfully')
    );
  } catch (error) {
    console.error('Error getting notifications:', error);
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

export default router;
