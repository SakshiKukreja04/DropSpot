package com.example.dropspot;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

/**
 * Tracking Status Helper
 * Manages order status display and formatting
 */
public class TrackingStatusHelper {

    public enum OrderStatus {
        PAID("PAID", "Payment Completed ✅", Color.parseColor("#4CAF50")),
        DISPATCHED("DISPATCHED", "Dispatched 🚚", Color.parseColor("#2196F3")),
        DELIVERED("DELIVERED", "Delivered 📦", Color.parseColor("#FF9800"));

        public final String dbValue;
        public final String displayText;
        public final int color;

        OrderStatus(String dbValue, String displayText, int color) {
            this.dbValue = dbValue;
            this.displayText = displayText;
            this.color = color;
        }

        public static OrderStatus fromString(String value) {
            for (OrderStatus status : values()) {
                if (status.dbValue.equals(value)) {
                    return status;
                }
            }
            return PAID;
        }
    }

    /**
     * Update order status display in UI
     */
    public static void updateStatusDisplay(TextView statusView, String statusString) {
        if (statusView == null) return;

        OrderStatus status = OrderStatus.fromString(statusString);
        statusView.setText(status.displayText);
        statusView.setTextColor(status.color);
    }

    /**
     * Get status badge color
     */
    public static int getStatusColor(String statusString) {
        OrderStatus status = OrderStatus.fromString(statusString);
        return status.color;
    }

    /**
     * Get status badge text
     */
    public static String getStatusText(String statusString) {
        OrderStatus status = OrderStatus.fromString(statusString);
        return status.displayText;
    }

    /**
     * Check if order can be marked as delivered (buyer action)
     */
    public static boolean canMarkAsDelivered(String statusString) {
        return "DISPATCHED".equals(statusString);
    }

    /**
     * Check if order can be marked as dispatched (seller action)
     */
    public static boolean canMarkAsDispatched(String statusString) {
        return "PAID".equals(statusString);
    }
}

