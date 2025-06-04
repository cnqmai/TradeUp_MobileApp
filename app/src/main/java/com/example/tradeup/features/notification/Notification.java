package com.example.tradeup.features.notification;

import java.util.Date;

public class Notification {
    private String notificationId;
    private String userId;
    private String title;
    private String message;
    private String type; // message, offer, update, promotion
    private boolean isRead;
    private Date createdAt;

    public Notification() {}

    public Notification(String notificationId, String userId, String title, String message,
                        String type, boolean isRead, Date createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public String getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public Date getCreatedAt() { return createdAt; }

    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setRead(boolean read) { isRead = read; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
