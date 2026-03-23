import express from 'express';
import { db } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';

const router = express.Router();

/**
 * POST /saved - Save a post
 */
router.post('/', async (req, res, next) => {
  try {
    const { postId } = req.body;
    const userId = req.user.uid;

    if (!postId || typeof postId !== 'string') {
      return res.status(400).json(errorResponse('Post ID is required', 'INVALID_INPUT', 400));
    }

    // Check if post exists
    const postDoc = await db.collection('posts').doc(postId).get();
    if (!postDoc.exists) {
      return res.status(404).json(errorResponse('Post not found', 'NOT_FOUND', 404));
    }

    const post = postDoc.data();

    // Check if already saved
    const existingSave = await db
      .collection('users')
      .doc(userId)
      .collection('savedPosts')
      .doc(postId)
      .get();

    if (existingSave.exists) {
      return res.status(400).json(errorResponse('Post already saved', 'ALREADY_SAVED', 400));
    }

    // Save the post
    const saveId = generateId();
    const savedPost = {
      id: saveId,
      postId,
      postTitle: post.title,
      postCategory: post.category,
      postOwnerId: post.userId,
      userId,
      savedAt: getCurrentTimestamp(),
    };

    // Add to user's saved posts collection
    await db.collection('users').doc(userId).collection('savedPosts').doc(postId).set(savedPost);

    // Also save in main savedPosts collection for analytics
    await db.collection('savedPosts').doc(saveId).set(savedPost);

    res.status(201).json(successResponse(savedPost, 'Post saved successfully'));
  } catch (error) {
    console.error('Error saving post:', error);
    next(error);
  }
});

/**
 * GET /saved/:userId - Get user's saved posts
 */
router.get('/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { limit = 20, offset = 0 } = req.query;

    // Check authorization
    if (req.user.uid !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to view these saved posts', 'FORBIDDEN', 403));
    }

    const snapshot = await db
      .collection('users')
      .doc(userId)
      .collection('savedPosts')
      .orderBy('savedAt', 'desc')
      .limit(parseInt(limit) + parseInt(offset))
      .get();

    let savedPosts = [];
    snapshot.forEach((doc) => {
      savedPosts.push(doc.data());
    });

    // Apply offset
    savedPosts = savedPosts.slice(parseInt(offset), parseInt(offset) + parseInt(limit));

    // Fetch full post details
    const postsWithDetails = await Promise.all(
      savedPosts.map(async (saved) => {
        const postDoc = await db.collection('posts').doc(saved.postId).get();
        if (postDoc.exists) {
          return { ...saved, post: postDoc.data() };
        }
        return saved;
      })
    );

    res.status(200).json(
      successResponse({ savedPosts: postsWithDetails, count: postsWithDetails.length }, 'Saved posts retrieved successfully')
    );
  } catch (error) {
    console.error('Error getting saved posts:', error);
    next(error);
  }
});

/**
 * DELETE /saved/:postId - Remove saved post
 */
router.delete('/:postId', async (req, res, next) => {
  try {
    const { postId } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('users').doc(userId).collection('savedPosts').doc(postId).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Saved post not found', 'NOT_FOUND', 404));
    }

    // Delete from user's saved posts
    await db.collection('users').doc(userId).collection('savedPosts').doc(postId).delete();

    // Delete from main saved posts collection
    const mainDoc = await db.collection('savedPosts').where('postId', '==', postId).where('userId', '==', userId).get();

    mainDoc.forEach(async (d) => {
      await d.ref.delete();
    });

    res.status(200).json(successResponse(null, 'Saved post removed successfully'));
  } catch (error) {
    console.error('Error removing saved post:', error);
    next(error);
  }
});

/**
 * GET /saved/check/:postId - Check if post is saved
 */
router.get('/check/:postId', async (req, res, next) => {
  try {
    const { postId } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('users').doc(userId).collection('savedPosts').doc(postId).get();

    res.status(200).json(
      successResponse({ isSaved: doc.exists }, doc.exists ? 'Post is saved' : 'Post is not saved')
    );
  } catch (error) {
    console.error('Error checking saved post:', error);
    next(error);
  }
});

export default router;
