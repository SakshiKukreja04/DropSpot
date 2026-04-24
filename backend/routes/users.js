import express from 'express';
import { db } from '../config/firebase.js';
import { getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';

const router = express.Router();

/**
 * POST /users - Create or update user profile
 * Structure: { uid, name, email, phone, photo, bio, rating, ratingCount, postsCount, isVerified, createdAt, updatedAt }
 */
router.post('/', async (req, res, next) => {
  try {
    const userId = req.user.uid;
    const { name, email, phone, photo, bio } = req.body;

    const userRef = db.collection('users').doc(userId);
    const userDoc = await userRef.get();
    const timestamp = getCurrentTimestamp();

    if (!userDoc.exists) {
      // New User
      if (!name || !email) {
        return res.status(400).json(errorResponse('Name and email are required for new users', 'VALIDATION_ERROR', 400));
      }

      const newUser = {
        uid: userId,
        name,
        email,
        phone: phone || "",
        photo: photo || "",
        bio: bio || "",
        rating: 0,
        ratingCount: 0,
        postsCount: 0,
        isVerified: false,
        createdAt: timestamp,
        updatedAt: timestamp
      };
      await userRef.set(newUser);
      return res.status(201).json(successResponse(newUser, 'User created'));
    } else {
      // Update User
      const updateData = { updatedAt: timestamp };
      if (name !== undefined) updateData.name = name;
      if (email !== undefined) updateData.email = email;
      if (phone !== undefined) updateData.phone = phone;
      if (photo !== undefined) updateData.photo = photo;
      if (bio !== undefined) updateData.bio = bio;

      await userRef.update(updateData);
      const updatedUser = { ...userDoc.data(), ...updateData };
      return res.status(200).json(successResponse(updatedUser, 'User updated'));
    }
  } catch (error) {
    next(error);
  }
});

/**
 * PUT /users/:userId - Update user profile (including FCM token)
 */
router.put('/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const authUserId = req.user.uid;

    // Ensure user can only update their own profile
    if (userId !== authUserId) {
      return res.status(403).json(errorResponse('Unauthorized to update this profile', 'FORBIDDEN', 403));
    }

    const userRef = db.collection('users').doc(userId);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      return res.status(404).json(errorResponse('User not found', 'NOT_FOUND', 404));
    }

    const { fcmToken, name, email, phone, photo, bio } = req.body;
    const updateData = { updatedAt: getCurrentTimestamp() };

    // Only update provided fields
    if (fcmToken !== undefined) updateData.fcmToken = fcmToken;
    if (name !== undefined) updateData.name = name;
    if (email !== undefined) updateData.email = email;
    if (phone !== undefined) updateData.phone = phone;
    if (photo !== undefined) updateData.photo = photo;
    if (bio !== undefined) updateData.bio = bio;

    await userRef.update(updateData);
    const updatedUser = { ...userDoc.data(), ...updateData };

  } catch (error) {
    next(error);
  }
});

/**
 * GET /users/:userId - Get user profile
 */
router.get('/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const doc = await db.collection('users').doc(userId).get();

    if (!doc.exists) return res.status(404).json(errorResponse('User not found', 'NOT_FOUND', 404));

    res.status(200).json(successResponse(doc.data(), 'User profile fetched'));
  } catch (error) {
    next(error);
  }
});

/**
 * POST /users/:userId/process-pending-notifications
 * Called after FCM token is registered to send queued notifications
 */
router.post('/:userId/process-pending-notifications', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const authUserId = req.user.uid;

    // Ensure user can only process their own notifications
    if (userId !== authUserId) {
      return res.status(403).json(errorResponse('Unauthorized', 'FORBIDDEN', 403));
    }

    console.log(`[PENDING_NOTIFICATIONS] Processing pending notifications for user: ${userId}`);

    // Check if user has FCM token
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists || !userDoc.data().fcmToken) {
      console.log(`[PENDING_NOTIFICATIONS] User ${userId} has no FCM token yet, skipping`);
      return res.status(200).json(successResponse({}, 'No FCM token available yet'));
    }

    // Fetch pending notifications
    const pendingSnap = await db.collection('pendingNotifications')
      .where('userId', '==', userId)
      .where('status', '==', 'pending')
      .orderBy('createdAt', 'asc')
      .limit(10)
      .get();

    if (pendingSnap.empty) {
      console.log(`[PENDING_NOTIFICATIONS] No pending notifications for ${userId}`);
      return res.status(200).json(successResponse({ sent: 0 }, 'No pending notifications'));
    }

    let sentCount = 0;
    let failedCount = 0;

    for (const doc of pendingSnap.docs) {
      const notification = doc.data();
      try {
        console.log(`[PENDING_NOTIFICATIONS] Sending pending notification: ${notification.queueId}`);

        await sendFCMNotification(
          userId,
          notification.title,
          notification.body,
          notification.data
        );

        // Mark as sent
        await db.collection('pendingNotifications').doc(doc.id).update({
          status: 'sent',
          sentAt: getCurrentTimestamp()
        });

        console.log(`[PENDING_NOTIFICATIONS] ✅ Sent: ${notification.queueId}`);
        sentCount++;
      } catch (error) {
        console.error(`[PENDING_NOTIFICATIONS] ❌ Error sending ${notification.queueId}:`, error.message);

        // Increment retry count
        const retryCount = (notification.retryCount || 0) + 1;
        if (retryCount < notification.maxRetries) {
          await db.collection('pendingNotifications').doc(doc.id).update({
            retryCount,
            lastError: error.message,
            lastAttempt: getCurrentTimestamp()
          });
          console.log(`[PENDING_NOTIFICATIONS] Marked for retry (${retryCount}/${notification.maxRetries})`);
        } else {
          await db.collection('pendingNotifications').doc(doc.id).update({
            status: 'failed',
            maxRetriesReached: true,
            finalError: error.message
          });
          console.log(`[PENDING_NOTIFICATIONS] Max retries reached, marking as failed`);
        }
        failedCount++;
      }
    }

    console.log(`[PENDING_NOTIFICATIONS] Complete - Sent: ${sentCount}, Failed: ${failedCount}`);
    res.status(200).json(successResponse(
      { sent: sentCount, failed: failedCount },
      `Processed pending notifications: ${sentCount} sent, ${failedCount} failed`
    ));
  } catch (error) {
    next(error);
  }
});

export default router;
