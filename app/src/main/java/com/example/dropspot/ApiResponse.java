package com.example.dropspot;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("success")
    public boolean success;
    
    @SerializedName("message")
    public String message;
    
    @SerializedName("data")
    public T data;
    
    @SerializedName("error")
    public String error;
    
    @SerializedName("code")
    public int code;

    public boolean isSuccess() {
        // If the server sends a success flag, we trust it.
        // Some responses might not include the 'code' field in the JSON body.
        return success;
    }

    public T getData() {
        return data;
    }
    
    public String getMessage() {
        return message != null ? message : "No message provided";
    }
}
