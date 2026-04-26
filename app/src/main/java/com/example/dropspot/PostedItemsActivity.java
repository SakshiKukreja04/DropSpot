package com.example.dropspot;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostedItemsActivity extends AppCompatActivity {
    private static final String TAG = "PostedItemsActivity";
    private RecyclerView rvPostedItems;
    private List<Post> myPosts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private ApiService apiService;
    private FirebaseFirestore firebaseFirestore;
    private String currentUserId;
    private TextView tvNoPosts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posted_items);

        apiService = ApiClient.getClient().create(ApiService.class);
        firebaseFirestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvPostedItems = findViewById(R.id.rv_posted_items);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        tvNoPosts = findViewById(R.id.tv_no_posts);

        setupRecyclerView();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchMyPosts);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FORCE DATA REFRESH when returning to this screen
        fetchMyPosts();
    }

    private void setupRecyclerView() {
        rvPostedItems.setLayoutManager(new LinearLayoutManager(this));
        UnifiedPostAdapter adapter = new UnifiedPostAdapter(this, myPosts, firebaseFirestore, currentUserId);
        rvPostedItems.setAdapter(adapter);
    }

    private void fetchMyPosts() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        apiService.getPosts(null, 50, 0, null, null, null, true).enqueue(new Callback<ApiResponse<PostList>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PostList>> call, @NonNull Response<ApiResponse<PostList>> response) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    myPosts.clear();
                    myPosts.addAll(response.body().getData().getPosts());
                    if (rvPostedItems.getAdapter() != null) {
                        rvPostedItems.getAdapter().notifyDataSetChanged();
                    }
                    
                    if (myPosts.isEmpty()) {
                        tvNoPosts.setVisibility(View.VISIBLE);
                    } else {
                        tvNoPosts.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(PostedItemsActivity.this, "Failed to load your posts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PostList>> call, @NonNull Throwable t) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Log.e(TAG, "Error fetching posts", t);
                Toast.makeText(PostedItemsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
