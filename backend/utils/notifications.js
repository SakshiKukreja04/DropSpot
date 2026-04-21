import admin from '../config/firebase.js';

/**
 * Send a push notification to a specific user via their FCM token
 * @param {string} fcmToken - The target device's FCM token
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {Object} data - Optional data payload (key-value pairs)
 */
export const sendPushNotification = async (fcmToken, title, body, data = {}) => {
  if (!fcmToken) {
    console.log('[FCM] No token provided, skipping push');
    return null;
  }

  console.log(`[FCM] Sending notification with token: ${fcmToken.substring(0, 20)}... Title: "${title}"`);

  const message = {
    notification: {
      title,
      body,
    },
    data: {
      ...data,
      click_action: 'FLUTTER_NOTIFICATION_CLICK',
    },
    token: fcmToken,
  };

  try {
    const response = await admin.messaging().send(message);
    console.log('[FCM] Message sent successfully:', response);
    return response;
  } catch (error) {
    console.error('[FCM] Error sending message:', error.message);
    throw error;
  }
};
