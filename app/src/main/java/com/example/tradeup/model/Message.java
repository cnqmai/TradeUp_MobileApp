package com.example.tradeup.model;

public class Message {
    private String sender_id;
    private String text;
    private String type;
    private String timestamp;
    private String imageUrl; // Thêm trường này

    public Message() {
        // Required for Firebase
    }

    public Message(String sender_id, String text, String type, String timestamp, String imageUrl) {
        this.sender_id = sender_id;
        this.text = text;
        this.type = type;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl; // Thêm vào constructor
    }

    // Getters and Setters
    public String getSender_id() { return sender_id; }
    public void setSender_id(String sender_id) { this.sender_id = sender_id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getImageUrl() { return imageUrl; } // Thêm getter cho imageUrl
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; } // Thêm setter cho imageUrl
}