package com.example.tradeup.model;

// import com.google.firebase.database.ServerValue; // Import này sẽ không còn cần thiết nếu bạn không dùng ServerValue.TIMESTAMP cho String timestamps

public class Transaction {
    private String transaction_id;
    private String item_id;
    private String buyer_id;
    private String seller_id;
    private Long final_price;
    private String offer_id;
    private String transaction_date;
    private boolean archived; // false by default

    // NEW: Fields for Escrow and Completion Status
    private String escrow_status; // e.g., "pending", "held", "released", "refunded", "disputed"
    private Boolean buyer_confirmed_receipt; // True if buyer confirmed item received
    private Boolean seller_confirmed_dispatch; // True if seller confirmed item dispatched
    private String completion_timestamp; // Đã đổi từ Long sang String
    private String payment_id; // Link to the associated Payment record
    private String item_title;

    public Transaction() {
        // Required for Firebase
    }

    public Transaction(String transaction_id, String item_id, String buyer_id, String seller_id, Long final_price, String offer_id, String transaction_date, boolean archived, String item_title) {
        this.transaction_id = transaction_id;
        this.item_id = item_id;
        this.buyer_id = buyer_id;
        this.seller_id = seller_id;
        this.final_price = final_price;
        this.offer_id = offer_id;
        this.transaction_date = transaction_date;
        this.archived = archived;
        this.item_title = item_title;
        // Các trường mới sẽ được khởi tạo mặc định hoặc set sau
    }

    // Getters
    public String getTransaction_id() { return transaction_id; }
    public String getItem_id() { return item_id; }
    public String getBuyer_id() { return buyer_id; }
    public String getSeller_id() { return seller_id; }
    public Long getFinal_price() { return final_price; }
    public String getOffer_id() { return offer_id; }
    public String getTransaction_date() { return transaction_date; }
    public boolean isArchived() { return archived; } // Use isArchived for boolean getter

    // NEW Getters for Escrow and Completion Status
    public String getEscrow_status() { return escrow_status; }
    public Boolean getBuyer_confirmed_receipt() { return buyer_confirmed_receipt; }
    public Boolean getSeller_confirmed_dispatch() { return seller_confirmed_dispatch; }
    public String getCompletion_timestamp() { return completion_timestamp; } // Đã đổi sang String
    public String getPayment_id() { return payment_id; }
    public String getItem_title() { return item_title; }


    // Setters
    public void setTransaction_id(String transaction_id) { this.transaction_id = transaction_id; }
    public void setItem_id(String item_id) { this.item_id = item_id; }
    public void setBuyer_id(String buyer_id) { this.buyer_id = buyer_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
    public void setFinal_price(Long final_price) { this.final_price = final_price; }
    public void setOffer_id(String offer_id) { this.offer_id = offer_id; }
    public void setTransaction_date(String transaction_date) { this.transaction_date = transaction_date; }
    public void setArchived(boolean archived) { this.archived = archived; }

    // NEW Setters for Escrow and Completion Status
    public void setEscrow_status(String escrow_status) { this.escrow_status = escrow_status; }
    public void setBuyer_confirmed_receipt(Boolean buyer_confirmed_receipt) { this.buyer_confirmed_receipt = buyer_confirmed_receipt; }
    public void setSeller_confirmed_dispatch(Boolean seller_confirmed_dispatch) { this.seller_confirmed_dispatch = seller_confirmed_dispatch; }
    public void setCompletion_timestamp(String completion_timestamp) { this.completion_timestamp = completion_timestamp; } // Đã đổi sang String
    public void setPayment_id(String payment_id) { this.payment_id = payment_id; }
    public void setItem_title(String item_title) { this.item_title = item_title; }
}