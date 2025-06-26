package com.example.tradeup.model;

import java.util.List;

public class Item {
    private String id;
    private String user_id;
    private String title;
    private String description;
    private Long price;
    private String category;
    private String condition;
    private String status;
    private Location location;
    private List<String> photos;
    private String item_behavior;
    private List<String> tags;
    private String created_at;
    private String updated_at;
    private Long rating_sum;
    private Long rating_count;
    private Double average_rating;

    public Item() {
        // Required for Firebase
    }

    public Item(String id, String user_id, String title, String description, Long price, String category, String condition, String status, Location location, List<String> photos, String item_behavior, List<String> tags, String created_at, String updated_at, Long rating_sum, Long rating_count, Double average_rating) {
        this.id = id;
        this.user_id = user_id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.condition = condition;
        this.status = status;
        this.location = location;
        this.photos = photos;
        this.item_behavior = item_behavior;
        this.tags = tags;
        this.created_at = created_at;
        this.updated_at = updated_at;
        // >>> ĐIỂM CHỈNH SỬA <<<
        // Gán các tham số đầu vào cho các thuộc tính rating
        this.rating_sum = rating_sum;
        this.rating_count = rating_count;
        this.average_rating = average_rating;
    }

    // Getters and Setters for 'id'
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Getters and Setters for other fields (unchanged)
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
    public String getItem_behavior() { return item_behavior; }
    public void setItem_behavior(String item_behavior) { this.item_behavior = item_behavior; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
    public Long getRating_sum() {
        return rating_sum;
    }

    public void setRating_sum(Long rating_sum) {
        this.rating_sum = rating_sum;
    }

    public Long getRating_count() {
        return rating_count;
    }

    public void setRating_count(Long rating_count) {
        this.rating_count = rating_count;
    }

    public Double getAverage_rating() {
        return average_rating;
    }

    public void setAverage_rating(Double average_rating) {
        this.average_rating = average_rating;
    }
}