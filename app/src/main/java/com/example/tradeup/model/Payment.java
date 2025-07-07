package com.example.tradeup.model;

public class Payment {
    private String payment_id;
    private Long amount;
    private String currency;
    private String method; // e.g., "Credit Card", "UPI", "Bank Transfer"
    private String status; // e.g., "completed", "failed", "refunded", "pending"
    private String timestamp;
    private String transaction_id; // Link to the associated transaction
    private String buyer_id;
    private String seller_id;
    private Boolean is_escrow; // Indicates if this payment is part of an escrow
    private String escrow_status; // e.g., "held", "released", "refunded"
    private String stripe_payment_intent_id; // For real payment gateway integration

    public Payment() {
        // Default constructor required for Firebase
    }

    public Payment(String payment_id, Long amount, String currency, String method, String status,
                   String timestamp, String transaction_id, String buyer_id, String seller_id,
                   Boolean is_escrow, String escrow_status, String stripe_payment_intent_id) {
        this.payment_id = payment_id;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.status = status;
        this.timestamp = timestamp;
        this.transaction_id = transaction_id;
        this.buyer_id = buyer_id;
        this.seller_id = seller_id;
        this.is_escrow = is_escrow;
        this.escrow_status = escrow_status;
        this.stripe_payment_intent_id = stripe_payment_intent_id;
    }

    // Getters
    public String getPayment_id() { return payment_id; }
    public Long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
    public String getTransaction_id() { return transaction_id; }
    public String getBuyer_id() { return buyer_id; }
    public String getSeller_id() { return seller_id; }
    public Boolean getIs_escrow() { return is_escrow; }
    public String getEscrow_status() { return escrow_status; }
    public String getStripe_payment_intent_id() { return stripe_payment_intent_id; }

    // Setters
    public void setPayment_id(String payment_id) { this.payment_id = payment_id; }
    public void setAmount(Long amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setMethod(String method) { this.method = method; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setTransaction_id(String transaction_id) { this.transaction_id = transaction_id; }
    public void setBuyer_id(String buyer_id) { this.buyer_id = buyer_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
    public void setIs_escrow(Boolean is_escrow) { this.is_escrow = is_escrow; }
    public void setEscrow_status(String escrow_status) { this.escrow_status = escrow_status; }
    public void setStripe_payment_intent_id(String stripe_payment_intent_id) { this.stripe_payment_intent_id = stripe_payment_intent_id; }
}
