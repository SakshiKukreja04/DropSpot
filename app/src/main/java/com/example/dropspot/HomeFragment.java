package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements FeedAdapter.OnItemClickListener {
    private RecyclerView rvHomeFeed;
    private FeedAdapter feedAdapter;
    private final List<FeedItem> allItems = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);

        FloatingActionButton fabPostItem = view.findViewById(R.id.fabPost);
        ChipGroup categoryChips = view.findViewById(R.id.category_chips);
        rvHomeFeed = view.findViewById(R.id.rvHomeFeed);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);

        setupRecyclerView();
        loadDummyData();

        fabPostItem.setOnClickListener(v -> Toast.makeText(getContext(), "Opening Post Screen", Toast.LENGTH_SHORT).show());

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

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Toast.makeText(getContext(), "Feed Refreshed", Toast.LENGTH_SHORT).show();
            loadDummyData();
            swipeRefreshLayout.setRefreshing(false);
        });

        final GestureDetector gestureDetector = new GestureDetector(getContext(), new SwipeGestureDetector(view));
        view.setOnTouchListener((v, event) -> {
            boolean handled = gestureDetector.onTouchEvent(event);
            if (!handled && event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return handled;
        });

        return view;
    }

    private void setupRecyclerView() {
        rvHomeFeed.setLayoutManager(new LinearLayoutManager(getContext()));
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
        Intent intent = new Intent(getActivity(), ItemDetailActivity.class);
        intent.putExtra("ITEM_TITLE", item.getTitle());
        intent.putExtra("ITEM_CATEGORY", item.getCategory());
        intent.putExtra("ITEM_DISTANCE", item.getDistance());
        intent.putExtra("ITEM_IMAGE", item.getImageResource());
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(FeedItem item) {
        feedAdapter.showDeleteButton(item);
        Toast.makeText(getContext(), "Delete option shown for: " + item.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(FeedItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    feedAdapter.removeItem(item);
                    allItems.remove(item);
                    Toast.makeText(getContext(), "Item Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        private final View view;

        SwipeGestureDetector(View view) {
            this.view = view;
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @Nullable MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        Snackbar.make(view, "Swiped Right", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(view, "Swiped Left", Snackbar.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
