package com.example.tradeup.model;

public class Notification {
    private String id; // ID của thông báo từ Firebase
    private String title;
    private String body;
    private String type; // "new_message", "promotion", "new_offer", etc.
    private String related_id;
    private String user_id;
    private Boolean read;
    private String timestamp; // ✅ Đổi từ Long → String

    public Notification() {
        // Required for Firebase
    }

    public Notification(String id, String title, String body, String type, String related_id, String user_id, Boolean read, String timestamp) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.type = type;
        this.related_id = related_id;
        this.user_id = user_id;
        this.read = read;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRelated_id() { return related_id; }
    public void setRelated_id(String related_id) { this.related_id = related_id; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
