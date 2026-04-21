import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Cloud Function: Send notification when a new request is created
 * Trigger: Collection "requests" - onCreate
 * 
 * Sends notification to the post owner
 */
export const sendRequestNotification = functions.firestore
  .document("requests/{requestId}")
  .onCreate(async (snap, context) => {
    const request = snap.data();
    const postOwnerId = request.postOwnerId;
    const postTitle = request.postTitle;
    const requesterName = request.requesterName;

    console.log(
      `New request created for post: ${postTitle} by ${requesterName}`
    );

    try {
      // Get the post owner's FCM token
      const userDoc = await db.collection("users").doc(postOwnerId).get();

      if (!userDoc.exists) {
        console.log(`User document not found for: ${postOwnerId}`);
        return;
      }

      const fcmToken = userDoc.data()?.fcmToken;
      if (!fcmToken) {
        console.log(`No FCM token found for user: ${postOwnerId}`);
        return;
      }

      // Send notification via FCM
      await messaging.send({
        token: fcmToken,
        notification: {
          title: "New Request Received",
          body: `${requesterName} is interested in your "${postTitle}" item`,
        },
        data: {
          type: "request",
          postId: request.postId,
          requestId: context.params.requestId,
          relatedId: request.postId,
        },
        android: {
          priority: "high",
          notification: {
            sound: "default",
            clickAction: "FLUTTER_NOTIFICATION_CLICK",
          },
        },
      });

      console.log(`Notification sent successfully to: ${postOwnerId}`);
    } catch (error) {
      console.error("Error sending request notification:", error);
      throw error;
    }
  });

/**
 * Cloud Function: Send notification when request status is updated
 * Trigger: Collection "requests" - onUpdate
 * 
 * Sends notification to requester if request is accepted
 */
export const sendRequestStatusNotification = functions.firestore
  .document("requests/{requestId}")
  .onUpdate(async (snap, context) => {
    const before = snap.before.data();
    const after = snap.after.data();

    // Check if status field has changed
    if (before.status === after.status) {
      console.log("Status unchanged, skipping notification");
      return;
    }

    const requesterId = after.requesterId;
    const postTitle = after.postTitle;
    const newStatus = after.status;

    console.log(
      `Request status updated to ${newStatus} for requester: ${requesterId}`
    );

    try {
      // Get the requester's FCM token
      const userDoc = await db.collection("users").doc(requesterId).get();

      if (!userDoc.exists) {
        console.log(`User document not found for: ${requesterId}`);
        return;
      }

      const fcmToken = userDoc.data()?.fcmToken;
      if (!fcmToken) {
        console.log(`No FCM token found for user: ${requesterId}`);
        return;
      }

      let title = "";
      let body = "";

      // Customize message based on status
      if (newStatus === "accepted") {
        title = "Request Accepted! 🎉";
        body = `Your request for "${postTitle}" has been accepted!`;
      } else if (newStatus === "rejected") {
        title = "Request Update";
        body = `Your request for "${postTitle}" has been declined.`;
      } else {
        console.log(`Unhandled status: ${newStatus}`);
        return;
      }

      // Send notification via FCM
      await messaging.send({
        token: fcmToken,
        notification: {
          title: title,
          body: body,
        },
        data: {
          type: newStatus,
          postId: after.postId,
          requestId: context.params.requestId,
          relatedId: after.postId,
          status: newStatus,
        },
        android: {
          priority: "high",
          notification: {
            sound: "default",
            clickAction: "FLUTTER_NOTIFICATION_CLICK",
          },
        },
      });

      console.log(`Status notification sent successfully to: ${requesterId}`);
    } catch (error) {
      console.error("Error sending status notification:", error);
      throw error;
    }
  });

/**
 * Cloud Function: Send notification when user joins an event
 * Trigger: Collection "events" - onUpdate
 * 
 * Sends notification to event creator when someone joins
 */
export const sendEventJoinNotification = functions.firestore
  .document("events/{eventId}")
  .onUpdate(async (snap, context) => {
    const before = snap.before.data();
    const after = snap.after.data();

    // Check if attendees count has increased
    const beforeAttendeeCount = before.attendeeCount || 0;
    const afterAttendeeCount = after.attendeeCount || 0;

    if (afterAttendeeCount <= beforeAttendeeCount) {
      console.log("No new attendees, skipping notification");
      return;
    }

    const creatorId = after.createdBy;
    const eventTitle = after.title;

    console.log(
      `New attendee joined event: ${eventTitle} (Creator: ${creatorId})`
    );

    try {
      // Get the event creator's FCM token
      const userDoc = await db.collection("users").doc(creatorId).get();

      if (!userDoc.exists) {
        console.log(`User document not found for: ${creatorId}`);
        return;
      }

      const fcmToken = userDoc.data()?.fcmToken;
      if (!fcmToken) {
        console.log(`No FCM token found for user: ${creatorId}`);
        return;
      }

      // Send notification via FCM
      await messaging.send({
        token: fcmToken,
        notification: {
          title: "New Event Attendee",
          body: `Someone just joined your "${eventTitle}" event!`,
        },
        data: {
          type: "event",
          eventId: context.params.eventId,
          relatedId: context.params.eventId,
        },
        android: {
          priority: "high",
          notification: {
            sound: "default",
          },
        },
      });

      console.log(`Event notification sent to: ${creatorId}`);
    } catch (error) {
      console.error("Error sending event notification:", error);
      throw error;
    }
  });

/**
 * Cloud Function: Cleanup FCM tokens for deleted users
 * Trigger: Collection "users" - onDelete
 */
export const cleanupUserOnDelete = functions.firestore
  .document("users/{userId}")
  .onDelete(async (snap, context) => {
    const userId = context.params.userId;
    console.log(`User deleted: ${userId}`);
    // Token will be automatically cleaned up
  });

/**
 * HTTP Function: Test FCM notification (for development)
 * Call: POST /sendTestNotification with userId in body
 */
export const sendTestNotification = functions.https.onCall(
  async (data, context) => {
    // Verify user is authenticated
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const userId = data.userId || context.auth.uid;

    try {
      // Get user's FCM token
      const userDoc = await db.collection("users").doc(userId).get();

      if (!userDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "User document not found"
        );
      }

      const fcmToken = userDoc.data()?.fcmToken;
      if (!fcmToken) {
        throw new functions.https.HttpsError(
          "not-found",
          "FCM token not found for user"
        );
      }

      // Send test notification
      await messaging.send({
        token: fcmToken,
        notification: {
          title: "Test Notification",
          body: "If you see this, FCM is working correctly!",
        },
        data: {
          type: "test",
          timestamp: new Date().toISOString(),
        },
      });

      return {
        success: true,
        message: "Test notification sent successfully",
        userId: userId,
      };
    } catch (error) {
      console.error("Error sending test notification:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to send test notification"
      );
    }
  }
);
