import express from 'express';
import { db } from '../config/firebase.js';
import {
  generateId,
  getCurrentTimestamp,
  validatePostData,
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
    const { title, description, category, latitude, longitude, images, condition } = req.body;
    const userId = req.user.uid;

    // Validate input
    const validation = validatePostData(req.body);
    if (!validation.isValid) {
      return res.status(400).json(errorResponse('Validation failed', 'VALIDATION_ERROR', 400, validation.errors));
    }

    // Create post object
    const postId = generateId();
    const post = {
      id: postId,
      userId,
      title,
      description,
      category,
      condition: condition || 'good',
      latitude: latitude || null,
      longitude: longitude || null,
      images: images || [],
      postedAt: getCurrentTimestamp(),
      updatedAt: getCurrentTimestamp(),
      requestCount: 0,
      viewCount: 0,
      isActive: true,
    };

    // Save to Firestore
    await db.collection('posts').doc(postId).set(post);

    // Also add to user's posts subcollection
    await db.collection('users').doc(userId).collection('posts').doc(postId).set({
      postId,
      createdAt: getCurrentTimestamp(),
    });

    res.status(201).json(successResponse(post, 'Post created successfully'));
  } catch (error) {
    console.error('Error creating post:', error);
    next(error);
  }
});

/**
 * GET /posts - Get all posts with optional filters
 */
router.get('/', async (req, res, next) => {
  try {
    const { category, userId, limit = 20, offset = 0, latitude, longitude, maxDistance = 50 } = req.query;

    let query = db.collection('posts').where('isActive', '==', true);

    // Filter by category if provided
    if (category) {
      query = query.where('category', '==', category);
    }

    // Filter by userId if provided
    if (userId) {
      query = query.where('userId', '==', userId);
    }

    // Get documents
    const snapshot = await query.orderBy('postedAt', 'desc').limit(parseInt(limit) + parseInt(offset)).get();

    let posts = [];
    snapshot.forEach((doc) => {
      posts.push(doc.data());
    });

    // Apply offset
    posts = posts.slice(parseInt(offset), parseInt(offset) + parseInt(limit));

    // Calculate distance if user coordinates provided
    if (latitude && longitude) {
      const userLat = parseFloat(latitude);
      const userLon = parseFloat(longitude);
      const maxDist = parseInt(maxDistance);

      posts = posts
        .map((post) => {
          if (post.latitude && post.longitude) {
            const distance = calculateDistance(userLat, userLon, post.latitude, post.longitude);
            return { ...post, distance };
          }
          return { ...post, distance: null };
        })
        .filter((post) => !post.distance || post.distance <= maxDist)
        .sort((a, b) => (a.distance || Infinity) - (b.distance || Infinity));
    }

    res.status(200).json(successResponse({ posts, count: posts.length }, 'Posts retrieved successfully'));
  } catch (error) {
    console.error('Error getting posts:', error);
    next(error);
  }
});

/**
 * GET /posts/:id - Get post details
 */
router.get('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;

    const doc = await db.collection('posts').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));
    }

    const post = doc.data();

    // Increment view count
    await db.collection('posts').doc(id).update({
      viewCount: (post.viewCount || 0) + 1,
      updatedAt: getCurrentTimestamp(),
    });

    // Get post owner info
    const userDoc = await db.collection('users').doc(post.userId).get();
    const userData = userDoc.exists ? userDoc.data() : null;

    res.status(200).json(
      successResponse(
        {
          ...post,
          owner: userData ? { uid: post.userId, name: userData.name, email: userData.email, photo: userData.photo } : null,
        },
        'Post retrieved successfully'
      )
    );
  } catch (error) {
    console.error('Error getting post:', error);
    next(error);
  }
});

/**
 * PUT /posts/:id - Update post
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;
    const { title, description, category, condition, images } = req.body;

    const doc = await db.collection('posts').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));
    }

    const post = doc.data();

    // Check ownership
    if (post.userId !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to update this post', 'FORBIDDEN', 403));
    }

    // Update fields
    const updateData = {};
    if (title !== undefined) updateData.title = title;
    if (description !== undefined) updateData.description = description;
    if (category !== undefined) updateData.category = category;
    if (condition !== undefined) updateData.condition = condition;
    if (images !== undefined) updateData.images = images;
    updateData.updatedAt = getCurrentTimestamp();

    await db.collection('posts').doc(id).update(updateData);

    const updatedPost = { ...post, ...updateData };

    res.status(200).json(successResponse(updatedPost, 'Post updated successfully'));
  } catch (error) {
    console.error('Error updating post:', error);
    next(error);
  }
});

/**
 * DELETE /posts/:id - Delete post
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('posts').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));
    }

    const post = doc.data();

    // Check ownership
    if (post.userId !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to delete this post', 'FORBIDDEN', 403));
    }

    // Soft delete (mark as inactive)
    await db.collection('posts').doc(id).update({
      isActive: false,
      updatedAt: getCurrentTimestamp(),
    });

    // Delete from user's posts subcollection
    await db.collection('users').doc(userId).collection('posts').doc(id).delete();

    res.status(200).json(successResponse(null, 'Post deleted successfully'));
  } catch (error) {
    console.error('Error deleting post:', error);
    next(error);
  }
});

/**
 * PUT /posts/:id/view - Increment view count
 */
router.put('/:id/view', async (req, res, next) => {
  try {
    const { id } = req.params;

    const doc = await db.collection('posts').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));
    }

    const post = doc.data();

    await db.collection('posts').doc(id).update({
      viewCount: (post.viewCount || 0) + 1,
    });

    res.status(200).json(successResponse(null, 'View count incremented'));
  } catch (error) {
    console.error('Error updating view count:', error);
    next(error);
  }
});

export default router;
