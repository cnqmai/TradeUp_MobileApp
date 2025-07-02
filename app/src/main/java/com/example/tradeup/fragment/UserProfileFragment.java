package com.example.tradeup.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton; // NEW: For report dialog
import android.widget.RadioGroup; // NEW: For report dialog
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // For AlertDialog
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.activity.LoginActivity; // Assuming this is still needed for logout/redirect
import com.example.tradeup.model.Report; // NEW: Import Report model
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputEditText; // NEW: For report comment input
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference; // For Firebase Database operations
import com.google.firebase.database.FirebaseDatabase; // For Firebase Database instance

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    // Views from the layout
    private ImageView ivBackButton; // NEW: Back button in toolbar
    private ImageView ivReportBlockButton; // NEW: Report/Block button in toolbar
    private CircleImageView imageUserProfile;
    private TextView textUserDisplayName, textUserRatingValue, textUserBio, textUserEmail, tvUserTotalTransactions;
    private RatingBar ratingBarUserProfile;
    private Button btnChatUser, btnViewUserListings;

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private FirebaseUser currentUser; // Current logged-in user
    private String targetUserId; // ID of the user whose profile is being viewed
    private User targetUser; // Object to hold the fetched user profile

    public UserProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (getArguments() != null) {
            targetUserId = getArguments().getString("userId");
            Log.d(TAG, "Received targetUserId: " + targetUserId);
        } else {
            Log.e(TAG, "No userId received in arguments for UserProfileFragment!");
            if (isAdded()) {
                Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID người dùng.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);
        initViews(view);
        setupListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view); // Initialize NavController
        loadUserProfile(); // Load profile details after NavController is ready
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void initViews(View view) {
        ivBackButton = view.findViewById(R.id.iv_back_button_user_profile);
        ivReportBlockButton = view.findViewById(R.id.iv_report_block_button_user_profile);
        imageUserProfile = view.findViewById(R.id.image_user_profile);
        textUserDisplayName = view.findViewById(R.id.text_user_display_name);
        textUserRatingValue = view.findViewById(R.id.text_user_rating_value);
        ratingBarUserProfile = view.findViewById(R.id.rating_bar_user_profile);
        textUserBio = view.findViewById(R.id.text_user_bio);
        textUserEmail = view.findViewById(R.id.text_user_email);
        tvUserTotalTransactions = view.findViewById(R.id.tv_user_total_transactions);
        btnChatUser = view.findViewById(R.id.btn_chat_user);
        btnViewUserListings = view.findViewById(R.id.btn_view_user_listings);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });

        // UPDATED: Report/Block button click listener to show choices
        ivReportBlockButton.setOnClickListener(v -> {
            if (isAdded() && targetUser != null && currentUser != null) {
                // Prevent reporting/blocking self
                if (currentUser.getUid().equals(targetUserId)) {
                    Toast.makeText(getContext(), "Bạn không thể báo cáo hoặc chặn hồ sơ của chính mình.", Toast.LENGTH_SHORT).show();
                } else {
                    showReportBlockOptionsDialog(targetUserId);
                }
            } else {
                Log.w(TAG, "Report/Block button clicked but fragment not added, targetUser is null, or currentUser is null.");
                if (isAdded()) {
                    Toast.makeText(getContext(), "Không thể thực hiện hành động lúc này. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnChatUser.setOnClickListener(v -> {
            if (isAdded() && targetUser != null && currentUser != null) {
                if (currentUser.getUid().equals(targetUserId)) {
                    Toast.makeText(getContext(), "Bạn không thể trò chuyện với chính mình.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putString("otherUserId", targetUserId);
                if (navController != null) {
                    navController.navigate(R.id.action_userProfileFragment_to_chatDetailFragment, bundle);
                }
            } else {
                Log.w(TAG, "Chat button clicked but targetUser or currentUser is null.");
                if (isAdded()) {
                    Toast.makeText(getContext(), "Không thể bắt đầu trò chuyện. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnViewUserListings.setOnClickListener(v -> {
            if (isAdded() && targetUser != null) {
                Bundle bundle = new Bundle();
                bundle.putString("userId", targetUserId);
                if (navController != null) {
                    navController.navigate(R.id.action_userProfileFragment_to_userListingsFragment, bundle);
                }
            } else {
                Log.w(TAG, "View Listings button clicked but targetUser is null.");
                if (isAdded()) {
                    Toast.makeText(getContext(), "Không thể xem tin đăng. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadUserProfile() {
        if (targetUserId == null) {
            Log.e(TAG, "Target User ID is null, cannot load profile.");
            if (isAdded()) {
                Toast.makeText(getContext(), "Lỗi: ID người dùng không hợp lệ.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        firebaseHelper.getUserProfile(targetUserId, new FirebaseHelper.DbReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;

                if (user != null) {
                    targetUser = user;
                    textUserDisplayName.setText(user.getFirst_name() + " " + user.getLast_name());
                    textUserBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Chưa có tiểu sử.");
                    textUserEmail.setText(user.getContact_info() != null && !user.getContact_info().isEmpty() ? user.getContact_info() : user.getEmail());

                    float currentRating = user.getAverage_rating() != null ? user.getAverage_rating().floatValue() : 0.0f;
                    ratingBarUserProfile.setRating(currentRating);
                    textUserRatingValue.setText(String.format(Locale.getDefault(), "%.1f / 5", currentRating));

                    Long totalTransactions = user.getTotal_transactions() != null ? user.getTotal_transactions().longValue() : 0L;
                    if (tvUserTotalTransactions != null) {
                        tvUserTotalTransactions.setText(String.format(Locale.getDefault(), "Tổng giao dịch: %d", totalTransactions));
                    }

                    if (user.getProfile_picture_url() != null && !user.getProfile_picture_url().isEmpty()) {
                        Glide.with(requireContext())
                                .load(user.getProfile_picture_url())
                                .placeholder(R.drawable.img_profile_placeholder)
                                .error(R.drawable.img_profile_placeholder)
                                .into(imageUserProfile);
                    } else {
                        imageUserProfile.setImageResource(R.drawable.img_profile_placeholder);
                    }

                    // Hide report/block button if viewing own profile
                    if (currentUser != null && currentUser.getUid().equals(targetUserId)) {
                        ivReportBlockButton.setVisibility(View.GONE);
                        Log.d(TAG, "Viewing own profile, hiding report/block button.");
                    } else {
                        ivReportBlockButton.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Viewing another user's profile, showing report/block button.");
                    }

                } else {
                    Log.w(TAG, "User data is null for targetUserId: " + targetUserId);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Không thể tải hồ sơ người dùng: Dữ liệu trống.", Toast.LENGTH_SHORT).show();
                    }
                    textUserDisplayName.setText("Người dùng không xác định");
                    textUserBio.setText("N/A");
                    textUserEmail.setText("N/A");
                    ratingBarUserProfile.setRating(0.0f);
                    textUserRatingValue.setText("N/A");
                    tvUserTotalTransactions.setText("Tổng giao dịch: 0");
                    imageUserProfile.setImageResource(R.drawable.img_profile_placeholder);
                    ivReportBlockButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load user profile: " + errorMessage);
                Toast.makeText(getContext(), "Lỗi tải hồ sơ: " + errorMessage, Toast.LENGTH_SHORT).show();
                textUserDisplayName.setText("Lỗi tải hồ sơ");
                textUserBio.setText("N/A");
                textUserEmail.setText("N/A");
                ratingBarUserProfile.setRating(0.0f);
                textUserRatingValue.setText("N/A");
                tvUserTotalTransactions.setText("Tổng giao dịch: 0");
                imageUserProfile.setImageResource(R.drawable.img_profile_placeholder);
                ivReportBlockButton.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Displays a dialog with options to Report or Block the user.
     * @param userId The ID of the user to report/block.
     */
    private void showReportBlockOptionsDialog(String userId) {
        if (!isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Tùy chọn người dùng");
        builder.setItems(new CharSequence[]{"Báo cáo người dùng", "Chặn người dùng"}, (dialog, which) -> {
            if (which == 0) { // Report User
                showReportDialog("user", userId);
            } else if (which == 1) { // Block User
                showBlockUserDialog(userId);
            }
        });
        builder.show();
    }

    /**
     * Displays the report dialog for a given object.
     * @param reportType The type of object being reported ("item", "user", "chat").
     * @param reportedObjectId The ID of the object being reported.
     */
    private void showReportDialog(String reportType, String reportedObjectId) {
        if (!isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_reason, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_report_dialog_title);
        RadioGroup rgReasons = dialogView.findViewById(R.id.rg_report_reasons);
        TextInputEditText etComment = dialogView.findViewById(R.id.et_report_comment);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_report);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit_report);

        String title = "";
        switch (reportType) {
            case "item":
                title = "Báo cáo tin đăng";
                break;
            case "user":
                title = "Báo cáo người dùng";
                break;
            case "chat":
                title = "Báo cáo trò chuyện";
                break;
            default:
                title = "Báo cáo";
                break;
        }
        tvTitle.setText(title);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            int selectedId = rgReasons.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(getContext(), "Vui lòng chọn một lý do báo cáo.", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
            String reason = selectedRadioButton.getText().toString();
            String comment = etComment.getText().toString().trim();

            if (reason.equals("Khác (vui lòng mô tả)") && comment.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng mô tả lý do khác.", Toast.LENGTH_SHORT).show();
                return;
            }

            String reporterId = currentUser != null ? currentUser.getUid() : "anonymous";
            Report report = new Report(reporterId, reportedObjectId, reportType, reason, comment);

            saveReportToFirebase(report);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Saves the Report object to Firebase Realtime Database.
     * @param report The Report object to save.
     */
    private void saveReportToFirebase(Report report) {
        if (!isAdded()) return;

        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        String reportId = reportsRef.push().getKey();

        if (reportId != null) {
            report.setReport_id(reportId);
            reportsRef.child(reportId).setValue(report)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Báo cáo của bạn đã được gửi thành công.", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Report submitted: " + reportId + " for " + report.getReported_object_id());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Lỗi khi gửi báo cáo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Failed to submit report: " + e.getMessage());
                        }
                    });
        } else {
            if (isAdded()) {
                Toast.makeText(getContext(), "Lỗi: Không thể tạo ID báo cáo.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Displays a confirmation dialog for blocking a user.
     * @param userIdToBlock The ID of the user to block.
     */
    private void showBlockUserDialog(String userIdToBlock) {
        if (!isAdded()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Chặn người dùng")
                .setMessage("Bạn có chắc chắn muốn chặn người dùng này không? Bạn sẽ không thể xem tin đăng của họ, trò chuyện với họ, hoặc nhận đề nghị từ họ.")
                .setPositiveButton("Chặn", (dialog, which) -> {
                    blockUser(userIdToBlock);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Blocks a user by adding their ID to the current user's blocked list in Firebase.
     * @param userIdToBlock The ID of the user to block.
     */
    private void blockUser(String userIdToBlock) {
        if (!isAdded() || currentUser == null || userIdToBlock == null || userIdToBlock.isEmpty()) {
            Log.w(TAG, "Cannot block user: Fragment not added, current user null, or target user ID invalid.");
            if (isAdded()) {
                Toast.makeText(getContext(), "Không thể chặn người dùng lúc này.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DatabaseReference blockedUsersRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid())
                .child("blocked_users")
                .child(userIdToBlock);

        blockedUsersRef.setValue(true) // Set value to true to mark as blocked
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Đã chặn người dùng thành công.", Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "User " + userIdToBlock + " blocked by " + currentUser.getUid());

                    // Optionally, you might want to:
                    // 1. Close the current profile view or navigate away
                    // 2. Remove any existing chat history with this user (complex, might need server-side)
                    // 3. Prevent future interactions (handled by checking blocked_users list in other features)
                    if (navController != null) {
                        navController.navigateUp(); // Go back after blocking
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi khi chặn người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Failed to block user " + userIdToBlock + ": " + e.getMessage());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");

        ivBackButton = null;
        ivReportBlockButton = null;
        imageUserProfile = null;
        textUserDisplayName = null;
        textUserRatingValue = null;
        ratingBarUserProfile = null;
        textUserBio = null;
        textUserEmail = null;
        tvUserTotalTransactions = null;
        btnChatUser = null;
        btnViewUserListings = null;
        targetUser = null;
    }
}
