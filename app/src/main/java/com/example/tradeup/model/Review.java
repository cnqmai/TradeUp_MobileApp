// models/Review.java
package com.example.tradeup.model; // Corrected package name

public class Review {
    private String reviewer_id;
    private String reviewee_id;
    private String transaction_id;
    private Integer rating;
    private String comment;
    private String status;

    public Review() {
        // Required for Firebase
    }

    public Review(String reviewer_id, String reviewee_id, String transaction_id, Integer rating, String comment, String status) {
        this.reviewer_id = reviewer_id;
        this.reviewee_id = reviewee_id;
        this.transaction_id = transaction_id;
        this.rating = rating;
        this.comment = comment;
        this.status = status;
    }

    // Getters and Setters
    public String getReviewer_id() { return reviewer_id; }
    public void setReviewer_id(String reviewer_id) { this.reviewer_id = reviewer_id; }
    public String getReviewee_id() { return reviewee_id; }
    public void setReviewee_id(String reviewee_id) { this.reviewee_id = reviewee_id; }
    public String getTransaction_id() { return transaction_id; }
    public void setTransaction_id(String transaction_id) { this.transaction_id = transaction_id; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}