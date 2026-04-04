package com.example.dropspot;

import android.util.Log;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    
    // CHANGE THIS TO YOUR COMPUTER'S IP IF USING PHYSICAL DEVICE
    // IF USING EMULATOR, USE 10.0.2.2
    private static final String BASE_URL = "http://192.168.29.133:5000/api/"; 
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        
                        String token = null;
                        if (user != null) {
                            try {
                                // Force refresh to get a fresh token
                                GetTokenResult result = Tasks.await(user.getIdToken(true), 30, TimeUnit.SECONDS);
                                token = result.getToken();
                                if (token != null) {
                                    // PRINT THIS TOKEN TO LOGCAT FOR POSTMAN
                                    Log.d("FIREBASE_TOKEN", "Token: " + token);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "CRITICAL: Failed to get Firebase Token: " + e.getMessage());
                            }
                        } else {
                            Log.w(TAG, "No user is logged in to Firebase.");
                        }

                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Content-Type", "application/json");

                        if (token != null) {
                            requestBuilder.header("Authorization", "Bearer " + token);
                        }

                        return chain.proceed(requestBuilder.build());
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();
        }
        return retrofit;
    }
}
