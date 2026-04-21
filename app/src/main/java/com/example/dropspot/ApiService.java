package com.example.dropspot;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Posts
    @POST("posts")
    Call<ApiResponse<Post>> createPost(@Body Post post);

    @GET("posts")
    Call<ApiResponse<PostList>> getPosts(
        @Query("category") String category,
        @Query("limit") Integer limit,
        @Query("offset") Integer offset,
        @Query("latitude") Double latitude,
        @Query("longitude") Double longitude,
        @Query("maxDistance") Integer maxDistance,
        @Query("myPostsOnly") Boolean myPostsOnly
    );

    @GET("posts/{id}")
    Call<ApiResponse<Post>> getPostDetails(@Path("id") String postId);

    @PUT("posts/{id}")
    Call<ApiResponse<Post>> updatePost(@Path("id") String postId, @Body Post post);

    @DELETE("posts/{id}")
    Call<ApiResponse<Void>> deletePost(@Path("id") String postId);

    // Requests
    @POST("requests")
    Call<ApiResponse<Object>> createRequest(@Body RequestBody request);

    @GET("requests")
    Call<ApiResponse<List<Request>>> getRequests(@Query("type") String type);

    @PUT("requests/{id}/status")
    Call<ApiResponse<Object>> updateRequestStatus(@Path("id") String requestId, @Body StatusUpdate status);

    @PUT("requests/{id}")
    Call<ApiResponse<Object>> updateRequest(@Path("id") String requestId, @Body StatusUpdate status);

    // Saved Posts
    @POST("savedPosts")
    Call<ApiResponse<Object>> savePost(@Body SavedPostRequest request);

    @GET("savedPosts/{userId}")
    Call<ApiResponse<List<Post>>> getSavedPosts(@Path("userId") String userId);

    @DELETE("savedPosts/{postId}")
    Call<ApiResponse<Void>> unsavePost(@Path("postId") String postId);

    // Events
    @GET("events")
    Call<ApiResponse<List<Event>>> getEvents();
    
    @GET("events/upcoming")
    Call<ApiResponse<List<Event>>> getUpcomingEvents();

    @POST("events")
    Call<ApiResponse<Event>> createEvent(@Body Event event);

    @POST("events/attend")
    Call<ApiResponse<Object>> attendEvent(@Body AttendRequest request);

    @POST("events/{id}/join")
    Call<ApiResponse<Void>> joinEvent(@Path("id") String eventId);

    @POST("events/{id}/leave")
    Call<ApiResponse<Void>> leaveEvent(@Path("id") String eventId);

    // Notifications
    @GET("notifications/{userId}")
    Call<ApiResponse<NotificationList>> getNotifications(@Path("userId") String userId);

    @GET("notifications/unread-count")
    Call<ApiResponse<Integer>> getUnreadNotificationCount();

    @PUT("notifications/{id}/read")
    Call<ApiResponse<Void>> markNotificationAsRead(@Path("id") String notificationId);

    // User Profile
    @POST("users")
    Call<ApiResponse<Object>> syncUserProfile(@Body UserProfile profile);

    @PUT("users/{id}")
    Call<ApiResponse<Object>> updateUserProfile(@Path("id") String userId, @Body Object user);

    @GET("users/{userId}")
    Call<ApiResponse<Object>> getUserProfile(@Path("userId") String userId);

    @POST("users/{userId}/process-pending-notifications")
    Call<ApiResponse<Object>> processPendingNotifications(@Path("userId") String userId);

    // Payments
    @POST("payments")
    Call<ApiResponse<Object>> savePayment(@Body PaymentRequest payment);
    
    // Dispatch & Delivery
    @POST("dispatch/mark-dispatched")
    Call<ApiResponse<Object>> markOrderDispatched(@Body DispatchRequest request);
    
    @POST("dispatch/mark-delivered")
    Call<ApiResponse<Object>> markOrderDelivered(@Body DeliveryRequest request);
    
    // FCM Notifications
    @POST("notifications/send-fcm")
    Call<ApiResponse<Object>> sendFcmNotification(@Body Object fcmPayload);

    // Wrapper Classes
    class RequestBody {
        public String postId;
        public String message;
        
        public RequestBody(String postId, String message) {
            this.postId = postId;
            this.message = message;
        }
    }

    class StatusUpdate {
        public String status;
        public StatusUpdate(String status) { this.status = status; }
    }

    class SavedPostRequest {
        public String postId;
        public SavedPostRequest(String postId) { this.postId = postId; }
    }

    class AttendRequest {
        public String eventId;
        public String userId;
        public AttendRequest(String eventId, String userId) {
            this.eventId = eventId;
            this.userId = userId;
        }
    }

    class PaymentRequest {
        public String paymentId;
        public String postId;
        public String requesterId;
        public String ownerId;
        public double amount;
        public String status;

        public PaymentRequest(String paymentId, String postId, String requesterId, String ownerId, double amount, String status) {
            this.paymentId = paymentId;
            this.postId = postId;
            this.requesterId = requesterId;
            this.ownerId = ownerId;
            this.amount = amount;
            this.status = status;
        }
    }

    class DispatchRequest {
        public String paymentId;
        public String buyerId;
        public String sellerId;
        public String itemTitle;
        public String trackingNumber;

        public DispatchRequest(String paymentId, String buyerId, String sellerId, String itemTitle, String trackingNumber) {
            this.paymentId = paymentId;
            this.buyerId = buyerId;
            this.sellerId = sellerId;
            this.itemTitle = itemTitle;
            this.trackingNumber = trackingNumber;
        }
    }

    class DeliveryRequest {
        public String paymentId;
        public String buyerId;
        public String sellerId;
        public String itemTitle;

        public DeliveryRequest(String paymentId, String buyerId, String sellerId, String itemTitle) {
            this.paymentId = paymentId;
            this.buyerId = buyerId;
            this.sellerId = sellerId;
            this.itemTitle = itemTitle;
        }
    }

    class NotificationList {
        public List<Notification> notifications;
        public int count;
    }

    class Notification {
        public String id;
        public String userId;
        public String title;
        public String message;
        public String type;
        public boolean isRead;
        public String createdAt;
    }

    class UserProfile {
        public String name;
        public String email;
        public String photo;

        public UserProfile(String name, String email, String photo) {
            this.name = name;
            this.email = email;
            this.photo = photo;
        }
    }
}
