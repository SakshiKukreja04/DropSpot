package com.example.dropspot;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnnouncementsFragment extends Fragment {
    private static final String TAG = "AnnouncementsFragment";
    private RecyclerView rvEvents;
    private RecyclerView rvNotifications;
    private EventsAdapter eventsAdapter;
    private NotificationsAdapter notificationsAdapter;
    private List<Event> eventsList = new ArrayList<>();
    private List<Notification> notificationsList = new ArrayList<>();
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_announcements, container, false);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Announcements");

        apiService = ApiClient.getClient().create(ApiService.class);
        
        // Setup RecyclerViews
        setupRecyclerViews(view);
        
        // Load data
        loadUpcomingEvents();
        loadNotifications();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment becomes visible
        loadUpcomingEvents();
        loadNotifications();
    }

    private void setupRecyclerViews(View view) {
        rvEvents = view.findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsAdapter = new EventsAdapter(getContext(), eventsList);
        rvEvents.setAdapter(eventsAdapter);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationsAdapter = new NotificationsAdapter(notificationsList);
        rvNotifications.setAdapter(notificationsAdapter);
    }

    private void loadUpcomingEvents() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        apiService.getUpcomingEvents().enqueue(new Callback<ApiResponse<List<Event>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Event>>> call, Response<ApiResponse<List<Event>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    eventsList.clear();
                    eventsList.addAll(response.body().getData());
                    eventsAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + eventsList.size() + " events");
                } else {
                    Log.e(TAG, "Failed to load events: " + (response.body() != null ? response.body().getMessage() : "null"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Event>>> call, Throwable t) {
                Log.e(TAG, "Error loading events", t);
            }
        });
    }

    private void loadNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        apiService.getNotifications(user.getUid()).enqueue(new Callback<ApiResponse<ApiService.NotificationList>>() {
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
                    Log.d(TAG, "Loaded " + notificationsList.size() + " notifications");
                } else {
                    Log.e(TAG, "Failed to load notifications: " + (response.body() != null ? response.body().getMessage() : "null"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiService.NotificationList>> call, Throwable t) {
                Log.e(TAG, "Error loading notifications", t);
            }
        });
    }
}
