package com.example.dropspot;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Event {
    @SerializedName("eventId")
    public String eventId;

    @SerializedName("ownerId")
    public String ownerId;

    @SerializedName("ownerName")
    public String ownerName;

    @SerializedName("eventName")
    public String eventName;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("date")
    public String date;

    @SerializedName("startTime")
    public String startTime;

    @SerializedName("endTime")
    public String endTime;

    @SerializedName("location")
    public String location;

    @SerializedName("category")
    public String category;

    @SerializedName("attendees")
    public List<String> attendees;

    @SerializedName("attendeesCount")
    public int attendeesCount;

    @SerializedName("createdAt")
    public long createdAt;

    @SerializedName("updatedAt")
    public long updatedAt;

    @SerializedName("latitude")
    public double latitude;

    @SerializedName("longitude")
    public double longitude;

    public Event() {
        this.attendees = new ArrayList<>();
    }

    public Event(String eventName, String description, String date, String startTime, String endTime, String location) {
        this();
        this.eventName = eventName;
        this.title = eventName;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
    }

    public int getAttendeeCount() {
        return attendeesCount;
    }

    public boolean isUserAttending(String userId) {
        if (attendees == null) return false;
        return attendees.contains(userId);
    }

    public static class EventAttendee {
        @SerializedName("userId")
        public String userId;

        @SerializedName("name")
        public String name;

        @SerializedName("joinedAt")
        public long joinedAt;

        public EventAttendee() {
        }

        public EventAttendee(String userId, String name) {
            this.userId = userId;
            this.name = name;
            this.joinedAt = System.currentTimeMillis();
        }
    }
}
