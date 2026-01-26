package com.example.dropspot;

public enum RequestState {
    NOT_REQUESTED, // No request has been made for this item
    REQUESTED,     // The item has been requested by a user
    ACCEPTED,      // The owner has accepted the request
    REJECTED       // The owner has rejected the request
}
