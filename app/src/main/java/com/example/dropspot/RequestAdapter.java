package com.example.dropspot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(Request request);
        void onReject(Request request);
    }

    private List<Request> requests;
    private OnRequestActionListener listener;

    public RequestAdapter(List<Request> requests, OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(requests.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView name, message, status;
        ImageView photo;
        Button btnAccept, btnReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_requester_name);
            message = itemView.findViewById(R.id.tv_request_message);
            status = itemView.findViewById(R.id.tv_request_status);
            photo = itemView.findViewById(R.id.iv_requester_photo);
            btnAccept = itemView.findViewById(R.id.btn_accept_request);
            btnReject = itemView.findViewById(R.id.btn_reject_request);
        }

        public void bind(Request request, OnRequestActionListener listener) {
            name.setText(request.requesterName);
            message.setText(request.message);
            status.setText("Status: " + request.status);

            if (request.requesterPhoto != null && !request.requesterPhoto.isEmpty()) {
                Glide.with(itemView.getContext()).load(request.requesterPhoto).into(photo);
            }

            if ("pending".equals(request.status)) {
                btnAccept.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
                btnAccept.setOnClickListener(v -> listener.onAccept(request));
                btnReject.setOnClickListener(v -> listener.onReject(request));
            } else {
                btnAccept.setVisibility(View.GONE);
                btnReject.setVisibility(View.GONE);
            }
        }
    }
}
