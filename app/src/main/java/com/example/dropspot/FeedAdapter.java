package com.example.dropspot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FeedItem item);
        void onItemLongClick(FeedItem item);
        void onDeleteClick(FeedItem item);
    }

    private final List<FeedItem> feedItems;
    private final OnItemClickListener listener;
    private final Set<FeedItem> itemsWithDeleteVisible = new HashSet<>();

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
        holder.bind(item, listener, itemsWithDeleteVisible.contains(item));
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    public void removeItem(FeedItem item) {
        int position = feedItems.indexOf(item);
        if (position != -1) {
            feedItems.remove(position);
            itemsWithDeleteVisible.remove(item);
            notifyItemRemoved(position);
        }
    }

    public void showDeleteButton(FeedItem item) {
        itemsWithDeleteVisible.add(item);
        int position = feedItems.indexOf(item);
        if (position != -1) {
            notifyItemChanged(position);
        }
    }

    class FeedViewHolder extends RecyclerView.ViewHolder {
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

        public void bind(final FeedItem item, final OnItemClickListener listener, boolean showDelete) {
            title.setText(item.getTitle());
            category.setText(item.getCategory());
            distance.setText(item.getDistance());
            image.setImageResource(item.getImageResource());
            
            btnDelete.setVisibility(showDelete ? View.VISIBLE : View.GONE);
            btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));

            itemView.setOnClickListener(v -> {
                if (itemsWithDeleteVisible.contains(item)) {
                    itemsWithDeleteVisible.remove(item);
                    notifyItemChanged(getAdapterPosition());
                } else {
                    listener.onItemClick(item);
                }
            });

            itemView.setOnLongClickListener(v -> {
                listener.onItemLongClick(item);
                return true;
            });
        }
    }
}
