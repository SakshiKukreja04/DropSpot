package com.example.dropspot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
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
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements FeedAdapter.OnItemClickListener {
    private RecyclerView rvHomeFeed;
    private FeedAdapter feedAdapter;
    private List<Post> allPosts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private GestureDetector swipeDetector;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;
    private double currentLat = 0, currentLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        FloatingActionButton fabPostItem = findViewById(R.id.fabPost);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        ChipGroup categoryChips = findViewById(R.id.category_chips);
        rvHomeFeed = findViewById(R.id.rvHomeFeed);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);

        setupRecyclerView();
        requestLocationAndLoadPosts();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::requestLocationAndLoadPosts);
        }

        // Gesture detector for swipe actions (optional feature)
        swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                if (Math.abs(e1.getY() - e2.getY()) > 250) return false;
                if (e1.getX() - e2.getX() > 120 && Math.abs(velocityX) > 200) {
                    Snackbar.make(rvHomeFeed, "Swiped Left", Snackbar.LENGTH_SHORT).show();
                    return true;
                } else if (e2.getX() - e1.getX() > 120 && Math.abs(velocityX) > 200) {
                    Snackbar.make(rvHomeFeed, "Swiped Right", Snackbar.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        rvHomeFeed.setOnTouchListener((v, event) -> {
            swipeDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });

        if (fabPostItem != null) {
            fabPostItem.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PostItemActivity.class)));
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) return true;
                if (itemId == R.id.nav_saved) {
                    startActivity(new Intent(this, PostedItemsActivity.class));
                    return true;
                }
                if (itemId == R.id.nav_announcements) {
                    startActivity(new Intent(this, AnnouncementsActivity.class));
                    return true;
                }
                if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }

        if (categoryChips != null) {
            categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                int checkedId = checkedIds.isEmpty() ? -1 : checkedIds.get(0);
                if (checkedId == R.id.chip_all || checkedId == -1) {
                    loadPosts(null);
                } else if (checkedId == R.id.chip_electronics) {
                    loadPosts("Electronics");
                } else if (checkedId == R.id.chip_furniture) {
                    loadPosts("Furniture");
                } else if (checkedId == R.id.chip_clothing) {
                    loadPosts("Clothing");
                }
            });
        }
    }

    private void setupRecyclerView() {
        rvHomeFeed.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new FeedAdapter(new ArrayList<>(), this);
        rvHomeFeed.setAdapter(feedAdapter);
    }

    private void requestLocationAndLoadPosts() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            loadPosts(null); // Load without location if permission not granted yet
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
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        apiService.getPosts(category, 50, 0, currentLat, currentLng, 3.0, false).enqueue(new Callback<ApiResponse<PostList>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PostList>> call, @NonNull Response<ApiResponse<PostList>> response) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allPosts = response.body().getData().getPosts();
                    updateAdapter(allPosts);
                } else {
                    Toast.makeText(HomeActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PostList>> call, @NonNull Throwable t) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(HomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAdapter(List<Post> posts) {
        feedAdapter = new FeedAdapter(posts, this);
        rvHomeFeed.setAdapter(feedAdapter);
    }

    @Override
    public void onItemClick(Post item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("POST_ID", item.id);
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(Post item) {
        feedAdapter.showDeleteButton(item);
        Toast.makeText(this, "Delete option shown for: " + item.title, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(Post item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    apiService.deletePost(item.id).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                            if (response.isSuccessful()) {
                                allPosts.remove(item);
                                feedAdapter.removeItem(item);
                                Toast.makeText(HomeActivity.this, "Post deleted successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(HomeActivity.this, "Failed to delete post", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                            Toast.makeText(HomeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
