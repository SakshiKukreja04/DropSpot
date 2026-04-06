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
    private ImageView ivItemImage, ivOwnerPhoto;
    private TextView tvTitle, tvCategory, tvDescription, tvDistance, tvPrice, tvCondition, tvOwnerName, tvOwnerEmail;
    private ApiService apiService;
    private String postId;
    private Post currentPost;
    private Button btnRequestItem;
    private RecyclerView rvRequests;
    private RequestAdapter requestAdapter;
    private List<Request> requestList = new ArrayList<>();
    private LinearLayout requestsSection, ownerInfoSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        apiService = ApiClient.getClient().create(ApiService.class);
        postId = getIntent().getStringExtra("POST_ID");

        initViews();
        setupRequestsRecyclerView();

        if (postId != null) {
            loadPostDetails();
        }

        btnRequestItem.setOnClickListener(v -> sendRequest());
    }

    private void initViews() {
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
        tvPrice = findViewById(R.id.tv_price_detail);
        tvCondition = findViewById(R.id.tv_condition_detail);
        tvOwnerName = findViewById(R.id.tv_owner_name);
        tvOwnerEmail = findViewById(R.id.tv_owner_email);
        ivItemImage = findViewById(R.id.iv_item_image_detail);
        ivOwnerPhoto = findViewById(R.id.iv_owner_photo);
        btnRequestItem = findViewById(R.id.btn_request_item);
        requestsSection = findViewById(R.id.requests_section);
        ownerInfoSection = findViewById(R.id.owner_info_section);
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
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Post>> call, @NonNull Throwable t) {
                Toast.makeText(ItemDetailActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPost(Post post) {
        tvTitle.setText(post.title);
        tvCategory.setText(post.category);
        tvDescription.setText(post.description);
        tvPrice.setText(String.format("₹%.2f", post.price));
        tvCondition.setText(post.condition);
        tvDistance.setText(String.format("%.1f km away", post.distance));
        
        tvOwnerName.setText(post.ownerName != null && !post.ownerName.isEmpty() ? post.ownerName : "Unknown Owner");
        tvOwnerEmail.setText(post.ownerEmail != null ? post.ownerEmail : "");
        
        if (post.ownerPhoto != null && !post.ownerPhoto.isEmpty()) {
            Glide.with(this).load(post.ownerPhoto).circleCrop().placeholder(R.drawable.ic_launcher_background).into(ivOwnerPhoto);
        }

        if (post.images != null && !post.images.isEmpty()) {
            Glide.with(this).load(post.images.get(0)).placeholder(R.drawable.ic_launcher_background).into(ivItemImage);
        }

        if (!post.isActive) {
            btnRequestItem.setEnabled(false);
            btnRequestItem.setText("Item No Longer Available");
        }
    }

    private void checkOwnershipAndLoadRequests() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentPost == null) return;

        if (currentUser.getUid().equals(currentPost.userId)) {
            btnRequestItem.setVisibility(View.GONE);
            ownerInfoSection.setVisibility(View.GONE);
            requestsSection.setVisibility(View.VISIBLE);
            loadRequestsForPost();
        } else {
            btnRequestItem.setVisibility(View.VISIBLE);
            ownerInfoSection.setVisibility(View.VISIBLE);
            requestsSection.setVisibility(View.GONE);
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
            public void onFailure(Call<ApiResponse<List<Request>>> call, Throwable t) {}
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
            public void onFailure(Call<ApiResponse<List<Request>>> call, Throwable t) {}
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
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                btnRequestItem.setEnabled(true);
            }
        });
    }

    @Override
    public void onAccept(Request request) {
        String rid = request.getEffectiveId();
        if (rid == null) {
            Toast.makeText(this, "Error: Request ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        updateStatus(rid, "accepted");
    }

    @Override
    public void onReject(Request request) {
        String rid = request.getEffectiveId();
        if (rid == null) {
            Toast.makeText(this, "Error: Request ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        updateStatus(rid, "rejected");
    }

    private void updateStatus(String requestId, String status) {
        Log.d(TAG, "Updating request: " + requestId + " to " + status);
        apiService.updateRequestStatus(requestId, new ApiService.StatusUpdate(status)).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    if ("accepted".equals(status)) {
                        closePostAfterAcceptance();
                    } else {
                        loadRequestsForPost();
                    }
                } else {
                    Toast.makeText(ItemDetailActivity.this, "Failed to update request", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(ItemDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void closePostAfterAcceptance() {
        if (currentPost == null) return;

        currentPost.isActive = false;
        apiService.updatePost(currentPost.id, currentPost).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
                loadPostDetails(); // Refresh everything
            }

            @Override
            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
                loadPostDetails();
            }
        });
    }
}
