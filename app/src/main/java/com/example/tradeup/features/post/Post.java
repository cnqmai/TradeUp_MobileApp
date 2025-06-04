package com.example.tradeup.features.post;

import java.util.List;

public class Post {
    private String postId;
    private String userId;         // Người đăng
    private String title;          // Tiêu đề
    private String description;    // Mô tả
    private double price;          // Giá
    private String categoryId;     // Danh mục (tham chiếu đến Category)
    private String condition;      // Tình trạng (ví dụ: Mới, Đã dùng)
    private String location;       // Vị trí (chuỗi địa điểm hoặc GPS)
    private List<String> imageUrls;// Danh sách ảnh (tối đa 10)
    private String itemBehavior;   // Hành vi mặt hàng (tùy chọn)
    private List<String> tags;     // Thẻ bổ sung (tùy chọn)
    private String status;         // Trạng thái: Có sẵn, Đã bán, Tạm dừng
    private long createdAt;        // Thời gian tạo (timestamp)
    private long updatedAt;        // Thời gian cập nhật (timestamp)
    private int viewsCount;        // Lượt xem
    private int interactionCount;  // Tương tác (ví dụ: lượt thích, bình luận)

    public Post() {
        // Bắt buộc với Firebase
    }

    public Post(String postId, String userId, String title, String description, double price, String categoryId,
                String condition, String location, List<String> imageUrls, String itemBehavior, List<String> tags,
                String status, long createdAt, long updatedAt, int viewsCount, int interactionCount) {
        this.postId = postId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.condition = condition;
        this.location = location;
        this.imageUrls = imageUrls;
        this.itemBehavior = itemBehavior;
        this.tags = tags;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.viewsCount = viewsCount;
        this.interactionCount = interactionCount;
    }

    // Getters
    public String getPostId() { return postId; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getCategoryId() { return categoryId; }
    public String getCondition() { return condition; }
    public String getLocation() { return location; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getItemBehavior() { return itemBehavior; }
    public List<String> getTags() { return tags; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public int getViewsCount() { return viewsCount; }
    public int getInteractionCount() { return interactionCount; }

    // Setters
    public void setPostId(String postId) { this.postId = postId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setLocation(String location) { this.location = location; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setItemBehavior(String itemBehavior) { this.itemBehavior = itemBehavior; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }
    public void setInteractionCount(int interactionCount) { this.interactionCount = interactionCount; }
}
