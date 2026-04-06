package com.example.dropspot;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Response;

public class NotificationWorker extends Worker {
    private static final String TAG = "NotificationWorker";
    private static final String CHANNEL_ID = "dropspot_notifications";
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_SEEN_REQUESTS = "seen_requests";
    private static final String KEY_LAST_STATUS = "last_status_";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker started checking for updates...");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, skipping.");
            return Result.success();
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // 1. Check for incoming requests (as Owner)
        checkIncomingRequests(apiService);

        // 2. Check for updates on sent requests (as Requester)
        checkSentRequests(apiService);

        return Result.success();
    }

    private void checkIncomingRequests(ApiService apiService) {
        try {
            Response<ApiResponse<List<Request>>> response = apiService.getRequests("my_received").execute();
            if (response.isSuccessful() && response.body() != null) {
                List<Request> requests = response.body().getData();
                if (requests != null) {
                    SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    Set<String> seenIds = new HashSet<>(prefs.getStringSet(KEY_SEEN_REQUESTS, new HashSet<>()));
                    boolean changed = false;

                    for (Request r : requests) {
                        String rid = r.getEffectiveId();
                        if (rid == null) {
                            Log.e(TAG, "Received request ID is null, skipping");
                            continue;
                        }
                        if (r.status != null && r.status.equals("pending") && !seenIds.contains(rid)) {
                            Log.d(TAG, "New request detected: " + rid);
                            showNotification(
                                rid.hashCode(),
                                "New Request Received",
                                (r.requesterName != null ? r.requesterName : "Someone") + " requested your item: " + (r.postTitle != null ? r.postTitle : "Item"),
                                r.postId
                            );
                            seenIds.add(rid);
                            changed = true;
                        }
                    }
                    if (changed) {
                        prefs.edit().putStringSet(KEY_SEEN_REQUESTS, seenIds).apply();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking incoming requests", e);
        }
    }

    private void checkSentRequests(ApiService apiService) {
        try {
            Response<ApiResponse<List<Request>>> response = apiService.getRequests("my_sent").execute();
            if (response.isSuccessful() && response.body() != null) {
                List<Request> requests = response.body().getData();
                if (requests != null) {
                    SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();

                    for (Request r : requests) {
                        String rid = r.getEffectiveId();
                        if (rid == null) {
                            Log.e(TAG, "Sent request ID is null, skipping");
                            continue;
                        }
                        
                        String lastStatus = prefs.getString(KEY_LAST_STATUS + rid, "pending");
                        if (r.status != null && !r.status.equals(lastStatus)) {
                            Log.d(TAG, "Status changed for request " + rid + ": " + lastStatus + " -> " + r.status);
                            if (r.status.equals("accepted")) {
                                showNotification(
                                    rid.hashCode(),
                                    "Request Accepted",
                                    "Your request for " + (r.postTitle != null ? r.postTitle : "an item") + " has been accepted!",
                                    r.postId
                                );
                            } else if (r.status.equals("rejected") || r.status.equals("rejected_auto")) {
                                showNotification(
                                    rid.hashCode(),
                                    "Request Rejected",
                                    "Your request for " + (r.postTitle != null ? r.postTitle : "an item") + " was not accepted.",
                                    r.postId
                                );
                            }
                            editor.putString(KEY_LAST_STATUS + rid, r.status);
                        }
                    }
                    editor.apply();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking sent requests", e);
        }
    }

    private void showNotification(int id, String title, String message, String postId) {
        createNotificationChannel();
        
        Intent intent = new Intent(getApplicationContext(), ItemDetailActivity.class);
        intent.putExtra("POST_ID", postId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 
                id, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        try {
            notificationManager.notify(id, builder.build());
            Log.d(TAG, "Notification sent: " + title);
        } catch (SecurityException e) {
            Log.e(TAG, "Missing notification permission", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "DropSpot Notifications";
            String description = "Notifications for item requests and updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
