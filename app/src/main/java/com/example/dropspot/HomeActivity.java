package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements FeedAdapter.OnItemClickListener {
    private static final String TAG = "HomeActivity";
    private RecyclerView rvHomeFeed;
    private FeedAdapter feedAdapter;
    private final List<FeedItem> allItems = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private GestureDetector swipeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FloatingActionButton fabPostItem = findViewById(R.id.fabPost);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        ChipGroup categoryChips = findViewById(R.id.category_chips);
        rvHomeFeed = findViewById(R.id.rvHomeFeed);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);

        setupRecyclerView();
        loadDummyData();

        // Feature 4: Swipe to Refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d(TAG, "Swipe to refresh triggered");
                Toast.makeText(HomeActivity.this, "Feed Refreshed", Toast.LENGTH_SHORT).show();
                loadDummyData(); // Simulate refresh
                swipeRefreshLayout.setRefreshing(false);
            });
        }

        // Feature 5 (Optional): Swipe Left/Right detection
        swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(@Nullable MotionEvent e1, @Nullable MotionEvent e2, float velocityX, float velocityY) {
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
            boolean handled = swipeDetector.onTouchEvent(event);
            if (!handled && event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return handled;
        });

        if (fabPostItem != null) {
            fabPostItem.setOnClickListener(v -> {
                Log.d(TAG, "FAB clicked, navigating to PostItemActivity");
                Toast.makeText(HomeActivity.this, "Opening Post Screen", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(HomeActivity.this, PostItemActivity.class));
            });
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_saved) {
                    startActivity(new Intent(HomeActivity.this, PostedItemsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_announcements) {
                    startActivity(new Intent(HomeActivity.this, AnnouncementsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    // Navigate to ProfileActivity instead of showing logout dialog
                    startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }

        if (categoryChips != null) {
            categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                int checkedId = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);
                if (checkedId == R.id.chip_all) {
                    feedAdapter = new FeedAdapter(allItems, this);
                } else if (checkedId == R.id.chip_electronics) {
                    filterItems("Electronics");
                } else if (checkedId == R.id.chip_furniture) {
                    filterItems("Furniture");
                } else if (checkedId == R.id.chip_clothing) {
                    filterItems("Clothing");
                }
                rvHomeFeed.setAdapter(feedAdapter);
            });
        }
    }

    private void setupRecyclerView() {
        rvHomeFeed.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new FeedAdapter(new ArrayList<>(), this);
        rvHomeFeed.setAdapter(feedAdapter);
    }

    private void loadDummyData() {
        allItems.clear();
        allItems.add(new FeedItem("Vintage Leather Jacket", "Clothing", "2.5 km", R.drawable.ic_launcher_background));
        allItems.add(new FeedItem("Antique Wooden Chair", "Furniture", "1.8 km", R.drawable.ic_launcher_background));
        allItems.add(new FeedItem("Wireless Headphones", "Electronics", "3.1 km", R.drawable.ic_launcher_background));
        allItems.add(new FeedItem("Graphic T-Shirt", "Clothing", "0.5 km", R.drawable.ic_launcher_background));
        feedAdapter = new FeedAdapter(allItems, this);
        rvHomeFeed.setAdapter(feedAdapter);
    }

    private void filterItems(String category) {
        List<FeedItem> filteredItems = new ArrayList<>();
        for (FeedItem item : allItems) {
            if (item.getCategory().equals(category)) {
                filteredItems.add(item);
            }
        }
        feedAdapter = new FeedAdapter(filteredItems, this);
        rvHomeFeed.setAdapter(feedAdapter);
    }

    @Override
    public void onItemClick(FeedItem item) {
        Intent intent = new Intent(HomeActivity.this, ItemDetailActivity.class);
        intent.putExtra("ITEM_TITLE", item.getTitle());
        intent.putExtra("ITEM_CATEGORY", item.getCategory());
        intent.putExtra("ITEM_DISTANCE", item.getDistance());
        intent.putExtra("ITEM_IMAGE", item.getImageResource());
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(FeedItem item) {
        feedAdapter.showDeleteButton(item);
        Toast.makeText(this, "Delete option shown for: " + item.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(FeedItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    feedAdapter.removeItem(item);
                    allItems.remove(item);
                    Toast.makeText(this, "Item Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
