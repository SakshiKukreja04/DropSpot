import express from 'express';
import { db } from '../config/firebase.js';
import { getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';
import { sendFCMNotification } from '../utils/fcm-helper.js';

const router = express.Router();

/**
 * POST /users - Create or update user profile
 */
router.post('/', async (req, res, next) => {
  try {
    const userId = req.user.uid;
    const { name, email, phone, photo, bio } = req.body;

    const userRef = db.collection('users').doc(userId);
    const userDoc = await userRef.get();
    const timestamp = getCurrentTimestamp();

    if (!userDoc.exists) {
      if (!name || !email) {
        return res.status(400).json(errorResponse('Name and email are required', 'VALIDATION_ERROR', 400));
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
      const updateData = { updatedAt: timestamp };
      if (name !== undefined) updateData.name = name;
      if (email !== undefined) updateData.email = email;
      if (phone !== undefined) updateData.phone = phone;
      if (photo !== undefined) updateData.photo = photo;
      if (bio !== undefined) updateData.bio = bio;

      await userRef.update(updateData);
      return res.status(200).json(successResponse({ ...userDoc.data(), ...updateData }, 'User updated'));
    }
  } catch (error) { next(error); }
});

/**
 * PUT /users/:userId - Update user profile (CRITICAL FIX: Added Location & FCM Support)
 */
router.put('/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const authUserId = req.user.uid;

    if (userId !== authUserId) return res.status(403).json(errorResponse('Unauthorized', 'FORBIDDEN', 403));

    const userRef = db.collection('users').doc(userId);
    const userDoc = await userRef.get();
    if (!userDoc.exists) return res.status(404).json(errorResponse('User not found', 'NOT_FOUND', 404));

    const { fcmToken, latitude, longitude, name, email, photo } = req.body;
    const updateData = { updatedAt: getCurrentTimestamp() };

    // Update FCM Token
    if (fcmToken !== undefined) updateData.fcmToken = fcmToken;

    // CRITICAL: Update Location (Ensures proximity notifications work)
    if (latitude !== undefined && latitude !== null) updateData.latitude = Number(latitude);
    if (longitude !== undefined && longitude !== null) updateData.longitude = Number(longitude);

    // Update basic info
    if (name !== undefined) updateData.name = name;
    if (email !== undefined) updateData.email = email;
    if (photo !== undefined) updateData.photo = photo;

    await userRef.update(updateData);
    console.log(`[USER_UPDATE] Sync complete for ${userId}: Loc(${updateData.latitude}, ${updateData.longitude}) Token: ${fcmToken ? 'Yes' : 'No'}`);

    return res.status(200).json(successResponse(null, 'User updated successfully'));
  } catch (error) { next(error); }
});

/**
 * GET /users/:userId - Get user profile
 */
router.get('/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const doc = await db.collection('users').doc(userId).get();
    if (!doc.exists) return res.status(404).json(errorResponse('User not found'));
    res.status(200).json(successResponse(doc.data(), 'User profile fetched'));
  } catch (error) { next(error); }
});

export default router;
