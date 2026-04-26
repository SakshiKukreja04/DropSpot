package com.example.dropspot;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

    private File cameraImageFile;
    
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cameraImageFile != null && cameraImageFile.exists()) {
                        Uri photoUri = Uri.fromFile(cameraImageFile);
                        selectedImageUris.add(photoUri);
                        Log.d(TAG, "📸 Camera photo saved: " + cameraImageFile.getAbsolutePath());
                        updateImagePreview();
                        Toast.makeText(PostItemActivity.this, "Photo captured successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "❌ Camera image file does not exist");
                        Toast.makeText(PostItemActivity.this, "Failed to save camera image", Toast.LENGTH_SHORT).show();
                    }
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

        btnSelectPhoto.setOnClickListener(v -> showPhotoSourceDialog());

        if (btnGetLocation != null) {
            btnGetLocation.setOnClickListener(v -> fetchLocation());
        }

        if (btnPostItem != null) {
            btnPostItem.setOnClickListener(v -> validateAndSubmit());
        }
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
        
        Log.d(TAG, "🔄 Starting upload of " + totalImages + " images to Cloudinary");

        for (Uri uri : selectedImageUris) {
            Log.d(TAG, "📸 Processing image URI: " + uri);
            File file = getFileFromUri(uri);
            if (file == null) {
                Log.e(TAG, "❌ Failed to get file from URI: " + uri);
                handleUploadError("Failed to process image file from: " + uri);
                return;
            }
            
            if (!file.exists()) {
                Log.e(TAG, "❌ File does not exist: " + file.getAbsolutePath());
                handleUploadError("Image file not found: " + file.getAbsolutePath());
                return;
            }
            
            Log.d(TAG, "✅ File found: " + file.getAbsolutePath() + " (Size: " + file.length() + " bytes)");

            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) {
                mimeType = "image/jpeg";
                Log.d(TAG, "⚠️  MIME type unknown, using default: " + mimeType);
            } else {
                Log.d(TAG, "📋 MIME type: " + mimeType);
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            RequestBody preset = RequestBody.create(MediaType.parse("text/plain"), CLOUDINARY_UPLOAD_PRESET);
            
            Log.d(TAG, "🚀 Uploading to Cloudinary: " + file.getName());

            cloudinaryService.uploadImage(CLOUDINARY_CLOUD_NAME, body, preset).enqueue(new Callback<CloudinaryResponse>() {
                @Override
                public void onResponse(Call<CloudinaryResponse> call, Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imageUrl = response.body().secureUrl;
                        Log.d(TAG, "✅ Image uploaded successfully: " + imageUrl);
                        uploadedImageUrls.add(imageUrl);
                        if (uploadCount.incrementAndGet() == totalImages) {
                            Log.d(TAG, "✅ All " + totalImages + " images uploaded, creating post...");
                            createPost(price);
                        }
                    } else {
                        String errorMsg = "Cloudinary upload failed: " + response.code() + " " + response.message();
                        Log.e(TAG, errorMsg);
                        handleUploadError(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<CloudinaryResponse> call, Throwable t) {
                    String errorMsg = "Cloudinary network error: " + t.getMessage();
                    Log.e(TAG, errorMsg, t);
                    handleUploadError(errorMsg);
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
            Log.d(TAG, "🔍 Converting URI to file: " + uri);
            Log.d(TAG, "   URI Scheme: " + uri.getScheme());
            Log.d(TAG, "   URI Path: " + uri.getPath());
            
            // Handle file:// URIs directly
            if ("file".equals(uri.getScheme())) {
                String path = uri.getPath();
                Log.d(TAG, "   File path detected: " + path);
                
                File sourceFile = new File(path);
                if (sourceFile.exists()) {
                    Log.d(TAG, "✅ File exists at: " + sourceFile.getAbsolutePath() + " (" + sourceFile.length() + " bytes)");
                    return sourceFile;
                } else {
                    Log.e(TAG, "❌ File does not exist at: " + sourceFile.getAbsolutePath());
                }
            }
            
            // Handle content:// URIs and others through content resolver
            Log.d(TAG, "   Opening input stream for content URI: " + uri);
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "❌ Could not open input stream for URI: " + uri);
                return null;
            }
            
            File tempFile = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[4096]; // Larger buffer for efficiency
            int length;
            long totalBytes = 0;
            
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalBytes += length;
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            Log.d(TAG, "✅ Temp file created: " + tempFile.getAbsolutePath() + " (" + totalBytes + " bytes)");
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "❌ Error creating temp file from URI: " + uri, e);
            e.printStackTrace();
            return null;
        }
    }

    private void createPost(double price) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            handleUploadError("User not logged in");
            return;
        }

        Log.d(TAG, "📝 Creating post for user: " + currentUser.getUid());
        Log.d(TAG, "📸 Total images uploaded: " + uploadedImageUrls.size());
        for (int i = 0; i < uploadedImageUrls.size(); i++) {
            Log.d(TAG, "   Image " + (i+1) + ": " + uploadedImageUrls.get(i));
        }

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
        
        Log.d(TAG, "📤 Sending post creation request to server...");

        apiService.createPost(post).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Post>> call, @NonNull Response<ApiResponse<Post>> response) {
                postProgressBar.setVisibility(View.GONE);
                btnPostItem.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Post> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Log.d(TAG, "✅ POST CREATED SUCCESSFULLY! Post ID: " + apiResponse.getData().id);
                        Toast.makeText(PostItemActivity.this, "Item Posted Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMsg = "Failed: " + apiResponse.getMessage();
                        Log.e(TAG, "❌ " + errorMsg);
                        Toast.makeText(PostItemActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = "Server error: " + response.code() + " " + response.message();
                    Log.e(TAG, "❌ " + errorMsg);
                    Toast.makeText(PostItemActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Post>> call, @NonNull Throwable t) {
                postProgressBar.setVisibility(View.GONE);
                btnPostItem.setEnabled(true);
                String errorMsg = "Network Error: " + t.getMessage();
                Log.e(TAG, "❌ " + errorMsg, t);
                Toast.makeText(PostItemActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPhotoSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Photo Source")
                .setItems(new String[]{"Gallery", "Camera"}, (dialog, which) -> {
                    if (which == 0) {
                        openGallery();
                    } else {
                        openCamera();
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImageLauncher.launch(intent);
    }

    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        File photoFile = null;
        try {
            photoFile = createImageFile();
            cameraImageFile = photoFile; // Store the file reference
            Log.d(TAG, "📸 Camera file created at: " + photoFile.getAbsolutePath());
        } catch (IOException ex) {
            Log.e(TAG, "❌ Error creating image file", ex);
            Toast.makeText(this, "Error creating image file: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);
        }
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile("JPEG_" + System.currentTimeMillis(), ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
