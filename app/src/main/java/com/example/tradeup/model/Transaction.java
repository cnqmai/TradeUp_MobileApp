// models/Transaction.java
package com.example.tradeup.model; // Corrected package name

public class Transaction {
    private String item_id;
    private String buyer_id;
    private String seller_id;
    private Long final_price;
    private String offer_id;
    private String transaction_date;
    private Boolean archived;

    public Transaction() {
        // Required for Firebase
    }

    public Transaction(String item_id, String buyer_id, String seller_id, Long final_price, String offer_id, String transaction_date, Boolean archived) {
        this.item_id = item_id;
        this.buyer_id = buyer_id;
        this.seller_id = seller_id;
        this.final_price = final_price;
        this.offer_id = offer_id;
        this.transaction_date = transaction_date;
        this.archived = archived;
    }

    // Getters and Setters
    public String getItem_id() { return item_id; }
    public void setItem_id(String item_id) { this.item_id = item_id; }
    public String getBuyer_id() { return buyer_id; }
    public void setBuyer_id(String buyer_id) { this.buyer_id = buyer_id; }
    public String getSeller_id() { return seller_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
    public Long getFinal_price() { return final_price; }
    public void setFinal_price(Long final_price) { this.final_price = final_price; }
    public String getOffer_id() { return offer_id; }
    public void setOffer_id(String offer_id) { this.offer_id = offer_id; }
    public String getTransaction_date() { return transaction_date; }
    public void setTransaction_date(String transaction_date) { this.transaction_date = transaction_date; }
    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }
}