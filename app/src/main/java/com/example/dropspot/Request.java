package com.example.dropspot;

import com.google.gson.annotations.SerializedName;

public class Request {
    @SerializedName("requestId")
    public String requestId;
    
    @SerializedName("id")
    public String id; // Fallback for some APIs

    @SerializedName("postId")
    public String postId;
    
    @SerializedName("postOwnerId")
    public String postOwnerId;
    
    @SerializedName("requesterId")
    public String requesterId;
    
    @SerializedName("requesterName")
    public String requesterName;
    
    @SerializedName("requesterEmail")
    public String requesterEmail;
    
    @SerializedName("requesterPhoto")
    public String requesterPhoto;
    
    @SerializedName("postTitle")
    public String postTitle;
    
    @SerializedName("message")
    public String message;
    
    @SerializedName("status")
    public String status; // "pending", "accepted", "rejected"
    
    @SerializedName("createdAt")
    public String createdAt;
    
    @SerializedName("respondedAt")
    public String respondedAt;

    public Request() {}

    public String getEffectiveId() {
        return requestId != null ? requestId : id;
    }
}
