package com.example.tradeup.model;

import com.google.firebase.database.Exclude; // Import for @Exclude annotation
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Report {
    private String report_id; // ID của báo cáo, sẽ được Firebase tạo tự động hoặc gán thủ công
    private String reporter_id; // ID của người báo cáo
    private String reported_object_id; // ID của đối tượng bị báo cáo (item, user, chat)
    private String report_type; // Loại đối tượng bị báo cáo ("item", "user", "chat")
    private String reason; // Lý do báo cáo (e.g., "Scam/Fraud", "Inappropriate Content", "Spam", "Other")
    private String comment; // Bình luận chi tiết từ người báo cáo (tùy chọn)
    private String status; // Trạng thái của báo cáo ("pending", "reviewed", "resolved", "rejected")
    private String created_at; // Thời gian báo cáo được tạo
    private String updated_at; // Thời gian báo cáo được cập nhật lần cuối

    // NEW: Các trường cho Admin
    private String admin_notes; // Ghi chú của quản trị viên về báo cáo
    private String resolved_timestamp; // Thời gian báo cáo được xử lý (đã đổi sang String)
    private String admin_id; // ID của quản trị viên đã xử lý báo cáo

    public Report() {
        // Default constructor required for Firebase
    }

    public Report(String reporter_id, String reported_object_id, String report_type, String reason, String comment) {
        this.reporter_id = reporter_id;
        this.reported_object_id = reported_object_id;
        this.report_type = report_type;
        this.reason = reason;
        this.comment = comment;
        this.status = "pending"; // Mặc định là "pending" khi mới tạo
        String currentTime = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        this.created_at = currentTime;
        this.updated_at = currentTime;
        // Các trường admin_notes, resolved_timestamp, admin_id sẽ được khởi tạo là null/mặc định
    }

    // Getters and Setters
    @Exclude // Exclude report_id from direct Firebase mapping as it's the key
    public String getReport_id() {
        return report_id;
    }

    public void setReport_id(String report_id) {
        this.report_id = report_id;
    }

    public String getReporter_id() {
        return reporter_id;
    }

    public void setReporter_id(String reporter_id) {
        this.reporter_id = reporter_id;
    }

    public String getReported_object_id() {
        return reported_object_id;
    }

    public void setReported_object_id(String reported_object_id) {
        this.reported_object_id = reported_object_id;
    }

    public String getReport_type() {
        return report_type;
    }

    public void setReport_type(String report_type) {
        this.report_type = report_type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    // NEW: Getters and Setters for Admin fields
    public String getAdmin_notes() {
        return admin_notes;
    }

    public void setAdmin_notes(String admin_notes) {
        this.admin_notes = admin_notes;
    }

    public String getResolved_timestamp() { // Đã đổi sang String
        return resolved_timestamp;
    }

    public void setResolved_timestamp(String resolved_timestamp) { // Đã đổi sang String
        this.resolved_timestamp = resolved_timestamp;
    }

    public String getAdmin_id() {
        return admin_id;
    }

    public void setAdmin_id(String admin_id) {
        this.admin_id = admin_id;
    }
}