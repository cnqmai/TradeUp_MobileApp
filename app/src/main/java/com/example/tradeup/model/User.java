package com.example.tradeup.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Map;
import com.example.tradeup.model.Location; // Import class Location ngoài

@IgnoreExtraProperties
public class User {

    private String uid; // Trường UID của người dùng
    private String email;
    private Boolean is_email_verified; // Trạng thái xác minh email
    private String display_name; // Tên hiển thị của người dùng
    private String bio; // Tiểu sử người dùng
    private String contact_info; // Thông tin liên hệ
    private String profile_picture_url; // URL ảnh đại diện
    private Integer total_transactions; // Tổng số giao dịch thành công
    private String role; // Vai trò của người dùng (ví dụ: user, admin)
    private String account_status; // Trạng thái tài khoản (ví dụ: active, suspended, banned)
    private Location location; // Vị trí người dùng, sử dụng class Location độc lập
    private String created_at; // Thời gian tạo tài khoản (ISO 8601 UTC timestamp)
    private String updated_at; // Thời gian cập nhật tài khoản gần nhất (ISO 8601 UTC timestamp)
    private String first_name; // Tên của người dùng
    private String last_name; // Họ của người dùng

    // Các trường mới cho chức năng Đánh giá (FR-7.2.1)
    private Long rating_sum; // Tổng điểm sao mà người dùng này đã nhận được từ các đánh giá
    private Long rating_count; // Tổng số lượt đánh giá mà người dùng này đã nhận được
    private Double average_rating; // Xếp hạng trung bình (thay thế cho 'rating' cũ)

    // NEW: Trường cho trạng thái bị khóa
    private Boolean is_banned; // Trạng thái bị khóa của người dùng

    public User() {
        // Constructor mặc định cần thiết cho Firebase
    }

    // Constructor đã điều chỉnh để khớp với các trường hiện tại và thêm uid
    public User(String uid, String email, Boolean is_email_verified, String display_name, String bio, String contact_info,
                String profile_picture_url, Integer total_transactions, String role,
                String account_status, Location location, String created_at, String updated_at,
                String first_name, String last_name,
                Long rating_sum, Long rating_count, Double average_rating,
                Boolean is_banned) { // NEW: Added is_banned to constructor
        this.uid = uid;
        this.email = email;
        this.is_email_verified = is_email_verified;
        this.display_name = display_name;
        this.bio = bio;
        this.contact_info = contact_info;
        this.profile_picture_url = profile_picture_url;
        this.total_transactions = total_transactions;
        this.role = role;
        this.account_status = account_status;
        this.location = location;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.first_name = first_name;
        this.last_name = last_name;
        this.rating_sum = rating_sum;
        this.rating_count = rating_count;
        this.average_rating = average_rating;
        this.is_banned = is_banned; // NEW
    }

    // Constructor cho việc tạo User khi đăng ký lần đầu (ít thông tin hơn)
    public User(String uid, String email, String first_name, String last_name, String display_name,
                String bio, String contact_info, String profile_picture_url,
                Integer total_transactions, String role, String account_status,
                Location location, String created_at) {
        this.uid = uid;
        this.email = email;
        this.is_email_verified = false; // Mặc định false khi đăng ký
        this.first_name = first_name;
        this.last_name = last_name;
        this.display_name = display_name;
        this.bio = bio;
        this.contact_info = contact_info;
        this.profile_picture_url = profile_picture_url;
        this.total_transactions = total_transactions;
        this.role = role;
        this.account_status = account_status;
        this.location = location;
        this.created_at = created_at;
        this.updated_at = created_at; // Ban đầu updated_at = created_at
        // Khởi tạo các trường rating mới với giá trị mặc định cho người dùng mới
        this.rating_sum = 0L;
        this.rating_count = 0L;
        this.average_rating = 0.0;
        this.is_banned = false; // NEW: Mặc định không bị khóa khi tạo
    }


    // Getters and Setters cho tất cả các trường

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

    public Location getLocation() { // Getter cho Location
        return location;
    }

    public void setLocation(Location location) { // Setter cho Location
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

    // Getters và Setters cho các trường rating mới
    public Long getRating_sum() {
        return rating_sum;
    }

    public void setRating_sum(Long rating_sum) {
        this.rating_sum = rating_sum;
    }

    public Long getRating_count() {
        return rating_count;
    }

    public void setRating_count(Long rating_count) {
        this.rating_count = rating_count;
    }

    public Double getAverage_rating() {
        return average_rating;
    }

    public void setAverage_rating(Double average_rating) {
        this.average_rating = average_rating;
    }

    // NEW: Getter and Setter for is_banned
    public Boolean getIs_banned() {
        return is_banned;
    }

    public void setIs_banned(Boolean is_banned) {
        this.is_banned = is_banned;
    }
}
