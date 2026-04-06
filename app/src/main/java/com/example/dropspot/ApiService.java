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

    @POST("events")
    Call<ApiResponse<Event>> createEvent(@Body Event event);

    @POST("events/{id}/join")
    Call<ApiResponse<Void>> joinEvent(@Path("id") String eventId);

    @POST("events/{id}/leave")
    Call<ApiResponse<Void>> leaveEvent(@Path("id") String eventId);

    // Notifications
    @GET("notifications/{userId}")
    Call<ApiResponse<List<Notification>>> getNotifications(@Path("userId") String userId);

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

    class Event {
        public String id;
        public String title;
        public String description;
        public String category;
        public String location;
        public String startTime;
        public String endTime;
        public String creatorId;
        public List<String> participants;
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
