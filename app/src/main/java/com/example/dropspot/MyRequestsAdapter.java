package com.example.dropspot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyRequestsAdapter extends RecyclerView.Adapter<MyRequestsAdapter.RequestViewHolder> {

    private final List<Request> requests;
    private final Context context;
    private final FirebaseFirestore firebaseFirestore;

    public MyRequestsAdapter(Context context, List<Request> requests) {
        this.context = context;
        this.requests = requests;
        this.firebaseFirestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        Request request = requests.get(position);
        holder.bind(request, firebaseFirestore, context);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, status, message, tvOrderStatus;
        Button btnPayment, btnConfirmDelivery;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_post_title);
            date = itemView.findViewById(R.id.tv_request_date);
            status = itemView.findViewById(R.id.tv_status_badge);
            message = itemView.findViewById(R.id.tv_request_message);
            btnPayment = itemView.findViewById(R.id.btn_proceed_payment);
            tvOrderStatus = itemView.findViewById(R.id.tv_delivery_status);
            btnConfirmDelivery = itemView.findViewById(R.id.btn_confirm_delivery);
        }

        public void bind(Request request, FirebaseFirestore firebaseFirestore, Context context) {
            title.setText(request.postTitle != null ? request.postTitle : "Unknown Item");
            
            String dateStr = request.createdAt;
            if (dateStr != null && dateStr.contains("T")) {
                dateStr = dateStr.split("T")[0];
            }
            date.setText("Requested on: " + dateStr);
            
            message.setText("Message: " + request.message);
            
            String s = request.status != null ? request.status : "pending";
            status.setText(s.toUpperCase());

            String buyerId = FirebaseAuth.getInstance().getUid();

            // Check for payment/order
            firebaseFirestore.collection("orders")
                    .whereEqualTo("postId", request.postId)
                    .whereEqualTo("buyerId", buyerId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            var document = queryDocumentSnapshots.getDocuments().get(0);
                            String orderStatus = (String) document.get("status");
                            
                            // Hide payment button, show order status
                            btnPayment.setVisibility(View.GONE);
                            tvOrderStatus.setVisibility(View.VISIBLE);
                            
                            if ("PAID".equals(orderStatus)) {
                                tvOrderStatus.setText("💰 Payment Completed ✅\n\nWaiting for seller to dispatch...");
                                tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                                btnConfirmDelivery.setVisibility(View.GONE);
                                
                            } else if ("DISPATCHED".equals(orderStatus)) {
                                String trackingNumber = (String) document.get("trackingNumber");
                                tvOrderStatus.setText("📦 Dispatched 🚚\n\nTracking: " + (trackingNumber != null ? trackingNumber : "N/A"));
                                tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark));
                                
                                // Show confirm delivery button
                                btnConfirmDelivery.setVisibility(View.VISIBLE);
                                btnConfirmDelivery.setText("Confirm Delivery ✅");
                                btnConfirmDelivery.setOnClickListener(v -> {
                                    String sellerId = (String) document.get("sellerId");
                                    String itemTitle = (String) document.get("itemTitle");
                                    String paymentId = (String) document.get("paymentId");
                                    
                                    // Mark as delivered
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("status", "DELIVERED");
                                    updates.put("deliveredAt", System.currentTimeMillis());
                                    updates.put("updatedAt", System.currentTimeMillis());
                                    
                                    firebaseFirestore.collection("orders")
                                            .document(document.getId())
                                            .update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(context, "Delivery confirmed! ✅", Toast.LENGTH_SHORT).show();
                                                
                                                // Send notification to seller
                                                DispatchTrackingHelper.sendDeliveryConfirmedNotification(
                                                        sellerId, buyerId, itemTitle, paymentId
                                                );
                                                
                                                // Refresh
                                                notifyItemChanged(getAdapterPosition());
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(context, "Failed to confirm delivery", Toast.LENGTH_SHORT).show();
                                            });
                                });
                                
                            } else if ("DELIVERED".equals(orderStatus)) {
                                tvOrderStatus.setText("✅ Delivered 📦\n\nOrder completed successfully!");
                                tvOrderStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                                btnConfirmDelivery.setVisibility(View.GONE);
                            }
                        } else {
                            // No payment yet
                            if ("accepted".equals(s)) {
                                status.getBackground().setTint(context.getResources().getColor(android.R.color.holo_green_dark));
                                btnPayment.setVisibility(View.VISIBLE);
                                btnPayment.setOnClickListener(v -> {
                                    Intent intent = new Intent(context, PaymentActivity.class);
                                    intent.putExtra("POST_ID", request.postId);
                                    intent.putExtra("POST_TITLE", request.postTitle);
                                    intent.putExtra("OWNER_ID", request.postOwnerId);
                                    intent.putExtra("AMOUNT", request.postPrice > 0 ? request.postPrice : 100.0);
                                    context.startActivity(intent);
                                });
                            } else if (s.startsWith("rejected")) {
                                status.getBackground().setTint(context.getResources().getColor(android.R.color.holo_red_dark));
                                btnPayment.setVisibility(View.GONE);
                            } else {
                                status.getBackground().setTint(context.getResources().getColor(android.R.color.holo_orange_dark));
                                btnPayment.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error
                        if ("accepted".equals(s)) {
                            status.getBackground().setTint(context.getResources().getColor(android.R.color.holo_green_dark));
                            btnPayment.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }
}
