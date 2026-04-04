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

export default router;
