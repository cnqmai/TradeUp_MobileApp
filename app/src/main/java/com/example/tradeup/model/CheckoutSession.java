// models/CheckoutSession.java
package com.example.tradeup.model; // Corrected package name

import java.util.Map;

public class CheckoutSession {
    private String user_id;
    private Map<String, CheckoutSessionItem> items; // Key là item_id
    private String status;
    private Long total_amount;
    private String created_at;

    public CheckoutSession() {
        // Required for Firebase
    }

    public CheckoutSession(String user_id, Map<String, CheckoutSessionItem> items, String status, Long total_amount, String created_at) {
        this.user_id = user_id;
        this.items = items;
        this.status = status;
        this.total_amount = total_amount;
        this.created_at = created_at;
    }

    // Getters and Setters
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public Map<String, CheckoutSessionItem> getItems() { return items; }
    public void setItems(Map<String, CheckoutSessionItem> items) { this.items = items; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTotal_amount() { return total_amount; }
    public void setTotal_amount(Long total_amount) { this.total_amount = total_amount; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}