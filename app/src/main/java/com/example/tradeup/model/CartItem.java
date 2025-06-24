// models/CartItem.java
package com.example.tradeup.model; // Corrected package name

public class CartItem {
    private Integer quantity;
    private String added_at;

    public CartItem() {
        // Required for Firebase
    }

    public CartItem(Integer quantity, String added_at) {
        this.quantity = quantity;
        this.added_at = added_at;
    }

    // Getters and Setters
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getAdded_at() { return added_at; }
    public void setAdded_at(String added_at) { this.added_at = added_at; }
}