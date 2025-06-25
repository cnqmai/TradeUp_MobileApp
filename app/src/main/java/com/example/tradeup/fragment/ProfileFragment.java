package com.example.tradeup.fragment;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.activity.EditProfileActivity;
import com.example.tradeup.activity.LoginActivity;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import org.apache.commons.io.IOUtils;


public class ProfileFragment extends Fragment {

    private FirebaseHelper firebaseHelper;
    private TextView textDisplayName, textBio, textEmail, textRatingValue;
    private ImageView imageProfile;
    private RatingBar ratingBar;
    private LinearLayout rowEditProfile, rowChangePhoto, rowMyListings, rowAccountManagement, rowLogout;

    private String targetUserId;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri cameraImageUri;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // GALLERY
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Log.d("PICKED_IMAGE", "Gallery URI: " + uri.toString());
                        // Call the method to upload image to Cloudinary
                        uploadImageToCloudinary(uri);
                    }
                }
        );

        // CAMERA
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        Log.d("CAMERA_IMAGE", "Captured URI: " + cameraImageUri.toString());
                        // Call the method to upload image to Cloudinary
                        uploadImageToCloudinary(cameraImageUri);
                    }
                }
        );

        // Permission Request Launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // If permission is granted, open gallery or camera as needed
                        // In this case, it's used for Gallery, so keep it.
                        openGallery();
                    } else {
                        Toast.makeText(getContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firebaseHelper = new FirebaseHelper();

        if (getArguments() != null) {
            targetUserId = getArguments().getString("userId");
        }

        textDisplayName = view.findViewById(R.id.text_display_name);
        textBio = view.findViewById(R.id.text_bio);
        textEmail = view.findViewById(R.id.text_email);
        imageProfile = view.findViewById(R.id.image_profile);
        ratingBar = view.findViewById(R.id.rating_bar);
        textRatingValue = view.findViewById(R.id.text_rating_value);

        rowEditProfile = view.findViewById(R.id.row_edit_profile);
        rowChangePhoto = view.findViewById(R.id.row_change_photo);
        rowMyListings = view.findViewById(R.id.row_my_listings);
        rowAccountManagement = view.findViewById(R.id.row_account_management);
        rowLogout = view.findViewById(R.id.row_logout);

        // Hide certain rows if viewing another user's profile
        if (targetUserId != null && !targetUserId.equals(firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null)) {
            rowEditProfile.setVisibility(View.GONE);
            rowChangePhoto.setVisibility(View.GONE);
            rowAccountManagement.setVisibility(View.GONE);
            rowLogout.setVisibility(View.GONE);
        }

        loadUserProfile();

        rowEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            if (textDisplayName.getText() != null) {
                String[] nameParts = textDisplayName.getText().toString().split(" ", 2);
                if (nameParts.length > 0) intent.putExtra("firstName", nameParts[0]);
                if (nameParts.length > 1) intent.putExtra("lastName", nameParts[1]);
            }
            if (textBio.getText() != null) intent.putExtra("bio", textBio.getText().toString());
            if (textEmail.getText() != null) intent.putExtra("contactInfo", textEmail.getText().toString());
            startActivity(intent);
        });

        rowChangePhoto.setOnClickListener(v -> {
            showImageSourceDialog();
        });

        rowMyListings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "View My Listings", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                // Điều hướng đến graph sell_nav_graph.xml đã lồng.
                // Vì myItemsFragment là điểm bắt đầu của sell_nav_graph, nó sẽ tự động mở MyItemsFragment.
                navController.navigate(R.id.sell_nav_graph); // <-- Thay đổi ID điều hướng tại đây
            } else {
                Log.e("ProfileFragment", "NavController is null for My Listings navigation.");
                Toast.makeText(getContext(), "Lỗi: Không thể điều hướng.", Toast.LENGTH_SHORT).show();
            }
        });

        rowAccountManagement.setOnClickListener(v -> {
            showAccountManagementOptionsDialog();
        });

        rowLogout.setOnClickListener(v -> {
            performLogout();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // **KHỞI TẠO NavController TẠI ĐÂY**
        navController = Navigation.findNavController(view);

        // Các cài đặt listener khác có thể đặt ở đây nếu bạn muốn
        // (Hoặc có thể để chúng trong onCreateView nếu chúng không phụ thuộc vào NavController được khởi tạo)

        rowMyListings.setOnClickListener(v -> {
            // Toast.makeText(getContext(), "View My Listings", Toast.LENGTH_SHORT).show(); // Có thể bỏ dòng này
            if (navController != null) {
                // Điều hướng đến graph sell_nav_graph.xml đã lồng.
                // Vì myItemsFragment là điểm bắt đầu của sell_nav_graph, nó sẽ tự động mở MyItemsFragment.
                navController.navigate(R.id.sell_nav_graph);
            } else {
                Log.e("ProfileFragment", "NavController is null for My Listings navigation.");
                Toast.makeText(getContext(), "Lỗi: Không thể điều hướng. NavController không khả dụng.", Toast.LENGTH_SHORT).show();
            }
        });

        // ... (các cài đặt click listener khác)
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        String uidToLoad = (targetUserId != null) ? targetUserId : (firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null);

        if (uidToLoad == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }

        firebaseHelper.getUserProfile(uidToLoad, new FirebaseHelper.DbReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    textDisplayName.setText(user.getFirst_name() + " " + user.getLast_name());
                    textBio.setText(user.getBio());
                    if (user.getContact_info() != null && !user.getContact_info().isEmpty()) {
                        textEmail.setText(user.getContact_info());
                    } else {
                        textEmail.setText(user.getEmail());
                    }

                    float currentRating = user.getRating() != null ? user.getRating().floatValue() : 0.0f;
                    ratingBar.setRating(currentRating);
                    textRatingValue.setText(String.format("%.1f / 5", currentRating));

                    if (user.getProfile_picture_url() != null && !user.getProfile_picture_url().isEmpty()) {
                        Glide.with(requireContext())
                                .load(user.getProfile_picture_url())
                                .placeholder(R.drawable.img_profile_placeholder)
                                .error(R.drawable.img_profile_placeholder)
                                .into(imageProfile);
                    } else {
                        Glide.with(requireContext())
                                .load(R.drawable.img_profile_placeholder)
                                .into(imageProfile);
                    }
                } else {
                    Toast.makeText(getContext(), "Could not load user profile data.", Toast.LENGTH_SHORT).show();
                    if (targetUserId == null && firebaseHelper.getCurrentUser() != null) {
                        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
                        textDisplayName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
                        textEmail.setText(currentUser.getEmail());
                        textBio.setText("No bio information available.");
                        ratingBar.setRating(0.0f);
                        textRatingValue.setText("0.0 / 5");
                        Glide.with(requireContext())
                                .load(R.drawable.img_profile_placeholder)
                                .into(imageProfile);
                    } else {
                        textDisplayName.setText("Unknown User");
                        textBio.setText("No bio information available.");
                        textEmail.setText("N/A");
                        ratingBar.setRating(0.0f);
                        textRatingValue.setText("0.0 / 5");
                        Glide.with(requireContext())
                                .load(R.drawable.img_profile_placeholder)
                                .into(imageProfile);
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(getContext(), "Error loading profile: " + errorMessage, Toast.LENGTH_SHORT).show();
                if (targetUserId == null && firebaseHelper.getCurrentUser() != null) {
                    FirebaseUser currentUser = firebaseHelper.getCurrentUser();
                    if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                        textDisplayName.setText(currentUser.getDisplayName());
                    } else {
                        textDisplayName.setText(currentUser.getEmail());
                    }
                    textEmail.setText(currentUser.getEmail());
                    textBio.setText("Bio not available.");
                    ratingBar.setRating(0.0f);
                    textRatingValue.setText("0.0 / 5");

                    if (currentUser.getPhotoUrl() != null) {
                        Glide.with(requireContext())
                                .load(currentUser.getPhotoUrl())
                                .placeholder(R.drawable.img_profile_placeholder)
                                .error(R.drawable.img_profile_placeholder)
                                .into(imageProfile);
                    } else {
                        Glide.with(requireContext())
                                .load(R.drawable.img_profile_placeholder)
                                .into(imageProfile);
                    }
                } else {
                    textDisplayName.setText("Error Loading");
                    textBio.setText("Could not load bio.");
                    textEmail.setText("Error");
                    ratingBar.setRating(0.0f);
                    textRatingValue.setText("0.0 / 5");
                    Glide.with(requireContext())
                            .load(R.drawable.img_profile_placeholder)
                            .into(imageProfile);
                }
            }
        });
    }

    // --- Image Handling Methods ---

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Choose Image Source");
        builder.setItems(new CharSequence[]{"Gallery", "Camera"}, (dialog, which) -> {
            if (which == 0) openGallery();
            else checkCameraPermissionAndOpenCamera();
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From TradeUp");

        cameraImageUri = requireContext().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (cameraImageUri != null) {
            takePictureLauncher.launch(cameraImageUri);
        } else {
            Toast.makeText(getContext(), "Unable to create camera URI", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                pickImageLauncher.launch("image/*");
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                pickImageLauncher.launch("image/*");
            }
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(getContext(), "Image URI is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Uploading image to Cloudinary...", Toast.LENGTH_SHORT).show();

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(getContext(), "Could not open input stream from image URI.", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] imageBytes = IOUtils.toByteArray(inputStream);
            inputStream.close(); // Close the stream after reading

            // IMPORTANT Step: Get MIME type from URI
            String mimeType = requireContext().getContentResolver().getType(imageUri);
            if (mimeType == null) {
                mimeType = "application/octet-stream"; // Default type if not determined
            }

            // IMPORTANT Step: Get file extension from MIME type
            String fileExtension = getFileExtensionFromMimeType(mimeType);
            String fileName = "image" + fileExtension; // Dynamic file name

            OkHttpClient client = new OkHttpClient();
            String cloudName = "dp6tzdsyt"; // Your Cloudinary Cloud Name
            String uploadPreset = "TradeUp"; // Your Cloudinary Unsigned Upload Preset

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, // Use dynamic file name
                            RequestBody.create(imageBytes, MediaType.parse(mimeType))) // Use dynamic MIME type
                    .addFormDataPart("upload_preset", uploadPreset)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    Log.e("CloudinaryUpload", "Upload failed", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Upload error: " + response.code() + " " + response.message() + " - " + errorBody, Toast.LENGTH_LONG).show();
                            Log.e("CloudinaryUpload", "Upload error: " + response.code() + " " + response.message() + " - " + errorBody);
                        });
                        return;
                    }

                    String json = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        String imageUrl = jsonObject.getString("secure_url"); // Get the secure URL

                        requireActivity().runOnUiThread(() ->
                                updateProfilePictureUrlInDatabase(firebaseHelper.getCurrentUser().getUid(), imageUrl)
                        );
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error processing Cloudinary response: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        Log.e("CloudinaryUpload", "Error parsing Cloudinary response", e);
                    } finally {
                        if (response.body() != null) {
                            response.body().close(); // Close the response body
                        }
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error reading image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("CloudinaryUpload", "Error reading image URI", e);
        }
    }

    // Helper function to get file extension from MIME type
    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".bin"; // Return default extension if MIME type is null
        }
        switch (mimeType) {
            case "image/jpeg":
                return ".jpeg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            // Add other MIME types if needed
            default:
                // Use MimeTypeMap for other types, ensures broader compatibility
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                return extension != null ? "." + extension : ".bin";
        }
    }

    private void updateProfilePictureUrlInDatabase(String uid, String newPhotoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profile_picture_url", newPhotoUrl);

        firebaseHelper.updateUserProfileFields(uid, updates, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                // Update profile picture URL in Firebase Authentication profile
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(Uri.parse(newPhotoUrl))
                            .build();
                    currentUser.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("ProfileFragment", "Firebase Auth profile photo updated.");
                                } else {
                                    Log.e("ProfileFragment", "Failed to update Firebase Auth profile photo: " + task.getException().getMessage());
                                }
                            });
                }

                // Update image on ImageView immediately
                Glide.with(requireContext())
                        .load(newPhotoUrl)
                        .placeholder(R.drawable.img_profile_placeholder)
                        .error(R.drawable.img_profile_placeholder)
                        .into(imageProfile);

                Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(getContext(), "Failed to update profile picture: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("ProfileFragment", "Failed to update profile picture in DB: " + errorMessage);
            }
        });
    }

    // --- Account Management Methods ---

    private void showAccountManagementOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Account Management");
        builder.setItems(new CharSequence[]{"Deactivate Account", "Delete Account Permanently"}, (dialog, which) -> {
            switch (which) {
                case 0: // Deactivate Account
                    showDeactivateAccountDialog();
                    break;
                case 1: // Delete Account Permanently
                    showDeleteAccountDialog();
                    break;
            }
        });
        builder.show();
    }


    private void showDeactivateAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Deactivate Account")
                .setMessage("Are you sure you want to deactivate your account? You can reactivate this account later.")
                .setPositiveButton("Deactivate", (dialog, which) -> {
                    deactivateAccount();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deactivateAccount() {
        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            firebaseHelper.deactivateAccount(uid, new FirebaseHelper.DbWriteCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Your account has been deactivated successfully.", Toast.LENGTH_SHORT).show();
                    performLogout();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getContext(), "Error deactivating account: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "No user logged in to deactivate.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account Permanently")
                .setMessage("WARNING: This action is irreversible. All your data will be permanently deleted. Are you absolutely sure you want to delete your account?")
                .setPositiveButton("Delete Permanently", (dialog, which) -> {
                    deleteAccountPermanently();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccountPermanently() {
        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "No user logged in to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uidToDelete = currentUser.getUid();

        // 1. Delete user data from Realtime Database
        firebaseHelper.deleteUserData(uidToDelete, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                // 2. Delete user from Firebase Authentication
                currentUser.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Your account has been permanently deleted.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                if (getActivity() != null) {
                                    getActivity().finish();
                                }
                            } else {
                                Toast.makeText(getContext(), "Failed to delete account from authentication. Please log in again and try.", Toast.LENGTH_LONG).show();
                                performLogout();
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(getContext(), "Failed to delete user data: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        firebaseHelper.signOut();
        Toast.makeText(getContext(), "Logged out successfully!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}