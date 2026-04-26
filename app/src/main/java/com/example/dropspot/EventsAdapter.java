package com.example.dropspot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {
    private Context context;
    private List<Event> events;
    private ApiService apiService;

    public EventsAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName, tvEventDescription, tvEventTime, tvEventLocation, tvAttendeeCount;
        MaterialButton btnAttend;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            tvEventTime = itemView.findViewById(R.id.tvEventTime);
            tvEventLocation = itemView.findViewById(R.id.tvEventLocation);
            tvAttendeeCount = itemView.findViewById(R.id.tvAttendeeCount);
            btnAttend = itemView.findViewById(R.id.btnAttend);
        }

        public void bind(Event event) {
            String name = event.eventName != null ? event.eventName : event.title;
            tvEventName.setText(name);
            tvEventDescription.setText(event.description);
            tvEventTime.setText(event.date + " at " + event.startTime);
            tvEventLocation.setText(event.location != null ? event.location : "Location TBA");
            tvAttendeeCount.setText("👥 " + event.getAttendeeCount() + " attending");
            
            String currentUserId = FirebaseAuth.getInstance().getUid();
            boolean isAttending = event.isUserAttending(currentUserId);
            
            if (isAttending) {
                btnAttend.setText("✓ Attending");
                btnAttend.setEnabled(false);
            } else {
                btnAttend.setText("Attend");
                btnAttend.setEnabled(true);
            }
            
            btnAttend.setOnClickListener(v -> {
                if (!isAttending) {
                    attendEvent(event, currentUserId);
                }
            });
        }
        
        private void attendEvent(Event event, String userId) {
            btnAttend.setEnabled(false);
            
            apiService.attendEvent(new ApiService.AttendRequest(event.eventId)).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(context, "You're attending this event!", Toast.LENGTH_SHORT).show();
                        
                        // Update the local UI
                        if (event.attendees == null) {
                            event.attendees = new java.util.ArrayList<>();
                        }
                        event.attendees.add(userId);
                        event.attendeesCount++;
                        
                        btnAttend.setText("✓ Attending");
                        btnAttend.setEnabled(false);
                        tvAttendeeCount.setText("👥 " + event.getAttendeeCount() + " attending");
                    } else {
                        btnAttend.setEnabled(true);
                        Toast.makeText(context, "Failed to attend event", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                    btnAttend.setEnabled(true);
                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
