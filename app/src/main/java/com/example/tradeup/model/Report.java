// models/Report.java
package com.example.tradeup.model;

public class Report {
    private String report_id; // Added to store Firebase push key
    private String reporter_id;
    private String target_type; // "listing", "profile", "chat"
    private String target_id;   // ID of the listing, user, or chat being reported
    private String reason;      // "Scam/fraud", "Inappropriate content", "Spam", "Other"
    private String description; // Detailed description from user
    private String timestamp;   // ISO 8601 UTC timestamp
    private String status;      // "pending", "resolved", "dismissed" (for admin moderation)
    private String evidence_url; // Optional: URL to uploaded evidence image/video

    public Report() {
        // Required for Firebase
    }

    public Report(String reporter_id, String target_type, String target_id, String reason, String description, String timestamp, String status, String evidence_url) {
        this.reporter_id = reporter_id;
        this.target_type = target_type;
        this.target_id = target_id;
        this.reason = reason;
        this.description = description;
        this.timestamp = timestamp;
        this.status = status;
        this.evidence_url = evidence_url;
    }

    // Getters and Setters
    public String getReport_id() { return report_id; }
    public void setReport_id(String report_id) { this.report_id = report_id; }

    public String getReporter_id() { return reporter_id; }
    public void setReporter_id(String reporter_id) { this.reporter_id = reporter_id; }

    public String getTarget_type() { return target_type; }
    public void setTarget_type(String target_type) { this.target_type = target_type; }

    public String getTarget_id() { return target_id; }
    public void setTarget_id(String target_id) { this.target_id = target_id; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEvidence_url() { return evidence_url; }
    public void setEvidence_url(String evidence_url) { this.evidence_url = evidence_url; }
}
