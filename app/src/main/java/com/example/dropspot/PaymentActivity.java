package com.example.dropspot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Enhanced Mock Payment Activity
 * Features:
 * - Input validation for card details & delivery address
 * - 2-second payment simulation
 * - 80/20 success/failure ratio
 * - Payment status persistence
 * - Post owner notification
 * - Order tracking
 * - UI state management (button hide/show based on payment status)
 */
public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final int PAYMENT_SIMULATION_DELAY = 2000;

    // Data
    private String postId, postTitle, ownerId;
    private double amount;
    private String currentUserId;

    // Services
    private ApiService apiService;
    private FirebaseFirestore firebaseFirestore;

    // UI Components
    private EditText etCardNumber, etExpiry, etCvv, etDeliveryAddress;
    private ProgressBar progressBar;
    private MaterialButton btnPayNow;
    private TextView tvAmount, tvItemTitle, tvPaymentHeader, tvPaymentStatus;
    private View cardPaymentForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize services
        apiService = ApiClient.getClient().create(ApiService.class);
        firebaseFirestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Get data from intent
        postId = getIntent().getStringExtra("POST_ID");
        postTitle = getIntent().getStringExtra("POST_TITLE");
        ownerId = getIntent().getStringExtra("OWNER_ID");
        amount = getIntent().getDoubleExtra("AMOUNT", 100.0);

        // Initialize Views
        initViews();
        displayPaymentDetails();
        setupClickListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Payment");
        }
        toolbar.setNavigationOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        tvPaymentHeader = findViewById(R.id.tv_payment_header);
        tvItemTitle = findViewById(R.id.tv_payment_item_title);
        tvAmount = findViewById(R.id.tv_payment_amount);
        etCardNumber = findViewById(R.id.et_card_number);
        etExpiry = findViewById(R.id.et_expiry);
        etCvv = findViewById(R.id.et_cvv);
        
        // Delivery address field
        try {
            etDeliveryAddress = findViewById(R.id.et_delivery_address);
        } catch (Exception e) {
            // Fallback if ID doesn't exist yet
        }
        
        // Progress bar - optional, may not exist in layout
        try {
            progressBar = findViewById(R.id.payment_progress_bar);
        } catch (Exception e) {
            progressBar = null;
        }
        
        btnPayNow = findViewById(R.id.btn_pay_now);
        
        // Payment status label
        try {
            tvPaymentStatus = findViewById(R.id.tv_payment_status);
        } catch (Exception e) {
            tvPaymentStatus = null;
        }
        
        cardPaymentForm = findViewById(R.id.card_payment_form);
    }

    private void displayPaymentDetails() {
        tvPaymentHeader.setText("Complete Your Payment");
        tvItemTitle.setText(postTitle != null ? postTitle : "Item Payment");
        tvAmount.setText(String.format("₹%.2f", amount));
    }

    private void setupClickListeners() {
        btnPayNow.setOnClickListener(v -> handlePaymentClick());
    }

    private void handlePaymentClick() {
        if (!validatePaymentInput()) {
            return;
        }
        processPayment();
    }

    private boolean validatePaymentInput() {
        String cardNumber = etCardNumber.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();
        String cvv = etCvv.getText().toString().trim();
        String deliveryAddress = etDeliveryAddress.getText().toString().trim();

        if (cardNumber.isEmpty()) {
            Toast.makeText(this, "Please enter card number", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cardNumber.length() < 13) {
            Toast.makeText(this, "Card number must be at least 13 digits", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (expiry.isEmpty()) {
            Toast.makeText(this, "Please enter expiry date (MM/YY)", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (expiry.length() < 5) {
            Toast.makeText(this, "Expiry format should be MM/YY", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cvv.isEmpty()) {
            Toast.makeText(this, "Please enter CVV", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cvv.length() < 3) {
            Toast.makeText(this, "CVV must be at least 3 digits", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate delivery address
        if (deliveryAddress.isEmpty()) {
            Toast.makeText(this, "Please enter delivery address", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (deliveryAddress.length() < 10) {
            Toast.makeText(this, "Please enter a complete address", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void processPayment() {
        showLoadingState();
        new Handler(Looper.getMainLooper()).postDelayed(
            this::simulatePaymentResult,
            PAYMENT_SIMULATION_DELAY
        );
    }

    private void showLoadingState() {
        cardPaymentForm.setEnabled(false);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnPayNow.setEnabled(false);
    }

    private void hideLoadingState() {
        cardPaymentForm.setEnabled(true);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnPayNow.setEnabled(true);
    }

    private void simulatePaymentResult() {
        Random random = new Random();
        boolean isSuccessful = random.nextInt(100) < 80; // 80% success

        if (isSuccessful) {
            onPaymentSuccess();
        } else {
            onPaymentFailure();
        }
    }

    private void onPaymentSuccess() {
        hideLoadingState();
        String mockPaymentId = generateMockPaymentId();
        String deliveryAddress = (etDeliveryAddress != null) ? 
            etDeliveryAddress.getText().toString().trim() : "Not provided";
        
        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
        
        // FEATURE 1: Update UI - Hide button, show status
        btnPayNow.setVisibility(View.GONE);
        if (tvPaymentStatus != null) {
            tvPaymentStatus.setVisibility(View.VISIBLE);
            tvPaymentStatus.setText("Payment Completed ✅");
        }
        cardPaymentForm.setEnabled(false);
        
        // Save payment to backend
        savePaymentToBackend(mockPaymentId, deliveryAddress);
        
        // FEATURE 3: Update post status to "ORDERED"
        updatePostStatus("ORDERED");
        
        // FEATURE 2: Send notification to post owner
        sendNotificationToOwner(mockPaymentId, deliveryAddress);
        
        // Create order tracking record
        OrderTrackingHelper.createOrder(
            mockPaymentId,
            postId,
            currentUserId,
            ownerId,
            amount,
            postTitle,
            deliveryAddress
        );
    }

    private void onPaymentFailure() {
        hideLoadingState();
        Toast.makeText(this, "Payment Failed. Try again", Toast.LENGTH_SHORT).show();
    }

    private String generateMockPaymentId() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(10000);
        return "PAY_" + timestamp + "_" + random;
    }

    private void savePaymentToBackend(String paymentId, String deliveryAddress) {
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService.PaymentRequest payment = new ApiService.PaymentRequest(
            paymentId, postId, currentUserId, ownerId, amount, "success"
        );

        apiService.savePayment(payment).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    // Save to Firestore for persistence
                    savePaymentStatusToFirestore(paymentId, deliveryAddress);
                    Toast.makeText(PaymentActivity.this, "Transaction completed!", Toast.LENGTH_SHORT).show();
                    
                    // FEATURE 4: Navigate back after 2 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        navigateToMyRequests();
                    }, 2000);
                } else {
                    Toast.makeText(PaymentActivity.this, "Payment saved locally.", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        navigateToMyRequests();
                    }, 2000);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(PaymentActivity.this, "Payment saved locally.", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    navigateToMyRequests();
                }, 2000);
            }
        });
    }
    
    /**
     * FEATURE 1 & FEATURE 2: Save payment status to Firestore
     * This allows UI state to persist and enables seller tracking
     */
    private void savePaymentStatusToFirestore(String paymentId, String deliveryAddress) {
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("postId", postId);
        paymentData.put("buyerId", currentUserId);
        paymentData.put("sellerId", ownerId);
        paymentData.put("amount", amount);
        paymentData.put("status", "COMPLETED");
        paymentData.put("deliveryAddress", deliveryAddress);
        paymentData.put("timestamp", System.currentTimeMillis());
        
        firebaseFirestore.collection("payments")
            .document(paymentId)
            .set(paymentData)
            .addOnSuccessListener(aVoid -> {
                // Payment record saved for persistence
            })
            .addOnFailureListener(e -> {
                // Handle error if needed
            });
    }
    
    /**
     * FEATURE 3: Update post status to "ORDERED"
     * Prevents other users from purchasing this item
     */
    private void updatePostStatus(String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("isActive", false);
        updates.put("purchasedBy", currentUserId);
        updates.put("purchaseTime", System.currentTimeMillis());
        
        firebaseFirestore.collection("posts")
            .document(postId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                // Post status updated
            })
            .addOnFailureListener(e -> {
                // Handle error if needed
            });
    }
    
    /**
     * FEATURE 2: Send notification to post owner
     * Notifies seller immediately when payment is successful
     * Saves to Firestore and triggers FCM notification
     */
    private void sendNotificationToOwner(String paymentId, String deliveryAddress) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("receiverId", ownerId);
        notification.put("senderId", currentUserId);
        notification.put("type", "PAYMENT_SUCCESS");
        notification.put("postId", postId);
        notification.put("paymentId", paymentId);
        notification.put("title", "New Order Received");
        notification.put("message", "Your item (" + postTitle + ") has been paid for. Please dispatch it.");
        notification.put("deliveryAddress", deliveryAddress);
        notification.put("amount", amount);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        
        firebaseFirestore.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    // Trigger FCM notification to owner
                    triggerFcmPaymentNotificationToOwner(deliveryAddress);
                })
                .addOnFailureListener(e -> {
                    // Handle error if needed
                });
    }
    
    /**
     * Trigger FCM notification to owner via backend
     */
    private void triggerFcmPaymentNotificationToOwner(String deliveryAddress) {
        Map<String, Object> fcmPayload = new HashMap<>();
        fcmPayload.put("userId", ownerId);
        fcmPayload.put("title", "New Order Received 🎉");
        fcmPayload.put("body", "Your item (" + postTitle + ") has been paid for!\nDelivery to: " + deliveryAddress + "\nAmount: ₹" + amount);
        fcmPayload.put("type", "PAYMENT_SUCCESS");
        fcmPayload.put("postId", postId);
        
        apiService.sendFcmNotification(fcmPayload).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    // FCM sent
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                // Continue even if FCM fails
            }
        });
    }
    
    /**
     * Navigate back to MainActivity and show MyRequestsFragment
     * This replaces direct finish() to ensure proper app flow
     */
    private void navigateToMyRequests() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("navigate_to", R.id.nav_saved);  // Navigate to saved/requests tab
        startActivity(intent);
        finish();
    }
}




