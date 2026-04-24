package com.example.dropspot;

public class Notification {
    public String id;
    public String userId;
    public String title;
    public String message;
    public String body;
    public String type;
    public boolean read;
    public boolean isRead;
    public String timestamp;
    public String postId;
    public String trackingNumber;
    public String itemTitle;

    public Notification() {
    }

    public Notification(String id, String userId, String title, String message, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = false;
        this.isRead = false;
    }

    public String getDisplayTitle() {
        return title != null ? title : "DropSpot Notification";
    }

    public String getDisplayMessage() {
        return message != null ? message : body;
    }

    public String getDisplayTimestamp() {
        return timestamp != null ? timestamp : "Just now";
    }
}

