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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

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

        // Load the default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "DropSpot Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for requests and updates");
            channel.enableLights(true);
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            } else {
                checkIfNotificationsBlocked();
            }
        } else {
            checkIfNotificationsBlocked();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkIfNotificationsBlocked();
            } else {
                Toast.makeText(this, "Notification permission denied. You might miss important updates.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkIfNotificationsBlocked() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null && !manager.areNotificationsEnabled()) {
            Toast.makeText(this, "Notifications are disabled in settings. Please enable them.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }
    }

    private void scheduleNotificationWorker() {
        PeriodicWorkRequest notificationWork = new PeriodicWorkRequest.Builder(
                NotificationWorker.class, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DropSpotNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                notificationWork);
    }

    private void runInstantNotificationCheck() {
        OneTimeWorkRequest instantWork = new OneTimeWorkRequest.Builder(NotificationWorker.class).build();
        WorkManager.getInstance(this).enqueue(instantWork);
    }

    private Fragment getFragment(int itemId) {
        if (fragmentCache.containsKey(itemId)) {
            return fragmentCache.get(itemId);
        }

        Fragment fragment = null;
        if (itemId == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_saved) {
            fragment = new PostedItemsFragment();
        } else if (itemId == R.id.nav_announcements) {
            fragment = new AnnouncementsFragment();
        } else if (itemId == R.id.nav_profile) {
            fragment = new ProfileFragment();
        }

        if (fragment != null) {
            fragmentCache.put(itemId, fragment);
        }
        return fragment;
    }

    private void switchFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    public void navigateToTab(int navId) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(navId);
    }
}
