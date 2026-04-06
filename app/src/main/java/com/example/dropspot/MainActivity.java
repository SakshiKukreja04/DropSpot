package com.example.dropspot;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final Map<Integer, Fragment> fragmentCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = getFragment(itemId);

            if (selectedFragment != null) {
                switchFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Load the default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private Fragment getFragment(int itemId) {
        if (fragmentCache.containsKey(itemId)) {
            return fragmentCache.get(itemId);
        }

        Fragment fragment = null;
        if (itemId == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_saved) {
            fragment = new PostedItemsFragment();
        } else if (itemId == R.id.nav_announcements) {
            fragment = new AnnouncementsFragment();
        } else if (itemId == R.id.nav_profile) {
            // Assuming ProfileFragment can be instantiated without arguments for now
            // or we can fetch user info from SessionManager inside the Fragment
            fragment = new ProfileFragment();
        }

        if (fragment != null) {
            fragmentCache.put(itemId, fragment);
        }
        return fragment;
    }

    private void switchFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Use a fade animation for smooth transitions
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    public void navigateToTab(int navId) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(navId);
    }
}
