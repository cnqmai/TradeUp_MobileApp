package com.example.tradeup.features.user;

import java.util.Date;

public class User {
    private String userId;
    private String displayName;
    private String email;
    private String passwordHash;
    private String avatarUrl;
    private String bio;
    private String contactInfo;
    private float rating;
    private int totalTransactions;
    private boolean isVerified;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;

    public User() {
        // Firebase requires a public no-argument constructor
    }

    public User(String userId, String displayName, String email, String passwordHash,
                String avatarUrl, String bio, String contactInfo, float rating,
                int totalTransactions, boolean isVerified, boolean isActive,
                Date createdAt, Date updatedAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.contactInfo = contactInfo;
        this.rating = rating;
        this.totalTransactions = totalTransactions;
        this.isVerified = isVerified;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
    public String getContactInfo() { return contactInfo; }
    public float getRating() { return rating; }
    public int getTotalTransactions() { return totalTransactions; }
    public boolean isVerified() { return isVerified; }
    public boolean isActive() { return isActive; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public void setRating(float rating) { this.rating = rating; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setActive(boolean active) { isActive = active; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}