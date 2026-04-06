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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private SessionManager sessionManager;
    private ApiService apiService;
    
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin, btnGoogleSignIn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        TextView registerLink = findViewById(R.id.tvRegister);

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
                            Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        btnLogin.setOnClickListener(v -> performEmailLogin());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        if (registerLink != null) {
            registerLink.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            });
        }
    }

    private void performEmailLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            syncUserWithBackend(user);
                        }
                    } else {
                        btnLogin.setEnabled(true);
                        Log.e(TAG, "Email login failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                                Toast.LENGTH_LONG).show();
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
                            syncUserWithBackend(user);
                        }
                    } else {
                        Log.e(TAG, "Firebase Auth failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Google Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncUserWithBackend(FirebaseUser user) {
        String tempName = user.getDisplayName();
        if (tempName == null || tempName.isEmpty()) {
            tempName = user.getEmail().split("@")[0];
        }
        final String name = tempName;
        final String email = user.getEmail();
        Uri photoUri = user.getPhotoUrl();
        final String photoUrl = photoUri != null ? photoUri.toString() : null;

        ApiService.UserProfile profile = new ApiService.UserProfile(name, email, photoUrl);
        
        apiService.syncUserProfile(profile).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                sessionManager.saveUser(name, email, photoUrl);
                Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                sessionManager.saveUser(name, email, photoUrl);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}
