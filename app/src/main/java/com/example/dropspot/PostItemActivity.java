package com.example.dropspot;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PostItemActivity extends AppCompatActivity {
    private static final String TAG = "PostItemActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);

        Button btnPostItem = findViewById(R.id.btnPostItem);
        if (btnPostItem != null) {
            btnPostItem.setOnClickListener(v -> {
                // Feature 1: Button Click Event
                Toast.makeText(PostItemActivity.this, "Opening Post Screen", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Post Item button clicked");
                // Original logic: just go back
                finish();
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}