package com.example.tradeup.model;

public class Chat {
    private String chatId;
    private String user_1;
    private String user_2;
    private Boolean blocked;
    private Boolean reported;
    private String reportReason; // NEW: Lý do báo cáo
    private String reportTimestamp; // NEW: Thời gian báo cáo
    private String lastMessageTimestamp;
    private String lastMessage;
    private Integer user1UnreadCount;
    private Integer user2UnreadCount;

    public Chat() {
        // Required for Firebase
    }

    public Chat(String chatId, String user_1, String user_2, Boolean blocked, Boolean reported,
                String reportReason, String reportTimestamp, // NEW: Added report fields to constructor
                String lastMessageTimestamp, String lastMessage, Integer user1UnreadCount, Integer user2UnreadCount) {
        this.chatId = chatId;
        this.user_1 = user_1;
        this.user_2 = user_2;
        this.blocked = blocked;
        this.reported = reported;
        this.reportReason = reportReason; // NEW
        this.reportTimestamp = reportTimestamp; // NEW
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessage = lastMessage;
        this.user1UnreadCount = user1UnreadCount;
        this.user2UnreadCount = user2UnreadCount;
    }


    // Getters and Setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getUser_1() { return user_1; }
    public void setUser_1(String user_1) { this.user_1 = user_1; }

    public String getUser_2() { return user_2; }
    public void setUser_2(String user_2) { this.user_2 = user_2; }

    public Boolean getBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }

    public Boolean getReported() { return reported; }
    public void setReported(Boolean reported) { this.reported = reported; }

    // NEW: Getters and Setters for reportReason
    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }

    // NEW: Getters and Setters for reportTimestamp
    public String getReportTimestamp() { return reportTimestamp; }
    public void setReportTimestamp(String reportTimestamp) { this.reportTimestamp = reportTimestamp; }

    public String getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(String lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Integer getUser1UnreadCount() { return user1UnreadCount; }
    public void setUser1UnreadCount(Integer user1UnreadCount) { this.user1UnreadCount = user1UnreadCount; }

    public Integer getUser2UnreadCount() { return user2UnreadCount; }
    public void setUser2UnreadCount(Integer user2UnreadCount) { this.user2UnreadCount = user2UnreadCount; }
}
