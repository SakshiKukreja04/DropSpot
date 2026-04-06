package com.example.dropspot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements FeedAdapter.OnItemClickListener {
    private static final String TAG = "HomeFragment";
    private RecyclerView rvHomeFeed;
    private FeedAdapter feedAdapter;
    private final List<Post> allPosts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;
    private double currentLat = 19.0760, currentLng = 72.8777; // Default to Mumbai
    private boolean isRefreshing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);

        // Hide UI elements that are now handled by MainActivity
        View bottomNavigation = view.findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.GONE);

        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        rvHomeFeed = view.findViewById(R.id.rvHomeFeed);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        FloatingActionButton fabPostItem = view.findViewById(R.id.fabPost);
        ChipGroup categoryChips = view.findViewById(R.id.category_chips);

        setupRecyclerView();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::requestLocationAndLoadPosts);
        }

        requestLocationAndLoadPosts();

        if (fabPostItem != null) {
            fabPostItem.setOnClickListener(v -> startActivity(new Intent(getActivity(), PostItemActivity.class)));
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

        return view;
    }

    private void setupRecyclerView() {
        rvHomeFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        feedAdapter = new FeedAdapter(allPosts, this);
        rvHomeFeed.setAdapter(feedAdapter);
    }

    private void requestLocationAndLoadPosts() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            loadPosts(null); 
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
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
        Intent intent = new Intent(getActivity(), ItemDetailActivity.class);
        intent.putExtra("POST_ID", item.id);
        startActivity(intent);
    }

    @Override public void onItemLongClick(Post item) { feedAdapter.showDeleteButton(item); }

    @Override public void onDeleteClick(Post item) {
        new AlertDialog.Builder(requireContext())
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
