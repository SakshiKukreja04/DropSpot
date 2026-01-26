package com.example.dropspot;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostedItemsAdapter extends RecyclerView.Adapter<PostedItemsAdapter.PostedItemsViewHolder> {

    private final List<FeedItem> postedItems;
    private final SharedPreferences prefs;

    public PostedItemsAdapter(Context context, List<FeedItem> postedItems) {
        this.postedItems = postedItems;
        this.prefs = context.getSharedPreferences("RequestPrefs", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public PostedItemsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_posted, parent, false);
        return new PostedItemsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostedItemsViewHolder holder, int position) {
        FeedItem item = postedItems.get(position);
        holder.bind(item, prefs);
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

        public void bind(final FeedItem item, final SharedPreferences prefs) {
            title.setText(item.getTitle());
            category.setText(item.getCategory());
            image.setImageResource(item.getImageResource());

            String requestStateString = prefs.getString(item.getTitle(), RequestState.NOT_REQUESTED.name());
            RequestState currentRequestState = RequestState.valueOf(requestStateString);

            updateOwnerUI(currentRequestState);

            acceptButton.setOnClickListener(v -> {
                saveRequestState(item.getTitle(), RequestState.ACCEPTED, prefs);
                updateOwnerUI(RequestState.ACCEPTED);
            });

            rejectButton.setOnClickListener(v -> {
                saveRequestState(item.getTitle(), RequestState.REJECTED, prefs);
                updateOwnerUI(RequestState.REJECTED);
            });
        }

        private void updateOwnerUI(RequestState state) {
            switch (state) {
                case REQUESTED:
                    ownerActions.setVisibility(View.VISIBLE);
                    contactDetails.setVisibility(View.GONE);
                    break;
                case ACCEPTED:
                    ownerActions.setVisibility(View.GONE);
                    contactDetails.setVisibility(View.VISIBLE);
                    break;
                case REJECTED:
                case NOT_REQUESTED:
                    ownerActions.setVisibility(View.GONE);
                    contactDetails.setVisibility(View.GONE);
                    break;
            }
        }

        private void saveRequestState(String itemTitle, RequestState state, SharedPreferences prefs) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(itemTitle, state.name());
            editor.apply();
        }
    }
}
