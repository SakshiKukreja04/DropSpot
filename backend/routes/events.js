import express from 'express';
import { db, FieldValue } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse, calculateDistance } from '../utils/helpers.js';
import admin from '../config/firebase.js';

const router = express.Router();

/**
 * Helper to send FCM notification
 */
async function sendFCMNotification(token, title, body, data) {
  if (!token) return;

  const stringData = {};
  if (data) {
    Object.keys(data).forEach(key => {
      stringData[key] = String(data[key]);
    });
  }

  const message = {
    token: token,
    notification: { title, body },
    data: stringData,
    android: {
      priority: 'high',
      notification: {
        channel_id: 'dropspot_notifications',
        priority: 'high'
      }
    }
  };

  try {
    await admin.messaging().send(message);
    console.log(`[FCM] Sent to: ...${token.substring(token.length - 10)}`);
  } catch (error) {
    console.error('[FCM] Error:', error.message);
  }
}

/**
 * POST /events - Create a new event and notify nearby users
 */
router.post('/', async (req, res, next) => {
  try {
    const { eventName, title: bodyTitle, description, date, startTime, endTime, location, category, latitude, longitude } = req.body;
    const userId = req.user.uid;

    const title = bodyTitle || eventName;
    if (!title || !description || !startTime) {
      return res.status(400).json(errorResponse('Missing required fields', 'INVALID_INPUT', 400));
    }

    const eventId = generateId();
    const timestamp = Date.now();

    const event = {
      eventId,
      title,
      eventName: title,
      description,
      latitude: latitude ? Number(latitude) : null,
      longitude: longitude ? Number(longitude) : null,
      ownerId: userId,
      attendeesCount: 1,
      attendees: [userId],
      category: category || 'General',
      date: date || (startTime.includes(',') ? startTime.split(',')[0].trim() : ''),
      startTime,
      endTime: endTime || null,
      location: location || null,
      isActive: true,
      createdAt: timestamp,
      updatedAt: timestamp
    };

    await db.collection('events').doc(eventId).set(event);
    console.log(`[EVENT_CREATE] Created: ${title} by ${userId}`);

    // Notify nearby users
    if (event.latitude && event.longitude) {
      const usersSnapshot = await db.collection('users').get();
      const nearbyPromises = [];

      usersSnapshot.forEach(doc => {
        const userData = doc.data();
        const recipientUid = userData.uid || doc.id;

        // Skip owner, but check proximity for others with tokens
        if (recipientUid !== userId && userData.fcmToken && userData.latitude && userData.longitude) {
          const distance = calculateDistance(event.latitude, event.longitude, userData.latitude, userData.longitude);
          if (distance <= 2.5) {
            nearbyPromises.push(
              sendFCMNotification(
                userData.fcmToken,
                "New Event Nearby 📍",
                `"${title}" is happening near you!`,
                {
                  type: "event_created",
                  eventId: eventId
                }
              )
            );
          }
        }
      });
      if (nearbyPromises.length > 0) Promise.all(nearbyPromises).catch(e => console.error(e));
    }

    res.status(201).json(successResponse(event, 'Event created successfully'));
  } catch (error) { next(error); }
});

/**
 * GET /events/upcoming - Get upcoming events
 */
router.get('/upcoming', async (req, res, next) => {
  try {
    const snapshot = await db.collection('events')
      .where('isActive', '==', true)
      .limit(50)
      .get();

    let events = [];
    snapshot.forEach(doc => events.push(doc.data()));

    // Sort manually to avoid index requirement
    events.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

    res.status(200).json(successResponse(events, 'Upcoming events fetched'));
  } catch (error) { next(error); }
});

/**
 * POST /events/attend - Attend an event
 */
router.post('/attend', async (req, res, next) => {
  try {
    const { eventId, userId: bodyUserId } = req.body;
    const userId = bodyUserId || req.user.uid;

    const eventRef = db.collection('events').doc(eventId);
    const eventDoc = await eventRef.get();
    if (!eventDoc.exists) return res.status(404).json(errorResponse('Event not found'));

    const event = eventDoc.data();
    if (event.attendees && event.attendees.includes(userId)) {
        return res.status(400).json(errorResponse('Already attending'));
    }

    await eventRef.update({
      attendees: FieldValue.arrayUnion(userId),
      attendeesCount: FieldValue.increment(1),
      updatedAt: Date.now()
    });

    // Notify owner
    const ownerDoc = await db.collection('users').doc(event.ownerId).get();
    if (ownerDoc.exists && ownerDoc.data().fcmToken && event.ownerId !== userId) {
        const userDoc = await db.collection('users').doc(userId).get();
        const userName = userDoc.exists ? (userDoc.data().name || userDoc.data().displayName) : 'Someone';

        await sendFCMNotification(
            ownerDoc.data().fcmToken,
            "New Attendee! 🎉",
            `${userName} joined your event: ${event.title}`,
            {
                type: "event_attend",
                eventId: eventId,
                recipientUserId: event.ownerId
            }
        );
    }

    res.status(200).json(successResponse(null, 'Joined event successfully'));
  } catch (error) { next(error); }
});

/**
 * GET /events - Get all active events
 */
router.get('/', async (req, res, next) => {
  try {
    const snapshot = await db.collection('events')
      .where('isActive', '==', true)
      .get();

    const events = [];
    snapshot.forEach(doc => events.push(doc.data()));

    // Sort manually
    events.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

    res.status(200).json(successResponse(events, 'Events retrieved successfully'));
  } catch (error) { next(error); }
});

export default router;
