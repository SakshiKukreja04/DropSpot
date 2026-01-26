package com.example.dropspot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FeedItem item);
    }

    private final List<FeedItem> feedItems;
    private final OnItemClickListener listener;

    public FeedAdapter(List<FeedItem> feedItems, OnItemClickListener listener) {
        this.feedItems = feedItems;
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
        FeedItem item = feedItems.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, distance;
        ImageView image;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvItemTitle);
            category = itemView.findViewById(R.id.tvCategory);
            distance = itemView.findViewById(R.id.tvDistance);
            image = itemView.findViewById(R.id.ivItemImage);
        }

        public void bind(final FeedItem item, final OnItemClickListener listener) {
            title.setText(item.getTitle());
            category.setText(item.getCategory());
            distance.setText(item.getDistance());
            image.setImageResource(item.getImageResource());
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
