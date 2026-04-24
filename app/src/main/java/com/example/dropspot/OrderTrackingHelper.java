package com.example.dropspot;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for managing order tracking
 * Handles post-payment order lifecycle:
 * - PAID: Payment successful
 * - DISPATCHED: Seller has shipped item
 * - IN_TRANSIT: Item in transit
 * - DELIVERED: Item delivered
 * - COMPLETED: Order complete
 */
public class OrderTrackingHelper {

    private static final String ORDERS_COLLECTION = "orders";
    private static final String POSTS_COLLECTION = "posts";
    private static final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    /**
     * Create order record after payment success
     */
    public static void createOrder(
            String paymentId,
            String postId,
            String buyerId,
            String sellerId,
            double amount,
            String itemTitle,
            String deliveryAddress) {

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("paymentId", paymentId);
        orderData.put("postId", postId);
        orderData.put("buyerId", buyerId);
        orderData.put("sellerId", sellerId);
        orderData.put("amount", amount);
        orderData.put("itemTitle", itemTitle);
        orderData.put("deliveryAddress", deliveryAddress);
        orderData.put("status", "PAID");
        orderData.put("createdAt", System.currentTimeMillis());
        orderData.put("updatedAt", System.currentTimeMillis());
        orderData.put("trackingId", generateTrackingId());

        firebaseFirestore.collection(ORDERS_COLLECTION)
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    // Order created
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    /**
     * Update order status when seller dispatches
     */
    public static void updateOrderStatusToDispatched(
            String paymentId,
            String trackingNumber) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "DISPATCHED");
        updates.put("trackingNumber", trackingNumber);
        updates.put("dispatchedAt", System.currentTimeMillis());
        updates.put("updatedAt", System.currentTimeMillis());

        firebaseFirestore.collection(ORDERS_COLLECTION)
                .whereEqualTo("paymentId", paymentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        document.getReference().update(updates);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    /**
     * Update order status to delivered
     */
    public static void updateOrderStatusToDelivered(String paymentId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "DELIVERED");
        updates.put("deliveredAt", System.currentTimeMillis());
        updates.put("updatedAt", System.currentTimeMillis());

        firebaseFirestore.collection(ORDERS_COLLECTION)
                .whereEqualTo("paymentId", paymentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        document.getReference().update(updates);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    /**
     * Mark order as completed
     */
    public static void completeOrder(String paymentId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "COMPLETED");
        updates.put("completedAt", System.currentTimeMillis());
        updates.put("updatedAt", System.currentTimeMillis());

        firebaseFirestore.collection(ORDERS_COLLECTION)
                .whereEqualTo("paymentId", paymentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        document.getReference().update(updates);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    /**
     * Get order by payment ID for tracking
     */
    public static void getOrderByPaymentId(
            String paymentId,
            OrderCallback callback) {

        firebaseFirestore.collection(ORDERS_COLLECTION)
                .whereEqualTo("paymentId", paymentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        var document = queryDocumentSnapshots.getDocuments().get(0);
                        callback.onOrderFound(document.getData());
                    } else {
                        callback.onOrderNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Generate unique tracking ID
     */
    private static String generateTrackingId() {
        return "TRACK_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    /**
     * Callback interface for order queries
     */
    public interface OrderCallback {
        void onOrderFound(Map<String, Object> orderData);
        void onOrderNotFound();
        void onError(String errorMessage);
    }
}

