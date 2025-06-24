// models/CheckoutSessionItem.java
package com.example.tradeup.model; // Corrected package name

public class CheckoutSessionItem {
    private Integer quantity;
    private String seller_id;

    public CheckoutSessionItem() {
        // Required for Firebase
    }

    public CheckoutSessionItem(Integer quantity, String seller_id) {
        this.quantity = quantity;
        this.seller_id = seller_id;
    }

    // Getters and Setters
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getSeller_id() { return seller_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
}