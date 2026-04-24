package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.Map;

/**
 * BaseActivity - Common activity base class with shared bottom navigation
 * All main app activities should extend this to maintain consistent UI
 * 
 * BUG 4 FIX: Centralizes navigation logic to prevent inconsistencies
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;
    protected final Map<Integer, Fragment> fragmentCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Setup bottom navigation view with consistent behavior
     * Call this in your activity's onCreate after setContentView
     */
    protected void setupBottomNavigation(int bottomNavViewId) {
        bottomNavigationView = findViewById(bottomNavViewId);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                Fragment selectedFragment = getFragmentForMenuId(itemId);

                if (selectedFragment != null) {
                    switchFragment(selectedFragment);
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Get the appropriate fragment for a menu item ID
     * Override this in subclasses to define your fragment mapping
     */
    protected Fragment getFragmentForMenuId(int menuId) {
        // Subclasses can override to implement their navigation logic
        return null;
    }

    /**
     * Switch to a specific fragment using FragmentTransaction
     * Prevents duplicate instances and ensures smooth transitions
     */
    protected void switchFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(getFragmentContainerId(), fragment)
                    .commit();
        }
    }

    /**
     * Get the ID of the fragment container layout
     * Override this in subclasses if using a different container ID
     */
    protected int getFragmentContainerId() {
        return R.id.fragment_container;
    }

    /**
     * Select a specific bottom navigation item by ID
     * Used for programmatic navigation (e.g., after payment success)
     */
    protected void selectBottomNavItem(int menuId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(menuId);
        }
    }

    /**
     * Navigate to another activity with FLAG_ACTIVITY_CLEAR_TOP
     * Prevents multiple instances and maintains proper back stack
     */
    protected void navigateToActivity(Class<?> activityClass, String action, int navId) {
        Intent intent = new Intent(this, activityClass);
        intent.setAction(action);
        intent.putExtra("navigate_to", navId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Clear fragment cache when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragmentCache.clear();
    }
}

