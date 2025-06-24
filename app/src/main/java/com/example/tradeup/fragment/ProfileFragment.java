// File: ProfileFragment.java

package com.example.tradeup.fragment; // Adjust package as necessary

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide; // Assuming Glide is used for image loading
import com.example.tradeup.R; // R.java will be generated based on your project resources
import com.example.tradeup.activity.LoginActivity; // Assuming LoginActivity for navigation
import com.example.tradeup.activity.MainActivity; // Assuming MainActivity for navigation
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private FirebaseHelper firebaseHelper;
    private TextView textDisplayName, textBio, textEmail;
    private ImageView imageProfile;
    private RatingBar ratingBar;
    private LinearLayout rowEditProfile, rowChangePhoto, rowMyListings, rowDeactivate, rowLogout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false); // Replace with your actual XML layout name if different

        firebaseHelper = new FirebaseHelper();

        // Initialize UI elements
        textDisplayName = view.findViewById(R.id.text_display_name);
        textBio = view.findViewById(R.id.text_bio);
        textEmail = view.findViewById(R.id.text_email);
        imageProfile = view.findViewById(R.id.image_profile);
        ratingBar = view.findViewById(R.id.rating_bar);

        rowEditProfile = view.findViewById(R.id.row_edit_profile);
        rowChangePhoto = view.findViewById(R.id.row_change_photo);
        rowMyListings = view.findViewById(R.id.row_my_listings);
        rowDeactivate = view.findViewById(R.id.row_deactivate);
        rowLogout = view.findViewById(R.id.row_logout);

        // Load user profile data
        loadUserProfile();

        // Set up click listeners
        rowEditProfile.setOnClickListener(v -> {
            // FR-1.2.2: Navigate to Edit Profile screen
            Toast.makeText(getContext(), "Chỉnh sửa hồ sơ", Toast.LENGTH_SHORT).show();
            // Example: Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            // startActivity(intent);
        });

        rowChangePhoto.setOnClickListener(v -> {
            // FR-1.2.2: Handle change photo
            Toast.makeText(getContext(), "Thay đổi ảnh đại diện", Toast.LENGTH_SHORT).show();
            // Example: Open image picker
        });

        rowMyListings.setOnClickListener(v -> {
            // Navigate to My Listings screen
            Toast.makeText(getContext(), "Xem danh sách của tôi", Toast.LENGTH_SHORT).show();
            // Example: Intent intent = new Intent(getActivity(), MyListingsActivity.class);
            // startActivity(intent);
        });

        rowDeactivate.setOnClickListener(v -> {
            // FR-1.2.3: Show confirmation dialog for deactivating account
            showDeactivateAccountDialog();
        });

        rowLogout.setOnClickListener(v -> {
            // FR-1.1.5: Perform logout
            performLogout();
        });

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            firebaseHelper.getUserProfile(uid, new FirebaseHelper.DbReadCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    // FR-1.1.1: Display user profile information
                    if (user != null) {
                        textDisplayName.setText(user.getFirst_name() + " " + user.getLast_name());
                        textBio.setText(user.getBio());
                        textEmail.setText(user.getEmail());
                        ratingBar.setRating(user.getRating().floatValue());

                        // Load profile picture using Glide (add Glide dependency to your project)
                        if (user.getProfile_picture_url() != null && !user.getProfile_picture_url().isEmpty()) {
                            Glide.with(requireContext())
                                    .load(user.getProfile_picture_url())
                                    .placeholder(R.drawable.img_profile_placeholder) // Set a placeholder image
                                    .error(R.drawable.img_profile_placeholder) // Set an error image
                                    .into(imageProfile);
                        } else {
                            imageProfile.setImageResource(R.drawable.img_profile_placeholder);
                        }
                    } else {
                        Toast.makeText(getContext(), "Không thể tải dữ liệu hồ sơ người dùng.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getContext(), "Lỗi tải hồ sơ: " + errorMessage, Toast.LENGTH_SHORT).show();
                    // Fallback to FirebaseUser data if Realtime Database fails
                    if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                        textDisplayName.setText(currentUser.getDisplayName());
                    } else {
                        textDisplayName.setText(currentUser.getEmail()); // Use email as display name if no display name
                    }
                    textEmail.setText(currentUser.getEmail());
                    textBio.setText("Bio not available."); // Default bio
                    ratingBar.setRating(0.0f); // Default rating

                    if (currentUser.getPhotoUrl() != null) {
                        Glide.with(requireContext())
                                .load(currentUser.getPhotoUrl())
                                .placeholder(R.drawable.img_profile_placeholder)
                                .error(R.drawable.img_profile_placeholder)
                                .into(imageProfile);
                    } else {
                        imageProfile.setImageResource(R.drawable.img_profile_placeholder);
                    }
                }
            });
        } else {
            // User not logged in, redirect to login or show appropriate message
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    private void showDeactivateAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Vô hiệu hóa tài khoản")
                .setMessage("Bạn có chắc chắn muốn vô hiệu hóa tài khoản của mình? Bạn có thể kích hoạt lại tài khoản này sau.")
                .setPositiveButton("Vô hiệu hóa", (dialog, which) -> {
                    deactivateAccount();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deactivateAccount() {
        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            firebaseHelper.deactivateAccount(uid, new FirebaseHelper.DbWriteCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Tài khoản của bạn đã được vô hiệu hóa thành công.", Toast.LENGTH_SHORT).show();
                    // Optionally, log out the user after deactivation
                    performLogout();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getContext(), "Lỗi khi vô hiệu hóa tài khoản: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "Không có người dùng nào đăng nhập để vô hiệu hóa.", Toast.LENGTH_SHORT).show();
        }
    }

    private void performLogout() {
        firebaseHelper.signOut();
        Toast.makeText(getContext(), "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}