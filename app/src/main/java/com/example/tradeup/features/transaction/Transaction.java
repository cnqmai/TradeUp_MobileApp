package com.example.tradeup.features.transaction;

import java.util.Date;

public class Transaction {
    private String transactionId;
    private String listingId;
    private String buyerId;
    private String sellerId;
    private double amount;
    private Date completedAt;
    private String paymentMethod; // credit, UPI, etc.
    private boolean escrowEnabled;

    public Transaction() {}

    public Transaction(String transactionId, String listingId, String buyerId, String sellerId,
                       double amount, Date completedAt, String paymentMethod, boolean escrowEnabled) {
        this.transactionId = transactionId;
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.completedAt = completedAt;
        this.paymentMethod = paymentMethod;
        this.escrowEnabled = escrowEnabled;
    }

    public String getTransactionId() { return transactionId; }
    public String getListingId() { return listingId; }
    public String getBuyerId() { return buyerId; }
    public String getSellerId() { return sellerId; }
    public double getAmount() { return amount; }
    public Date getCompletedAt() { return completedAt; }
    public String getPaymentMethod() { return paymentMethod; }
    public boolean isEscrowEnabled() { return escrowEnabled; }

    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setEscrowEnabled(boolean escrowEnabled) { this.escrowEnabled = escrowEnabled; }
}
