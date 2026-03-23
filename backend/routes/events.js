import express from 'express';
import { db } from '../config/firebase.js';
import { generateId, getCurrentTimestamp, successResponse, errorResponse } from '../utils/helpers.js';

const router = express.Router();

/**
 * POST /events - Create a new event
 */
router.post('/', async (req, res, next) => {
  try {
    const { title, description, category, startDate, endDate, location, latitude, longitude, images } = req.body;
    const userId = req.user.uid;

    // Validate required fields
    if (!title || !description || !category || !startDate) {
      return res.status(400).json(errorResponse('Missing required fields', 'INVALID_INPUT', 400));
    }

    // Create event object
    const eventId = generateId();
    const event = {
      id: eventId,
      userId,
      title,
      description,
      category,
      startDate,
      endDate: endDate || null,
      location: location || null,
      latitude: latitude || null,
      longitude: longitude || null,
      images: images || [],
      attendees: [userId], // Creator is first attendee
      attendeeCount: 1,
      createdAt: getCurrentTimestamp(),
      updatedAt: getCurrentTimestamp(),
      isActive: true,
    };

    // Save to Firestore
    await db.collection('events').doc(eventId).set(event);

    // Add to user's events
    await db.collection('users').doc(userId).collection('events').doc(eventId).set({
      eventId,
      createdAt: getCurrentTimestamp(),
      isCreator: true,
    });

    res.status(201).json(successResponse(event, 'Event created successfully'));
  } catch (error) {
    console.error('Error creating event:', error);
    next(error);
  }
});

/**
 * GET /events - Get all events with optional filters
 */
router.get('/', async (req, res, next) => {
  try {
    const { category, userId, limit = 20, offset = 0, upcomingOnly = true } = req.query;

    let query = db.collection('events').where('isActive', '==', true);

    // Filter by category if provided
    if (category) {
      query = query.where('category', '==', category);
    }

    // Filter by userId if provided
    if (userId) {
      query = query.where('userId', '==', userId);
    }

    // Get documents
    let snapshot;
    if (upcomingOnly === 'true') {
      const now = new Date().toISOString();
      snapshot = await query
        .where('startDate', '>=', now)
        .orderBy('startDate', 'asc')
        .limit(parseInt(limit) + parseInt(offset))
        .get();
    } else {
      snapshot = await query.orderBy('startDate', 'desc').limit(parseInt(limit) + parseInt(offset)).get();
    }

    let events = [];
    snapshot.forEach((doc) => {
      events.push(doc.data());
    });

    // Apply offset
    events = events.slice(parseInt(offset), parseInt(offset) + parseInt(limit));

    res.status(200).json(successResponse({ events, count: events.length }, 'Events retrieved successfully'));
  } catch (error) {
    console.error('Error getting events:', error);
    next(error);
  }
});

/**
 * GET /events/:id - Get event details
 */
router.get('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;

    const doc = await db.collection('events').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Event not found', 'NOT_FOUND', 404));
    }

    const event = doc.data();

    // Get event creator info
    const userDoc = await db.collection('users').doc(event.userId).get();
    const userData = userDoc.exists ? userDoc.data() : null;

    res.status(200).json(
      successResponse(
        {
          ...event,
          creator: userData
            ? { uid: event.userId, name: userData.name, email: userData.email, photo: userData.photo }
            : null,
        },
        'Event retrieved successfully'
      )
    );
  } catch (error) {
    console.error('Error getting event:', error);
    next(error);
  }
});

/**
 * PUT /events/:id - Update event
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;
    const { title, description, category, startDate, endDate, location, images } = req.body;

    const doc = await db.collection('events').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Event not found', 'NOT_FOUND', 404));
    }

    const event = doc.data();

    // Check ownership
    if (event.userId !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to update this event', 'FORBIDDEN', 403));
    }

    // Update fields
    const updateData = {};
    if (title !== undefined) updateData.title = title;
    if (description !== undefined) updateData.description = description;
    if (category !== undefined) updateData.category = category;
    if (startDate !== undefined) updateData.startDate = startDate;
    if (endDate !== undefined) updateData.endDate = endDate;
    if (location !== undefined) updateData.location = location;
    if (images !== undefined) updateData.images = images;
    updateData.updatedAt = getCurrentTimestamp();

    await db.collection('events').doc(id).update(updateData);

    const updatedEvent = { ...event, ...updateData };

    res.status(200).json(successResponse(updatedEvent, 'Event updated successfully'));
  } catch (error) {
    console.error('Error updating event:', error);
    next(error);
  }
});

/**
 * DELETE /events/:id - Delete event
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('events').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Event not found', 'NOT_FOUND', 404));
    }

    const event = doc.data();

    // Check ownership
    if (event.userId !== userId) {
      return res.status(403).json(errorResponse('Unauthorized to delete this event', 'FORBIDDEN', 403));
    }

    // Soft delete
    await db.collection('events').doc(id).update({
      isActive: false,
      updatedAt: getCurrentTimestamp(),
    });

    // Delete from user's events
    await db.collection('users').doc(userId).collection('events').doc(id).delete();

    res.status(200).json(successResponse(null, 'Event deleted successfully'));
  } catch (error) {
    console.error('Error deleting event:', error);
    next(error);
  }
});

/**
 * POST /events/:id/join - Join event as attendee
 */
router.post('/:id/join', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('events').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Event not found', 'NOT_FOUND', 404));
    }

    const event = doc.data();

    // Check if already attendee
    if (event.attendees && event.attendees.includes(userId)) {
      return res.status(400).json(errorResponse('Already attending this event', 'ALREADY_ATTENDEE', 400));
    }

    // Add attendee
    const newAttendees = [...(event.attendees || []), userId];
    await db.collection('events').doc(id).update({
      attendees: newAttendees,
      attendeeCount: newAttendees.length,
    });

    // Add event to user's events
    await db.collection('users').doc(userId).collection('events').doc(id).set({
      eventId: id,
      joinedAt: getCurrentTimestamp(),
      isCreator: false,
    });

    res.status(200).json(successResponse({ attendeeCount: newAttendees.length }, 'Joined event successfully'));
  } catch (error) {
    console.error('Error joining event:', error);
    next(error);
  }
});

/**
 * DELETE /events/:id/leave - Leave event
 */
router.delete('/:id/leave', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.uid;

    const doc = await db.collection('events').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json(errorResponse('Event not found', 'NOT_FOUND', 404));
    }

    const event = doc.data();

    // Check if event creator (cannot leave own event)
    if (event.userId === userId) {
      return res.status(400).json(errorResponse('Event creator cannot leave event', 'INVALID_ACTION', 400));
    }

    // Remove attendee
    const newAttendees = (event.attendees || []).filter((id) => id !== userId);
    await db.collection('events').doc(id).update({
      attendees: newAttendees,
      attendeeCount: newAttendees.length,
    });

    // Remove from user's events
    await db.collection('users').doc(userId).collection('events').doc(id).delete();

    res.status(200).json(successResponse({ attendeeCount: newAttendees.length }, 'Left event successfully'));
  } catch (error) {
    console.error('Error leaving event:', error);
    next(error);
  }
});

export default router;
