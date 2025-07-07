package com.example.tradeup.model;

import com.google.firebase.database.Exclude;
import java.util.HashMap;
import java.util.Map;

public class ActivityLog {
    public String activity_id;
    public String type; // e.g., "new_user", "new_item", "transaction_completed", "report_filed"
    public String timestamp; // Reverted to timestamp
    public String primary_actor_id; // User who performed the action (e.g., new user's UID, reporter's UID)
    public String secondary_object_id; // ID of the object involved (e.g., item ID, reported user ID, offer ID)
    public String related_transaction_id; // Optional: Link to a transaction if relevant
    public String details; // Optional: Any additional details as a string

    public ActivityLog() {
        // Default constructor required for calls to DataSnapshot.getValue(ActivityLog.class)
    }

    public ActivityLog(String activity_id, String type, String timestamp, String primary_actor_id,
                       String secondary_object_id, String related_transaction_id, String details) {
        this.activity_id = activity_id;
        this.type = type;
        this.timestamp = timestamp;
        this.primary_actor_id = primary_actor_id;
        this.secondary_object_id = secondary_object_id;
        this.related_transaction_id = related_transaction_id;
        this.details = details;
    }

    // Getters
    public String getActivity_id() { return activity_id; }
    public String getType() { return type; }
    public String getTimestamp() { return timestamp; }
    public String getPrimary_actor_id() { return primary_actor_id; }
    public String getSecondary_object_id() { return secondary_object_id; }
    public String getRelated_transaction_id() { return related_transaction_id; }
    public String getDetails() { return details; }

    // Setters (optional, depending on your needs)
    public void setActivity_id(String activity_id) { this.activity_id = activity_id; }
    public void setType(String type) { this.type = type; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setPrimary_actor_id(String primary_actor_id) { this.primary_actor_id = primary_actor_id; }
    public void setSecondary_object_id(String secondary_object_id) { this.secondary_object_id = secondary_object_id; }
    public void setRelated_transaction_id(String related_transaction_id) { this.related_transaction_id = related_transaction_id; }
    public void setDetails(String details) { this.details = details; }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("activity_id", activity_id);
        result.put("type", type);
        result.put("timestamp", timestamp);
        result.put("primary_actor_id", primary_actor_id);
        result.put("secondary_object_id", secondary_object_id);
        result.put("related_transaction_id", related_transaction_id);
        result.put("details", details);
        return result;
    }
}
