package com.example.dropspot;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;

public class ItemDetailActivity extends AppCompatActivity {
    private static final String TAG = "ItemDetailActivity";
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private ImageView ivItemImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        Button btnRequestItem = findViewById(R.id.btn_request_item);
        ivItemImage = findViewById(R.id.iv_item_image_detail);

        if (btnRequestItem != null) {
            btnRequestItem.setOnClickListener(v -> {
                // Feature 1: Button Click Event (Snackbar)
                Snackbar.make(v, "Request Sent", Snackbar.LENGTH_LONG).show();
                Log.d(TAG, "Request Item button clicked");
            });
        }

        // Feature 3: Double Tap Gesture using GestureDetector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Toast.makeText(ItemDetailActivity.this, "Item Saved", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // Feature: Pinch-to-Zoom Gesture using ScaleGestureDetector
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                // Prevent over-zooming
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
                ivItemImage.setScaleX(scaleFactor);
                ivItemImage.setScaleY(scaleFactor);
                return true;
            }
        });

        if (ivItemImage != null) {
            ivItemImage.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                scaleGestureDetector.onTouchEvent(event);
                return true;
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
