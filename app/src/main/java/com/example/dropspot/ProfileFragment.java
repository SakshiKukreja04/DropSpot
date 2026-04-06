package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;

    public static ProfileFragment newInstance(String fullName, String email) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("USER_FULL_NAME", fullName);
        args.putString("USER_EMAIL", email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Fixed: Use activity_profile layout as fragment_profile does not exist
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        sessionManager = new SessionManager(requireContext());

        // Fixed: Updated IDs to match activity_profile.xml
        ImageView ivProfile = view.findViewById(R.id.ivProfileImage);
        TextView tvName = view.findViewById(R.id.tvProfileName);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        Button btnEditProfile = view.findViewById(R.id.btnEditProfile);
        Button btnViewMyPosts = view.findViewById(R.id.btnViewMyPosts);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // Populate data from SessionManager
        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        // Fixed: Use getUserPhotoUrl() instead of getUserPhoto()
        String photoUrl = sessionManager.getUserPhotoUrl();

        tvName.setText(name != null ? name : "User Name");
        tvEmail.setText(email != null ? email : "user@example.com");

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().into(ivProfile);
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> Toast.makeText(getContext(), "Edit Profile Clicked", Toast.LENGTH_SHORT).show());
        }

        if (btnViewMyPosts != null) {
            btnViewMyPosts.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), PostedItemsActivity.class);
                startActivity(intent);
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                // Fixed: Use logout() instead of clearSession()
                sessionManager.logout();
                Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        return view;
    }
}
