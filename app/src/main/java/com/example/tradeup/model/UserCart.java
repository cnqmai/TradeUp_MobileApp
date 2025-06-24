// models/UserCart.java
package com.example.tradeup.model; // Corrected package name

import java.util.Map;

public class UserCart {
    private Map<String, CartItem> items; // Key là item_id

    public UserCart() {
        // Required for Firebase
    }

    public UserCart(Map<String, CartItem> items) {
        this.items = items;
    }

    public Map<String, CartItem> getItems() {
        return items;
    }

    public void setItems(Map<String, CartItem> items) {
        this.items = items;
    }
}