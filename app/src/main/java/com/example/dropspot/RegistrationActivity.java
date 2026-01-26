package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        Button registerButton = findViewById(R.id.btnRegister);
        TextView loginLink = findViewById(R.id.tvLoginLink);

        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                Log.d(TAG, "Register button clicked, navigating to HomeActivity");
                startActivity(new Intent(RegistrationActivity.this, HomeActivity.class));
                finish(); // Finish this activity to remove it from the back stack
            });
        }

        if (loginLink != null) {
            loginLink.setOnClickListener(v -> {
                Log.d(TAG, "Login link clicked, navigating to LoginActivity");
                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                finish(); // Finish this activity to remove it from the back stack
            });
        }
    }
}
