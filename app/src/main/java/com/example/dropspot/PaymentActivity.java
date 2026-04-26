package com.example.dropspot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final int PAYMENT_SIMULATION_DELAY = 2000;

    private String postId, postTitle, ownerId;
    private double amount;
    private String currentUserId;

    private ApiService apiService;
    private FirebaseFirestore firebaseFirestore;

    private EditText etCardNumber, etExpiry, etCvv, etDeliveryAddress;
    private ProgressBar progressBar;
    private MaterialButton btnPayNow;
    private TextView tvAmount, tvItemTitle, tvPaymentHeader, tvPaymentStatus;
    private View cardPaymentForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        apiService = ApiClient.getClient().create(ApiService.class);
        firebaseFirestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        postId = getIntent().getStringExtra("POST_ID");
        postTitle = getIntent().getStringExtra("POST_TITLE");
        ownerId = getIntent().getStringExtra("OWNER_ID");
        amount = getIntent().getDoubleExtra("AMOUNT", 100.0);

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
        toolbar.setNavigationOnClickListener(v -> finish());

        tvPaymentHeader = findViewById(R.id.tv_payment_header);
        tvItemTitle = findViewById(R.id.tv_payment_item_title);
        tvAmount = findViewById(R.id.tv_payment_amount);
        etCardNumber = findViewById(R.id.et_card_number);
        etExpiry = findViewById(R.id.et_expiry);
        etCvv = findViewById(R.id.et_cvv);
        etDeliveryAddress = findViewById(R.id.et_delivery_address);
        progressBar = findViewById(R.id.payment_progress_bar);
        btnPayNow = findViewById(R.id.btn_pay_now);
        tvPaymentStatus = findViewById(R.id.tv_payment_status);
        cardPaymentForm = findViewById(R.id.card_payment_form);
    }

    private void displayPaymentDetails() {
        tvItemTitle.setText(postTitle != null ? postTitle : "Item Payment");
        tvAmount.setText(String.format("₹%.2f", amount));
    }

    private void setupClickListeners() {
        btnPayNow.setOnClickListener(v -> handlePaymentClick());
    }

    private void handlePaymentClick() {
        if (!validatePaymentInput()) return;
        processPayment();
    }

    private boolean validatePaymentInput() {
        if (etCardNumber.getText().toString().length() < 13) return false;
        if (etExpiry.getText().toString().length() < 5) return false;
        if (etCvv.getText().toString().length() < 3) return false;
        if (etDeliveryAddress.getText().toString().length() < 5) return false;
        return true;
    }

    private void processPayment() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnPayNow.setEnabled(false);
        new Handler(Looper.getMainLooper()).postDelayed(this::onPaymentSuccess, PAYMENT_SIMULATION_DELAY);
    }

    private void onPaymentSuccess() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        String mockPaymentId = "PAY_" + System.currentTimeMillis();
        String deliveryAddress = etDeliveryAddress.getText().toString().trim();
        
        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
        
        btnPayNow.setVisibility(View.GONE);
        if (tvPaymentStatus != null) {
            tvPaymentStatus.setVisibility(View.VISIBLE);
            tvPaymentStatus.setText("Payment Completed ✅");
        }

        savePaymentToBackend(mockPaymentId, deliveryAddress);
    }

     private void savePaymentToBackend(String paymentId, String deliveryAddress) {
         // Now call backend for notifications
         // BACKEND REQUIREMENT: status should be "paid"
         Log.d("PAYMENT_DEBUG", "==============================================");
         Log.d("PAYMENT_DEBUG", "Creating Payment Record");
         Log.d("PAYMENT_DEBUG", "Payment ID: " + paymentId);
         Log.d("PAYMENT_DEBUG", "Post ID: " + postId);
         Log.d("PAYMENT_DEBUG", "Requester ID (Buyer): " + currentUserId);
         Log.d("PAYMENT_DEBUG", "Owner ID (Seller): " + ownerId);
         Log.d("PAYMENT_DEBUG", "Amount: " + amount);
         Log.d("PAYMENT_DEBUG", "Status: paid");
         Log.d("PAYMENT_DEBUG", "==============================================");
         
         ApiService.PaymentRequest payment = new ApiService.PaymentRequest(
             paymentId, postId, currentUserId, ownerId, amount, "paid"
         );
         apiService.savePayment(payment).enqueue(new Callback<ApiResponse<Object>>() {
             @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                 Log.d("PAYMENT_DEBUG", "Backend response successful: " + response.isSuccessful());
                 // Sync with Firestore
                 Map<String, Object> paymentData = new HashMap<>();
                 paymentData.put("paymentId", paymentId);
                 paymentData.put("postId", postId);
                 paymentData.put("requesterId", currentUserId);
                 paymentData.put("ownerId", ownerId);
                 paymentData.put("amount", amount);
                 paymentData.put("status", "paid"); // Standardized to "paid"
                 paymentData.put("deliveryAddress", deliveryAddress);
                 paymentData.put("timestamp", System.currentTimeMillis());
                 
                 Log.d("PAYMENT_DEBUG", "Saving to Firestore with these fields:");
                 Log.d("PAYMENT_DEBUG", "  postId: " + postId);
                 Log.d("PAYMENT_DEBUG", "  ownerId: " + ownerId);
                 Log.d("PAYMENT_DEBUG", "  status: paid");
                 
                 firebaseFirestore.collection("payments").document(paymentId).set(paymentData)
                     .addOnCompleteListener(task -> {
                         Log.d("PAYMENT_DEBUG", "Firestore write completed. Success: " + task.isSuccessful());
                         if (task.isSuccessful()) {
                             Log.d("PAYMENT_DEBUG", "✅ Payment saved to Firestore!");
                         } else {
                             Log.e("PAYMENT_DEBUG", "❌ Error saving payment: " + task.getException());
                         }
                         new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 2000);
                     });
             }
             @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                 Log.e("PAYMENT_DEBUG", "Backend API failed: " + t.getMessage());
                 t.printStackTrace();
                 finish();
             }
         });
     }
}
