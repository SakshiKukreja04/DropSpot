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
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyRequestsActivity extends AppCompatActivity {
    private static final String TAG = "MyRequestsActivity";
    private RecyclerView rvMyRequests;
    private MyRequestsAdapter requestsAdapter;
    private List<Request> requestList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private ApiService apiService;
    private TextView tvNoRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        apiService = ApiClient.getClient().create(ApiService.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMyRequests = findViewById(R.id.rv_my_requests);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        tvNoRequests = findViewById(R.id.tv_no_requests);

        setupRecyclerView();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchMyRequests);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FORCE DATA REFRESH when returning from Payment or when screen is revisited
        fetchMyRequests();
    }

    private void setupRecyclerView() {
        rvMyRequests.setLayoutManager(new LinearLayoutManager(this));
        requestsAdapter = new MyRequestsAdapter(this, requestList);
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
                        // Log fetched request status list for debugging
                        Log.d("STATUS_CHECK", "Fetched " + data.size() + " requests for requester");
                        for (Request r : data) {
                            Log.d("STATUS_CHECK", "Requester View - Request ID: " + r.getEffectiveId() + ", Status: " + r.status);
                        }
                    }
                    requestsAdapter.notifyDataSetChanged();
                    tvNoRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(MyRequestsActivity.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Request>>> call, @NonNull Throwable t) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Log.e(TAG, "Error fetching requests", t);
                Toast.makeText(MyRequestsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
