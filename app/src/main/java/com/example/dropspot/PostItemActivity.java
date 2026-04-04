package com.example.dropspot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostItemActivity extends AppCompatActivity {
    private static final String TAG = "PostItemActivity";
    private TextInputEditText etTitle, etCategory, etDescription;
    private ApiService apiService;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();
    private ImageView ivPostImage;
    private ProgressBar postProgressBar;
    private MaterialButton btnPostItem;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedImageUris.clear(); // For now, only one image
                        selectedImageUris.add(imageUri);
                        ivPostImage.setImageURI(imageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);

        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etTitle = findViewById(R.id.etPostTitle);
        etCategory = findViewById(R.id.etPostCategory);
        etDescription = findViewById(R.id.etPostDescription);
        ivPostImage = findViewById(R.id.iv_post_image);
        postProgressBar = findViewById(R.id.post_progress_bar);
        btnPostItem = findViewById(R.id.btnPostItem);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        MaterialButton btnUploadImage = findViewById(R.id.btn_upload_image);
        if (btnUploadImage != null) {
            btnUploadImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(intent);
            });
        }

        if (btnPostItem != null) {
            btnPostItem.setOnClickListener(v -> {
                validateAndFetchLocation();
            });
        }

        setupBottomNavigation();
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

    private void validateAndFetchLocation() {
        String title = etTitle.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty() || category.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            requestLocationPermission();
            return;
        }

        postProgressBar.setVisibility(View.VISIBLE);
        btnPostItem.setEnabled(false);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                uploadImagesAndPost();
            } else {
                postProgressBar.setVisibility(View.GONE);
                btnPostItem.setEnabled(true);
                Toast.makeText(this, "Could not fetch location. Ensure GPS is ON.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            postProgressBar.setVisibility(View.GONE);
            btnPostItem.setEnabled(true);
            Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadImagesAndPost() {
        if (selectedImageUris.isEmpty()) {
            createPost();
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("post_images/" + UUID.randomUUID().toString());
        Uri imageUri = selectedImageUris.get(0);

        storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                uploadedImageUrls.clear();
                uploadedImageUrls.add(uri.toString());
                createPost();
            });
        }).addOnFailureListener(e -> {
            postProgressBar.setVisibility(View.GONE);
            btnPostItem.setEnabled(true);
            Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void createPost() {
        Post post = new Post();
        post.title = etTitle.getText().toString().trim();
        post.category = etCategory.getText().toString().trim();
        post.description = etDescription.getText().toString().trim();
        post.condition = "Good"; 
        post.isActive = true;
        post.latitude = currentLat;
        post.longitude = currentLng;
        post.images = uploadedImageUrls;

        apiService.createPost(post).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Post>> call, @NonNull Response<ApiResponse<Post>> response) {
                postProgressBar.setVisibility(View.GONE);
                btnPostItem.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(PostItemActivity.this, "Item Posted Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PostItemActivity.this, "Failed to post item", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Post>> call, @NonNull Throwable t) {
                postProgressBar.setVisibility(View.GONE);
                btnPostItem.setEnabled(true);
                Toast.makeText(PostItemActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_saved) {
                    startActivity(new Intent(this, PostedItemsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_announcements) {
                    startActivity(new Intent(this, AnnouncementsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }
}
