package com.example.dropspot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ItemDetailActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private RequestState currentRequestState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        ImageView itemImage = findViewById(R.id.iv_item_image_detail);
        TextView itemTitle = findViewById(R.id.tv_item_title_detail);
        TextView itemCategory = findViewById(R.id.tv_category_detail);
        TextView itemDistance = findViewById(R.id.tv_distance_detail);
        Button requestButton = findViewById(R.id.btn_request_item);
        TextView contactDetails = findViewById(R.id.tv_contact_details);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("RequestPrefs", MODE_PRIVATE);

        Intent intent = getIntent();
        String itemTitleText = "";
        if (intent != null) {
            itemTitleText = intent.getStringExtra("ITEM_TITLE");
            itemTitle.setText(itemTitleText);
            itemCategory.setText(intent.getStringExtra("ITEM_CATEGORY"));
            itemDistance.setText(intent.getStringExtra("ITEM_DISTANCE"));
            itemImage.setImageResource(intent.getIntExtra("ITEM_IMAGE", R.drawable.ic_launcher_background));
        }

        // Load the request state from SharedPreferences
        String requestStateString = prefs.getString(itemTitleText, RequestState.NOT_REQUESTED.name());
        currentRequestState = RequestState.valueOf(requestStateString);

        updateUIBasedOnState(requestButton, contactDetails);

        final String finalItemTitleText = itemTitleText;
        requestButton.setOnClickListener(v -> {
            if (currentRequestState == RequestState.NOT_REQUESTED) {
                currentRequestState = RequestState.REQUESTED;
                saveRequestState(finalItemTitleText, currentRequestState);
                updateUIBasedOnState(requestButton, contactDetails);
                Toast.makeText(this, "Request Sent", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIBasedOnState(Button requestButton, TextView contactDetails) {
        switch (currentRequestState) {
            case NOT_REQUESTED:
                requestButton.setText("Request Item");
                requestButton.setEnabled(true);
                contactDetails.setVisibility(View.GONE);
                break;
            case REQUESTED:
                requestButton.setText("Request Sent");
                requestButton.setEnabled(false);
                contactDetails.setVisibility(View.GONE);
                break;
            case ACCEPTED:
                requestButton.setText("Request Accepted");
                requestButton.setEnabled(false);
                contactDetails.setVisibility(View.VISIBLE);
                break;
            case REJECTED:
                requestButton.setText("Request Rejected");
                requestButton.setEnabled(false);
                contactDetails.setVisibility(View.GONE);
                break;
        }
    }

    private void saveRequestState(String itemTitle, RequestState state) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(itemTitle, state.name());
        editor.apply();
    }
}
