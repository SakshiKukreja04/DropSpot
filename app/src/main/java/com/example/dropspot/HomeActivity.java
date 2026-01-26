package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements FeedAdapter.OnItemClickListener {
    private static final String TAG = "HomeActivity";
    private RecyclerView rvHomeFeed;
    private FeedAdapter feedAdapter;
    private List<FeedItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FloatingActionButton fabPostItem = findViewById(R.id.fabPost);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        ChipGroup categoryChips = findViewById(R.id.category_chips);
        rvHomeFeed = findViewById(R.id.rvHomeFeed);

        setupRecyclerView();
        loadDummyData();

        if (fabPostItem != null) {
            fabPostItem.setOnClickListener(v -> {
                Log.d(TAG, "FAB clicked, navigating to PostItemActivity");
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
                    Log.d(TAG, "Navigating to PostedItemsActivity");
                    startActivity(new Intent(HomeActivity.this, PostedItemsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_announcements) {
                    Log.d(TAG, "Navigating to AnnouncementsActivity");
                    startActivity(new Intent(HomeActivity.this, AnnouncementsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Log.d(TAG, "Navigating to ProfileActivity");
                    startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }

        categoryChips.setOnCheckedChangeListener((group, checkedId) -> {
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

    private void setupRecyclerView() {
        rvHomeFeed.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new FeedAdapter(new ArrayList<>(), this);
        rvHomeFeed.setAdapter(feedAdapter);
    }

    private void loadDummyData() {
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
}
