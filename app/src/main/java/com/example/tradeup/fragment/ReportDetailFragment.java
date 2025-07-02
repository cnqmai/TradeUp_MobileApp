package com.example.tradeup.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.tradeup.R;
import com.example.tradeup.model.Chat; // Import Chat model
import com.example.tradeup.model.Item; // Import Item model
import com.example.tradeup.model.Report;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper; // Assuming you have FirebaseHelper for common Firebase operations
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ReportDetailFragment extends Fragment {

    private static final String TAG = "ReportDetailFragment";

    // UI Elements
    private ImageView ivBackButton;
    private TextView tvReportId, tvReportType, tvReportReason, tvReportStatus, tvReportTimestamp;
    private TextView tvReportedEntityTitle, tvReportedEntityId, tvReportedEntityName, tvReportedEntityStatus;
    private Button btnViewReportedEntity;
    private TextView tvReporterId, tvReporterName;
    private Button btnViewReporterProfile;
    private Button btnDeleteContent, btnBanUser, btnWarnUser, btnMarkResolved;
    private EditText etAdminNotes;
    private Button btnSaveAdminNotes;

    private NavController navController;
    private FirebaseHelper firebaseHelper;

    private String reportId;
    private Report currentReport;

    public ReportDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reportId = getArguments().getString("reportId");
        }
        firebaseHelper = new FirebaseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_detail, container, false);
        initViews(view);
        setupListeners();
        loadReportDetails();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        ivBackButton = view.findViewById(R.id.iv_back_button_report_detail);
        tvReportId = view.findViewById(R.id.tv_detail_report_id);
        tvReportType = view.findViewById(R.id.tv_detail_report_type);
        tvReportReason = view.findViewById(R.id.tv_detail_report_reason);
        tvReportStatus = view.findViewById(R.id.tv_detail_report_status);
        tvReportTimestamp = view.findViewById(R.id.tv_detail_report_timestamp);

        tvReportedEntityTitle = view.findViewById(R.id.tv_reported_entity_title);
        tvReportedEntityId = view.findViewById(R.id.tv_reported_entity_id);
        tvReportedEntityName = view.findViewById(R.id.tv_reported_entity_name);
        tvReportedEntityStatus = view.findViewById(R.id.tv_reported_entity_status);
        btnViewReportedEntity = view.findViewById(R.id.btn_view_reported_entity);

        tvReporterId = view.findViewById(R.id.tv_reporter_id);
        tvReporterName = view.findViewById(R.id.tv_reporter_name);
        btnViewReporterProfile = view.findViewById(R.id.btn_view_reporter_profile);

        btnDeleteContent = view.findViewById(R.id.btn_delete_content);
        btnBanUser = view.findViewById(R.id.btn_ban_user);
        btnWarnUser = view.findViewById(R.id.btn_warn_user);
        btnMarkResolved = view.findViewById(R.id.btn_mark_resolved);

        etAdminNotes = view.findViewById(R.id.et_admin_notes);
        btnSaveAdminNotes = view.findViewById(R.id.btn_save_admin_notes);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });

        btnViewReportedEntity.setOnClickListener(v -> {
            if (currentReport != null && navController != null) {
                // Navigate based on report type
                Bundle bundle = new Bundle();
                String reportType = currentReport.getReport_type(); // Corrected getter
                String reportedObjectId = currentReport.getReported_object_id(); // Corrected getter

                if ("user".equalsIgnoreCase(reportType) && reportedObjectId != null) {
                    bundle.putString("userId", reportedObjectId);
                    navController.navigate(R.id.action_reportDetailFragment_to_userProfileFragment, bundle);
                } else if ("item".equalsIgnoreCase(reportType) && reportedObjectId != null) {
                    bundle.putString("itemId", reportedObjectId);
                    navController.navigate(R.id.action_reportDetailFragment_to_itemDetailFragment, bundle);
                } else if ("chat".equalsIgnoreCase(reportType) && reportedObjectId != null) {
                    bundle.putString("chatId", reportedObjectId);
                    // You might need to pass otherUserId for ChatDetailFragment as well
                    // To get otherUserId for chat, you'd need to fetch chat details first
                    // For now, only passing chatId. If ChatDetailFragment requires otherUserId,
                    // you'll need to fetch it here or in ChatDetailFragment itself.
                    navController.navigate(R.id.action_reportDetailFragment_to_chatDetailFragment, bundle);
                } else {
                    Toast.makeText(getContext(), "Không thể xem chi tiết đối tượng bị báo cáo.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnViewReporterProfile.setOnClickListener(v -> {
            if (currentReport != null && navController != null && currentReport.getReporter_id() != null) { // Corrected getter
                Bundle bundle = new Bundle();
                bundle.putString("userId", currentReport.getReporter_id()); // Corrected getter
                navController.navigate(R.id.action_reportDetailFragment_to_userProfileFragment, bundle);
            } else {
                Toast.makeText(getContext(), "Không thể xem hồ sơ người báo cáo.", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteContent.setOnClickListener(v -> showConfirmationDialog("Xóa nội dung",
                "Bạn có chắc chắn muốn xóa nội dung/tài khoản này? Hành động này không thể hoàn tác.",
                this::deleteReportedContent));
        btnBanUser.setOnClickListener(v -> showConfirmationDialog("Khóa/Tạm khóa người dùng",
                "Bạn có chắc chắn muốn khóa/tạm khóa người dùng này?",
                this::banReportedUser));
        btnWarnUser.setOnClickListener(v -> showConfirmationDialog("Cảnh báo người dùng",
                "Bạn có chắc chắn muốn gửi cảnh báo đến người dùng này?",
                this::warnReportedUser));
        btnMarkResolved.setOnClickListener(v -> showConfirmationDialog("Đánh dấu đã xử lý",
                "Bạn có chắc chắn muốn đánh dấu báo cáo này là đã xử lý?",
                this::markReportAsResolved));

        btnSaveAdminNotes.setOnClickListener(v -> saveAdminNotes());
    }

    private void loadReportDetails() {
        if (reportId == null) {
            Toast.makeText(getContext(), "Không tìm thấy ID báo cáo.", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigateUp();
            }
            return;
        }

        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("reports").child(reportId);
        reportRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    currentReport = snapshot.getValue(Report.class);
                    if (currentReport != null) {
                        currentReport.setReport_id(snapshot.getKey()); // Corrected setter
                        updateUIWithReportDetails();
                        loadReportedEntityDetails();
                        loadReporterDetails();
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy báo cáo.", Toast.LENGTH_SHORT).show();
                        if (navController != null) {
                            navController.navigateUp();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load report details: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải chi tiết báo cáo: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        navController.navigateUp();
                    }
                }
            }
        });
    }

    private void updateUIWithReportDetails() {
        if (currentReport == null) return;

        tvReportId.setText("ID Báo cáo: " + currentReport.getReport_id()); // Corrected getter
        tvReportType.setText("Loại: " + currentReport.getReport_type()); // Corrected getter
        tvReportReason.setText("Lý do: " + currentReport.getReason());
        tvReportStatus.setText("Trạng thái: " + currentReport.getStatus());

        // Use getCreated_at() for timestamp, convert to Long for Date constructor if it's a String
        // If created_at is String (ISO 8601), parse it first
        long timestampMillis = 0;
        try {
            // Assuming created_at is in ISO 8601 format like "2024-07-01T08:00:00Z"
            // You might need a more robust parsing for different formats
            timestampMillis = java.time.OffsetDateTime.parse(currentReport.getCreated_at()).toInstant().toEpochMilli();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing created_at timestamp: " + e.getMessage());
            // Fallback if parsing fails, or handle error appropriately
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(timestampMillis));
        tvReportTimestamp.setText("Thời gian: " + formattedDate);

        // Set admin notes if they exist
        if (currentReport.getAdmin_notes() != null && !currentReport.getAdmin_notes().isEmpty()) { // Corrected getter
            etAdminNotes.setText(currentReport.getAdmin_notes()); // Corrected getter
        } else {
            etAdminNotes.setText("");
        }

        // Adjust visibility and text for action buttons based on report status
        if ("resolved".equalsIgnoreCase(currentReport.getStatus())) {
            btnDeleteContent.setEnabled(false);
            btnBanUser.setEnabled(false);
            btnWarnUser.setEnabled(false);
            btnMarkResolved.setEnabled(false);
            btnMarkResolved.setText("Đã xử lý");
            etAdminNotes.setEnabled(false);
            btnSaveAdminNotes.setEnabled(false);
        } else {
            btnDeleteContent.setEnabled(true);
            btnBanUser.setEnabled(true);
            btnWarnUser.setEnabled(true);
            btnMarkResolved.setEnabled(true);
            btnMarkResolved.setText("Đánh dấu đã xử lý");
            etAdminNotes.setEnabled(true);
            btnSaveAdminNotes.setEnabled(true);
        }
    }

    private void loadReportedEntityDetails() {
        if (currentReport == null) return;

        String type = currentReport.getReport_type(); // Corrected getter
        String entityId = currentReport.getReported_object_id(); // Corrected getter
        String titlePrefix = "";

        switch (type.toLowerCase()) {
            case "user":
                titlePrefix = "Thông tin Người bị báo cáo";
                break;
            case "item":
                titlePrefix = "Thông tin Tin đăng bị báo cáo";
                break;
            case "chat":
                titlePrefix = "Thông tin Cuộc trò chuyện bị báo cáo";
                break;
        }

        tvReportedEntityTitle.setText(titlePrefix);
        if (entityId != null) {
            tvReportedEntityId.setText("ID: " + entityId);
            DatabaseReference entityRef = null;

            if ("user".equalsIgnoreCase(type)) {
                entityRef = FirebaseDatabase.getInstance().getReference("users").child(entityId);
                entityRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                tvReportedEntityName.setText("Tên: " + user.getDisplay_name()); // Corrected getter
                                tvReportedEntityName.setVisibility(View.VISIBLE);
                                tvReportedEntityStatus.setText("Trạng thái: " + (user.getIs_banned() != null && user.getIs_banned() ? "Bị khóa" : "Hoạt động")); // Corrected getter
                                tvReportedEntityStatus.setVisibility(View.VISIBLE);
                            } else {
                                tvReportedEntityName.setVisibility(View.GONE);
                                tvReportedEntityStatus.setVisibility(View.GONE);
                                tvReportedEntityId.setText("ID: " + entityId + " (Không tìm thấy)");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load reported user details: " + error.getMessage());
                        if (isAdded()) {
                            tvReportedEntityName.setVisibility(View.GONE);
                            tvReportedEntityStatus.setVisibility(View.GONE);
                            tvReportedEntityId.setText("ID: " + entityId + " (Lỗi tải)");
                        }
                    }
                });
            } else if ("item".equalsIgnoreCase(type)) {
                entityRef = FirebaseDatabase.getInstance().getReference("items").child(entityId);
                entityRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            Item item = snapshot.getValue(Item.class);
                            if (item != null) {
                                tvReportedEntityName.setText("Tên tin đăng: " + item.getTitle()); // Corrected getter
                                tvReportedEntityName.setVisibility(View.VISIBLE);
                                tvReportedEntityStatus.setText("Trạng thái: " + item.getStatus());
                                tvReportedEntityStatus.setVisibility(View.VISIBLE);
                            } else {
                                tvReportedEntityName.setVisibility(View.GONE);
                                tvReportedEntityStatus.setVisibility(View.GONE);
                                tvReportedEntityId.setText("ID: " + entityId + " (Không tìm thấy)");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load reported item details: " + error.getMessage());
                        if (isAdded()) {
                            tvReportedEntityName.setVisibility(View.GONE);
                            tvReportedEntityStatus.setVisibility(View.GONE);
                            tvReportedEntityId.setText("ID: " + entityId + " (Lỗi tải)");
                        }
                    }
                });
            } else if ("chat".equalsIgnoreCase(type)) {
                entityRef = FirebaseDatabase.getInstance().getReference("chats").child(entityId);
                entityRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            Chat chat = snapshot.getValue(Chat.class);
                            if (chat != null) {
                                // For chat, display participant names if available in Chat model
                                String participant1Name = (chat.getUser_1() != null) ? chat.getUser_1() : "N/A"; // Assuming user_1 is ID
                                String participant2Name = (chat.getUser_2() != null) ? chat.getUser_2() : "N/A"; // Assuming user_2 is ID

                                // Ideally, you'd fetch user names from 'users' node using user_1 and user_2 IDs
                                // For now, just display IDs or a placeholder.
                                tvReportedEntityName.setText("Cuộc trò chuyện giữa: " + participant1Name + " và " + participant2Name);
                                tvReportedEntityName.setVisibility(View.VISIBLE);
                                // Assuming chat has a 'status' field, if not, remove this line
                                tvReportedEntityStatus.setText("Trạng thái: " + (chat.getReported() != null && chat.getReported() ? "Đã báo cáo" : "Bình thường"));
                                tvReportedEntityStatus.setVisibility(View.VISIBLE);
                            } else {
                                tvReportedEntityName.setVisibility(View.GONE);
                                tvReportedEntityStatus.setVisibility(View.GONE);
                                tvReportedEntityId.setText("ID: " + entityId + " (Không tìm thấy)");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load reported chat details: " + error.getMessage());
                        if (isAdded()) {
                            tvReportedEntityName.setVisibility(View.GONE);
                            tvReportedEntityStatus.setVisibility(View.GONE);
                            tvReportedEntityId.setText("ID: " + entityId + " (Lỗi tải)");
                        }
                    }
                });
            }
        } else {
            tvReportedEntityId.setText("ID: Không có");
            tvReportedEntityName.setVisibility(View.GONE);
            tvReportedEntityStatus.setVisibility(View.GONE);
            btnViewReportedEntity.setEnabled(false);
        }
    }

    private void loadReporterDetails() {
        if (currentReport == null || currentReport.getReporter_id() == null) { // Corrected getter
            tvReporterId.setText("ID: Không có");
            tvReporterName.setVisibility(View.GONE);
            btnViewReporterProfile.setEnabled(false);
            return;
        }

        String reporterId = currentReport.getReporter_id(); // Corrected getter
        tvReporterId.setText("ID: " + reporterId);

        DatabaseReference reporterRef = FirebaseDatabase.getInstance().getReference("users").child(reporterId);
        reporterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    User reporter = snapshot.getValue(User.class);
                    if (reporter != null) {
                        tvReporterName.setText("Tên: " + reporter.getDisplay_name()); // Corrected getter
                        tvReporterName.setVisibility(View.VISIBLE);
                    } else {
                        tvReporterName.setVisibility(View.GONE);
                        tvReporterId.setText("ID: " + reporterId + " (Không tìm thấy)");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load reporter details: " + error.getMessage());
                if (isAdded()) {
                    tvReporterName.setVisibility(View.GONE);
                    tvReporterId.setText("ID: " + reporterId + " (Lỗi tải)");
                }
            }
        });
    }

    private void showConfirmationDialog(String title, String message, Runnable action) {
        if (!isAdded()) return; // Ensure fragment is attached
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> action.run())
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteReportedContent() {
        if (currentReport == null) return;

        String type = currentReport.getReport_type(); // Corrected getter
        String entityId = currentReport.getReported_object_id(); // Corrected getter

        if (entityId == null) {
            Toast.makeText(getContext(), "Không tìm thấy ID đối tượng để xóa.", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (type.toLowerCase()) {
            case "user":
                // Deleting user account from Firebase Authentication is a server-side operation
                // or requires the user to be currently logged in.
                // Here, we'll just update their account_status to "deleted" and mark them banned.
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(entityId);
                Map<String, Object> userUpdates = new HashMap<>();
                userUpdates.put("account_status", "deleted");
                userUpdates.put("is_banned", true); // Mark as banned
                userRef.updateChildren(userUpdates)
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Đã đánh dấu tài khoản người dùng là đã xóa và bị khóa.", Toast.LENGTH_SHORT).show();
                                updateReportStatus("resolved", "Tài khoản đã bị đánh dấu là đã xóa.");
                                recordAdminLog("DELETE_USER_DATA", entityId); // Log action
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Lỗi khi đánh dấu tài khoản là đã xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case "item":
                DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("items").child(entityId);
                itemRef.removeValue()
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Đã xóa tin đăng.", Toast.LENGTH_SHORT).show();
                                updateReportStatus("resolved", "Tin đăng đã bị xóa.");
                                recordAdminLog("DELETE_ITEM", entityId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Lỗi khi xóa tin đăng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case "chat":
                DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(entityId);
                DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(entityId);
                chatRef.removeValue()
                        .addOnSuccessListener(aVoid -> {
                            messagesRef.removeValue() // Also delete associated messages
                                    .addOnSuccessListener(aVoid2 -> {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), "Đã xóa cuộc trò chuyện và tin nhắn liên quan.", Toast.LENGTH_SHORT).show();
                                            updateReportStatus("resolved", "Cuộc trò chuyện đã bị xóa.");
                                            recordAdminLog("DELETE_CHAT", entityId);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), "Lỗi khi xóa tin nhắn cuộc trò chuyện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Lỗi khi xóa cuộc trò chuyện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            default:
                Toast.makeText(getContext(), "Loại báo cáo không hợp lệ để xóa nội dung.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void banReportedUser() {
        if (currentReport == null || currentReport.getReported_object_id() == null) { // Corrected getter
            Toast.makeText(getContext(), "Không có người dùng để khóa.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!"user".equalsIgnoreCase(currentReport.getReport_type())) { // Corrected getter
            Toast.makeText(getContext(), "Hành động khóa chỉ áp dụng cho báo cáo người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userIdToBan = currentReport.getReported_object_id(); // Corrected getter
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userIdToBan);
        userRef.child("is_banned").setValue(true) // Corrected setter for is_banned
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Đã khóa người dùng: " + userIdToBan, Toast.LENGTH_SHORT).show();
                        updateReportStatus("resolved", "Người dùng đã bị khóa.");
                        recordAdminLog("BAN_USER", userIdToBan);
                        // Optionally, force logout the banned user
                        // firebaseHelper.forceSignOutUser(userIdToBan); // Requires cloud functions or admin SDK
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi khi khóa người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void warnReportedUser() {
        if (currentReport == null || currentReport.getReported_object_id() == null) { // Corrected getter
            Toast.makeText(getContext(), "Không có người dùng để cảnh báo.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!"user".equalsIgnoreCase(currentReport.getReport_type())) { // Corrected getter
            Toast.makeText(getContext(), "Hành động cảnh báo chỉ áp dụng cho báo cáo người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userIdToWarn = currentReport.getReported_object_id(); // Corrected getter
        // Here you would implement logic to send a warning notification/message to the user.
        // For simplicity, we'll just log it and update report status.
        if (isAdded()) {
            Toast.makeText(getContext(), "Đã gửi cảnh báo đến người dùng: " + userIdToWarn, Toast.LENGTH_SHORT).show();
            updateReportStatus("resolved", "Người dùng đã được cảnh báo.");
            recordAdminLog("WARN_USER", userIdToWarn);
        }
    }

    private void markReportAsResolved() {
        if (currentReport == null) return;
        updateReportStatus("resolved", "Báo cáo đã được đánh dấu là đã xử lý.");
        recordAdminLog("MARK_REPORT_RESOLVED", currentReport.getReport_id()); // Corrected getter
    }

    private void updateReportStatus(String newStatus, String toastMessage) {
        if (currentReport == null || currentReport.getReport_id() == null) return; // Corrected getter

        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("reports").child(currentReport.getReport_id()); // Corrected getter
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("resolved_timestamp", ServerValue.TIMESTAMP); // NEW: Use resolved_timestamp
        updates.put("admin_id", firebaseHelper.getCurrentUser()); // Store admin who resolved it

        reportRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();
                        // UI will be updated by the ValueEventListener on currentReport
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi khi cập nhật trạng thái báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAdminNotes() {
        if (currentReport == null || currentReport.getReport_id() == null) return; // Corrected getter

        String notes = etAdminNotes.getText().toString().trim();
        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("reports").child(currentReport.getReport_id()); // Corrected getter
        reportRef.child("admin_notes").setValue(notes) // Corrected field name
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Đã lưu ghi chú quản trị viên.", Toast.LENGTH_SHORT).show();
                        recordAdminLog("SAVE_ADMIN_NOTES", currentReport.getReport_id()); // Corrected getter
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi khi lưu ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void recordAdminLog(String action, String targetId) {
        DatabaseReference adminLogsRef = FirebaseDatabase.getInstance().getReference("admin_logs");
        String logId = adminLogsRef.push().getKey();
        if (logId != null) {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("action", action);
            logEntry.put("admin_id", firebaseHelper.getCurrentUser()); // Current logged-in admin
            logEntry.put("target_id", targetId);
            logEntry.put("report_id", currentReport != null ? currentReport.getReport_id() : "N/A"); // Corrected getter
            logEntry.put("timestamp", ServerValue.TIMESTAMP); // Use Firebase ServerValue.TIMESTAMP

            adminLogsRef.child(logId).setValue(logEntry)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Admin log recorded: " + action + " for " + targetId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to record admin log: " + e.getMessage()));
        }
    }
}
