package com.example.dropspot;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PostItemActivity extends AppCompatActivity {
    private static final String TAG = "PostItemActivity";
    
    // Cloudinary Configuration
    private static final String CLOUDINARY_CLOUD_NAME = "dlyngtijw"; 
    private static final String CLOUDINARY_UPLOAD_PRESET = "dropspot_preset";
    
    private TextInputEditText etTitle, etCategory, etDescription, etPrice;
    private TextView tvLocationStatus;
    private ApiService apiService;
    private CloudinaryService cloudinaryService;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0;
    private boolean isLocationFetched = false;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();
    private ImageView ivPostImage;
    private TextView tvImageCount;
    private ProgressBar postProgressBar;
    private MaterialButton btnPostItem;
    private MaterialButton btnSelectPhoto;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUris.clear();
                    if (result.getData().getClipData() != null) {
                        ClipData clipData = result.getData().getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            selectedImageUris.add(clipData.getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        selectedImageUris.add(result.getData().getData());
                    }
                    
                    updateImagePreview();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);

        apiService = ApiClient.getClient().create(ApiService.class);
        
        // Initialize Cloudinary Retrofit
        Retrofit cloudinaryRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.cloudinary.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        cloudinaryService = cloudinaryRetrofit.create(CloudinaryService.class);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etTitle = findViewById(R.id.etPostTitle);
        etCategory = findViewById(R.id.etPostCategory);
        etDescription = findViewById(R.id.etPostDescription);
        etPrice = findViewById(R.id.etPostPrice);
        ivPostImage = findViewById(R.id.iv_post_image);
        tvImageCount = new TextView(this); // Placeholder for count UI
        postProgressBar = findViewById(R.id.post_progress_bar);
        btnPostItem = findViewById(R.id.btnPostItem);
        tvLocationStatus = findViewById(R.id.tv_location_status);
        MaterialButton btnGetLocation = findViewById(R.id.btn_get_location);
        btnSelectPhoto = findViewById(R.id.btn_upload_image);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        btnSelectPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImageLauncher.launch(intent);
        });

        if (btnGetLocation != null) {
            btnGetLocation.setOnClickListener(v -> fetchLocation());
        }

        if (btnPostItem != null) {
            btnPostItem.setOnClickListener(v -> validateAndSubmit());
        }

        setupBottomNavigation();
    }

    private void updateImagePreview() {
        if (!selectedImageUris.isEmpty()) {
            Glide.with(this).load(selectedImageUris.get(0)).into(ivPostImage);
            btnSelectPhoto.setText("Selected " + selectedImageUris.size() + " Photos");
        } else {
            ivPostImage.setImageResource(android.R.drawable.ic_menu_gallery);
            btnSelectPhoto.setText("Select Photo");
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        tvLocationStatus.setText("Fetching location...");
        tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                isLocationFetched = true;
                
                String locationText = String.format(Locale.getDefault(), "Location: %.5f, %.5f", currentLat, currentLng);
                tvLocationStatus.setText(locationText);
                tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                btnPostItem.setEnabled(true);
            } else {
                tvLocationStatus.setText("Could not fetch location. Try again.");
                tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        });
    }

    private void validateAndSubmit() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (title.isEmpty() || category.isEmpty() || description.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 10) {
                Toast.makeText(this, "Price must be greater than ₹10", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isLocationFetched) {
            Toast.makeText(this, "Please fetch location first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        postProgressBar.setVisibility(View.VISIBLE);
        btnPostItem.setEnabled(false);
        uploadImagesToCloudinary(price);
    }

    private void uploadImagesToCloudinary(double price) {
        uploadedImageUrls.clear();
        AtomicInteger uploadCount = new AtomicInteger(0);
        int totalImages = selectedImageUris.size();

        for (Uri uri : selectedImageUris) {
            File file = getFileFromUri(uri);
            if (file == null) {
                handleUploadError("Failed to process image file");
                return;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(uri)), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            RequestBody preset = RequestBody.create(MediaType.parse("text/plain"), CLOUDINARY_UPLOAD_PRESET);

            cloudinaryService.uploadImage(CLOUDINARY_CLOUD_NAME, body, preset).enqueue(new Callback<CloudinaryResponse>() {
                @Override
                public void onResponse(Call<CloudinaryResponse> call, Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        uploadedImageUrls.add(response.body().secureUrl);
                        if (uploadCount.incrementAndGet() == totalImages) {
                            createPost(price);
                        }
                    } else {
                        handleUploadError("Cloudinary upload failed: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<CloudinaryResponse> call, Throwable t) {
                    handleUploadError("Cloudinary network error: " + t.getMessage());
                }
            });
        }
    }

    private void handleUploadError(String message) {
        runOnUiThread(() -> {
            postProgressBar.setVisibility(View.GONE);
            btnPostItem.setEnabled(true);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.e(TAG, message);
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File tempFile = new File(getCacheDir(), "temp_image_" + UUID.randomUUID().toString());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file from URI", e);
            return null;
        }
    }

    private void createPost(double price) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Post post = new Post();
        post.userId = currentUser.getUid();
        post.title = etTitle.getText().toString().trim();
        post.category = etCategory.getText().toString().trim();
        post.description = etDescription.getText().toString().trim();
        post.price = price;
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
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Post> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(PostItemActivity.this, "Item Posted Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(PostItemActivity.this, "Failed: " + apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PostItemActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
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
                }
                return false;
            });
        }
    }
}
