package com.example.dropspot;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dispatch and Delivery Tracking Helper
 * Manages post-payment order dispatch and delivery tracking
 */
public class DispatchTrackingHelper {

    private static final String TAG = "DispatchTrackingHelper";
    private static final String ORDERS_COLLECTION = "orders";
    private static final String NOTIFICATIONS_COLLECTION = "notifications";
    private static final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    /**
     * Send dispatch notification to BUYER ONLY via Backend API
     * Called when seller marks order as dispatched
     * 
     * FIX: Ensure notification is sent ONLY to buyerId, NOT to seller
     * Backend API handles: database save + FCM + recipientUserId validation
     */
    public static void sendDispatchNotification(
            String buyerId,
            String sellerId,
            String itemTitle,
            String trackingNumber,
            String paymentId,
            String shipperName) {

        Log.d(TAG, "[DISPATCH] Calling backend API - Payment: " + paymentId + ", Buyer: " + buyerId);
        
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        
        // Create dispatch request
        ApiService.DispatchRequest dispatchRequest = new ApiService.DispatchRequest(
            paymentId,
            buyerId,
            sellerId,
            itemTitle,
            trackingNumber,
            shipperName
        );
        
        // Call backend API endpoint
        apiService.markOrderDispatched(dispatchRequest).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "[DISPATCH] Dispatch notification sent successfully via API");
                } else {
                    Log.e(TAG, "[DISPATCH] Dispatch notification API failed with code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Log.e(TAG, "[DISPATCH] Dispatch notification API error", t);
            }
        });
    }

    /**
     * Mark order as dispatched with tracking number
     * Called from seller's MyPostsActivity when dispatch button clicked
     * Now delegates to backend API instead of direct Firestore write
     */
    public static void markOrderAsDispatched(
            String paymentId,
            String trackingNumber,
            String buyerId,
            String sellerId,
            String itemTitle) {

        Log.d(TAG, "[DISPATCH] Mark order as dispatched - Payment: " + paymentId);
        
        // Call the notification method which will call backend API
        sendDispatchNotification(buyerId, sellerId, itemTitle, trackingNumber, paymentId, null);
    }


    /**
     * Send delivery confirmation notification to SELLER ONLY via Backend API
     * Called when buyer confirms delivery
     * 
     * FIX: Ensure notification is sent ONLY to sellerId, NOT to buyer
     * Backend API handles: database save + FCM + recipientUserId validation
     */
    public static void sendDeliveryConfirmedNotification(
            String sellerId,
            String buyerId,
            String itemTitle,
            String paymentId) {

        Log.d(TAG, "[DELIVERY] Calling backend API - Payment: " + paymentId + ", Seller: " + sellerId);
        
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        
        // Create delivery request
        ApiService.DeliveryRequest deliveryRequest = new ApiService.DeliveryRequest(
            paymentId,
            buyerId,
            sellerId,
            itemTitle
        );
        
        // Call backend API endpoint
        apiService.markOrderDelivered(deliveryRequest).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "[DELIVERY] Delivery notification sent successfully via API");
                } else {
                    Log.e(TAG, "[DELIVERY] Delivery notification API failed with code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Log.e(TAG, "[DELIVERY] Delivery notification API error", t);
            }
        });
    }


    /**
     * Mark order as delivered
     * Called when buyer confirms delivery
     * Now delegates to backend API instead of direct Firestore write
     */
    public static void markOrderAsDelivered(
            String paymentId,
            String buyerId,
            String sellerId,
            String itemTitle) {

        Log.d(TAG, "[DELIVERY] Mark order as delivered - Payment: " + paymentId);
        
        // Call the notification method which will call backend API
        sendDeliveryConfirmedNotification(sellerId, buyerId, itemTitle, paymentId);
    }

    /**
     * Get order details for tracking
     */
    public static void getOrderDetails(
            String paymentId,
            OrderDetailsCallback callback) {

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
     * Callback interface for order details
     */
    public interface OrderDetailsCallback {
        void onOrderFound(Map<String, Object> orderData);
        void onOrderNotFound();
        void onError(String errorMessage);
    }
}
