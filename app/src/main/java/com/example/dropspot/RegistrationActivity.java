package com.example.dropspot;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "ActivityResult received. ResultCode: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null && account.getIdToken() != null) {
                                Log.d(TAG, "Google Sign-In successful, authenticating with Firebase...");
                                firebaseAuthWithGoogle(account.getIdToken());
                            } else {
                                Log.e(TAG, "ID Token is null");
                                Toast.makeText(this, "ID Token error", Toast.LENGTH_SHORT).show();
                            }
                        } catch (ApiException e) {
                            Log.e(TAG, "Google sign in failed code: " + e.getStatusCode(), e);
                            Toast.makeText(this, "Google Sign-In Failed (Code: " + e.getStatusCode() + ")", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Activity Result not OK: " + result.getResultCode());
                        Toast.makeText(this, "Sign-up cancelled or failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        Button registerButton = findViewById(R.id.btnRegister);
        Button googleSignUpButton = findViewById(R.id.btnGoogleSignUp);
        TextView loginLink = findViewById(R.id.tvLoginLink);

        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                Toast.makeText(this, "Email registration not implemented. Use Google Sign-In.", Toast.LENGTH_SHORT).show();
            });
        }

        if (googleSignUpButton != null) {
            googleSignUpButton.setOnClickListener(v -> signIn());
        }

        if (loginLink != null) {
            loginLink.setOnClickListener(v -> {
                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                finish();
            });
        }
    }

    private void signIn() {
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
                            Log.d(TAG, "Firebase Auth success, saving session...");
                            
                            String name = user.getDisplayName();
                            String email = user.getEmail();
                            Uri photoUri = user.getPhotoUrl();
                            String photoUrl = photoUri != null ? photoUri.toString() : null;
                            
                            sessionManager.saveUser(name, email, photoUrl);
                            
                            Toast.makeText(RegistrationActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegistrationActivity.this, HomeActivity.class));
                            finish();
                        }
                    } else {
                        Log.e(TAG, "Firebase Auth failed", task.getException());
                        Toast.makeText(RegistrationActivity.this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
