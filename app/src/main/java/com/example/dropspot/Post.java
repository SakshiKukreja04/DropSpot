package com.example.dropspot;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Post {
    @SerializedName("id")
    public String id;
    
    @SerializedName("userId")
    public String userId;
    
    @SerializedName("title")
    public String title;
    
    @SerializedName("description")
    public String description;
    
    @SerializedName("category")
    public String category;
    
    @SerializedName("condition")
    public String condition;
    
    @SerializedName("latitude")
    public double latitude;
    
    @SerializedName("longitude")
    public double longitude;

    @SerializedName("distance")
    public double distance;
    
    @SerializedName("requestCount")
    public int requestCount;
    
    @SerializedName("viewCount")
    public int viewCount;
    
    @SerializedName("isActive")
    public boolean isActive;
    
    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("images")
    public List<String> images;

    public Post() {}
}
