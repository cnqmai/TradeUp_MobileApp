package com.example.tradeup.model;

import com.google.firebase.database.Exclude;
import java.util.HashMap;
import java.util.Map;

public class SavedCard {
    public String card_id;
    public String last4;
    public String brand; // e.g., "Visa", "MasterCard"
    public String expiry_month;
    public String expiry_year;
    public String card_holder_name;
    public String user_id; // User who saved this card
    public String created_at; // ISO 8601 format
    public boolean is_default; // NEW: Field to indicate if this is the default card

    public SavedCard() {
        // Default constructor required for calls to DataSnapshot.getValue(SavedCard.class)
    }

    public SavedCard(String card_id, String last4, String brand, String expiry_month, String expiry_year,
                     String card_holder_name, String user_id, String created_at, boolean is_default) { // UPDATED CONSTRUCTOR
        this.card_id = card_id;
        this.last4 = last4;
        this.brand = brand;
        this.expiry_month = expiry_month;
        this.expiry_year = expiry_year;
        this.card_holder_name = card_holder_name;
        this.user_id = user_id;
        this.created_at = created_at;
        this.is_default = is_default; // NEW
    }

    // Getters
    public String getCard_id() { return card_id; }
    public String getLast4() { return last4; }
    public String getBrand() { return brand; } // Used instead of getCard_type()
    public String getExpiry_month() { return expiry_month; }
    public String getExpiry_year() { return expiry_year; }
    public String getCard_holder_name() { return card_holder_name; }
    public String getUser_id() { return user_id; }
    public String getCreated_at() { return created_at; }
    public boolean getIs_default() { return is_default; } // NEW: Getter for is_default

    // Setters
    public void setCard_id(String card_id) { this.card_id = card_id; }
    public void setLast4(String last4) { this.last4 = last4; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setExpiry_month(String expiry_month) { this.expiry_month = expiry_month; }
    public void setExpiry_year(String expiry_year) { this.expiry_year = expiry_year; }
    public void setCard_holder_name(String card_holder_name) { this.card_holder_name = card_holder_name; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public void setIs_default(boolean is_default) { this.is_default = is_default; } // NEW: Setter for is_default

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("card_id", card_id);
        result.put("last4", last4);
        result.put("brand", brand);
        result.put("expiry_month", expiry_month);
        result.put("expiry_year", expiry_year);
        result.put("card_holder_name", card_holder_name);
        result.put("user_id", user_id);
        result.put("created_at", created_at);
        result.put("is_default", is_default); // NEW: Add is_default to map
        return result;
    }
}
