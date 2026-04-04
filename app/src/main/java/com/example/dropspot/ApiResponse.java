package com.example.dropspot;

public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;
    public String error;
    public int code;

    public boolean isSuccess() {
        return success && (code >= 200 && code < 300);
    }

    public T getData() {
        return data;
    }
}
