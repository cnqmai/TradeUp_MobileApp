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
import com.example.tradeup.model.Chat;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Report;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
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
                String reportType = currentReport.getReport_type();
                String reportedObjectId = currentReport.getReported_object_id();

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
                    Toast.makeText(getContext(), getString(R.string.toast_cannot_view_reported_entity_details), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnViewReporterProfile.setOnClickListener(v -> {
            if (currentReport != null && navController != null && currentReport.getReporter_id() != null) {
                Bundle bundle = new Bundle();
                bundle.putString("userId", currentReport.getReporter_id());
                navController.navigate(R.id.action_reportDetailFragment_to_userProfileFragment, bundle);
            } else {
                Toast.makeText(getContext(), getString(R.string.toast_cannot_view_reporter_profile), Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteContent.setOnClickListener(v -> showConfirmationDialog(getString(R.string.dialog_title_delete_content),
                getString(R.string.dialog_message_delete_content),
                this::deleteReportedContent));
        btnBanUser.setOnClickListener(v -> showConfirmationDialog(getString(R.string.dialog_title_ban_user),
                getString(R.string.dialog_message_ban_user),
                this::banReportedUser));
        btnWarnUser.setOnClickListener(v -> showConfirmationDialog(getString(R.string.dialog_title_warn_user),
                getString(R.string.dialog_message_warn_user),
                this::warnReportedUser));
        btnMarkResolved.setOnClickListener(v -> showConfirmationDialog(getString(R.string.dialog_title_mark_resolved),
                getString(R.string.dialog_message_mark_resolved),
                this::markReportAsResolved));

        btnSaveAdminNotes.setOnClickListener(v -> saveAdminNotes());
    }

    private void loadReportDetails() {
        if (reportId == null) {
            Toast.makeText(getContext(), getString(R.string.toast_report_id_not_found), Toast.LENGTH_SHORT).show();
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
                        currentReport.setReport_id(snapshot.getKey());
                        updateUIWithReportDetails();
                        loadReportedEntityDetails();
                        loadReporterDetails();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.toast_report_not_found), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), getString(R.string.toast_error_loading_report_details) + error.getMessage(), Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        navController.navigateUp();
                    }
                }
            }
        });
    }

    private void updateUIWithReportDetails() {
        if (currentReport == null) return;

        tvReportId.setText(getString(R.string.report_id_label) + " " + currentReport.getReport_id());
        tvReportType.setText(getString(R.string.report_type_label) + " " + currentReport.getReport_type());
        tvReportReason.setText(getString(R.string.report_reason_label) + " " + currentReport.getReason());
        tvReportStatus.setText(getString(R.string.report_status_label) + " " + currentReport.getStatus());

        long timestampMillis = 0;
        try {
            timestampMillis = java.time.OffsetDateTime.parse(currentReport.getCreated_at()).toInstant().toEpochMilli();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing created_at timestamp: " + e.getMessage());
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(timestampMillis));
        tvReportTimestamp.setText(getString(R.string.report_timestamp_label) + " " + formattedDate);

        if (currentReport.getAdmin_notes() != null && !currentReport.getAdmin_notes().isEmpty()) {
            etAdminNotes.setText(currentReport.getAdmin_notes());
        } else {
            etAdminNotes.setText("");
        }

        if ("resolved".equalsIgnoreCase(currentReport.getStatus())) {
            btnDeleteContent.setEnabled(false);
            btnBanUser.setEnabled(false);
            btnWarnUser.setEnabled(false);
            btnMarkResolved.setEnabled(false);
            btnMarkResolved.setText(getString(R.string.button_text_resolved));
            etAdminNotes.setEnabled(false);
            btnSaveAdminNotes.setEnabled(false);
        } else {
            btnDeleteContent.setEnabled(true);
            btnBanUser.setEnabled(true);
            btnWarnUser.setEnabled(true);
            btnMarkResolved.setEnabled(true);
            btnMarkResolved.setText(getString(R.string.button_text_mark_as_resolved));
            etAdminNotes.setEnabled(true);
            btnSaveAdminNotes.setEnabled(true);
        }
    }

    private void loadReportedEntityDetails() {
        if (currentReport == null) return;

        String type = currentReport.getReport_type();
        String entityId = currentReport.getReported_object_id();
        String titlePrefix = "";

        switch (type.toLowerCase()) {
            case "user":
                titlePrefix = getString(R.string.reported_entity_card_title_user);
                break;
            case "item":
                titlePrefix = getString(R.string.reported_entity_card_title_item);
                break;
            case "chat":
                titlePrefix = getString(R.string.reported_entity_card_title_chat);
                break;
        }

        tvReportedEntityTitle.setText(titlePrefix);
        if (entityId != null) {
            tvReportedEntityId.setText(getString(R.string.reported_entity_id_label) + " " + entityId);
            DatabaseReference entityRef = null;

            if ("user".equalsIgnoreCase(type)) {
                entityRef = FirebaseDatabase.getInstance().getReference("users").child(entityId);
                entityRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                tvReportedEntityName.setText(getString(R.string.reported_entity_name_label) + " " + user.getDisplay_name());
                                tvReportedEntityName.setVisibility(View.VISIBLE);
                                tvReportedEntityStatus.setText(getString(R.string.reported_entity_status_label) + " " + (user.getIs_banned() != null && user.getIs_banned() ? getString(R.string.status_banned) : getString(R.string.status_active)));
                                tvReportedEntityStatus.setVisibility(View.VISIBLE);
                            } else {
                                tvReportedEntityName.setVisibility(View.GONE);
                                tvReportedEntityStatus.setVisibility(View.GONE);
                                tvReportedEntityId.setText(getString(R.string.reported_entity_id_label) + " " + entityId + " (" + getString(R.string.not_found) + ")");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load reported user details: " + error.getMessage());
                        if (isAdded()) {
                            tvReportedEntityName.setVisibility(View.GONE);
                            tvReportedEntityStatus.setVisibility(View.GONE);
                            tvReportedEntityId.setText(getString(R.string.reported_entity_id_label) + " " + entityId + " (" + getString(R.string.error_loading) + ")");
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
                                tvReportedEntityName.setText(getString(R.string.reported_item_title_label) + " " + item.getTitle());
                                tvReportedEntityName.setVisibility(View.VISIBLE);
                                tvReportedEntityStatus.setText(getString(R.string.reported_entity_status_label) + " " + item.getStatus());
                                tvReportedEntityStatus.setVisibility(View.VISIBLE);
                            } else {
                                tvReportedEntityName.setVisibility(View.GONE);
                                tvReportedEntityStatus.setVisibility(View.GONE);
                                tvReportedEntityId.setText(getString(R.string.reported_entity_id_label) + " " + entityId + " (" + getString(R.string.not_found) + ")");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load reported item details: " + error.getMessage());
                        if (isAdded()) {
                            tvReportedEntityName.setVisibility(View.GONE);
                            tvReportedEntityStatus.setVisibility(View.GONE);
                            tvReportedEntityId.setText(getString(R.string.reported_entity_id_label) + " " + entityId + " (" + getString(R.string.error_loading) + ")");
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
                                String participant1Name = (chat.getUser_1() != null) ? chat.getUser_1() : getString(R.string.not_applicable_short);
                                String participant2Name = (chat.getUser_2() != null) ? chat.getUser_2() : getString(R.string.not_applicable_short);

                                tvReportedEntityName.setText(getString(R.string.reported_chat_participants_label) + " " + participant1Name + " " + getString(R.string.and) + " " + participant2Name);
                                tvReportedEntityName.setVisibility(View.VISIBLE);
                                tvReportedEntityStatus.setText(getString(R.string.reported_entity_status_label) + " " + (chat.getReported() != null && chat.getReported() ? getString(R.string.status_reported) : getString(R.string.status_normal)));
                                tvReportedEntityStatus.setVisibility(View.VISIBLE);
                            } else {
                                tvReportedEntityName.setVisibility(View.GONE);
                                tvReportedEntityStatus.setVisibility(View.GONE);
                                tvReportedEntityId.setText(getString(R.string.reported_entity_id_label) + " " + entityId + " (" + getString(R.string.not_found) + ")");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load reported chat details: " + error.getMessage());
                        if (isAdded()) {
                            tvReportedEntityName.setVisibility(View.GONE);
                            tvReportedEntityStatus.setVisibility(View.GONE);
                            tvReportedEntityId.setText(getString(R.string.reported_entity_id_label) + " " + entityId + " (" + getString(R.string.error_loading) + ")");
                        }
                    }
                });
            }
        } else {
            tvReportedEntityId.setText(getString(R.string.reported_entity_id_label) + " " + getString(R.string.not_available));
            tvReportedEntityName.setVisibility(View.GONE);
            tvReportedEntityStatus.setVisibility(View.GONE);
            btnViewReportedEntity.setEnabled(false);
        }
    }

    private void loadReporterDetails() {
        if (currentReport == null || currentReport.getReporter_id() == null) {
            tvReporterId.setText(getString(R.string.reporter_id_label) + " " + getString(R.string.not_available));
            tvReporterName.setVisibility(View.GONE);
            btnViewReporterProfile.setEnabled(false);
            return;
        }

        String reporterId = currentReport.getReporter_id();
        tvReporterId.setText(getString(R.string.reporter_id_label) + " " + reporterId);

        DatabaseReference reporterRef = FirebaseDatabase.getInstance().getReference("users").child(reporterId);
        reporterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    User reporter = snapshot.getValue(User.class);
                    if (reporter != null) {
                        tvReporterName.setText(getString(R.string.reporter_name_label) + " " + reporter.getDisplay_name());
                        tvReporterName.setVisibility(View.VISIBLE);
                    } else {
                        tvReporterName.setVisibility(View.GONE);
                        tvReporterId.setText(getString(R.string.reporter_id_label) + " " + reporterId + " (" + getString(R.string.not_found) + ")");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load reporter details: " + error.getMessage());
                if (isAdded()) {
                    tvReporterName.setVisibility(View.GONE);
                    tvReporterId.setText(getString(R.string.reporter_id_label) + " " + reporterId + " (" + getString(R.string.error_loading) + ")");
                }
            }
        });
    }

    private void showConfirmationDialog(String title, String message, Runnable action) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_confirm_button), (dialog, which) -> action.run())
                .setNegativeButton(getString(R.string.dialog_cancel_button), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteReportedContent() {
        if (currentReport == null) return;

        String type = currentReport.getReport_type();
        String entityId = currentReport.getReported_object_id();

        if (entityId == null) {
            Toast.makeText(getContext(), getString(R.string.toast_no_object_id_to_delete), Toast.LENGTH_SHORT).show();
            return;
        }

        switch (type.toLowerCase()) {
            case "user":
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(entityId);
                Map<String, Object> userUpdates = new HashMap<>();
                userUpdates.put("account_status", "deleted");
                userUpdates.put("is_banned", true);
                userRef.updateChildren(userUpdates)
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.toast_user_account_marked_deleted_banned), Toast.LENGTH_SHORT).show();
                                updateReportStatus("resolved", getString(R.string.report_status_user_account_marked_deleted));
                                recordAdminLog("DELETE_USER_DATA", entityId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.toast_error_marking_user_account_deleted) + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case "item":
                DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("items").child(entityId);
                itemRef.removeValue()
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.toast_item_deleted), Toast.LENGTH_SHORT).show();
                                updateReportStatus("resolved", getString(R.string.report_status_item_deleted));
                                recordAdminLog("DELETE_ITEM", entityId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.toast_error_deleting_item) + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case "chat":
                DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(entityId);
                DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(entityId);
                chatRef.removeValue()
                        .addOnSuccessListener(aVoid -> {
                            messagesRef.removeValue()
                                    .addOnSuccessListener(aVoid2 -> {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), getString(R.string.toast_chat_and_messages_deleted), Toast.LENGTH_SHORT).show();
                                            updateReportStatus("resolved", getString(R.string.report_status_chat_deleted));
                                            recordAdminLog("DELETE_CHAT", entityId);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), getString(R.string.toast_error_deleting_chat_messages) + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.toast_error_deleting_chat) + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            default:
                Toast.makeText(getContext(), getString(R.string.toast_invalid_report_type_for_deletion), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void banReportedUser() {
        if (currentReport == null || currentReport.getReported_object_id() == null) {
            Toast.makeText(getContext(), getString(R.string.toast_no_user_to_ban), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!"user".equalsIgnoreCase(currentReport.getReport_type())) {
            Toast.makeText(getContext(), getString(R.string.toast_ban_action_only_for_user_reports), Toast.LENGTH_SHORT).show();
            return;
        }

        String userIdToBan = currentReport.getReported_object_id();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userIdToBan);
        userRef.child("is_banned").setValue(true)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.toast_user_banned) + " " + userIdToBan, Toast.LENGTH_SHORT).show();
                        updateReportStatus("resolved", getString(R.string.report_status_user_banned));
                        recordAdminLog("BAN_USER", userIdToBan);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.toast_error_banning_user) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void warnReportedUser() {
        if (currentReport == null || currentReport.getReported_object_id() == null) {
            Toast.makeText(getContext(), getString(R.string.toast_no_user_to_warn), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!"user".equalsIgnoreCase(currentReport.getReport_type())) {
            Toast.makeText(getContext(), getString(R.string.toast_warn_action_only_for_user_reports), Toast.LENGTH_SHORT).show();
            return;
        }

        String userIdToWarn = currentReport.getReported_object_id();
        if (isAdded()) {
            Toast.makeText(getContext(), getString(R.string.toast_warning_sent_to_user) + " " + userIdToWarn, Toast.LENGTH_SHORT).show();
            updateReportStatus("resolved", getString(R.string.report_status_user_warned));
            recordAdminLog("WARN_USER", userIdToWarn);
        }
    }

    private void markReportAsResolved() {
        if (currentReport == null) return;
        updateReportStatus("resolved", getString(R.string.report_status_marked_resolved));
        recordAdminLog("MARK_REPORT_RESOLVED", currentReport.getReport_id());
    }

    private void updateReportStatus(String newStatus, String toastMessage) {
        if (currentReport == null || currentReport.getReport_id() == null) return;

        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("reports").child(currentReport.getReport_id());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("resolved_timestamp", ServerValue.TIMESTAMP);
        updates.put("admin_id", firebaseHelper.getCurrentUser());

        reportRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.toast_error_updating_report_status) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAdminNotes() {
        if (currentReport == null || currentReport.getReport_id() == null) return;

        String notes = etAdminNotes.getText().toString().trim();
        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("reports").child(currentReport.getReport_id());
        reportRef.child("admin_notes").setValue(notes)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.toast_admin_notes_saved), Toast.LENGTH_SHORT).show();
                        recordAdminLog("SAVE_ADMIN_NOTES", currentReport.getReport_id());
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.toast_error_saving_notes) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void recordAdminLog(String action, String targetId) {
        DatabaseReference adminLogsRef = FirebaseDatabase.getInstance().getReference("admin_logs");
        String logId = adminLogsRef.push().getKey();
        if (logId != null) {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("action", action);
            logEntry.put("admin_id", firebaseHelper.getCurrentUser());
            logEntry.put("target_id", targetId);
            logEntry.put("report_id", currentReport != null ? currentReport.getReport_id() : "N/A");
            logEntry.put("timestamp", ServerValue.TIMESTAMP);

            adminLogsRef.child(logId).setValue(logEntry)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Admin log recorded: " + action + " for " + targetId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to record admin log: " + e.getMessage()));
        }
    }
}