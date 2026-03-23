# DropSpot Android Integration Guide

This guide explains how to integrate the DropSpot Android app with the Firestore REST API backend.

## Architecture Overview

```
┌─────────────────────┐
│   Android App       │
│  (Firebase Auth)    │
└──────────┬──────────┘
           │
      Get ID Token
           │
           ▼
┌─────────────────────┐         ┌──────────────────┐
│  Node.js API        │────────▶│  Firebase Admin  │
│  (Express)          │         │  SDK             │
└─────────────────────┘         └──────────────────┘
           │
           │
           ▼
    ┌─────────────┐
    │  Firestore  │
    │  Database   │
    └─────────────┘
```

## Setup Steps

### 1. Configure API Base URL

In your Android app, create a constants file:

**res/values/strings.xml:**
```xml
<resources>
    <string name="api_base_url">http://10.0.2.2:5000</string>
    <!-- Use 10.0.2.2 for emulator, actual IP for device -->
</resources>
```

### 2. Setup Retrofit with Interceptor

**ApiClient.java:**
```java
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.Interceptor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            // Add interceptor to attach Firebase token
            httpClient.addInterceptor(chain -> {
                okhttp3.Request originalRequest = chain.request();
                
                // Get Firebase ID token
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    user.getIdToken(true).addOnSuccessListener(result -> {
                        String token = result.getToken();
                        
                        // Add token to header
                        okhttp3.Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .header("Content-Type", "application/json")
                            .build();
                        
                        return chain.proceed(newRequest);
                    });
                }
                
                return chain.proceed(originalRequest);
            });

            String baseUrl = context.getString(R.string.api_base_url) + "/api/";

            retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        }

        return retrofit;
    }
}
```

### 3. Create API Service Interface

**ApiService.java:**
```java
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // Posts
    @POST("posts")
    Call<ApiResponse<Post>> createPost(@Body Post post);

    @GET("posts")
    Call<ApiResponse<PostList>> getPosts(
        @Query("category") String category,
        @Query("limit") int limit,
        @Query("offset") int offset,
        @Query("latitude") double latitude,
        @Query("longitude") double longitude,
        @Query("maxDistance") int maxDistance
    );

    @GET("posts/{id}")
    Call<ApiResponse<Post>> getPostDetails(@Path("id") String postId);

    @PUT("posts/{id}")
    Call<ApiResponse<Post>> updatePost(@Path("id") String postId, @Body Post post);

    @DELETE("posts/{id}")
    Call<ApiResponse<Void>> deletePost(@Path("id") String postId);

    // Requests
    @POST("requests")
    Call<ApiResponse<Request>> createRequest(@Body RequestBody request);

    @GET("requests")
    Call<ApiResponse<RequestList>> getRequests(@Query("type") String type);

    @PUT("requests/{id}")
    Call<ApiResponse<Request>> respondToRequest(
        @Path("id") String requestId,
        @Body RequestResponse response
    );

    // Saved Posts
    @POST("saved")
    Call<ApiResponse<SavedPost>> savePost(@Body SavePostRequest request);

    @GET("saved/{userId}")
    Call<ApiResponse<SavedPostList>> getSavedPosts(@Path("userId") String userId);

    @DELETE("saved/{postId}")
    Call<ApiResponse<Void>> removeSavedPost(@Path("postId") String postId);

    // User Profile
    @POST("users")
    Call<ApiResponse<User>> updateUserProfile(@Body User user);

    @GET("users/{userId}")
    Call<ApiResponse<User>> getUserProfile(@Path("userId") String userId);

    @GET("users/{userId}/stats")
    Call<ApiResponse<UserStats>> getUserStats(@Path("userId") String userId);

    // Notifications
    @GET("notifications/{userId}")
    Call<ApiResponse<NotificationList>> getNotifications(@Path("userId") String userId);

    @PUT("notifications/{id}")
    Call<ApiResponse<Void>> markNotificationAsRead(@Path("id") String notificationId);

    @GET("notifications/{userId}/unread-count")
    Call<ApiResponse<UnreadCount>> getUnreadCount(@Path("userId") String userId);
}
```

### 4. Create Response Model

**ApiResponse.java:**
```java
public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;
    public String error;
    public int code;

    public boolean isSuccess() {
        return success && code >= 200 && code < 300;
    }
}
```

### 5. Implement Post Creation

**PostItemActivity.java:**
```java
public class PostItemActivity extends AppCompatActivity {
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        sessionManager = new SessionManager(this);

        findViewById(R.id.btn_post_item).setOnClickListener(v -> submitPost());
    }

    private void submitPost() {
        String title = ((EditText) findViewById(R.id.et_title)).getText().toString();
        String description = ((EditText) findViewById(R.id.et_description)).getText().toString();
        String category = ((Spinner) findViewById(R.id.spinner_category)).getSelectedItem().toString();

        Post post = new Post();
        post.title = title;
        post.description = description;
        post.category = category;
        post.condition = "good";

        showLoading(true);

        apiService.createPost(post).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body().isSuccess()) {
                    Toast.makeText(PostItemActivity.this, "Post created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PostItemActivity.this, "Error creating post", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(PostItemActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PostItemActivity", "API Error", t);
            }
        });
    }

    private void showLoading(boolean show) {
        findViewById(R.id.progress_bar).setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
```

### 6. Implement Feed Loading

**HomeFragment.java:**
```java
public class HomeFragment extends Fragment {
    private ApiService apiService;
    private FeedAdapter adapter;
    private LocationManager locationManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        apiService = ApiClient.getClient(getContext()).create(ApiService.class);
        adapter = new FeedAdapter(new ArrayList<>());

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadFeed();

        return root;
    }

    private void loadFeed() {
        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        Location location = getLastKnownLocation();

        double latitude = location != null ? location.getLatitude() : 0;
        double longitude = location != null ? location.getLongitude() : 0;

        apiService.getPosts(
            null, // category
            20,   // limit
            0,    // offset
            latitude,
            longitude,
            50    // maxDistance in km
        ).enqueue(new Callback<ApiResponse<PostList>>() {
            @Override
            public void onResponse(Call<ApiResponse<PostList>> call, Response<ApiResponse<PostList>> response) {
                if (response.isSuccessful() && response.body().isSuccess()) {
                    PostList postList = response.body().data;
                    adapter.updateItems(postList.posts);
                } else {
                    Toast.makeText(getContext(), "Error loading posts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PostList>> call, Throwable t) {
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                Log.e("HomeFragment", "API Error", t);
            }
        });
    }
}
```

### 7. Handle Authentication

Do this after Firebase login in LoginActivity:

```java
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // After Firebase Google Sign-In succeeds:
        firebaseAuthWithGoogle(account.getIdToken());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        // ... existing Firebase auth code ...

        // After successful authentication, create user profile in backend
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            updateUserProfile(user);
        }
    }

    private void updateUserProfile(FirebaseUser user) {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        User profileUser = new User();
        profileUser.name = user.getDisplayName();
        profileUser.email = user.getEmail();
        profileUser.photo = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

        apiService.updateUserProfile(profileUser).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful()) {
                    // User profile created, navigate to main activity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Log.e("LoginActivity", "Failed to create user profile", t);
            }
        });
    }
}
```

### 8. Add Required Dependencies

**app/build.gradle:**
```gradle
dependencies {
    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // OkHttp
    implementation 'com.squareup.okhttp3:okhttp:4.10.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.10.0'
    
    // Firebase
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
    
    // Gson
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

## Usage Examples

### 1. Create a Post

```java
Post post = new Post();
post.title = "iPhone 12";
post.description = "Excellent condition";
post.category = "Electronics";
post.condition = "excellent";

apiService.createPost(post).enqueue(callback);
```

### 2. Load Posts with Distance Filter

```java
apiService.getPosts(
    "Electronics",  // category
    20,            // limit
    0,             // offset
    40.7128,       // latitude
    -74.0060,      // longitude
    50             // max distance in km
).enqueue(callback);
```

### 3. Request an Item

```java
RequestBody request = new RequestBody();
request.postId = "post-id-here";
request.message = "Hi, I'm interested!";

apiService.createRequest(request).enqueue(callback);
```

### 4. Save a Post

```java
SavePostRequest save = new SavePostRequest();
save.postId = "post-id-here";

apiService.savePost(save).enqueue(callback);
```

### 5. Respond to Request

```java
RequestResponse response = new RequestResponse();
response.status = "accepted"; // or "rejected"

apiService.respondToRequest(requestId, response).enqueue(callback);
```

## Error Handling

```java
private void handleApiError(Response<?> response) {
    try {
        if (response.errorBody() != null) {
            String errorJson = response.errorBody().string();
            JSONObject errorObj = new JSONObject(errorJson);
            
            String message = errorObj.getString("message");
            String errorCode = errorObj.getString("error");
            
            Log.e("API Error", "Code: " + errorCode + ", Message: " + message);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    } catch (Exception e) {
        Log.e("API Error", "Error parsing response", e);
    }
}
```

## Network Connectivity Check

```java
public class NetworkUtils {
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}

// Usage:
if (NetworkUtils.isNetworkConnected(this)) {
    makeApiCall();
} else {
    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
}
```

## Testing

Use the Android Emulator with:
- Base URL: `http://10.0.2.2:5000` (maps to localhost)

Use a Physical Device with:
- Base URL: `http://<your-computer-ip>:5000`
- Example: `http://192.168.1.100:5000`

## Troubleshooting

### "Unable to resolve host"
- Check backend is running
- Verify correct IP/URL
- Check firewall settings

### "Token expired"
- Re-authenticate Firebase user
- Token automatically refreshed when needed

### "403 Forbidden"
- User trying to access another user's data
- Check authorization logic

### "CORS errors" (shouldn't happen with Android, but if using WebView)
- Backend CORS is configured in index.js
- Add your domain to `corsOptions.origin`
