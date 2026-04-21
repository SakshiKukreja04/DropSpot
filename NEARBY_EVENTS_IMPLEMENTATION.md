# Nearby Events Implementation Guide

## Overview
This implementation enables DropSpot to show upcoming events to nearby users (within 2.5 km radius) and notify them with attendance tracking and event owner notifications.

## Features Implemented

### 1. **Event Location Tracking**
- Added `latitude` and `longitude` fields to the `Event` model
- Automatically captures the creator's location when they create an event
- Uses Fused Location Provider for accurate positioning

### 2. **Nearby Events Discovery (2.5 km Radius)**
- AnnouncementsActivity filters events based on user's location
- Uses Haversine formula to calculate distance between event and user
- Only displays events within 2.5 km radius
- Falls back to showing all events if location is unavailable

### 3. **Event Notifications to Nearby Users**
When a new event is created:
- System queries users collection
- Identifies users within 2.5 km radius
- Creates `EVENT_NEARBY` notifications for each nearby user
- Notifications appear in the Announcements section

### 4. **Event Attendance System**
- Attend button in event list and details
- Clicking "Attend" adds user to event attendees list
- Button shows "✓ Attending" after user attends
- Updates attendee count in real-time

### 5. **Event Owner Notifications**
When a user attends an event:
- Creates `EVENT_ATTEND` notification for event owner
- Shows: "[User Name] is attending your event: [Event Name]"
- Appears in Announcements notifications section
- Includes attendee ID for owner reference

## Database Schema Updates

### Event Collection
```json
{
  "eventId": "uuid",
  "eventName": "Event Name",
  "description": "Event details",
  "location": "Address",
  "date": "DD MMM YYYY",
  "startTime": "HH:MM",
  "endTime": "HH:MM",
  "latitude": 19.0760,
  "longitude": 72.8777,
  "category": "Category",
  "ownerId": "user-id",
  "ownerName": "Owner Name",
  "attendees": [
    {
      "userId": "user-id",
      "name": "Attendee Name",
      "joinedAt": 1629800000000
    }
  ],
  "createdAt": 1629800000000,
  "updatedAt": 1629800000000
}
```

### Notifications Collection
```json
{
  "receiverId": "user-id",
  "type": "EVENT_NEARBY" | "EVENT_ATTEND",
  "message": "Notification text",
  "eventId": "event-id",
  "attendeeId": "attendee-id",
  "timestamp": 1629800000000
}
```

## Files Modified

### 1. **Event.java**
- Added `latitude` and `longitude` fields
- Used for location-based filtering

### 2. **CreateEventActivity.java**
- Integrated FusedLocationProviderClient
- Captures current user location when event is created
- Added location permission handling
- Added `notifyNearbyUsers()` method
- Added `calculateDistance()` using Haversine formula

### 3. **EventsAdapter.java**
- Added Attend button functionality
- Implements attendance logic
- Notifies event owner when user attends
- Updates UI to show attendance status
- Real-time attendee count update

### 4. **AnnouncementsActivity.java**
- Integrated FusedLocationProviderClient
- Added location permission handling
- Filters events by 2.5 km radius
- Added `calculateDistance()` method
- Added support for `EVENT_NEARBY` notification type

## User Flow

### Creating an Event
1. User clicks + icon → selects "📍 Event"
2. Opens CreateEventActivity
3. System captures user's location (with permission)
4. User enters: Title, Description, Date, Time, Location, Category
5. Clicks "Create Event"
6. Event is saved with coordinates
7. Nearby users (within 2.5 km) receive `EVENT_NEARBY` notification
8. Events appear in all nearby users' Announcements section

### Viewing Nearby Events
1. User navigates to Announcements
2. System gets user's location
3. Loads only events within 2.5 km radius
4. Displays events with:
   - Event name and description
   - Date and time
   - Location
   - Number of attendees
   - "Attend" button

### Attending an Event
1. User sees event in list
2. Clicks "Attend" button
3. User added to event's attendees list
4. Button changes to "✓ Attending" and becomes disabled
5. Event owner receives `EVENT_ATTEND` notification
6. Attendee count increments

## Permissions Required

Add to AndroidManifest.xml (already configured):
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## Testing Checklist

- [ ] Create an event with location
- [ ] Verify event appears in Announcements for nearby users
- [ ] Verify event doesn't appear for distant users
- [ ] Click "Attend" button
- [ ] Verify button changes to "✓ Attending"
- [ ] Verify event owner receives notification
- [ ] Verify attendee count increments
- [ ] Check notifications appear in Announcements section
- [ ] Test location permission handling

## Distance Calculation

Uses Haversine formula with Earth radius of 6371 km:
```
distance = R × c
where c = 2 × atan2(√a, √(1-a))
and a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlng/2)
```

## Notifications Types

- **EVENT_NEARBY**: Sent to users within 2.5 km when new event created
- **EVENT_ATTEND**: Sent to event owner when someone attends

## Future Enhancements

1. Add event filtering by category
2. Show distance to each event
3. Add event search functionality
4. Implement real-time location updates
5. Add event reminders
6. Event capacity/limit attendees
7. Cancel attendance option
8. Event rating/reviews system

