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
            
            // Check for orders/payments
            firebaseFirestore.collection("orders")
                    .whereEqualTo("postId", item.id)
                    .whereEqualTo("sellerId", currentUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Order exists
                            var document = queryDocumentSnapshots.getDocuments().get(0);
                            String orderStatus = (String) document.get("status");
                            
                            if (tvOrderStatus != null) {
                                tvOrderStatus.setVisibility(View.VISIBLE);
                                
                                if ("PAID".equals(orderStatus)) {
                                    tvOrderStatus.setText("💰 Payment Received!");
                                    tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                                    
                                    // Show dispatch button
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
                                    String trackingNumber = (String) document.get("trackingNumber");
                                    tvOrderStatus.setText("📦 Dispatched 🚚\n\nTracking: " + (trackingNumber != null ? trackingNumber : "N/A"));
                                    tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark));
                                    
                                    if (btnDispatch != null) {
                                        btnDispatch.setVisibility(View.GONE);
                                    }
                                } else if ("DELIVERED".equals(orderStatus)) {
                                    tvOrderStatus.setText("✅ Delivered 📦\n\nOrder completed successfully!");
                                    tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                                    
                                    if (btnDispatch != null) {
                                        btnDispatch.setVisibility(View.GONE);
                                    }
                                }
                            }
                        } else {
                            // No order
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
            
            if (item.images != null && !item.images.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(item.images.get(0))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(image);
            } else {
                image.setImageResource(R.drawable.ic_launcher_background);
            }
        }
        
        private void showDispatchDialog(String orderId, String postId, String buyerId, String sellerId, String itemTitle, String paymentId, FirebaseFirestore firebaseFirestore, Context context, View itemView) {
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
                        
                        // Update order status
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
                                    
                                    // Send notification to buyer
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

