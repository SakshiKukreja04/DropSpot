package com.example.dropspot;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final Map<Integer, Fragment> fragmentCache = new HashMap<>();
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private static final String CHANNEL_ID = "dropspot_notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        checkNotificationPermission();
        scheduleNotificationWorker();
        runInstantNotificationCheck();
        
        // Sync FCM Token and Location to Firestore for Proximity Notifications
        syncUserData();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = getFragment(itemId);
            if (selectedFragment != null) {
                switchFragment(selectedFragment);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void syncUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> updateData = new HashMap<>();

        // 1. Get and sync FCM Token
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            updateData.put("fcmToken", token);
            
            // 2. Get and sync Location
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        updateData.put("latitude", location.getLatitude());
                        updateData.put("longitude", location.getLongitude());
                        Log.d(TAG, "Syncing location: " + location.getLatitude() + ", " + location.getLongitude());
                    }
                    sendUpdateToBackend(user.getUid(), updateData);
                });
            } else {
                sendUpdateToBackend(user.getUid(), updateData);
            }
        });
    }

    private void sendUpdateToBackend(String uid, Map<String, Object> data) {
        ApiClient.getClient().create(ApiService.class)
            .updateUserProfile(uid, data)
            .enqueue(new Callback<ApiResponse<Object>>() {
                @Override public void onResponse(Call<ApiResponse<Object>> c, Response<ApiResponse<Object>> r) {
                    if (r.isSuccessful()) Log.d(TAG, "User metadata synced to Firestore");
                }
                @Override public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {
                    Log.e(TAG, "Metadata sync failed", t);
                }
            });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "DropSpot Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for requests and updates");
            channel.enableLights(true);
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void scheduleNotificationWorker() {
        PeriodicWorkRequest notificationWork = new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("DropSpotNotificationWork", ExistingPeriodicWorkPolicy.KEEP, notificationWork);
    }

    private void runInstantNotificationCheck() {
        OneTimeWorkRequest instantWork = new OneTimeWorkRequest.Builder(NotificationWorker.class).build();
        WorkManager.getInstance(this).enqueue(instantWork);
    }

    private Fragment getFragment(int itemId) {
        if (fragmentCache.containsKey(itemId)) return fragmentCache.get(itemId);
        Fragment fragment = null;
        if (itemId == R.id.nav_home) fragment = new HomeFragment();
        else if (itemId == R.id.nav_saved) fragment = new PostedItemsFragment();
        else if (itemId == R.id.nav_announcements) fragment = new AnnouncementsFragment();
        else if (itemId == R.id.nav_profile) fragment = new ProfileFragment();
        if (fragment != null) fragmentCache.put(itemId, fragment);
        return fragment;
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    public void navigateToTab(int navId) {
        ((BottomNavigationView) findViewById(R.id.bottom_navigation)).setSelectedItemId(navId);
    }
}
