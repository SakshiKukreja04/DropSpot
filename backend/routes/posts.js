import express from 'express';
import { db, FieldValue } from '../config/firebase.js';
import {
  generateId,
  getCurrentTimestamp,
  successResponse,
  errorResponse,
  calculateDistance,
} from '../utils/helpers.js';

const router = express.Router();

/**
 * POST /posts - Create a new post
 */
router.post('/', async (req, res, next) => {
  try {
    const { title, description, category, condition, price, images, latitude, longitude } = req.body;
    const userId = req.user.uid;

    if (!title || !description || !category || !condition) {
      return res.status(400).json(errorResponse('Missing required fields', 'VALIDATION_ERROR', 400));
    }

    if (latitude === undefined || latitude === null || longitude === undefined || longitude === null) {
      return res.status(400).json(errorResponse('Location coordinates are required', 'VALIDATION_ERROR', 400));
    }

    const postId = generateId();
    const timestamp = getCurrentTimestamp();

    const post = {
      id: postId,
      userId: userId,
      title,
      description,
      category,
      condition,
      price: price ? Number(price) : 0,
      images: Array.isArray(images) ? images : [],
      latitude: Number(latitude),
      longitude: Number(longitude),
      requestCount: 0,
      viewCount: 0,
      isActive: true,
      createdAt: timestamp,
      updatedAt: timestamp
    };

    await db.collection('posts').doc(postId).set(post);

    await db.collection('users').doc(userId).update({
      postsCount: FieldValue.increment(1),
      updatedAt: timestamp
    }).catch(() => {});

    res.status(201).json(successResponse(post, 'Post created successfully'));
  } catch (error) {
    next(error);
  }
});

/**
 * GET /posts - Get posts with distance filtering
 */
router.get('/', async (req, res, next) => {
  try {
    const { category, limit = 50, offset = 0, latitude, longitude, maxDistance = 5.0, myPostsOnly } = req.query;
    const currentUserId = req.user.uid;

    let query = db.collection('posts');

    if (myPostsOnly === 'true') {
      query = query.where('userId', '==', currentUserId);
    } else {
      query = query.where('isActive', '==', true);
    }

    if (category && category !== 'All') {
      query = query.where('category', '==', category);
    }

    const snapshot = await query.get();
    let posts = [];

    snapshot.forEach(doc => {
      const data = doc.data();
      if (myPostsOnly !== 'true' && data.userId === currentUserId) return;
      posts.push({ ...data, id: data.id || doc.id });
    });

    if (latitude !== undefined && longitude !== undefined) {
      const userLat = parseFloat(latitude);
      const userLon = parseFloat(longitude);
      const maxDist = parseFloat(maxDistance);

      posts = posts
        .map(post => ({ ...post, distance: calculateDistance(userLat, userLon, post.latitude, post.longitude) }))
        .filter(post => post.distance <= maxDist)
        .sort((a, b) => a.distance - b.distance);
    } else {
      posts.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''));
    }

    const paginatedPosts = posts.slice(Number(offset), Number(offset) + Number(limit));
    res.status(200).json(successResponse({ posts: paginatedPosts, count: posts.length }, 'Posts retrieved successfully'));
  } catch (error) {
    next(error);
  }
});

/**
 * GET /posts/:id - Get a single post by ID with owner details
 */
router.get('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const doc = await db.collection('posts').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));
    }

    const post = { ...doc.data(), id: doc.id };

    const ownerId = post.userId;
    if (ownerId) {
        const userDoc = await db.collection('users').doc(ownerId).get();
        if (userDoc.exists) {
            const userData = userDoc.data();
            post.ownerName = userData.name || userData.fullName || userData.displayName || 'Anonymous';
            post.ownerEmail = userData.email;
            post.ownerPhoto = userData.photo || userData.photoURL || userData.photoUri;
        }
    }

    res.status(200).json(successResponse(post, 'Post retrieved successfully'));
  } catch (error) {
    next(error);
  }
});

export default router;
