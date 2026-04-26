package com.example.dropspot;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostedItemsFragment extends Fragment {
    private static final String TAG = "PostedItemsFragment";
    private RecyclerView rvPostedItems;
    private final List<Post> myPosts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private ApiService apiService;
    private FirebaseFirestore firebaseFirestore;
    private String currentUserId;
    private TextView tvNoPosts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_posted_items, container, false);

        // Hide the BottomNavigationView and Toolbar from the inflated layout 
        // because MainActivity already provides them.
        View bottomNav = view.findViewById(R.id.bottomNavigation);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        
        View toolbar = view.findViewById(R.id.appBarLayout);
        if (toolbar != null) toolbar.setVisibility(View.GONE);

        apiService = ApiClient.getClient().create(ApiService.class);
        firebaseFirestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvPostedItems = view.findViewById(R.id.rv_posted_items);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        tvNoPosts = view.findViewById(R.id.tv_no_posts);

        setupRecyclerView();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchMyPosts);
        }

        fetchMyPosts();

        return view;
    }

    private void setupRecyclerView() {
        rvPostedItems.setLayoutManager(new LinearLayoutManager(getContext()));
        // Use UnifiedPostAdapter instead of PostedItemsAdapter (BUG 2 FIX)
        UnifiedPostAdapter adapter = new UnifiedPostAdapter(requireContext(), myPosts, firebaseFirestore, currentUserId);
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
                    List<Post> posts = response.body().getData().getPosts();
                    Log.d(TAG, "Loaded " + posts.size() + " posts");
                    for (Post p : posts) {
                        Log.d(TAG, "Post: " + p.title + ", ID: " + p.id + ", Active: " + p.isActive);
                    }
                    myPosts.addAll(posts);
                    rvPostedItems.getAdapter().notifyDataSetChanged();
                    
                    if (myPosts.isEmpty()) {
                        tvNoPosts.setVisibility(View.VISIBLE);
                    } else {
                        tvNoPosts.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load your posts", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load posts: response = " + response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PostList>> call, @NonNull Throwable t) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Log.e(TAG, "Error fetching posts", t);
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
