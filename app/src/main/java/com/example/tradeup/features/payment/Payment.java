package com.example.tradeup.features.payment;

import java.util.Date;

public class Payment {
    private String paymentId;
    private String transactionId;
    private double amount;
    private String method; // credit, UPI, wallet, etc.
    private Date paidAt;

    public Payment() {}

    public Payment(String paymentId, String transactionId, double amount, String method, Date paidAt) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.method = method;
        this.paidAt = paidAt;
    }

    public String getPaymentId() { return paymentId; }
    public String getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public String getMethod() { return method; }
    public Date getPaidAt() { return paidAt; }

    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setMethod(String method) { this.method = method; }
    public void setPaidAt(Date paidAt) { this.paidAt = paidAt; }
}
