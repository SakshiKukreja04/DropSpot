package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Note: The XML IDs are btnLogin and tvRegister based on the layout file
        Button loginButton = findViewById(R.id.btnLogin);
        TextView registerLink = findViewById(R.id.tvRegister);

        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                Log.d(TAG, "Login button clicked, navigating to HomeActivity");
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (registerLink != null) {
            registerLink.setOnClickListener(v -> {
                Log.d(TAG, "Register link clicked, navigating to RegistrationActivity");
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            });
        }
    }
}