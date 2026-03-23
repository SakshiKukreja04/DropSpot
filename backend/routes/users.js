import express from 'express';
import { db } from '../config/firebase.js';
import { getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';

const router = express.Router();

/**
 * POST /users - Create or update user profile
 */
router.post('/', async (req, res, next) => {
  try {
    const userId = req.user.uid;
    const { name, email, phone, bio, location, latitude, longitude, photo } = req.body;

    const userRef = db.collection('users').doc(userId);
    const userDoc = await userRef.get();

    const userData = {
      uid: userId,
      name: name || req.user.name,
      email: email || req.user.email,
      phone: phone || null,
      bio: bio || '',
      location: location || null,
      latitude: latitude || null,
      longitude: longitude || null,
      photo: photo || req.user.picture,
      updatedAt: getCurrentTimestamp(),
    };

    if (!userDoc.exists) {
      userData.createdAt = getCurrentTimestamp();
      userData.rating = 0;
      userData.ratingCount = 0;
      userData.postsCount = 0;
      userData.isVerified = false;
    }

    await userRef.set(userData, { merge: true });

    res.status(userDoc.exists ? 200 : 201).json(successResponse(userData, 'User profile saved successfully'));
  } catch (error) {
    console.error('Error saving user profile:', error);
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

    if (!doc.exists) {
      return res.status(404).json(errorResponse('User not found', 'NOT_FOUND', 404));
    }

    const user = doc.data();

    // Remove sensitive data if not the requesting user
    if (req.user.uid !== userId) {
      delete user.phone;
      delete user.email;
    }

    res.status(200).json(successResponse(user, 'User profile retrieved successfully'));
  } catch (error) {
    console.error('Error getting user profile:', error);
    next(error);
  }
});

/**
 * GET /users/:userId/posts - Get user's posts count and details
 */
router.get('/:userId/posts', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { limit = 10, offset = 0 } = req.query;

    const snapshot = await db
      .collection('posts')
      .where('userId', '==', userId)
      .where('isActive', '==', true)
      .orderBy('postedAt', 'desc')
      .limit(parseInt(limit) + parseInt(offset))
      .get();

    let posts = [];
    snapshot.forEach((doc) => {
      posts.push(doc.data());
    });

    // Apply offset
    posts = posts.slice(parseInt(offset), parseInt(offset) + parseInt(limit));

    res.status(200).json(successResponse({ posts, count: posts.length }, 'User posts retrieved successfully'));
  } catch (error) {
    console.error('Error getting user posts:', error);
    next(error);
  }
});

/**
 * GET /users/:userId/stats - Get user statistics
 */
router.get('/:userId/stats', async (req, res, next) => {
  try {
    const { userId } = req.params;

    // Get user data
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      return res.status(404).json(errorResponse('User not found', 'NOT_FOUND', 404));
    }

    const user = userDoc.data();

    // Get posts count
    const postsSnapshot = await db
      .collection('posts')
      .where('userId', '==', userId)
      .where('isActive', '==', true)
      .get();

    // Get requests count
    const requestsSnapshot = await db
      .collection('requests')
      .where('postOwnerId', '==', userId)
      .get();

    // Get accepted requests
    const acceptedSnapshot = await db
      .collection('requests')
      .where('postOwnerId', '==', userId)
      .where('status', '==', 'accepted')
      .get();

    const stats = {
      postsCount: postsSnapshot.size,
      requestsCount: requestsSnapshot.size,
      acceptedCount: acceptedSnapshot.size,
      rating: user.rating || 0,
      ratingCount: user.ratingCount || 0,
      averageRating: user.ratingCount ? (user.rating / user.ratingCount).toFixed(1) : 0,
      joinedDate: user.createdAt,
      isVerified: user.isVerified || false,
    };

    res.status(200).json(successResponse(stats, 'User statistics retrieved successfully'));
  } catch (error) {
    console.error('Error getting user stats:', error);
    next(error);
  }
});

/**
 * PUT /users/:userId/rating - Rate a user
 */
router.put('/:userId/rating', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { rating, comment } = req.body;
    const raterId = req.user.uid;

    if (userId === raterId) {
      return res.status(400).json(errorResponse('Cannot rate yourself', 'INVALID_ACTION', 400));
    }

    if (!rating || rating < 1 || rating > 5) {
      return res.status(400).json(errorResponse('Rating must be between 1 and 5', 'INVALID_INPUT', 400));
    }

    // Check if already rated
    const ratingDoc = await db
      .collection('users')
      .doc(userId)
      .collection('ratings')
      .doc(raterId)
      .get();

    const isUpdate = ratingDoc.exists;

    // Save rating
    await db.collection('users').doc(userId).collection('ratings').doc(raterId).set(
      {
        rating,
        comment: comment || '',
        ratedBy: raterId,
        ratedAt: getCurrentTimestamp(),
      },
      { merge: true }
    );

    // Get user data
    const userDoc = await db.collection('users').doc(userId).get();
    const user = userDoc.data();

    let newRating = user.rating || 0;
    let newRatingCount = user.ratingCount || 0;

    if (!isUpdate) {
      newRating += rating;
      newRatingCount += 1;
    } else {
      newRating = newRating - (ratingDoc.data().rating || 0) + rating;
    }

    // Update user rating
    await db.collection('users').doc(userId).update({
      rating: newRating,
      ratingCount: newRatingCount,
    });

    res.status(200).json(
      successResponse(
        {
          rating: newRating,
          ratingCount: newRatingCount,
          averageRating: (newRating / newRatingCount).toFixed(1),
        },
        'User rated successfully'
      )
    );
  } catch (error) {
    console.error('Error rating user:', error);
    next(error);
  }
});

export default router;
