package com.example.tradeup.features.feedback;

import java.util.Date;

public class Feedback {
    private String feedbackId;
    private String fromUserId;
    private String toUserId;
    private String transactionId;
    private int rating; // 1–5 stars
    private String comment;
    private Date createdAt;

    public Feedback() {}

    public Feedback(String feedbackId, String fromUserId, String toUserId,
                    String transactionId, int rating, String comment, Date createdAt) {
        this.feedbackId = feedbackId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.transactionId = transactionId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getFeedbackId() { return feedbackId; }
    public String getFromUserId() { return fromUserId; }
    public String getToUserId() { return toUserId; }
    public String getTransactionId() { return transactionId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Date getCreatedAt() { return createdAt; }

    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
