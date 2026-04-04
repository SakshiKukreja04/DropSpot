package com.example.dropspot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class PostedItemsAdapter extends RecyclerView.Adapter<PostedItemsAdapter.PostedItemsViewHolder> {

    private final List<Post> postedItems;
    private final Context context;

    public PostedItemsAdapter(Context context, List<Post> postedItems) {
        this.context = context;
        this.postedItems = postedItems;
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
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return postedItems.size();
    }

    static class PostedItemsViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, contactDetails;
        ImageView image;
        Button acceptButton, rejectButton;
        LinearLayout ownerActions;

        public PostedItemsViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvItemTitle);
            category = itemView.findViewById(R.id.tvCategory);
            image = itemView.findViewById(R.id.ivItemImage);
            acceptButton = itemView.findViewById(R.id.btn_accept);
            rejectButton = itemView.findViewById(R.id.btn_reject);
            contactDetails = itemView.findViewById(R.id.tv_contact_details);
            ownerActions = itemView.findViewById(R.id.owner_actions_layout);
        }

        public void bind(final Post item) {
            title.setText(item.title);
            category.setText(item.category);
            
            if (item.images != null && !item.images.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(item.images.get(0))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(image);
            } else {
                image.setImageResource(R.drawable.ic_launcher_background);
            }

            // Logic for requests would go here. For now, let's keep it simple.
            // If item.requestCount > 0, show owner actions
            if (item.requestCount > 0) {
                ownerActions.setVisibility(View.VISIBLE);
            } else {
                ownerActions.setVisibility(View.GONE);
            }

            acceptButton.setOnClickListener(v -> {
                // Logic to accept first request
                ownerActions.setVisibility(View.GONE);
                contactDetails.setVisibility(View.VISIBLE);
            });

            rejectButton.setOnClickListener(v -> {
                ownerActions.setVisibility(View.GONE);
            });
        }
    }
}
