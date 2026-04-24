package com.example.dropspot;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Calendar;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateEventActivity extends AppCompatActivity {
    private EditText etEventTitle, etEventDescription, etEventCategory, etEventLocation, etEventStart, etEventEnd;
    private MaterialButton btnCreateEvent;
    private String currentUserId, currentUserName;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getUid();
        currentUserName = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Anonymous";
        
        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        getLocationForEvent();

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
                createEvent();
            }
        });
    }

    private void getLocationForEvent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
            }
        });
    }

    private void createEvent() {
        String eventName = etEventTitle.getText().toString().trim();
        String description = etEventDescription.getText().toString().trim();
        String startTime = etEventStart.getText().toString().trim();
        String endTime = etEventEnd.getText().toString().trim();
        String location = etEventLocation.getText().toString().trim();
        String category = etEventCategory.getText().toString().trim();

        Event event = new Event(eventName, description, "", startTime, endTime, location);
        event.category = category;
        event.latitude = currentLat;
        event.longitude = currentLng;
        
        btnCreateEvent.setEnabled(false);
        
        apiService.createEvent(event).enqueue(new Callback<ApiResponse<Event>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Event>> call, @NonNull Response<ApiResponse<Event>> response) {
                btnCreateEvent.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(CreateEventActivity.this, "Event Created Successfully! Nearby users will be notified.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateEventActivity.this, "Failed to create event", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Event>> call, @NonNull Throwable t) {
                btnCreateEvent.setEnabled(true);
                Toast.makeText(CreateEventActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
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
}
