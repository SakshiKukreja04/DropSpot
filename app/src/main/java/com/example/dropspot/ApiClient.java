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
    
    // IF USING EMULATOR, USE 10.0.2.2:5000
    // IF USING PHYSICAL DEVICE, USE YOUR COMPUTER'S IP:5000
    // ⚠️ CRITICAL: Device and Computer MUST be on SAME WiFi network!
    // Computer IP: 192.168.29.133 (WiFi: VESITSTUDENT)
    // Backend running on port 5000
    //private static final String BASE_URL = "http://192.168.174.183:5000/api/";
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
                                // Get a fresh token (refreshes if expired)
                                GetTokenResult result = Tasks.await(user.getIdToken(true), 10, TimeUnit.SECONDS);
                                token = result.getToken();
                                if (token != null) {
                                    Log.d(TAG, "Firebase Token attached to request");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error getting Firebase Token: " + e.getMessage());
                            }
                        }

                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Content-Type", "application/json");

                        if (token != null) {
                            requestBuilder.header("Authorization", "Bearer " + token);
                        }

                        return chain.proceed(requestBuilder.build());
                    })
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
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
