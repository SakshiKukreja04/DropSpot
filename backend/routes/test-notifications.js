/**
 * Test Notifications Route (Local Testing Only)
 * 
 * ⚠️  WARNING: This route is for LOCAL TESTING ONLY
 * In production, remove or secure this route behind admin-only middleware
 * 
 * These endpoints allow manual testing of FCM notifications without
 * deployed Cloud Functions. Use these to test the Android app locally.
 */

import express from 'express';
import { sendFCMNotification, sendFCMToMultipleUsers, sendTopicMessage } from '../utils/fcm-helper.js';
import { successResponse, errorResponse } from '../utils/helpers.js';

const router = express.Router();

/**
 * POST /api/test-notifications/send
 * 
 * Send a test FCM notification to a specific user
 * 
 * Request body:
 * {
 *   userId: "user_id_here",
 *   title: "Test Title",
 *   body: "Test message body",
 *   data: { type: "test", customField: "value" }  // optional
 * }
 * 
 * Example curl:
 * curl -X POST http://localhost:5000/api/test-notifications/send \
 *   -H "Authorization: Bearer AUTH_TOKEN" \
 *   -H "Content-Type: application/json" \
 *   -d '{
 *     "userId": "YOUR_USER_ID",
 *     "title": "Test Notification",
 *     "body": "This is a test FCM notification"
 *   }'
 */
router.post('/send', async (req, res, next) => {
  try {
    const { userId, title, body, data } = req.body;
    
    // Validation
    if (!userId || !title || !body) {
      return res.status(400).json(
        errorResponse(
          'Missing required fields: userId, title, body',
          'VALIDATION_ERROR',
          400
        )
      );
    }
    
    console.log(`[TEST] Sending FCM to user: ${userId}`);
    
    const result = await sendFCMNotification(userId, title, body, data || {});
    
    res.status(200).json(
      successResponse(result, 'Test notification sent successfully')
    );
  } catch (error) {
    console.error('[TEST] Error sending test notification:', error);
    res.status(500).json(
      errorResponse(error.message, 'FCM_ERROR', 500)
    );
  }
});

/**
 * POST /api/test-notifications/send-request
 * 
 * Simulate a "New Request" notification
 * 
 * Request body:
 * {
 *   postOwnerId: "user_a_id",
 *   requesterName: "User B",
 *   postTitle: "iPhone 13"
 * }
 */
router.post('/send-request', async (req, res, next) => {
  try {
    const { postOwnerId, requesterName, postTitle } = req.body;
    
    if (!postOwnerId || !requesterName || !postTitle) {
      return res.status(400).json(
        errorResponse(
          'Missing required fields: postOwnerId, requesterName, postTitle',
          'VALIDATION_ERROR',
          400
        )
      );
    }
    
    console.log(`[TEST] Simulating new request notification`);
    
    const result = await sendFCMNotification(
      postOwnerId,
      'New Request Received',
      `${requesterName} is interested in your ${postTitle}`,
      {
        type: 'request',
        requesterName,
        postTitle,
      }
    );
    
    res.status(200).json(
      successResponse(result, 'Request notification sent successfully')
    );
  } catch (error) {
    console.error('[TEST] Error sending request notification:', error);
    res.status(500).json(
      errorResponse(error.message, 'FCM_ERROR', 500)
    );
  }
});

/**
 * POST /api/test-notifications/send-accepted
 * 
 * Simulate a "Request Accepted" notification
 * 
 * Request body:
 * {
 *   requesterId: "user_b_id",
 *   postTitle: "iPhone 13"
 * }
 */
router.post('/send-accepted', async (req, res, next) => {
  try {
    const { requesterId, postTitle } = req.body;
    
    if (!requesterId || !postTitle) {
      return res.status(400).json(
        errorResponse(
          'Missing required fields: requesterId, postTitle',
          'VALIDATION_ERROR',
          400
        )
      );
    }
    
    console.log(`[TEST] Simulating request accepted notification`);
    
    const result = await sendFCMNotification(
      requesterId,
      'Request Accepted! 🎉',
      `Your request for ${postTitle} has been accepted!`,
      {
        type: 'accepted',
        postTitle,
        status: 'accepted',
      }
    );
    
    res.status(200).json(
      successResponse(result, 'Accepted notification sent successfully')
    );
  } catch (error) {
    console.error('[TEST] Error sending accepted notification:', error);
    res.status(500).json(
      errorResponse(error.message, 'FCM_ERROR', 500)
    );
  }
});

/**
 * POST /api/test-notifications/send-rejected
 * 
 * Simulate a "Request Rejected" notification
 * 
 * Request body:
 * {
 *   requesterId: "user_b_id",
 *   postTitle: "iPhone 13"
 * }
 */
router.post('/send-rejected', async (req, res, next) => {
  try {
    const { requesterId, postTitle } = req.body;
    
    if (!requesterId || !postTitle) {
      return res.status(400).json(
        errorResponse(
          'Missing required fields: requesterId, postTitle',
          'VALIDATION_ERROR',
          400
        )
      );
    }
    
    console.log(`[TEST] Simulating request rejected notification`);
    
    const result = await sendFCMNotification(
      requesterId,
      'Request Update',
      `Your request for ${postTitle} was not accepted.`,
      {
        type: 'rejected',
        postTitle,
        status: 'rejected',
      }
    );
    
    res.status(200).json(
      successResponse(result, 'Rejected notification sent successfully')
    );
  } catch (error) {
    console.error('[TEST] Error sending rejected notification:', error);
    res.status(500).json(
      errorResponse(error.message, 'FCM_ERROR', 500)
    );
  }
});

/**
 * POST /api/test-notifications/send-broadcast
 * 
 * Send notification to multiple users at once
 * 
 * Request body:
 * {
 *   userIds: ["user_id_1", "user_id_2"],
 *   title: "Broadcast Message",
 *   body: "This goes to multiple users",
 *   data: { type: "broadcast" }  // optional
 * }
 */
router.post('/send-broadcast', async (req, res, next) => {
  try {
    const { userIds, title, body, data } = req.body;
    
    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      return res.status(400).json(
        errorResponse(
          'Missing or invalid userIds array',
          'VALIDATION_ERROR',
          400
        )
      );
    }
    
    if (!title || !body) {
      return res.status(400).json(
        errorResponse(
          'Missing required fields: title, body',
          'VALIDATION_ERROR',
          400
        )
      );
    }
    
    console.log(`[TEST] Sending broadcast to ${userIds.length} users`);
    
    const results = await sendFCMToMultipleUsers(userIds, title, body, data || {});
    
    const successCount = results.filter((r) => r.success).length;
    const failureCount = results.filter((r) => !r.success).length;
    
    res.status(200).json(
      successResponse(
        { results, successCount, failureCount },
        `Broadcast sent (${successCount} success, ${failureCount} failures)`
      )
    );
  } catch (error) {
    console.error('[TEST] Error sending broadcast:', error);
    res.status(500).json(
      errorResponse(error.message, 'FCM_ERROR', 500)
    );
  }
});

/**
 * GET /api/test-notifications/help
 * 
 * Get help and usage information for test endpoints
 */
router.get('/help', (req, res) => {
  res.status(200).json({
    success: true,
    message: 'Test Notifications Endpoints Documentation',
    warning: '⚠️  These endpoints are for LOCAL TESTING ONLY. Remove in production.',
    endpoints: [
      {
        method: 'POST',
        path: '/api/test-notifications/send',
        description: 'Send a custom test notification',
        body: {
          userId: 'string (required)',
          title: 'string (required)',
          body: 'string (required)',
          data: 'object (optional)',
        },
      },
      {
        method: 'POST',
        path: '/api/test-notifications/send-request',
        description: 'Simulate a new request notification',
        body: {
          postOwnerId: 'string (required)',
          requesterName: 'string (required)',
          postTitle: 'string (required)',
        },
      },
      {
        method: 'POST',
        path: '/api/test-notifications/send-accepted',
        description: 'Simulate a request accepted notification',
        body: {
          requesterId: 'string (required)',
          postTitle: 'string (required)',
        },
      },
      {
        method: 'POST',
        path: '/api/test-notifications/send-rejected',
        description: 'Simulate a request rejected notification',
        body: {
          requesterId: 'string (required)',
          postTitle: 'string (required)',
        },
      },
      {
        method: 'POST',
        path: '/api/test-notifications/send-broadcast',
        description: 'Send notification to multiple users',
        body: {
          userIds: 'array of strings (required)',
          title: 'string (required)',
          body: 'string (required)',
          data: 'object (optional)',
        },
      },
    ],
  });
});

export default router;
