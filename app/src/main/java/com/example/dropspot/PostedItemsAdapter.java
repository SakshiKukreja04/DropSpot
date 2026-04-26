package com.example.dropspot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostedItemsAdapter extends RecyclerView.Adapter<PostedItemsAdapter.PostedItemsViewHolder> {

    private final List<Post> postedItems;
    private final Context context;
    private final FirebaseFirestore firebaseFirestore;
    private final String currentUserId;

    public PostedItemsAdapter(Context context, List<Post> postedItems, FirebaseFirestore firebaseFirestore, String currentUserId) {
        this.context = context;
        this.postedItems = postedItems;
        this.firebaseFirestore = firebaseFirestore;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public PostedItemsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_posted, parent, false);
        return new PostedItemsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostedItemsViewHolder holder, int position) {
        Post item = postedItems.get(position);
        holder.bind(item, firebaseFirestore, currentUserId, context);
        
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ItemDetailActivity.class);
            intent.putExtra("POST_ID", item.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postedItems.size();
    }

    static class PostedItemsViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, status, tvOrderStatus;
        ImageView image;
        Button btnDispatch;

        public PostedItemsViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvItemTitle);
            category = itemView.findViewById(R.id.tvCategory);
            image = itemView.findViewById(R.id.ivItemImage);
            status = itemView.findViewById(R.id.tv_item_status);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            btnDispatch = itemView.findViewById(R.id.btn_dispatch);
        }

        public void bind(final Post item, FirebaseFirestore firebaseFirestore, String currentUserId, Context context) {
            title.setText(item.title);
            category.setText(item.category);
            
            // Debug logging for current item
            Log.d("PostedItemsAdapter", "Binding item: " + item.title + ", ID: " + item.id + ", currentUserId: " + currentUserId);
            
            // Initial UI state
            if (btnDispatch != null) btnDispatch.setVisibility(View.GONE);
            if (tvOrderStatus != null) tvOrderStatus.setVisibility(View.GONE);
            if (status != null) status.setVisibility(View.VISIBLE);
            
            // Check for transactions - First check payments collection
            firebaseFirestore.collection("payments")
                    .whereEqualTo("postId", item.id)
                    .whereEqualTo("ownerId", currentUserId)
                    .get()
                    .addOnSuccessListener(paymentSnapshots -> {
                        Log.d("PostedItemsAdapter", "Payments query result: " + paymentSnapshots.size() + " documents");
                        if (!paymentSnapshots.isEmpty()) {
                            // Payment found - use payment status
                            handlePaymentStatus(paymentSnapshots.getDocuments().get(0), item, context, currentUserId);
                        } else {
                            // No payment found - check requests collection as fallback
                            Log.d("PostedItemsAdapter", "No payments found, checking requests collection");
                            firebaseFirestore.collection("requests")
                                    .whereEqualTo("postId", item.id)
                                    .whereEqualTo("postOwnerId", currentUserId)
                                    .whereEqualTo("status", "paid")
                                    .get()
                                    .addOnSuccessListener(requestSnapshots -> {
                                        Log.d("PostedItemsAdapter", "Requests query result: " + requestSnapshots.size() + " documents");
                                        if (!requestSnapshots.isEmpty()) {
                                            // Request found with paid status - treat as paid
                                            handleRequestStatus(requestSnapshots.getDocuments().get(0), item, context, currentUserId);
                                        } else {
                                            // No transaction - show listing status
                                            Log.d("PostedItemsAdapter", "No paid requests found, showing listing status");
                                            showListingStatus(item, context);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("PostedItemsAdapter", "Error checking requests: " + e.getMessage());
                                        showListingStatus(item, context);
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PostedItemsAdapter", "Error checking payments: " + e.getMessage());
                        showListingStatus(item, context);
                    });
            
            if (item.images != null && !item.images.isEmpty()) {
                Glide.with(itemView.getContext()).load(item.images.get(0)).placeholder(R.drawable.ic_launcher_background).into(image);
            }
        }
        
        private void handlePaymentStatus(DocumentSnapshot document, Post item, Context context, String currentUserId) {
            String paymentStatus = document.getString("status");
            String paymentStatusField = document.getString("paymentStatus");
            
            // Debug logging
            Log.d("PostedItemsAdapter", "Payment found - status: " + paymentStatus + ", paymentStatus: " + paymentStatusField);
            
            if (tvOrderStatus != null) {
                tvOrderStatus.setVisibility(View.VISIBLE);
                
                // Check both status and paymentStatus fields - be more permissive
                boolean isPaid = ("paid".equalsIgnoreCase(paymentStatus) || 
                                "success".equalsIgnoreCase(paymentStatus) || 
                                "success".equalsIgnoreCase(paymentStatusField) ||
                                "PAID".equalsIgnoreCase(paymentStatus) ||
                                "completed".equalsIgnoreCase(paymentStatus) ||
                                "dispatched".equalsIgnoreCase(paymentStatus) ||
                                "delivered".equalsIgnoreCase(paymentStatus));
                
                if (isPaid) {
                    // Check if already dispatched or delivered
                    if ("dispatched".equalsIgnoreCase(paymentStatus) || "DISPATCHED".equalsIgnoreCase(paymentStatus)) {
                        String contact = document.getString("trackingNumber");
                        tvOrderStatus.setText("📦 Dispatched 🚚\n\nDelivery Contact: " + (contact != null ? contact : "N/A"));
                        tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark));
                        if (btnDispatch != null) btnDispatch.setVisibility(View.GONE);
                    } else if ("delivered".equalsIgnoreCase(paymentStatus) || "DELIVERED".equalsIgnoreCase(paymentStatus) || 
                              "completed".equalsIgnoreCase(paymentStatus) || "COMPLETED".equalsIgnoreCase(paymentStatus)) {
                        tvOrderStatus.setText("✅ Delivered 📦\n\nOrder completed!");
                        tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                        if (btnDispatch != null) btnDispatch.setVisibility(View.GONE);
                    } else {
                        // Status is paid but not dispatched/delivered - show dispatch button
                        tvOrderStatus.setText("💰 Payment Received!");
                        tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                        
                        // Show dispatch button
                        if (btnDispatch != null) {
                            btnDispatch.setVisibility(View.VISIBLE);
                            btnDispatch.setText("Dispatch Order 🚚");
                            btnDispatch.setOnClickListener(v -> {
                                String buyerId = document.getString("requesterId");
                                String paymentId = document.getString("paymentId");
                                showDispatchDialog(paymentId, buyerId, currentUserId, item.title, context);
                            });
                        }
                    }
                } else {
                    // Status doesn't match any expected values - show listing status
                    showListingStatus(item, context);
                }
            }
        }
        
        private void handleRequestStatus(DocumentSnapshot document, Post item, Context context, String currentUserId) {
            // For requests, we assume paid status directly
            if (tvOrderStatus != null) {
                tvOrderStatus.setVisibility(View.VISIBLE);
                tvOrderStatus.setText("💰 Payment Received!");
                tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                
                // Show dispatch button
                if (btnDispatch != null) {
                    btnDispatch.setVisibility(View.VISIBLE);
                    btnDispatch.setText("Dispatch Order 🚚");
                    btnDispatch.setOnClickListener(v -> {
                        String buyerId = document.getString("requesterId");
                        String paymentId = document.getString("paymentId");
                        showDispatchDialog(paymentId, buyerId, currentUserId, item.title, context);
                    });
                }
            }
        }
        
        private void showListingStatus(Post item, Context context) {
            if (status != null) {
                status.setVisibility(View.VISIBLE);
                if (item.isActive) {
                    status.setText("Active");
                    status.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    status.setText("Closed");
                    status.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                }
            }
        }
        
        private void showDispatchDialog(String paymentId, String buyerId, String sellerId, String itemTitle, Context context) {
            final EditText etPhoneNumber = new EditText(context);
            etPhoneNumber.setHint("Enter phone number of delivery person");
            etPhoneNumber.setInputType(InputType.TYPE_CLASS_PHONE);
            etPhoneNumber.setPadding(50, 40, 50, 40);
            
            new AlertDialog.Builder(context)
                    .setTitle("Dispatch Order")
                    .setMessage("Enter the phone number of the person/agent handling the shipping.")
                    .setView(etPhoneNumber)
                    .setPositiveButton("Mark as Dispatched", (dialog, which) -> {
                        String phoneNumber = etPhoneNumber.getText().toString().trim();
                        if (phoneNumber.isEmpty()) {
                            Toast.makeText(context, "Phone number is required", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        ApiService apiService = ApiClient.getClient().create(ApiService.class);
                        // We use the trackingNumber field to send the phone number
                        apiService.markOrderDispatched(new ApiService.DispatchRequest(paymentId, buyerId, sellerId, itemTitle, phoneNumber, null))
                                .enqueue(new Callback<ApiResponse<Object>>() {
                                    @Override
                                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(context, "Order Dispatched! 🚀", Toast.LENGTH_SHORT).show();
                                            tvOrderStatus.setText("📦 Dispatched 🚚\n\nDelivery Contact: " + phoneNumber);
                                            tvOrderStatus.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                                            btnDispatch.setVisibility(View.GONE);
                                        } else {
                                            Toast.makeText(context, "Dispatch failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}
