package com.example.dropspot;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "dropspot_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "📬 Message received from: " + remoteMessage.getFrom());

        // Check if the message is intended for the current logged-in user
        String recipientUserId = remoteMessage.getData().get("recipientUserId");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        Log.d(TAG, "👤 Current user: " + (currentUser != null ? currentUser.getUid() : "null"));
        Log.d(TAG, "📍 Recipient user ID: " + (recipientUserId != null ? recipientUserId : "null"));
        
        // CRITICAL: If a recipientUserId is specified, ONLY show notification if it matches current user
        if (recipientUserId != null && !recipientUserId.isEmpty()) {
            if (currentUser == null || !currentUser.getUid().equals(recipientUserId)) {
                Log.d(TAG, "⛔ Notification not for current user. Ignoring.");
                Log.d(TAG, "   Current: " + (currentUser != null ? currentUser.getUid() : "null"));
                Log.d(TAG, "   Recipient: " + recipientUserId);
                return; // Don't show notification - this is for another user
            }
            Log.d(TAG, "✅ Notification is for current user: " + currentUser.getUid());
        } else {
            // No recipientUserId specified - show to everyone
            Log.d(TAG, "ℹ️  No recipientUserId specified, showing notification to current user");
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "📢 Notification object found");
            Log.d(TAG, "   Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "   Body: " + remoteMessage.getNotification().getBody());
            showNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
            );
        } else if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "📦 Using data payload instead of notification object");
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            Log.d(TAG, "   Title: " + title);
            Log.d(TAG, "   Message: " + message);
            showNotification(title, message);
        } else {
            Log.w(TAG, "⚠️  No notification or data in message");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "🔄 Token refreshed: " + token.substring(0, Math.min(30, token.length())) + "...");
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(TAG, "📤 Sending FCM token to server for user: " + user.getUid());
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Map<String, Object> update = new HashMap<>();
            update.put("fcmToken", token);

            apiService.updateUserProfile(user.getUid(), update).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ FCM Token updated on server");
                    } else {
                        Log.e(TAG, "❌ Failed to update FCM Token: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    Log.e(TAG, "❌ Error sending FCM Token", t);
                }
            });
        } else {
            Log.d(TAG, "⏳ No user logged in, token refresh will be sent on next login");
        }
    }

    private void showNotification(String title, String message) {
        Log.d(TAG, "🎯 Creating and displaying notification");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title != null ? title : "DropSpot")
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "DropSpot Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
        Log.d(TAG, "✅ Notification displayed with ID: " + notificationId);
    }
}
