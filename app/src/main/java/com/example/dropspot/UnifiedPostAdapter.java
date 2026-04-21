package com.example.dropspot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UnifiedPostAdapter - Single adapter for all Post displays
 * 
 * BUG 2 FIX: Replaces separate PostedItemsAdapter and ProfilePostAdapter with one unified adapter
 * Supports both Activity and Fragment contexts
 * Handles post details, order status, and dispatch functionality
 */
public class UnifiedPostAdapter extends RecyclerView.Adapter<UnifiedPostAdapter.PostViewHolder> {

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
        Post post = posts.get(position);
        holder.bind(post, firebaseFirestore, currentUserId, context, onPostClickListener);
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

        public void bind(final Post item, FirebaseFirestore firebaseFirestore, String currentUserId, Context context, UnifiedPostAdapter.OnPostClickListener listener) {
            title.setText(item.title);
            category.setText(item.category);
            
            // Set up click listener for item details
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClicked(item);
                } else {
                    // Default behavior: open ItemDetailActivity
                    Intent intent = new Intent(context, ItemDetailActivity.class);
                    intent.putExtra("POST_ID", item.id);
                    context.startActivity(intent);
                }
            });

            // Load image
            if (item.images != null && !item.images.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(item.images.get(0))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(image);
            } else {
                image.setImageResource(R.drawable.ic_launcher_background);
            }

            // Check for orders/payments to show order status
            firebaseFirestore.collection("orders")
                    .whereEqualTo("postId", item.id)
                    .whereEqualTo("sellerId", currentUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Order exists - show order status
                            var document = queryDocumentSnapshots.getDocuments().get(0);
                            String orderStatus = (String) document.get("status");
                            
                            if (tvOrderStatus != null) {
                                tvOrderStatus.setVisibility(View.VISIBLE);
                                
                                if ("PAID".equals(orderStatus)) {
                                    // Payment received - show dispatch button
                                    tvOrderStatus.setText("💰 Payment Received!");
                                    tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                                    
                                    if (btnDispatch != null) {
                                        btnDispatch.setVisibility(View.VISIBLE);
                                        btnDispatch.setText("Dispatch 🚚");
                                        btnDispatch.setOnClickListener(v -> {
                                            String buyerId = (String) document.get("buyerId");
                                            String paymentId = (String) document.get("paymentId");
                                            showDispatchDialog(document.getId(), item.id, buyerId, currentUserId, item.title, paymentId, firebaseFirestore, context, itemView);
                                        });
                                    }
                                } else if ("DISPATCHED".equals(orderStatus)) {
                                    // Order dispatched - show tracking
                                    String trackingNumber = (String) document.get("trackingNumber");
                                    tvOrderStatus.setText("📦 Dispatched 🚚\n\nTracking: " + (trackingNumber != null ? trackingNumber : "N/A"));
                                    tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark));
                                    
                                    if (btnDispatch != null) {
                                        btnDispatch.setVisibility(View.GONE);
                                    }
                                } else if ("DELIVERED".equals(orderStatus)) {
                                    // Delivery complete
                                    tvOrderStatus.setText("✅ Delivered 📦\n\nOrder completed successfully!");
                                    tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                                    
                                    if (btnDispatch != null) {
                                        btnDispatch.setVisibility(View.GONE);
                                    }
                                }
                            }
                        } else {
                            // No order - show post status
                            if (status != null) {
                                if (item.isActive) {
                                    status.setText("Active");
                                    status.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                                } else {
                                    status.setText("Closed");
                                    status.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error - show default status
                        if (status != null) {
                            if (item.isActive) {
                                status.setText("Active");
                                status.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                            } else {
                                status.setText("Closed");
                                status.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                            }
                        }
                    });
        }

        /**
         * Show dispatch dialog to enter tracking number
         * Called when seller marks item as dispatched
         */
        private void showDispatchDialog(String orderId, String postId, String buyerId, String sellerId, String itemTitle, String paymentId,
                                       FirebaseFirestore firebaseFirestore, Context context, View itemView) {
            EditText etTracking = new EditText(context);
            etTracking.setHint("Enter tracking number");
            
            new AlertDialog.Builder(context)
                    .setTitle("Dispatch Order")
                    .setMessage("Enter tracking number for dispatch")
                    .setView(etTracking)
                    .setPositiveButton("Dispatch", (dialog, which) -> {
                        String trackingNumber = etTracking.getText().toString().trim();
                        if (trackingNumber.isEmpty()) {
                            Toast.makeText(context, "Please enter tracking number", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Update order status to DISPATCHED
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "DISPATCHED");
                        updates.put("trackingNumber", trackingNumber);
                        updates.put("dispatchedAt", System.currentTimeMillis());
                        updates.put("updatedAt", System.currentTimeMillis());
                        
                        firebaseFirestore.collection("orders")
                                .document(orderId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Order dispatched!", Toast.LENGTH_SHORT).show();
                                    
                                    // Send notification to buyer (NOT seller)
                                    // BUG 3 FIX: Ensure notification goes to buyerId only
                                    DispatchTrackingHelper.sendDispatchNotification(
                                            buyerId, sellerId, itemTitle, trackingNumber, paymentId
                                    );
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to dispatch", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}

