package com.example.tradeup.fragment;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity; // Added for Activity.RESULT_OK
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
import android.widget.Button; // Import Button
import android.widget.ImageView; // Keep ImageView for general use, but CircleImageView for profile
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
import com.example.tradeup.activity.EditProfileActivity; // Ensure this is still needed
import com.example.tradeup.activity.LoginActivity;
import com.example.tradeup.model.User; // Ensure User model is imported
import com.example.tradeup.utils.FirebaseHelper; // Ensure FirebaseHelper is imported
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView; // Import CircleImageView

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
    private TextView textDisplayName, textBio, textEmail, textRatingValue, tvTotalTransactions;
    private CircleImageView imageProfile; // Changed to CircleImageView
    private RatingBar ratingBar;
    private LinearLayout rowEditProfile, rowChangePhoto, rowMyListings, rowTransactionHistory, rowAccountManagement, rowLogout;
    private LinearLayout rowAdminDashboard; // NEW: Admin Dashboard row
    private View dividerAdminDashboard; // NEW: Divider for Admin Dashboard

    // NEW: Buttons for history and analytics
    private Button btnSavedItems, btnOfferHistory, btnPurchaseHistory, btnSalesHistory, btnItemAnalytics;
    private Button btnPaymentHistory; // NEW: Payment History Button
    private Button btnPaymentMethods; // NEW: Payment Methods Button


    private String targetUserId; // For viewing other users' profiles

    // ActivityResultLauncher declarations (from your original code)
    private ActivityResultLauncher<String> pickImageLauncher; // For gallery
    private ActivityResultLauncher<Uri> takePictureLauncher; // For camera
    private Uri cameraImageUri; // URI for camera capture
    private ActivityResultLauncher<String> requestPermissionLauncher; // For permissions

    private NavController navController;
    private FirebaseUser currentUser; // Current logged-in FirebaseUser

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIX: Initialize FirebaseHelper with Context
        firebaseHelper = new FirebaseHelper(requireContext());
        currentUser = FirebaseAuth.getInstance().getCurrentUser(); // Get current user

        // GALLERY (from your original code)
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Log.d("PICKED_IMAGE", "Gallery URI: " + uri.toString());
                        if (isAdded()) {
                            uploadImageToCloudinary(uri);
                        }
                    }
                }
        );

        // CAMERA (from your original code)
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        Log.d("CAMERA_IMAGE", "Captured URI: " + cameraImageUri.toString());
                        if (isAdded()) {
                            uploadImageToCloudinary(cameraImageUri);
                        }
                    }
                }
        );

        // Permission Request Launcher (from your original code)
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isAdded()) {
                        if (isGranted) {
                            // If permission is granted, open gallery or camera as needed
                            // In this case, it's used for Gallery, so keep it.
                            // You might need to refine this if requestPermissionLauncher is used for both camera/gallery
                            // and you need to know which one to open after permission.
                            // For now, assuming it's for gallery as per your original code's usage.
                            openGallery(); // Re-attempt opening gallery after permission
                        } else {
                            Toast.makeText(getContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        textDisplayName = view.findViewById(R.id.text_display_name);
        textBio = view.findViewById(R.id.text_bio);
        textEmail = view.findViewById(R.id.text_email);
        imageProfile = view.findViewById(R.id.image_profile); // Changed to CircleImageView
        ratingBar = view.findViewById(R.id.rating_bar);
        textRatingValue = view.findViewById(R.id.text_rating_value);
        tvTotalTransactions = view.findViewById(R.id.tv_total_transactions);

        rowEditProfile = view.findViewById(R.id.row_edit_profile);
        rowChangePhoto = view.findViewById(R.id.row_change_photo);
        rowMyListings = view.findViewById(R.id.row_my_listings);
        rowTransactionHistory = view.findViewById(R.id.row_transaction_history); // Re-initialized
        rowAccountManagement = view.findViewById(R.id.row_account_management);
        rowLogout = view.findViewById(R.id.row_logout);


        // NEW: Initialize Admin Dashboard row and divider
        rowAdminDashboard = view.findViewById(R.id.row_admin_dashboard);
        dividerAdminDashboard = view.findViewById(R.id.divider_admin_dashboard);

        // NEW: Initialize history and analytics buttons
        btnSavedItems = view.findViewById(R.id.btn_saved_items);
        btnOfferHistory = view.findViewById(R.id.btn_offer_history);
        btnPurchaseHistory = view.findViewById(R.id.btn_purchase_history);
        btnSalesHistory = view.findViewById(R.id.btn_sales_history);
        btnItemAnalytics = view.findViewById(R.id.btn_item_analytics);
        btnPaymentHistory = view.findViewById(R.id.btn_payment_history); // NEW: Initialize Payment History Button
        btnPaymentMethods = view.findViewById(R.id.btn_payment_methods); // NEW: Initialize Payment Methods Button


        // Get targetUserId if viewing another user's profile
        if (getArguments() != null) {
            targetUserId = getArguments().getString("userId");
        }

        // Hide certain rows if viewing another user's profile
        // This logic needs to be careful about currentUser being null if not logged in
        String currentUserId = (currentUser != null) ? currentUser.getUid() : null;
        if (targetUserId != null && !targetUserId.equals(currentUserId)) {
            rowEditProfile.setVisibility(View.GONE);
            rowChangePhoto.setVisibility(View.GONE);
            rowAccountManagement.setVisibility(View.GONE);
            rowLogout.setVisibility(View.GONE);
            // Ensure rowTransactionHistory is also hidden if viewing another user's profile
            if (rowTransactionHistory != null) rowTransactionHistory.setVisibility(View.GONE);


            // Hide Admin Dashboard if viewing another user's profile
            rowAdminDashboard.setVisibility(View.GONE);
            dividerAdminDashboard.setVisibility(View.GONE);

            // NEW: Hide history/analytics buttons if viewing another user's profile
            btnSavedItems.setVisibility(View.GONE);
            btnOfferHistory.setVisibility(View.GONE);
            btnPurchaseHistory.setVisibility(View.GONE);
            btnSalesHistory.setVisibility(View.GONE);
            btnItemAnalytics.setVisibility(View.GONE);
            btnPaymentHistory.setVisibility(View.GONE); // NEW: Hide Payment History Button
            btnPaymentMethods.setVisibility(View.GONE); // NEW: Hide Payment Methods Button
        }

        setupListeners(); // Setup listeners after initViews

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize NavController here, as it requires the view to be created
        navController = Navigation.findNavController(view);
        loadUserProfile(); // Load profile data here to ensure navController is ready
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile(); // Reload profile data on resume to reflect any changes (like new ratings)
    }

    private void loadUserProfile() {
        String uidToLoad = (targetUserId != null) ? targetUserId : (currentUser != null ? currentUser.getUid() : null);

        if (uidToLoad == null) {
            if (isAdded()) {
                Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
            return;
        }

        firebaseHelper.getUserProfile(uidToLoad, new FirebaseHelper.DbReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return; // Ensure fragment is still attached

                if (user != null) {
                    textDisplayName.setText(user.getFirst_name() + " " + user.getLast_name());
                    textBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Chưa có tiểu sử.");
                    if (user.getContact_info() != null && !user.getContact_info().isEmpty()) {
                        textEmail.setText(user.getContact_info());
                    } else {
                        textEmail.setText(user.getEmail());
                    }

                    // Update display for average rating and total transactions (FR-7.2.1)
                    float currentRating = user.getAverage_rating() != null ? user.getAverage_rating().floatValue() : 0.0f;
                    ratingBar.setRating(currentRating);
                    textRatingValue.setText(String.format(Locale.getDefault(), "%.1f / 5", currentRating));

                    Long totalTransactions = user.getTotal_transactions() != null ? user.getTotal_transactions().longValue() : 0L;
                    if (tvTotalTransactions != null) {
                        tvTotalTransactions.setText(String.format(Locale.getDefault(), "Tổng giao dịch: %d", totalTransactions));
                    }

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

                    // NEW: Show Admin Dashboard option if user is admin and is viewing their own profile
                    if (currentUser != null && currentUser.getUid().equals(uidToLoad) && "admin".equalsIgnoreCase(user.getRole())) {
                        rowAdminDashboard.setVisibility(View.VISIBLE);
                        dividerAdminDashboard.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Admin user detected, showing Admin Dashboard option.");
                    } else {
                        rowAdminDashboard.setVisibility(View.GONE);
                        dividerAdminDashboard.setVisibility(View.GONE);
                        Log.d(TAG, "User is not admin or viewing another user's profile, hiding Admin Dashboard option.");
                    }

                } else {
                    Log.w(TAG, "User data is null or fragment is not added.");
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Không thể tải hồ sơ người dùng.", Toast.LENGTH_SHORT).show();
                    }
                    // Fallback to FirebaseUser data if custom user data is null
                    if (targetUserId == null && currentUser != null) {
                        textDisplayName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
                        textEmail.setText(currentUser.getEmail());
                        textBio.setText("No bio information available.");
                        ratingBar.setRating(0.0f);
                        textRatingValue.setText("0.0 / 5");
                        if (tvTotalTransactions != null) tvTotalTransactions.setText("Tổng giao dịch: 0");
                        if (isAdded()) {
                            Glide.with(requireContext())
                                    .load(R.drawable.img_profile_placeholder)
                                    .into(imageProfile);
                        }
                    } else {
                        textDisplayName.setText("Unknown User");
                        textBio.setText("No bio information available.");
                        textEmail.setText("N/A");
                        ratingBar.setRating(0.0f);
                        textRatingValue.setText("0.0 / 5");
                        if (tvTotalTransactions != null) tvTotalTransactions.setText("Tổng giao dịch: 0");
                        if (isAdded()) {
                            Glide.with(requireContext())
                                    .load(R.drawable.img_profile_placeholder)
                                    .into(imageProfile);
                        }
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return; // Ensure fragment is still attached

                Toast.makeText(getContext(), "Error loading profile: " + errorMessage, Toast.LENGTH_SHORT).show();
                // Fallback to FirebaseUser data if custom user data loading fails
                if (targetUserId == null && currentUser != null) {
                    if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                        textDisplayName.setText(currentUser.getDisplayName());
                    } else {
                        textDisplayName.setText(currentUser.getEmail());
                    }
                    textEmail.setText(currentUser.getEmail());
                    textBio.setText("Bio not available.");
                    ratingBar.setRating(0.0f);
                    textRatingValue.setText("0.0 / 5");
                    if (tvTotalTransactions != null) tvTotalTransactions.setText("Tổng giao dịch: 0");

                    if (currentUser.getPhotoUrl() != null) {
                        if (isAdded()) {
                            Glide.with(requireContext())
                                    .load(currentUser.getPhotoUrl())
                                    .placeholder(R.drawable.img_profile_placeholder)
                                    .error(R.drawable.img_profile_placeholder)
                                    .into(imageProfile);
                        }
                    } else {
                        if (isAdded()) {
                            Glide.with(requireContext())
                                    .load(R.drawable.img_profile_placeholder)
                                    .into(imageProfile);
                        }
                    }
                } else {
                    textDisplayName.setText("Error Loading");
                    textBio.setText("Could not load bio.");
                    textEmail.setText("Error");
                    ratingBar.setRating(0.0f);
                    textRatingValue.setText("0.0 / 5");
                    if (tvTotalTransactions != null) tvTotalTransactions.setText("Tổng giao dịch: 0");
                    if (isAdded()) {
                        Glide.with(requireContext())
                                .load(R.drawable.img_profile_placeholder)
                                .into(imageProfile);
                    }
                }
            }
        });
    }

    // --- Image Handling Methods ---
    private void setupListeners() {
        rowEditProfile.setOnClickListener(v -> {
            if (isAdded()) {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                if (textDisplayName.getText() != null) {
                    String[] nameParts = textDisplayName.getText().toString().split(" ", 2);
                    if (nameParts.length > 0) intent.putExtra("firstName", nameParts[0]);
                    if (nameParts.length > 1) intent.putExtra("lastName", nameParts[1]);
                }
                if (textBio.getText() != null) intent.putExtra("bio", textBio.getText().toString());
                if (textEmail.getText() != null) intent.putExtra("contactInfo", textEmail.getText().toString());
                startActivity(intent);
            }
        });

        rowChangePhoto.setOnClickListener(v -> {
            if (isAdded()) {
                showImageSourceDialog();
            }
        });

        rowMyListings.setOnClickListener(v -> {
            if (navController != null && isAdded()) {
                // Navigate to MyItemsFragment, which is the start destination of sell_nav_graph
                navController.navigate(R.id.sell_nav_graph);
            } else {
                Log.e("ProfileFragment", "NavController is null or fragment not added for My Listings navigation.");
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi: Không thể điều hướng. NavController không khả dụng.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Re-added listener for rowTransactionHistory
        rowTransactionHistory.setOnClickListener(v -> {
            if (navController != null && isAdded()) {
                navController.navigate(R.id.action_profileFragment_to_transactionHistoryFragment);
            } else {
                Log.e("ProfileFragment", "NavController is null or fragment not added for Transaction History navigation.");
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi: Không thể điều hướng.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rowAccountManagement.setOnClickListener(v -> {
            if (isAdded()) {
                showAccountManagementOptionsDialog();
            }
        });

        rowLogout.setOnClickListener(v -> {
            if (isAdded()) {
                performLogout();
            }
        });

        // NEW: Admin Dashboard Listener
        rowAdminDashboard.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.action_profileFragment_to_admin_nav_graph); // Navigate to the nested graph
            }
        });

        // NEW: Listeners for history and analytics buttons
        btnSavedItems.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.action_profileFragment_to_savedItemsFragment);
            }
        });

        btnOfferHistory.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.action_profileFragment_to_offerHistoryFragment);
            }
        });

        btnPurchaseHistory.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.action_profileFragment_to_purchaseHistoryFragment);
            }
        });

        btnSalesHistory.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.action_profileFragment_to_salesHistoryFragment);
            }
        });

        // NEW: Payment History Button Listener
        btnPaymentHistory.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.action_profileFragment_to_paymentHistoryFragment);
            }
        });

        // NEW: Payment Methods Button Listener
        btnPaymentMethods.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.action_profileFragment_to_paymentMethodsFragment);
            }
        });

        btnItemAnalytics.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Vui lòng chọn tin đăng để xem phân tích.", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigate(R.id.action_profileFragment_to_myItemsFragment);
            }
        });
    }

    private void showImageSourceDialog() {
        if (!isAdded()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Choose Image Source");
        builder.setItems(new CharSequence[]{"Gallery", "Camera"}, (dialog, which) -> {
            if (which == 0) openGallery();
            else checkCameraPermissionAndOpenCamera();
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (!isAdded()) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        if (!isAdded()) return;
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
            if (isAdded()) {
                Toast.makeText(getContext(), "Unable to create camera URI", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        if (!isAdded()) return;
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
        if (!isAdded()) return;

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
            inputStream.close();

            String mimeType = requireContext().getContentResolver().getType(imageUri);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            String fileExtension = getFileExtensionFromMimeType(mimeType);
            String fileName = "image" + fileExtension;

            OkHttpClient client = new OkHttpClient();
            String cloudName = "dp6tzdsyt";
            String uploadPreset = "TradeUp";

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName,
                            RequestBody.create(imageBytes, MediaType.parse(mimeType)))
                    .addFormDataPart("upload_preset", uploadPreset)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                    Log.e("CloudinaryUpload", "Upload failed", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Upload error: " + response.code() + " " + response.message() + " - " + errorBody, Toast.LENGTH_LONG).show();
                                Log.e("CloudinaryUpload", "Upload error: " + response.code() + " " + response.message() + " - " + errorBody);
                            });
                        }
                        return;
                    }

                    String json = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        String imageUrl = jsonObject.getString("secure_url");

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    updateProfilePictureUrlInDatabase(Objects.requireNonNull(firebaseHelper.getCurrentUser()).getUid(), imageUrl)
                            );
                        }
                    } catch (Exception e) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Error processing Cloudinary response: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                        Log.e("CloudinaryUpload", "Error parsing Cloudinary response", e);
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            if (isAdded()) {
                Toast.makeText(getContext(), "Error reading image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            Log.e("CloudinaryUpload", "Error reading image URI", e);
        }
    }

    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".bin";
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
            default:
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                return extension != null ? "." + extension : ".bin";
        }
    }

    private void updateProfilePictureUrlInDatabase(String uid, String newPhotoUrl) {
        if (!isAdded()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("profile_picture_url", newPhotoUrl);

        firebaseHelper.updateUserProfileFields(uid, updates, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;

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
                                    Log.e("ProfileFragment", "Failed to update Firebase Auth profile photo: " + Objects.requireNonNull(task.getException()).getMessage());
                                }
                            });
                }

                Glide.with(requireContext())
                        .load(newPhotoUrl)
                        .placeholder(R.drawable.img_profile_placeholder)
                        .error(R.drawable.img_profile_placeholder)
                        .into(imageProfile);

                Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to update profile picture: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("ProfileFragment", "Failed to update profile picture in DB: " + errorMessage);
            }
        });
    }

    // --- Account Management Methods ---

    private void showAccountManagementOptionsDialog() {
        if (!isAdded()) return;

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
        if (!isAdded()) return;

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
        if (!isAdded()) return;
        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            firebaseHelper.deactivateAccount(uid, new FirebaseHelper.DbWriteCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Your account has been deactivated successfully.", Toast.LENGTH_SHORT).show();
                    }
                    performLogout();
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error deactivating account: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            if (isAdded()) {
                Toast.makeText(getContext(), "No user logged in to deactivate.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeleteAccountDialog() {
        if (!isAdded()) return;

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
        if (!isAdded()) return;
        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "No user logged in to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uidToDelete = currentUser.getUid();

        firebaseHelper.deleteUserData(uidToDelete, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                currentUser.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Your account has been permanently deleted.", Toast.LENGTH_SHORT).show();
                                }
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                if (getActivity() != null) {
                                    getActivity().finish();
                                }
                            } else {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Failed to delete account from authentication. Please log in again and try.", Toast.LENGTH_LONG).show();
                                }
                                performLogout();
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to delete user data: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void performLogout() {
        if (!isAdded()) return;
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