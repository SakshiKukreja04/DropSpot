package com.example.dropspot;

import android.util.Log;
import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class FcmSender {
    private static final String TAG = "FcmSender";
    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";
    
    // NOTE: Replace this with your actual Firebase Cloud Messaging Server Key
    // You can find it in Firebase Console > Project Settings > Cloud Messaging (Legacy API)
    private static final String SERVER_KEY = "YOUR_SERVER_KEY_HERE";

    public static void sendNotification(String toToken, String title, String body) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("to", toToken);
            jsonBody.put("notification", notification);

            RequestBody requestBody = RequestBody.create(jsonBody.toString(), mediaType);

            Request request = new Request.Builder()
                    .url(FCM_API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "key=" + SERVER_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Notification Send Failed", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Notification Sent Successfully: " + response.body().string());
                    } else {
                        Log.e(TAG, "Notification Send Failed with code: " + response.code());
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON Construction Error", e);
        }
    }
}
