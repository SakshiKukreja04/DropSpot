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
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for displaying user's sent requests (items they're interested in buying)
 * Replaces MyRequestsActivity to maintain consistent navigation within MainActivity
 */
public class MyRequestsFragment extends Fragment {
    private static final String TAG = "MyRequestsFragment";
    private RecyclerView rvMyRequests;
    private MyRequestsAdapter requestsAdapter;
    private List<Request> requestList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private ApiService apiService;
    private TextView tvNoRequests;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_my_requests, container, false);

        // Hide elements that are handled by MainActivity
        View toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setVisibility(View.GONE);

        apiService = ApiClient.getClient().create(ApiService.class);

        rvMyRequests = view.findViewById(R.id.rv_my_requests);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        tvNoRequests = view.findViewById(R.id.tv_no_requests);

        setupRecyclerView();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchMyRequests);
        }

        fetchMyRequests();

        return view;
    }

    private void setupRecyclerView() {
        rvMyRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsAdapter = new MyRequestsAdapter(getContext(), requestList);
        requestsAdapter.setOnRefresh(this::fetchMyRequests);
        rvMyRequests.setAdapter(requestsAdapter);
    }

    private void fetchMyRequests() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        apiService.getRequests("my_sent").enqueue(new Callback<ApiResponse<List<Request>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Request>>> call, @NonNull Response<ApiResponse<List<Request>>> response) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (response.isSuccessful() && response.body() != null) {
                    requestList.clear();
                    List<Request> data = response.body().getData();
                    if (data != null) {
                        requestList.addAll(data);
                    }
                    requestsAdapter.notifyDataSetChanged();
                    tvNoRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(getContext(), "Failed to load requests", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Request>>> call, @NonNull Throwable t) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Log.e(TAG, "Error fetching requests", t);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
