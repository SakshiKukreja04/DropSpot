package com.example.dropspot;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemDetailActivity extends AppCompatActivity {
    private static final String TAG = "ItemDetailActivity";
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private ImageView ivItemImage;
    private TextView tvTitle, tvCategory, tvDescription, tvDistance;
    private ApiService apiService;
    private String postId;

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
        Button btnRequestItem = findViewById(R.id.btn_request_item);

        if (postId != null) {
            loadPostDetails();
        }

        if (btnRequestItem != null) {
            btnRequestItem.setOnClickListener(v -> {
                sendRequest();
            });
        }

        setupGestures();
    }

    private void loadPostDetails() {
        apiService.getPostDetails(postId).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Post>> call, @NonNull Response<ApiResponse<Post>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Post post = response.body().getData();
                    displayPost(post);
                } else {
                    Toast.makeText(ItemDetailActivity.this, "Failed to load item details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Post>> call, @NonNull Throwable t) {
                Toast.makeText(ItemDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPost(Post post) {
        if (tvTitle != null) tvTitle.setText(post.title);
        if (tvCategory != null) tvCategory.setText(post.category);
        if (tvDescription != null) tvDescription.setText(post.description);
        if (tvDistance != null) tvDistance.setText(String.format("%.1f km", post.distance));
        
        if (post.images != null && !post.images.isEmpty()) {
            Glide.with(this)
                .load(post.images.get(0))
                .placeholder(R.drawable.ic_launcher_background)
                .into(ivItemImage);
        }
    }

    private void sendRequest() {
        ApiService.RequestBody requestBody = new ApiService.RequestBody(postId, "I am interested in this item!");
        apiService.createRequest(requestBody).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Snackbar.make(ivItemImage, "Request Sent Successfully", Snackbar.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ItemDetailActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                Toast.makeText(ItemDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Toast.makeText(ItemDetailActivity.this, "Item Saved", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
                ivItemImage.setScaleX(scaleFactor);
                ivItemImage.setScaleY(scaleFactor);
                return true;
            }
        });

        if (ivItemImage != null) {
            ivItemImage.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                scaleGestureDetector.onTouchEvent(event);
                return true;
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
