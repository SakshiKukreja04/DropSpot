package com.example.dropspot;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;
import java.util.Locale;

public class CreateEventActivity extends AppCompatActivity {

    private EditText etEventTitle, etEventDescription, etEventCategory, etEventLocation, etEventStart, etEventEnd;
    private MaterialButton btnCreateEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etEventTitle = findViewById(R.id.etEventTitle);
        etEventDescription = findViewById(R.id.etEventDescription);
        etEventCategory = findViewById(R.id.etEventCategory);
        etEventLocation = findViewById(R.id.etEventLocation);
        etEventStart = findViewById(R.id.etEventStart);
        etEventEnd = findViewById(R.id.etEventEnd);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);

        etEventStart.setOnClickListener(v -> showDateTimePicker(etEventStart));
        etEventEnd.setOnClickListener(v -> showDateTimePicker(etEventEnd));

        btnCreateEvent.setOnClickListener(v -> {
            if (validateInputs()) {
                Toast.makeText(this, "Event Created Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        setupBottomNavigation();
    }

    private void showDateTimePicker(final EditText editText) {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);
                String format = "dd MMM yyyy, HH:mm";
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, Locale.getDefault());
                editText.setText(sdf.format(date.getTime()));
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    private boolean validateInputs() {
        if (etEventTitle.getText().toString().trim().isEmpty()) {
            etEventTitle.setError("Title required");
            return false;
        }
        if (etEventStart.getText().toString().trim().isEmpty()) {
            etEventStart.setError("Start time required");
            return false;
        }
        return true;
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
