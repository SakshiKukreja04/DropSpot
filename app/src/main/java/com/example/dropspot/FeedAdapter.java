package com.example.dropspot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Post item);
        void onItemLongClick(Post item);
        void onDeleteClick(Post item);
    }

    private final List<Post> posts;
    private final OnItemClickListener listener;
    private Post itemWithDeleteVisible;

    public FeedAdapter(List<Post> posts, OnItemClickListener listener) {
        this.posts = posts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        Post item = posts.get(position);
        holder.bind(item, listener, item == itemWithDeleteVisible);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void showDeleteButton(Post item) {
        itemWithDeleteVisible = item;
        notifyDataSetChanged();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, distance;
        ImageView image;
        ImageButton btnDelete;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvItemTitle);
            category = itemView.findViewById(R.id.tvCategory);
            distance = itemView.findViewById(R.id.tvDistance);
            image = itemView.findViewById(R.id.ivItemImage);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(final Post item, final OnItemClickListener listener, boolean showDelete) {
            title.setText(item.title);
            category.setText(item.category);
            
            // Show Price and Distance: "₹500 • 1.2 km away"
            String priceText = String.format("₹%.0f", item.price);
            String distanceText = String.format("%.1f km away", item.distance);
            distance.setText(priceText + " • " + distanceText);
            
            if (item.images != null && !item.images.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(item.images.get(0))
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(image);
            } else {
                image.setImageResource(R.drawable.ic_launcher_background);
            }
            
            itemView.setOnClickListener(v -> listener.onItemClick(item));
            itemView.setOnLongClickListener(v -> {
                listener.onItemLongClick(item);
                return true;
            });

            if (btnDelete != null) {
                btnDelete.setVisibility(showDelete ? View.VISIBLE : View.GONE);
                btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
            }
        }
    }
}
