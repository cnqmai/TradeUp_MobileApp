package com.example.tradeup.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Map;

@IgnoreExtraProperties
public class User {

    private String uid; // Thêm trường UID
    private String email;
    private Boolean is_email_verified;
    // Password should NEVER be stored in the database, even hashed on client side.
    // private String password; // Do not include password here
    private String display_name;
    private String bio;
    private String contact_info;
    private String profile_picture_url;
    private Double rating;
    private Integer total_transactions;
    private String role;
    private String account_status;
    private UserLocation location; // Inner class for location
    private String created_at;
    private String updated_at;
    private String first_name;
    private String last_name;


    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    // Constructor đã điều chỉnh để khớp với các trường hiện tại và thêm uid
    public User(String uid, String email, Boolean is_email_verified, String display_name, String bio, String contact_info,
                String profile_picture_url, Double rating, Integer total_transactions, String role,
                String account_status, UserLocation location, String created_at, String updated_at,
                String first_name, String last_name) {
        this.uid = uid;
        this.email = email;
        this.is_email_verified = is_email_verified;
        this.display_name = display_name;
        this.bio = bio;
        this.contact_info = contact_info;
        this.profile_picture_url = profile_picture_url;
        this.rating = rating;
        this.total_transactions = total_transactions;
        this.role = role;
        this.account_status = account_status;
        this.location = location;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.first_name = first_name;
        this.last_name = last_name;
    }

    // Constructor cho việc tạo User khi đăng ký lần đầu (ít thông tin hơn)
    public User(String uid, String email, String first_name, String last_name, String display_name,
                String bio, String contact_info, String profile_picture_url, Double rating,
                Integer total_transactions, String role, String account_status,
                UserLocation location, String created_at) {
        this.uid = uid;
        this.email = email;
        this.is_email_verified = false; // Mặc định false khi đăng ký, sẽ được cập nhật sau
        this.first_name = first_name;
        this.last_name = last_name;
        this.display_name = display_name;
        this.bio = bio;
        this.contact_info = contact_info;
        this.profile_picture_url = profile_picture_url;
        this.rating = rating;
        this.total_transactions = total_transactions;
        this.role = role;
        this.account_status = account_status;
        this.location = location;
        this.created_at = created_at;
        this.updated_at = created_at; // Ban đầu updated_at = created_at
    }


    // Getters and Setters for all fields

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIs_email_verified() {
        return is_email_verified;
    }

    public void setIs_email_verified(Boolean is_email_verified) {
        this.is_email_verified = is_email_verified;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getContact_info() {
        return contact_info;
    }

    public void setContact_info(String contact_info) {
        this.contact_info = contact_info;
    }

    public String getProfile_picture_url() {
        return profile_picture_url;
    }

    public void setProfile_picture_url(String profile_picture_url) {
        this.profile_picture_url = profile_picture_url;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotal_transactions() {
        return total_transactions;
    }

    public void setTotal_transactions(Integer total_transactions) {
        this.total_transactions = total_transactions;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAccount_status() {
        return account_status;
    }

    public void setAccount_status(String account_status) {
        this.account_status = account_status;
    }

    public UserLocation getLocation() {
        return location;
    }

    public void setLocation(UserLocation location) {
        this.location = location;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    // Inner class for location as per your JSON structure
    @IgnoreExtraProperties
    public static class UserLocation {
        public Double lat;
        public Double lng;
        public String manual_address;

        public UserLocation() {
            // Default constructor required for calls to DataSnapshot.getValue(UserLocation.class)
        }

        public UserLocation(Double lat, Double lng, String manual_address) {
            this.lat = lat;
            this.lng = lng;
            this.manual_address = manual_address;
        }

        // Getters and setters (optional, but good practice)
        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }

        public String getManual_address() {
            return manual_address;
        }

        public void setManual_address(String manual_address) {
            this.manual_address = manual_address;
        }
    }
}