package com.example.tradeup.features.listing;

import java.util.Date;
import java.util.List;

public class Listing {
    private String listingId;
    private String sellerId;
    private String title;
    private String description;
    private double price;
    private String category;
    private String condition;
    private String location;
    private List<String> imageUrls;
    private boolean negotiable;
    private String status;
    private int viewCount;
    private int interactionCount;
    private Date createdAt;
    private Date updatedAt;

    public Listing() {
        // Firebase requires a public no-argument constructor
    }

    public Listing(String listingId, String sellerId, String title, String description, double price,
                   String category, String condition, String location, List<String> imageUrls,
                   boolean negotiable, String status, int viewCount, int interactionCount,
                   Date createdAt, Date updatedAt) {
        this.listingId = listingId;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.condition = condition;
        this.location = location;
        this.imageUrls = imageUrls;
        this.negotiable = negotiable;
        this.status = status;
        this.viewCount = viewCount;
        this.interactionCount = interactionCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getListingId() { return listingId; }
    public String getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getCondition() { return condition; }
    public String getLocation() { return location; }
    public List<String> getImageUrls() { return imageUrls; }
    public boolean isNegotiable() { return negotiable; }
    public String getStatus() { return status; }
    public int getViewCount() { return viewCount; }
    public int getInteractionCount() { return interactionCount; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }

    public void setListingId(String listingId) { this.listingId = listingId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setCategory(String category) { this.category = category; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setLocation(String location) { this.location = location; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setNegotiable(boolean negotiable) { this.negotiable = negotiable; }
    public void setStatus(String status) { this.status = status; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public void setInteractionCount(int interactionCount) { this.interactionCount = interactionCount; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
