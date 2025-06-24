// models/Chat.java
package com.example.tradeup.model; // Corrected package name

public class Chat {
    private String user_1;
    private String user_2;
    private Boolean blocked;
    private Boolean reported;

    public Chat() {
        // Required for Firebase
    }

    public Chat(String user_1, String user_2, Boolean blocked, Boolean reported) {
        this.user_1 = user_1;
        this.user_2 = user_2;
        this.blocked = blocked;
        this.reported = reported;
    }

    // Getters and Setters
    public String getUser_1() { return user_1; }
    public void setUser_1(String user_1) { this.user_1 = user_1; }
    public String getUser_2() { return user_2; }
    public void setUser_2(String user_2) { this.user_2 = user_2; }
    public Boolean getBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }
    public Boolean getReported() { return reported; }
    public void setReported(Boolean reported) { this.reported = reported; }
}