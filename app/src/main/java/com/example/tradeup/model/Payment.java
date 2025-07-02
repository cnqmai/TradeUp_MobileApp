package com.example.tradeup.model;

import com.google.firebase.database.Exclude;
import java.util.HashMap;
import java.util.Map;

public class Payment {
    public String payment_id;
    public double amount;
    public String currency;
    public String method; // e.g., "Credit Card", "UPI", "Cash"
    public String status; // e.g., "pending", "completed", "failed", "refunded"
    public String timestamp; // ISO 8601 format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
    public String transaction_id; // Link to the transaction
    public String payer_id; // ID of the user who made the payment
    public String payee_id; // ID of the user who received the payment
    public boolean escrow_enabled; // Optional: true if escrow is used
    public String escrow_status; // Optional: "pending", "released", "refunded"
    public String stripe_payment_intent_id; // Optional: Stripe Payment Intent ID

    public Payment() {
        // Default constructor required for calls to DataSnapshot.getValue(Payment.class)
    }

    public Payment(String payment_id, double amount, String currency, String method, String status,
                   String timestamp, String transaction_id, String payer_id, String payee_id,
                   boolean escrow_enabled, String escrow_status, String stripe_payment_intent_id) {
        this.payment_id = payment_id;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.status = status;
        this.timestamp = timestamp;
        this.transaction_id = transaction_id;
        this.payer_id = payer_id;
        this.payee_id = payee_id;
        this.escrow_enabled = escrow_enabled;
        this.escrow_status = escrow_status;
        this.stripe_payment_intent_id = stripe_payment_intent_id;
    }

    // Getters
    public String getPayment_id() { return payment_id; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
    public String getTransaction_id() { return transaction_id; }
    public String getPayer_id() { return payer_id; }
    public String getPayee_id() { return payee_id; }
    public boolean isEscrow_enabled() { return escrow_enabled; }
    public String getEscrow_status() { return escrow_status; }
    public String getStripe_payment_intent_id() { return stripe_payment_intent_id; }

    // Setters (optional, depending on your needs)
    public void setPayment_id(String payment_id) { this.payment_id = payment_id; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setMethod(String method) { this.method = method; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setTransaction_id(String transaction_id) { this.transaction_id = transaction_id; }
    public void setPayer_id(String payer_id) { this.payer_id = payer_id; }
    public void setPayee_id(String payee_id) { this.payee_id = payee_id; }
    public void setEscrow_enabled(boolean escrow_enabled) { this.escrow_enabled = escrow_enabled; }
    public void setEscrow_status(String escrow_status) { this.escrow_status = escrow_status; }
    public void setStripe_payment_intent_id(String stripe_payment_intent_id) { this.stripe_payment_intent_id = stripe_payment_intent_id; }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("payment_id", payment_id);
        result.put("amount", amount);
        result.put("currency", currency);
        result.put("method", method);
        result.put("status", status);
        result.put("timestamp", timestamp);
        result.put("transaction_id", transaction_id);
        result.put("payer_id", payer_id);
        result.put("payee_id", payee_id);
        result.put("escrow_enabled", escrow_enabled);
        result.put("escrow_status", escrow_status);
        result.put("stripe_payment_intent_id", stripe_payment_intent_id);
        return result;
    }
}