package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Button btnEditProfile = findViewById(R.id.btnEditProfile);
        Button btnViewMyPosts = findViewById(R.id.btnViewMyPosts);
        Button btnLogout = findViewById(R.id.btnLogout);

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                // Feature 1: Button Click Event
                Toast.makeText(ProfileActivity.this, "Edit Profile Clicked", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnViewMyPosts != null) {
            btnViewMyPosts.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, PostedItemsActivity.class));
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Feature 1: Button Click Event
                Toast.makeText(ProfileActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Logout clicked, redirecting to LoginActivity");
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
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