package com.example.tradeup.model;

import java.time.Instant;

public class Cart {
    private String user_id;
    private String listing_id;
    private Long quantity;
    private Instant created_at;
    private Instant updated_at;

    public Cart() {}

    public Cart(String user_id, String listing_id, Long quantity, Instant created_at, Instant updated_at) {
        this.user_id = user_id;
        this.listing_id = listing_id;
        this.quantity = quantity;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public String getUser_id() { return user_id; }
    public String getListing_id() { return listing_id; }
    public Long getQuantity() { return quantity; }
    public Instant getCreated_at() { return created_at; }
    public Instant getUpdated_at() { return updated_at; }

    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setListing_id(String listing_id) { this.listing_id = listing_id; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
    public void setCreated_at(Instant created_at) { this.created_at = created_at; }
    public void setUpdated_at(Instant updated_at) { this.updated_at = updated_at; }
}