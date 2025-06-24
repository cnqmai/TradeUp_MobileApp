// models/Offer.java
package com.example.tradeup.model; // Corrected package name

public class Offer {
    private String item_id;
    private String buyer_id;
    private String seller_id;
    private Long offer_price;
    private String status;
    private Long counter_price;
    private String created_at;
    private String updated_at;

    public Offer() {
        // Required for Firebase
    }

    public Offer(String item_id, String buyer_id, String seller_id, Long offer_price, String status, Long counter_price, String created_at, String updated_at) {
        this.item_id = item_id;
        this.buyer_id = buyer_id;
        this.seller_id = seller_id;
        this.offer_price = offer_price;
        this.status = status;
        this.counter_price = counter_price;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    // Getters and Setters
    public String getItem_id() { return item_id; }
    public void setItem_id(String item_id) { this.item_id = item_id; }
    public String getBuyer_id() { return buyer_id; }
    public void setBuyer_id(String buyer_id) { this.buyer_id = buyer_id; }
    public String getSeller_id() { return seller_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
    public Long getOffer_price() { return offer_price; }
    public void setOffer_price(Long offer_price) { this.offer_price = offer_price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCounter_price() { return counter_price; }
    public void setCounter_price(Long counter_price) { this.counter_price = counter_price; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}