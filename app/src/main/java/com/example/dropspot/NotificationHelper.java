package com.example.dropspot;

import android.app.NotificationManager;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for managing notifications
 * Handles in-app notifications and Firestore notification records
 */
public class NotificationHelper {

    private static final String NOTIFICATIONS_COLLECTION = "notifications";
    private static final String TAG = "NotificationHelper";

    /**
     * Send notification to user via Firestore
     * Used for in-app notification system
     */
    public static void sendNotification(
            Context context,
            String receiverId,
            String senderId,
            String type,
            String title,
            String message,
            Map<String, Object> data) {

        Map<String, Object> notification = new HashMap<>();
        notification.put("receiverId", receiverId);
        notification.put("senderId", senderId);
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        // Add any additional data
        if (data != null) {
            notification.putAll(data);
        }

        FirebaseFirestore.getInstance()
                .collection(NOTIFICATIONS_COLLECTION)
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    // Notification saved
                })
                .addOnFailureListener(e -> {
                    // Error handling
                });
    }

    /**
     * Send payment success notification to seller
     */
    public static void sendPaymentSuccessNotification(
            Context context,
            String sellerId,
            String buyerId,
            String itemTitle,
            double amount,
            String deliveryAddress,
            String paymentId) {

        Map<String, Object> data = new HashMap<>();
        data.put("postId", itemTitle);
        data.put("paymentId", paymentId);
        data.put("amount", amount);
        data.put("deliveryAddress", deliveryAddress);

        sendNotification(
                context,
                sellerId,
                buyerId,
                "PAYMENT_SUCCESS",
                "New Order Received",
                "Your item (" + itemTitle + ") has been paid for. Please dispatch it.",
                data
        );
    }

    /**
     * Send order dispatch notification to buyer
     */
    public static void sendDispatchNotification(
            Context context,
            String buyerId,
            String sellerId,
            String itemTitle,
            String trackingId) {

        Map<String, Object> data = new HashMap<>();
        data.put("trackingId", trackingId);
        data.put("itemTitle", itemTitle);

        sendNotification(
                context,
                buyerId,
                sellerId,
                "ORDER_DISPATCHED",
                "Order Dispatched",
                "Your order for " + itemTitle + " has been dispatched.",
                data
        );
    }

    /**
     * Send delivery confirmed notification to seller
     */
    public static void sendDeliveryConfirmedNotification(
            Context context,
            String sellerId,
            String buyerId,
            String itemTitle) {

        Map<String, Object> data = new HashMap<>();
        data.put("itemTitle", itemTitle);

        sendNotification(
                context,
                sellerId,
                buyerId,
                "DELIVERY_CONFIRMED",
                "Delivery Confirmed",
                "The buyer has confirmed delivery of " + itemTitle + ".",
                data
        );
    }
}

