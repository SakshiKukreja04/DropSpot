package com.example.dropspot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements FeedAdapter.OnItemClickListener {
    private static final String TAG = "HomeActivity";
    private RecyclerView rvHomeFeed;
    private FeedAdapter feedAdapter;
    private List<Post> allPosts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;
    private double currentLat = 19.0760, currentLng = 72.8777; // Default to Mumbai
    private boolean isRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        rvHomeFeed = findViewById(R.id.rvHomeFeed);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        FloatingActionButton fabPostItem = findViewById(R.id.fabPost);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        ChipGroup categoryChips = findViewById(R.id.category_chips);

        setupRecyclerView();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::requestLocationAndLoadPosts);
        }

        requestLocationAndLoadPosts();

        if (fabPostItem != null) {
            fabPostItem.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PostItemActivity.class)));
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) return true; // Already here
                
                Intent intent = null;
                if (itemId == R.id.nav_saved) intent = new Intent(this, PostedItemsActivity.class);
                else if (itemId == R.id.nav_announcements) intent = new Intent(this, AnnouncementsActivity.class);
                else if (itemId == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);
                
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }

        if (categoryChips != null) {
            categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                int checkedId = checkedIds.isEmpty() ? -1 : checkedIds.get(0);
                String category = null;
                if (checkedId == R.id.chip_electronics) category = "Electronics";
                else if (checkedId == R.id.chip_furniture) category = "Furniture";
                else if (checkedId == R.id.chip_clothing) category = "Clothing";
                loadPosts(category);
            });
        }
    }

    private void setupRecyclerView() {
        rvHomeFeed.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new FeedAdapter(allPosts, this);
        rvHomeFeed.setAdapter(feedAdapter);
    }

    private void requestLocationAndLoadPosts() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            loadPosts(null); 
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
            }
            loadPosts(null);
        }).addOnFailureListener(e -> loadPosts(null));
    }

    private void loadPosts(String category) {
        if (isRefreshing) return;
        isRefreshing = true;
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        apiService.getPosts(category, 50, 0, currentLat, currentLng, 50.0, false).enqueue(new Callback<ApiResponse<PostList>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PostList>> call, @NonNull Response<ApiResponse<PostList>> response) {
                isRefreshing = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allPosts.clear();
                    List<Post> fetchedPosts = response.body().getData().getPosts();
                    if (fetchedPosts != null) allPosts.addAll(fetchedPosts);
                    feedAdapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "Load failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PostList>> call, @NonNull Throwable t) {
                isRefreshing = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Network failure: " + t.getMessage());
            }
        });
    }

    @Override public void onItemClick(Post item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("POST_ID", item.id);
        startActivity(intent);
    }

    @Override public void onItemLongClick(Post item) { feedAdapter.showDeleteButton(item); }

    @Override public void onDeleteClick(Post item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Delete this post?")
                .setPositiveButton("Delete", (dialog, id) -> {
                    apiService.deletePost(item.id).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> r) {
                            if (r.isSuccessful()) {
                                allPosts.remove(item);
                                feedAdapter.notifyDataSetChanged();
                            }
                        }
                        @Override public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {}
                    });
                }).setNegativeButton("Cancel", null).show();
    }
}
