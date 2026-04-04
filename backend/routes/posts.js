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
    const { title, description, category, condition, images, latitude, longitude } = req.body;
    const userId = req.user.uid;

    // Strict validation
    if (!title || !description || !category || !condition) {
      return res.status(400).json(errorResponse('Missing required fields', 'VALIDATION_ERROR', 400));
    }

    // Ensure coordinates are present
    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json(errorResponse('Location required', 'VALIDATION_ERROR', 400));
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

    // Increment user's post count
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
    const { category, limit = 50, offset = 0, latitude, longitude, maxDistance = 2.5, myPostsOnly } = req.query;
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
      // Exclude current user's posts from home feed
      if (myPostsOnly !== 'true' && data.userId === currentUserId) return;
      posts.push(data);
    });

    // Distance filtering
    if (latitude && longitude) {
      const userLat = parseFloat(latitude);
      const userLon = parseFloat(longitude);
      const maxDist = parseFloat(maxDistance);

      posts = posts
        .map(post => {
          const distance = calculateDistance(userLat, userLon, post.latitude, post.longitude);
          return { ...post, distance };
        })
        .filter(post => post.distance <= maxDist)
        .sort((a, b) => a.distance - b.distance);
    } else {
      // Sort by newest if no location
      posts.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''));
    }

    // Pagination
    const paginatedPosts = posts.slice(Number(offset), Number(offset) + Number(limit));

    res.status(200).json(successResponse({ posts: paginatedPosts, count: posts.length }, 'Posts retrieved successfully'));
  } catch (error) {
    next(error);
  }
});

export default router;
