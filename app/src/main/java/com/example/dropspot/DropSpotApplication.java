package com.example.dropspot;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class DropSpotApplication extends android.app.Application {

    private static final String TAG = "DropSpotApp";
    private int fcmRetryCount = 0;
    private static final int MAX_FCM_RETRIES = 3;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        Log.d(TAG, "✅ DropSpot Application created");
        // Get FCM token on app start
        initializeFCM();
    }
    
    private void initializeFCM() {
        Log.d(TAG, "🔄 Initializing FCM token...");
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "❌ Fetching FCM token failed", task.getException());

                // Retry after delay
                if (fcmRetryCount < MAX_FCM_RETRIES) {
                    fcmRetryCount++;
                    Log.d(TAG, "🔄 Retrying FCM initialization (attempt " + fcmRetryCount + "/" + MAX_FCM_RETRIES + ")");
                    new Handler(Looper.getMainLooper()).postDelayed(
                        this::initializeFCM,
                        5000L * fcmRetryCount  // Exponential backoff
                    );
                }
                return;
            }

            // Get token
            String token = task.getResult();
            Log.d(TAG, "✅ FCM Token obtained: " + token.substring(0, Math.min(30, token.length())) + "...");

            // Save to Firestore if user is logged in
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                Log.d(TAG, "👤 User is logged in: " + auth.getCurrentUser().getUid());
                saveFCMTokenToServer(token);
            } else {
                Log.d(TAG, "⏳ No user logged in yet - will save FCM token after login");
            }
        });
    }
    
    private void saveFCMTokenToServer(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.d(TAG, "❌ No user to save FCM token for");
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "💾 Saving FCM token for user: " + userId);
        Log.d(TAG, "📱 FCM Token: " + token.substring(0, Math.min(30, token.length())) + "...");
        
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        
        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("fcmToken", token);
        
        apiService.updateUserProfile(userId, update).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ FCM Token saved to server on app start");
                    // Now process any pending notifications that were queued while user was offline
                    processPendingNotifications(userId);
                } else {
                    Log.e(TAG, "❌ Failed to save FCM token: " + response.code() + " " + response.message());
                    // Retry
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "🔄 Retrying FCM token save...");
                        saveFCMTokenToServer(token);
                    }, 5000);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                Log.e(TAG, "❌ Error saving FCM token: " + t.getMessage(), t);
                // Retry
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "🔄 Retrying FCM token save after error...");
                    saveFCMTokenToServer(token);
                }, 5000);
            }
        });
    }
    
    private void processPendingNotifications(String userId) {
        Log.d(TAG, "⏳ Processing pending notifications for user: " + userId);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        
        apiService.processPendingNotifications(userId).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Pending notifications processed");
                } else {
                    Log.e(TAG, "⚠️  Error processing pending notifications: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                Log.e(TAG, "⚠️  Failed to process pending notifications", t);
            }
        });
    }
}
