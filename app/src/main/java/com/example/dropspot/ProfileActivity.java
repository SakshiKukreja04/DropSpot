package com.example.dropspot;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private ImageView ivProfileImage;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private static final String TAG = "ProfileActivity";
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        ivProfileImage = findViewById(R.id.ivProfileImage);
        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Load user data
        tvProfileName.setText(sessionManager.getUserName());
        tvProfileEmail.setText(sessionManager.getUserEmail());

        String photoUrl = sessionManager.getUserPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().into(ivProfileImage);
        } else {
            // Use a default placeholder if no image is available
            Glide.with(this).load(R.drawable.ic_launcher_background).circleCrop().into(ivProfileImage);
        }

        // Initialize Image Picker
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivProfileImage.setImageURI(uri);
                        // In a real app, you would upload this URI to Firebase Storage
                        // and update the user's profile URL in Firebase Auth and SessionManager.
                        Toast.makeText(this, "Profile image updated (locally)", Toast.LENGTH_SHORT).show();
                    }
                });

        ivProfileImage.setOnClickListener(v -> {
            // Allow user to pick a new profile image
            imagePickerLauncher.launch("image/*");
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        String userId = FirebaseAuth.getInstance().getUid();
        
        // Delete FCM token on backend
        if (userId != null && !userId.isEmpty()) {
            java.util.Map<String, Object> update = new java.util.HashMap<>();
            update.put("fcmToken", null);
            
            apiService.updateUserProfile(userId, update).enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                    Log.d(TAG, "FCM token cleared on backend");
                }

                @Override
                public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                    Log.e(TAG, "Failed to clear FCM token on backend", t);
                }
            });
        }
        
        // Delete local FCM token
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "FCM token deleted locally");
            } else {
                Log.e(TAG, "Failed to delete FCM token locally");
            }
        });
        
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            sessionManager.logout();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });
    }
}
