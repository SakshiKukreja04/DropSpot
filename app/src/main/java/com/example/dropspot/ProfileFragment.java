package com.example.dropspot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String ARG_FULL_NAME = "USER_FULL_NAME";
    private static final String ARG_EMAIL = "USER_EMAIL";

    private String fullName;
    private String email;

    public static ProfileFragment newInstance(String fullName, String email) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FULL_NAME, fullName);
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fullName = getArguments().getString(ARG_FULL_NAME);
            email = getArguments().getString(ARG_EMAIL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        TextView tvFullName = view.findViewById(R.id.tvProfileName);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnEditProfile = view.findViewById(R.id.btnEditProfile);
        Button btnViewMyPosts = view.findViewById(R.id.btnViewMyPosts);

        if (tvFullName != null) {
            tvFullName.setText(fullName);
        }
        if (tvEmail != null) {
            tvEmail.setText(email);
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Logout clicked, redirecting to LoginActivity");
                Intent logoutIntent = new Intent(getActivity(), LoginActivity.class);
                logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(logoutIntent);
                getActivity().finish();
            });
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Edit Profile Clicked", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnViewMyPosts != null) {
            btnViewMyPosts.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSavedFragment();
                }
            });
        }

        return view;
    }
}
