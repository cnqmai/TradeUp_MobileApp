// models/Notification.java
package com.example.tradeup.model; // Corrected package name

public class Notification {
    private String user_id;
    private String type;
    private String title;
    private String related_id;
    private Boolean read;
    private String timestamp;

    public Notification() {
        // Required for Firebase
    }

    public Notification(String user_id, String type, String title, String related_id, Boolean read, String timestamp) {
        this.user_id = user_id;
        this.type = type;
        this.title = title;
        this.related_id = related_id;
        this.read = read;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRelated_id() { return related_id; }
    public void setRelated_id(String related_id) { this.related_id = related_id; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}