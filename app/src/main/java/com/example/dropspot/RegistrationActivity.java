package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        TextInputEditText etName = findViewById(R.id.etName);
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        Button registerButton = findViewById(R.id.btnRegister);
        TextView loginLink = findViewById(R.id.tvLoginLink);

        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                // Get user input
                String fullName = etName.getText().toString();
                String email = etEmail.getText().toString();

                // Create an Intent to navigate to ProfileActivity
                Intent intent = new Intent(RegistrationActivity.this, ProfileActivity.class);

                // Pass the user's full name and email as extras
                intent.putExtra("USER_FULL_NAME", fullName);
                intent.putExtra("USER_EMAIL", email);

                // Start the ProfileActivity
                startActivity(intent);

                // Finish the RegistrationActivity to remove it from the back stack
                finish();
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
