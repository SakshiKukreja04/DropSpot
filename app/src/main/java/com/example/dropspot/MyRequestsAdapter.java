package com.example.dropspot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyRequestsAdapter extends RecyclerView.Adapter<MyRequestsAdapter.RequestViewHolder> {

    private static final String TAG = "MyRequestsAdapter";
    private final List<Request> requests;
    private final Context context;
    private Runnable onRefresh;

    public MyRequestsAdapter(Context context, List<Request> requests) {
        this.context = context;
        this.requests = requests;
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(requests.get(position));
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
            
            btnPayment.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    Request request = requests.get(pos);
                    Log.d(TAG, "Proceeding to payment for request: " + request.getEffectiveId());
                    Intent intent = new Intent(context, PaymentActivity.class);
                    intent.putExtra("POST_ID", request.postId);
                    intent.putExtra("POST_TITLE", request.postTitle);
                    intent.putExtra("OWNER_ID", request.postOwnerId);
                    double finalAmount = request.postPrice > 0 ? request.postPrice : 100.0;
                    intent.putExtra("AMOUNT", finalAmount);
                    if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });

            btnConfirmDelivery.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    confirmDelivery(requests.get(pos));
                }
            });
        }

        public void bind(final Request request) {
            // DEBUG: Log all statuses
            Log.d("STATUS_CHECK", request.status != null ? request.status : "null");
            
            title.setText(request.postTitle != null ? request.postTitle : "Unknown Item");
            date.setText("Requested on: " + (request.createdAt != null ? request.createdAt.split("T")[0] : "N/A"));
            message.setText("Message: " + request.message);
            
            String currentStatus = request.status != null ? request.status.trim().toLowerCase() : "pending";
            status.setText(currentStatus.toUpperCase());

            // Reset UI
            btnPayment.setVisibility(View.GONE);
            tvOrderStatus.setVisibility(View.GONE);
            btnConfirmDelivery.setVisibility(View.GONE);
            
            // Status logic
            if ("accepted".equals(currentStatus)) {
                status.getBackground().setTint(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                btnPayment.setVisibility(View.VISIBLE);
                Log.d(TAG, "ACCEPTED status found -> Proceed to Payment visible");
            } else if ("paid".equals(currentStatus)) {
                status.getBackground().setTint(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                tvOrderStatus.setVisibility(View.VISIBLE);
                tvOrderStatus.setText("💰 Paid. Waiting for seller to dispatch order...");
            } else if ("dispatched".equals(currentStatus)) {
                status.getBackground().setTint(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
                tvOrderStatus.setVisibility(View.VISIBLE);
                // Show tracking number and shipper details
                String trackingInfo = (request.trackingNumber != null ? request.trackingNumber : "N/A");
                String shipperInfo = (request.shipperName != null ? request.shipperName : "Delivery Partner");
                tvOrderStatus.setText("📦 Order Dispatched!\n\nShipper: " + shipperInfo + "\nDelivery Contact: " + trackingInfo);
                btnConfirmDelivery.setVisibility(View.VISIBLE);
            } else if ("completed".equals(currentStatus)) {
                status.getBackground().setTint(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                tvOrderStatus.setVisibility(View.VISIBLE);
                tvOrderStatus.setText("✅ Order Completed Successfully!");
            } else if ("rejected".equals(currentStatus)) {
                status.getBackground().setTint(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            } else {
                status.getBackground().setTint(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
            }
        }

        private void confirmDelivery(Request request) {
            btnConfirmDelivery.setEnabled(false);
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            
            // Use paymentId from the Request object which is populated after payment
            String paymentIdToUse = (request.paymentId != null && !request.paymentId.isEmpty()) 
                    ? request.paymentId : request.getEffectiveId();
            
            apiService.markOrderDelivered(new ApiService.DeliveryRequest(
                    paymentIdToUse, request.requesterId, request.postOwnerId, request.postTitle))
                    .enqueue(new Callback<ApiResponse<Object>>() {
                        @Override 
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(context, "Delivery confirmed! ✅ Order Completed.", Toast.LENGTH_SHORT).show();
                                request.status = "completed";
                                notifyItemChanged(getAdapterPosition());
                                if (onRefresh != null) onRefresh.run();
                            } else {
                                btnConfirmDelivery.setEnabled(true);
                                Toast.makeText(context, "Confirmation failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            btnConfirmDelivery.setEnabled(true);
                            Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
