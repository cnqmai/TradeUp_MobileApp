package com.example.tradeup.features.report;

import java.util.Date;

public class Report {
    private String reportId;
    private String reportedByUserId;
    private String targetId; // listingId, userId, messageId
    private String targetType; // listing, user, message
    private String reason;
    private Date createdAt;

    public Report() {}

    public Report(String reportId, String reportedByUserId, String targetId,
                  String targetType, String reason, Date createdAt) {
        this.reportId = reportId;
        this.reportedByUserId = reportedByUserId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public String getReportId() { return reportId; }
    public String getReportedByUserId() { return reportedByUserId; }
    public String getTargetId() { return targetId; }
    public String getTargetType() { return targetType; }
    public String getReason() { return reason; }
    public Date getCreatedAt() { return createdAt; }

    public void setReportId(String reportId) { this.reportId = reportId; }
    public void setReportedByUserId(String reportedByUserId) { this.reportedByUserId = reportedByUserId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
