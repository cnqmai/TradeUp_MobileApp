// models/Payment.java
package com.example.tradeup.model; // Corrected package name

public class Payment {
    private String transaction_id;
    private String payer_id;
    private String payee_id;
    private Long amount;
    private String method;
    private String status;
    private Boolean escrow;

    public Payment() {
        // Required for Firebase
    }

    public Payment(String transaction_id, String payer_id, String payee_id, Long amount, String method, String status, Boolean escrow) {
        this.transaction_id = transaction_id;
        this.payer_id = payer_id;
        this.payee_id = payee_id;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.escrow = escrow;
    }

    // Getters and Setters
    public String getTransaction_id() { return transaction_id; }
    public void setTransaction_id(String transaction_id) { this.transaction_id = transaction_id; }
    public String getPayer_id() { return payer_id; }
    public void setPayer_id(String payer_id) { this.payer_id = payer_id; }
    public String getPayee_id() { return payee_id; }
    public void setPayee_id(String payee_id) { this.payee_id = payee_id; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getEscrow() { return escrow; }
    public void setEscrow(Boolean escrow) { this.escrow = escrow; }
}