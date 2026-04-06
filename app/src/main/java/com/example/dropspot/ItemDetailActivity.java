package com.example.dropspot;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemDetailActivity extends AppCompatActivity implements RequestAdapter.OnRequestActionListener {
    private static final String TAG = "ItemDetailActivity";
    private ImageView ivItemImage;
    private TextView tvTitle, tvCategory, tvDescription, tvDistance, tvPrice;
    private ApiService apiService;
    private String postId;
    private Post currentPost;
    private Button btnRequestItem;
    private RecyclerView rvRequests;
    private RequestAdapter requestAdapter;
    private List<Request> requestList = new ArrayList<>();
    private LinearLayout requestsSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        apiService = ApiClient.getClient().create(ApiService.class);
        postId = getIntent().getStringExtra("POST_ID");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvTitle = findViewById(R.id.tv_item_title_detail);
        tvCategory = findViewById(R.id.tv_category_detail);
        tvDescription = findViewById(R.id.tv_description_detail);
        tvDistance = findViewById(R.id.tv_distance_detail);
        ivItemImage = findViewById(R.id.iv_item_image_detail);
        btnRequestItem = findViewById(R.id.btn_request_item);
        requestsSection = findViewById(R.id.requests_section);
        
        setupRequestsRecyclerView();

        if (postId != null) {
            loadPostDetails();
        }

        if (btnRequestItem != null) {
            btnRequestItem.setOnClickListener(v -> sendRequest());
        }
    }

    private void setupRequestsRecyclerView() {
        rvRequests = findViewById(R.id.rv_requests);
        if (rvRequests != null) {
            rvRequests.setLayoutManager(new LinearLayoutManager(this));
            requestAdapter = new RequestAdapter(requestList, this);
            rvRequests.setAdapter(requestAdapter);
        }
    }

    private void loadPostDetails() {
        apiService.getPostDetails(postId).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Post>> call, @NonNull Response<ApiResponse<Post>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentPost = response.body().getData();
                    if (currentPost != null) {
                        displayPost(currentPost);
                        checkOwnershipAndLoadRequests();
                        checkIfAlreadyRequested();
                    } else {
                        Toast.makeText(ItemDetailActivity.this, "Post not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ItemDetailActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Post>> call, @NonNull Throwable t) {
                Toast.makeText(ItemDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPost(Post post) {
        tvTitle.setText(post.title != null ? post.title : "No Title");
        tvCategory.setText(post.category != null ? post.category : "No Category");
        tvDescription.setText(post.description != null ? post.description : "No Description");
        
        // Show distance if available, otherwise show price or something else
        if (post.distance > 0) {
            tvDistance.setText(String.format("%.1f km away", post.distance));
        } else {
            tvDistance.setText(String.format("$%.2f", post.price));
        }
        
        if (post.images != null && !post.images.isEmpty()) {
            Glide.with(this)
                .load(post.images.get(0))
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(ivItemImage);
        } else {
            ivItemImage.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    private void checkOwnershipAndLoadRequests() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentPost == null) return;

        if (currentUser.getUid().equals(currentPost.userId)) {
            // I am the owner
            btnRequestItem.setVisibility(View.GONE);
            if (requestsSection != null) {
                requestsSection.setVisibility(View.VISIBLE);
            }
            loadRequestsForPost();
        } else {
            // I am a viewer
            btnRequestItem.setVisibility(View.VISIBLE);
            if (requestsSection != null) {
                requestsSection.setVisibility(View.GONE);
            }
        }
    }

    private void checkIfAlreadyRequested() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentPost == null || currentUser.getUid().equals(currentPost.userId)) return;

        apiService.getRequests("my_sent").enqueue(new Callback<ApiResponse<List<Request>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Request>>> call, Response<ApiResponse<List<Request>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Request> sentRequests = response.body().getData();
                    if (sentRequests != null) {
                        for (Request r : sentRequests) {
                            if (r.postId.equals(postId)) {
                                btnRequestItem.setEnabled(false);
                                btnRequestItem.setText("Already Requested");
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Request>>> call, Throwable t) {
                Log.e(TAG, "Error checking sent requests", t);
            }
        });
    }

    private void loadRequestsForPost() {
        apiService.getRequests("my_received").enqueue(new Callback<ApiResponse<List<Request>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Request>>> call, Response<ApiResponse<List<Request>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Request> allRequests = response.body().getData();
                    requestList.clear();
                    if (allRequests != null) {
                        for (Request r : allRequests) {
                            if (r.postId.equals(postId)) {
                                requestList.add(r);
                            }
                        }
                    }
                    if (requestAdapter != null) {
                        requestAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Request>>> call, Throwable t) {
                Log.e(TAG, "Error loading requests", t);
            }
        });
    }

    private void sendRequest() {
        btnRequestItem.setEnabled(false);
        ApiService.RequestBody body = new ApiService.RequestBody(postId, "I am interested in this item!");
        apiService.createRequest(body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Snackbar.make(btnRequestItem, "Request sent!", Snackbar.LENGTH_SHORT).show();
                    btnRequestItem.setText("Request Sent");
                } else {
                    btnRequestItem.setEnabled(true);
                    Toast.makeText(ItemDetailActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                btnRequestItem.setEnabled(true);
                Toast.makeText(ItemDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAccept(Request request) {
        updateStatus(request.id, "accepted");
    }

    @Override
    public void onReject(Request request) {
        updateStatus(request.id, "rejected");
    }

    private void updateStatus(String requestId, String status) {
        apiService.updateRequestStatus(requestId, new ApiService.StatusUpdate(status)).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ItemDetailActivity.this, "Status updated to " + status, Toast.LENGTH_SHORT).show();
                    loadRequestsForPost(); // Refresh list
                } else {
                    Toast.makeText(ItemDetailActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(ItemDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
