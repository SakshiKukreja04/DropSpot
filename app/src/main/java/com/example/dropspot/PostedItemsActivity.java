package com.example.dropspot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

public class PostedItemsActivity extends AppCompatActivity {
    private static final String TAG = "PostedItemsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posted_items);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                Log.d(TAG, "Back clicked, finishing activity");
                finish();
            });
        }

        RecyclerView rvPostedItems = findViewById(R.id.rv_posted_items);
        if (rvPostedItems != null) {
            rvPostedItems.setLayoutManager(new LinearLayoutManager(this));

            // Add dummy data
            List<FeedItem> postedItems = new ArrayList<>();
            postedItems.add(new FeedItem("Vintage Leather Jacket", "Clothing", "2.5 km", R.drawable.ic_launcher_background));
            postedItems.add(new FeedItem("Antique Wooden Chair", "Furniture", "1.8 km", R.drawable.ic_launcher_background));
            postedItems.add(new FeedItem("Wireless Headphones", "Electronics", "3.1 km", R.drawable.ic_launcher_background));

            // Simulate a request for one of the items
            SharedPreferences prefs = getSharedPreferences("RequestPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("Antique Wooden Chair", "REQUESTED");
            editor.apply();

            PostedItemsAdapter postedItemsAdapter = new PostedItemsAdapter(this, postedItems);
            rvPostedItems.setAdapter(postedItemsAdapter);
        }
    }
}