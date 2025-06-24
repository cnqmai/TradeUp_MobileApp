// models/Report.java
package com.example.tradeup.model; // Corrected package name

public class Report {
    private String reporter_id;
    private String target_type;
    private String target_id;
    private String reason;
    private String timestamp;

    public Report() {
        // Required for Firebase
    }

    public Report(String reporter_id, String target_type, String target_id, String reason, String timestamp) {
        this.reporter_id = reporter_id;
        this.target_type = target_type;
        this.target_id = target_id;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getReporter_id() { return reporter_id; }
    public void setReporter_id(String reporter_id) { this.reporter_id = reporter_id; }
    public String getTarget_type() { return target_type; }
    public void setTarget_type(String target_type) { this.target_type = target_type; }
    public String getTarget_id() { return target_id; }
    public void setTarget_id(String target_id) { this.target_id = target_id; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}