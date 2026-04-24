package com.example.dropspot;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private SessionManager sessionManager;
    private ApiService apiService;
    
    private TextInputEditText etName, etEmail, etPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progressBar = new ProgressBar(this); // Should ideally be in XML but adding safe check

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null && account.getIdToken() != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Log.e(TAG, "Google sign in failed", e);
                        }
                    }
                }
        );

        Button registerButton = findViewById(R.id.btnRegister);
        Button googleSignUpButton = findViewById(R.id.btnGoogleSignUp);
        TextView loginLink = findViewById(R.id.tvLoginLink);

        if (registerButton != null) {
            registerButton.setOnClickListener(v -> registerUser());
        }

        if (googleSignUpButton != null) {
            googleSignUpButton.setOnClickListener(v -> signInWithGoogle());
        }

        if (loginLink != null) {
            loginLink.setOnClickListener(v -> {
                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                finish();
            });
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Set display name in Firebase
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            
                            user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                syncUserWithBackend(user, name);
                            });
                        }
                    } else {
                        Toast.makeText(RegistrationActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            syncUserWithBackend(user, user.getDisplayName());
                        }
                    } else {
                        Toast.makeText(RegistrationActivity.this, "Google Auth Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncUserWithBackend(FirebaseUser user, String name) {
        String email = user.getEmail();
        Uri photoUri = user.getPhotoUrl();
        String photoUrl = photoUri != null ? photoUri.toString() : null;

        ApiService.UserProfile profile = new ApiService.UserProfile(name, email, photoUrl);
        
        apiService.syncUserProfile(profile).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                sessionManager.saveUser(name, email, photoUrl);
                Toast.makeText(RegistrationActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                
                // Save FCM token after successful registration
                saveFCMTokenAfterRegistration(user.getUid());
                
                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                sessionManager.saveUser(name, email, photoUrl);
                
                // Save FCM token even if sync fails
                saveFCMTokenAfterRegistration(user.getUid());
                
                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                finish();
            }
        });
    }
    
    private void saveFCMTokenAfterRegistration(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.d(TAG, "FCM Token obtained during registration: " + token.substring(0, 20) + "...");
                
                java.util.Map<String, Object> update = new java.util.HashMap<>();
                update.put("fcmToken", token);
                
                apiService.updateUserProfile(userId, update).enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        Log.d(TAG, "FCM Token saved to server after registration");
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        Log.e(TAG, "Failed to save FCM token after registration", t);
                    }
                });
            } else {
                Log.e(TAG, "Failed to get FCM token during registration", task.getException());
            }
        });
    }
}
