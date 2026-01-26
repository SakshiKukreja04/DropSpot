package com.example.dropspot;

public class FeedItem {
    private String title;
    private String category;
    private String distance;
    private int imageResource;

    public FeedItem(String title, String category, String distance, int imageResource) {
        this.title = title;
        this.category = category;
        this.distance = distance;
        this.imageResource = imageResource;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDistance() {
        return distance;
    }

    public int getImageResource() {
        return imageResource;
    }
}
