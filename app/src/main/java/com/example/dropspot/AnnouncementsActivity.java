package com.example.dropspot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnnouncementsActivity extends AppCompatActivity {
    private RecyclerView rvEvents, rvNotifications;
    private EventsAdapter eventsAdapter;
    private NotificationsAdapter notificationsAdapter;
    private List<Event> eventsList;
    private List<Notification> notificationsList;
    private ApiService apiService;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0;
    private static final String TAG = "AnnouncementsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Events & Announcements");
        }

        rvEvents = findViewById(R.id.rvEvents);
        rvNotifications = findViewById(R.id.rvNotifications);

        eventsList = new ArrayList<>();
        notificationsList = new ArrayList<>();

        eventsAdapter = new EventsAdapter(this, eventsList);
        notificationsAdapter = new NotificationsAdapter(notificationsList);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventsAdapter);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(notificationsAdapter);

        // Get user location and load events
        getLocationAndLoadData();
    }

    private void getLocationAndLoadData() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            loadEvents();
            loadNotifications();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
            }
            loadEvents();
            loadNotifications();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Location error: " + e.getMessage());
            loadEvents();
            loadNotifications();
        });
    }

    private void loadEvents() {
        firestore.collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        eventsList.clear();
                        for (var doc : value.getDocuments()) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                // Filter events within 2.5 km radius
                                if (isNearby(event.latitude, event.longitude)) {
                                    eventsList.add(event);
                                }
                            }
                        }
                        eventsAdapter.notifyDataSetChanged();
                    }
                });
    }

    private boolean isNearby(double eventLat, double eventLng) {
        if (currentLat == 0 && currentLng == 0) return true; // Show all if location not available
        double distance = calculateDistance(currentLat, currentLng, eventLat, eventLng);
        return distance <= 2.5; // 2.5 km radius
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void loadNotifications() {
        if (currentUserId == null) return;
        
        // Use real-time listener to auto-refresh when new notifications arrive
        Log.d(TAG, "Setting up real-time listener for notifications");
        firestore.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to notifications: " + error.getMessage());
                        // Fallback to API call
                        loadNotificationsFromAPI();
                        return;
                    }
                    
                    if (snapshot != null) {
                        notificationsList.clear();
                        for (var doc : snapshot.getDocuments()) {
                            Notification notif = new Notification();
                            notif.id = doc.getId();
                            notif.userId = doc.getString("userId");
                            notif.title = doc.getString("title");
                            notif.message = doc.getString("message");
                            notif.type = doc.getString("type");
                            notif.timestamp = doc.getString("createdAt");
                            notif.read = doc.getBoolean("read") != null ? doc.getBoolean("read") : false;
                            notif.isRead = notif.read;
                            notificationsList.add(notif);
                        }
                        notificationsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Real-time: Loaded " + notificationsList.size() + " notifications");
                    }
                });
    }
    
    private void loadNotificationsFromAPI() {
        if (currentUserId == null) return;
        
        Log.d(TAG, "Loading notifications from API (fallback)");
        apiService.getNotifications(currentUserId).enqueue(new Callback<ApiResponse<ApiService.NotificationList>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiService.NotificationList>> call, Response<ApiResponse<ApiService.NotificationList>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    notificationsList.clear();
                    ApiService.NotificationList notifList = response.body().getData();
                    if (notifList.notifications != null) {
                        for (ApiService.Notification apiNotif : notifList.notifications) {
                            Notification notif = new Notification();
                            notif.id = apiNotif.id;
                            notif.userId = apiNotif.userId;
                            notif.title = apiNotif.title;
                            notif.message = apiNotif.message;
                            notif.type = apiNotif.type;
                            notif.read = apiNotif.isRead;
                            notif.isRead = apiNotif.isRead;
                            notif.timestamp = apiNotif.createdAt;
                            notificationsList.add(notif);
                        }
                    }
                    notificationsAdapter.notifyDataSetChanged();
                    Log.d(TAG, "API: Loaded " + notificationsList.size() + " notifications");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiService.NotificationList>> call, Throwable t) {
                Log.e(TAG, "Error loading notifications from API", t);
                Toast.makeText(AnnouncementsActivity.this, "Error loading notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
