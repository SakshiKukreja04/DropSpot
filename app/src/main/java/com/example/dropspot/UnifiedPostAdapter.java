package com.example.dropspot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UnifiedPostAdapter extends RecyclerView.Adapter<UnifiedPostAdapter.PostViewHolder> {
    private static final String TAG = "UnifiedPostAdapter";
    private final List<Post> posts;
    private final Context context;
    private final FirebaseFirestore firebaseFirestore;
    private final String currentUserId;
    private OnPostClickListener onPostClickListener;

    public UnifiedPostAdapter(Context context, List<Post> posts, FirebaseFirestore firebaseFirestore, String currentUserId) {
        this.context = context;
        this.posts = posts;
        this.firebaseFirestore = firebaseFirestore;
        this.currentUserId = currentUserId;
    }

    public interface OnPostClickListener {
        void onPostClicked(Post post);
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.onPostClickListener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_posted, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(posts.get(position), firebaseFirestore, currentUserId, context, onPostClickListener);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, status, tvOrderStatus;
        ImageView image;
        Button btnDispatch;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvItemTitle);
            category = itemView.findViewById(R.id.tvCategory);
            image = itemView.findViewById(R.id.ivItemImage);
            status = itemView.findViewById(R.id.tv_item_status);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            btnDispatch = itemView.findViewById(R.id.btn_dispatch);
        }

        public void bind(final Post item, FirebaseFirestore firebaseFirestore, String currentUserId, Context context, OnPostClickListener listener) {
            title.setText(item.title);
            category.setText(item.category);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPostClicked(item);
                else {
                    Intent intent = new Intent(context, ItemDetailActivity.class);
                    intent.putExtra("POST_ID", item.id);
                    context.startActivity(intent);
                }
            });

            if (item.images != null && !item.images.isEmpty()) {
                Glide.with(itemView.getContext()).load(item.images.get(0)).placeholder(R.drawable.ic_launcher_background).into(image);
            }

            // Reset UI state for recycled views
            if (btnDispatch != null) btnDispatch.setVisibility(View.GONE);
            if (tvOrderStatus != null) tvOrderStatus.setVisibility(View.GONE);
            if (status != null) {
                status.setVisibility(View.VISIBLE);
                status.setText(item.isActive ? "Active" : "Closed");
                status.setTextColor(ContextCompat.getColor(context, item.isActive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // DEBUG LOG
            Log.d("STATUS_CHECK", "===============================================");
            Log.d("STATUS_CHECK", "Binding Post: " + item.title);
            Log.d("STATUS_CHECK", "Post ID: " + item.id);
            Log.d("STATUS_CHECK", "Post Owner ID: " + item.userId);
            Log.d("STATUS_CHECK", "Current User ID: " + currentUserId);
            Log.d("STATUS_CHECK", "Is Owner? " + (item.userId != null && item.userId.equals(currentUserId)));
            Log.d("STATUS_CHECK", "===============================================");

            // Only show dispatch button if CURRENT USER is the POST OWNER
            if (item.userId == null || !item.userId.equals(currentUserId)) {
                Log.d("STATUS_CHECK", "Skipping dispatch check - current user is NOT the post owner");
                return;
            }

            // Sync with Payments collection for status
            firebaseFirestore.collection("payments")
                    .whereEqualTo("postId", item.id)
                    .whereEqualTo("ownerId", currentUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Log.d("STATUS_CHECK", "Payment query returned " + queryDocumentSnapshots.size() + " documents");
                        
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d("STATUS_CHECK", "Post: " + item.title + " - Found " + queryDocumentSnapshots.size() + " payments");
                            
                            DocumentSnapshot paymentDoc = queryDocumentSnapshots.getDocuments().get(0);
                            String paymentStatus = paymentDoc.getString("status");
                            String buyerId = paymentDoc.getString("requesterId");
                            final String paymentId = paymentDoc.getString("paymentId");
                            
                            // Normalize status to lowercase for comparison
                            String normalizedStatus = paymentStatus != null ? paymentStatus.toLowerCase().trim() : "";
                            
                            Log.d("STATUS_CHECK", "Post: " + item.title + " - Payment Status: [" + paymentStatus + "]");
                            Log.d("STATUS_CHECK", "BuyerId: " + buyerId + ", PaymentId: " + paymentId);
                            Log.d("STATUS_CHECK", "Normalized Status: [" + normalizedStatus + "]");
                            
                            tvOrderStatus.setVisibility(View.VISIBLE);
                            if (status != null) status.setVisibility(View.GONE);

                            // UI Logic based on payment status
                            if ("paid".equals(normalizedStatus)) {
                                // SHOW DISPATCH BUTTON - Seller can now dispatch
                                Log.d("STATUS_CHECK", "✅ PAID - Showing Dispatch button for: " + item.title);
                                tvOrderStatus.setText("💰 Paid\nReady to dispatch");
                                tvOrderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                                
                                if (btnDispatch != null) {
                                    btnDispatch.setVisibility(View.VISIBLE);
                                    btnDispatch.setText("Dispatch Order 🚚");
                                    btnDispatch.setOnClickListener(v -> showDispatchDialog(item.id, paymentId, buyerId, currentUserId, item.title, context));
                                }
                            } else if ("dispatched".equals(normalizedStatus)) {
                                // HIDE DISPATCH BUTTON - Already dispatched
                                Log.d("STATUS_CHECK", "✅ DISPATCHED - Hiding Dispatch button for: " + item.title);
                                String shipper = paymentDoc.getString("shipperName");
                                String contact = paymentDoc.getString("trackingNumber");
                                tvOrderStatus.setText("📦 Order Dispatched!\nShipper: " + (shipper != null ? shipper : "N/A") + "\nTracking: " + (contact != null ? contact : "N/A"));
                                tvOrderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
                                if (btnDispatch != null) btnDispatch.setVisibility(View.GONE);
                            } else if ("delivered".equals(normalizedStatus)) {
                                // HIDE DISPATCH BUTTON - Already delivered
                                Log.d("STATUS_CHECK", "✅ DELIVERED - Hiding Dispatch button for: " + item.title);
                                tvOrderStatus.setText("✅ Delivery Confirmed!\nOrder Completed Successfully.");
                                tvOrderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                                if (btnDispatch != null) btnDispatch.setVisibility(View.GONE);
                            } else {
                                Log.d("STATUS_CHECK", "❌ Unknown status: [" + normalizedStatus + "]");
                                if (btnDispatch != null) btnDispatch.setVisibility(View.GONE);
                            }
                        } else {
                            // No payment - check requests for accepted status
                            Log.d("STATUS_CHECK", "No payments found for post: " + item.title + " - Checking requests...");
                            firebaseFirestore.collection("requests")
                                    .whereEqualTo("postId", item.id)
                                    .whereEqualTo("postOwnerId", currentUserId)
                                    .whereEqualTo("status", "accepted")
                                    .get()
                                    .addOnSuccessListener(requestSnapshots -> {
                                        if (!requestSnapshots.isEmpty()) {
                                            Log.d("STATUS_CHECK", "Found " + requestSnapshots.size() + " requests for post: " + item.title);
                                            
                                            boolean foundAccepted = false;
                                            for (DocumentSnapshot doc : requestSnapshots.getDocuments()) {
                                                String reqStatus = doc.getString("status");
                                                Log.d("STATUS_CHECK", "Request status: [" + reqStatus + "]");
                                                if ("accepted".equalsIgnoreCase(reqStatus)) {
                                                    foundAccepted = true;
                                                    break;
                                                }
                                            }
                                            
                                            if (foundAccepted) {
                                                Log.d("STATUS_CHECK", "✅ ACCEPTED - Showing waiting for payment");
                                                if (status != null) status.setVisibility(View.GONE);
                                                tvOrderStatus.setVisibility(View.VISIBLE);
                                                tvOrderStatus.setText("⏳ Waiting for Buyer Payment...");
                                                tvOrderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                                                if (btnDispatch != null) btnDispatch.setVisibility(View.GONE);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching requests for post: " + item.id, e));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching payments for post: " + item.id, e);
                        Log.e("STATUS_CHECK", "❌ Error: " + e.getMessage());
                    });
        };

        void showDispatchDialog(String requestId, String paymentId, String buyerId, String sellerId, String itemTitle, Context context) {
            // Create a container for the form fields
            android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 40);
            
            final EditText etShipperName = new EditText(context);
            etShipperName.setHint("Enter shipper name");
            etShipperName.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            etShipperName.setPadding(20, 20, 20, 20);
            layout.addView(etShipperName);
            
            final EditText etPhone = new EditText(context);
            etPhone.setHint("Enter phone number/tracking number");
            etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            etPhone.setPadding(20, 20, 20, 20);
            layout.addView(etPhone);
            
            new AlertDialog.Builder(context)
                    .setTitle("Dispatch Order")
                    .setMessage("Provide shipper details and contact number")
                    .setView(layout)
                    .setPositiveButton("Confirm Dispatch", (dialog, which) -> {
                        String shipperName = etShipperName.getText().toString().trim();
                        String phone = etPhone.getText().toString().trim();
                        
                        if (shipperName.isEmpty()) {
                            Toast.makeText(context, "Shipper name is required", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (phone.isEmpty()) {
                            Toast.makeText(context, "Phone number is required", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        ApiService apiService = ApiClient.getClient().create(ApiService.class);
                        // paymentId is critical for markOrderDispatched. fallback to requestId if missing.
                        String pId = (paymentId != null && !paymentId.isEmpty()) ? paymentId : requestId;
                        
                        // Create dispatch request with shipper details
                        apiService.markOrderDispatched(new ApiService.DispatchRequest(
                                pId, buyerId, sellerId, itemTitle, phone, shipperName
                        )).enqueue(new Callback<ApiResponse<Object>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(context, "Order dispatched! 🚀\nShipper: " + shipperName, Toast.LENGTH_SHORT).show();
                                    // FORCE DATA REFRESH
                                    if (context instanceof Activity) {
                                        ((Activity)context).recreate();
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to update dispatch status.", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}
