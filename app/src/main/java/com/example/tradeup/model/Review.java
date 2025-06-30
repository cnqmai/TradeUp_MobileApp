package com.example.tradeup.model;

import com.google.firebase.database.IgnoreExtraProperties;

// Review model
// Dùng để lưu trữ thông tin đánh giá (sao và nhận xét) giữa người dùng sau giao dịch
@IgnoreExtraProperties
public class Review {
    private String review_id; // ID của đánh giá (key từ Firebase)
    private String reviewer_id; // ID của người tạo đánh giá
    private String reviewee_id; // ID của người được đánh giá
    private String transaction_id; // ID của giao dịch liên quan
    private Integer rating; // Số sao (1-5)
    private String comment; // Nhận xét bằng văn bản (tùy chọn)
    private String status; // Trạng thái kiểm duyệt: "pending", "approved", "rejected"
    private String created_at; // Thời gian tạo đánh giá (ISO 8601 UTC timestamp)

    public Review() {
        // Constructor mặc định cần thiết cho Firebase
    }

    public Review(String reviewer_id, String reviewee_id, String transaction_id, Integer rating, String comment, String status) {
        this.reviewer_id = reviewer_id;
        this.reviewee_id = reviewee_id;
        this.transaction_id = transaction_id;
        this.rating = rating;
        this.comment = comment;
        this.status = status;
    }

    // Constructor đầy đủ với review_id và created_at
    public Review(String review_id, String reviewer_id, String reviewee_id, String transaction_id, Integer rating, String comment, String status, String created_at) {
        this.review_id = review_id;
        this.reviewer_id = reviewer_id;
        this.reviewee_id = reviewee_id;
        this.transaction_id = transaction_id;
        this.rating = rating;
        this.comment = comment;
        this.status = status;
        this.created_at = created_at;
    }


    // Getters
    public String getReview_id() { return review_id; }
    public String getReviewer_id() { return reviewer_id; }
    public String getReviewee_id() { return reviewee_id; }
    public String getTransaction_id() { return transaction_id; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public String getStatus() { return status; }
    public String getCreated_at() { return created_at; }


    // Setters
    public void setReview_id(String review_id) { this.review_id = review_id; }
    public void setReviewer_id(String reviewer_id) { this.reviewer_id = reviewer_id; }
    public void setReviewee_id(String reviewee_id) { this.reviewee_id = reviewee_id; }
    public void setTransaction_id(String transaction_id) { this.transaction_id = transaction_id; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setStatus(String status) { this.status = status; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
