import admin from 'firebase-admin';

/**
 * Send FCM notification via Firebase Admin SDK
 * Used for local testing and manual notification triggers
 */
export async function sendFCMNotification(userId, title, body, data = {}) {
  try {
    const db = admin.firestore();
    
    console.log(`[FCM] 🚀 Attempting to send notification to user: ${userId}`);

    // Validate inputs
    if (!userId) {
      throw new Error('userId is required');
    }
    if (!title || !body) {
      throw new Error('title and body are required');
    }

    // Get FCM token from Firestore
    console.log(`[FCM] Fetching user document for: ${userId}`);
    const userDoc = await db.collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      console.error(`[FCM] ❌ User document not found for ${userId}`);
      throw new Error(`User ${userId} not found in Firestore`);
    }
    
    const userData = userDoc.data();
    console.log(`[FCM] User document found. Fields:`, Object.keys(userData));

    const fcmToken = userData.fcmToken;

    if (!fcmToken || fcmToken.trim() === '') {
      console.error(`[FCM] ❌ No valid fcmToken in user document for ${userId}`);
      console.error(`[FCM] fcmToken value:`, fcmToken);
      console.error(`[FCM] Available fields:`, Object.keys(userData));
      throw new Error(`No FCM token found for user ${userId}. User may not have logged in or granted permissions.`);
    }
    
    console.log(`[FCM] ✅ Found FCM token for user ${userId}: ${fcmToken.substring(0, 30)}...`);

    // Validate FCM token format (should be long string)
    if (fcmToken.length < 50) {
      console.warn(`[FCM] ⚠️  FCM token seems short (${fcmToken.length} chars), might be invalid`);
    }

    // Send notification via FCM
    const message = {
      notification: {
        title: title || 'DropSpot',
        body: body,
      },
      data: {
        ...data,
        timestamp: new Date().toISOString(),
      },
      token: fcmToken,
      android: {
        ttl: 86400, // 24 hours
        priority: 'high',
      },
      apns: {
        headers: {
          'apns-priority': '10',
        },
      },
    };
    
    console.log(`[FCM] Sending message to token: ${fcmToken.substring(0, 30)}...`);
    const response = await admin.messaging().send(message);
    
    console.log(`[FCM] ✅ Notification sent successfully!`);
    console.log(`[FCM] Message ID:`, response);

    return {
      success: true,
      messageId: response,
      token: fcmToken.substring(0, 30) + '...', // Log truncated token for privacy
      timestamp: new Date().toISOString(),
    };
  } catch (error) {
    console.error('[FCM] ❌ Error sending FCM notification:', error.message);
    console.error('[FCM] Full error:', error);
    throw error;
  }
}

/**
 * Send notification to multiple users
 */
export async function sendFCMToMultipleUsers(userIds, title, body, data = {}) {
  const results = [];
  
  for (const userId of userIds) {
    try {
      const result = await sendFCMNotification(userId, title, body, data);
      results.push({ userId, success: true, ...result });
    } catch (error) {
      results.push({ userId, success: false, error: error.message });
    }
  }
  
  return results;
}

/**
 * Subscribe user to a topic for topic-based messaging
 */
export async function subscribeToTopic(tokens, topic) {
  try {
    if (!Array.isArray(tokens)) {
      tokens = [tokens];
    }
    
    const response = await admin.messaging().subscribeToTopic(tokens, topic);
    console.log(`Subscribed to topic "${topic}":`, response);
    
    return response;
  } catch (error) {
    console.error('Error subscribing to topic:', error);
    throw error;
  }
}

/**
 * Send topic message (broadcast to all subscribers)
 */
export async function sendTopicMessage(topic, title, body, data = {}) {
  try {
    const message = {
      notification: {
        title: title,
        body: body,
      },
      data: data,
      topic: topic,
    };
    
    const response = await admin.messaging().send(message);
    console.log(`Topic message sent to "${topic}":`, response);
    
    return {
      success: true,
      messageId: response,
      topic: topic,
    };
  } catch (error) {
    console.error('Error sending topic message:', error);
    throw error;
  }
}
