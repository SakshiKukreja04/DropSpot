package com.example.dropspot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {
    private List<Notification> notifications;

    public NotificationsAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvType, tvTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvType = itemView.findViewById(R.id.tvType);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(Notification notification) {
            tvMessage.setText(notification.getDisplayMessage());
            String typeEmoji = getTypeEmoji(notification.type);
            tvType.setText(typeEmoji + " " + notification.type);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
            try {
                Date date = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(notification.timestamp);
                tvTime.setText(sdf.format(date));
            } catch (Exception e) {
                tvTime.setText(notification.getDisplayTimestamp());
            }
        }

        private String getTypeEmoji(String type) {
            switch (type) {
                case "EVENT_ATTEND":
                    return "📍";
                case "PAYMENT_SUCCESS":
                    return "💰";
                case "ORDER_DISPATCHED":
                    return "📦";
                case "DELIVERY_CONFIRMED":
                    return "✅";
                default:
                    return "📢";
            }
        }
    }
}

