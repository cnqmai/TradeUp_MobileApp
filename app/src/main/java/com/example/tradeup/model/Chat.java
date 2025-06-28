// models/Chat.java
package com.example.tradeup.model;

public class Chat {
    private String chatId;
    private String user_1;
    private String user_2;
    private Boolean blocked;
    private Boolean reported;
    private String lastMessageTimestamp;
    private String lastMessage; // Thêm trường này
    private Integer user1UnreadCount; // Số tin nhắn chưa đọc cho user_1
    private Integer user2UnreadCount;

    public Chat() {
        // Required for Firebase
    }

    public Chat(String chatId, String user_1, String user_2, Boolean blocked, Boolean reported, String lastMessageTimestamp, String lastMessage, Integer user1UnreadCount, Integer user2UnreadCount) {
        this.chatId = chatId;
        this.user_1 = user_1;
        this.user_2 = user_2;
        this.blocked = blocked;
        this.reported = reported;
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

    public String getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(String lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getLastMessage() { return lastMessage; } // Getter mới
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; } // Setter mới

    // Getters and Setters cho các trường mới
    public Integer getUser1UnreadCount() { return user1UnreadCount; }
    public void setUser1UnreadCount(Integer user1UnreadCount) { this.user1UnreadCount = user1UnreadCount; }

    public Integer getUser2UnreadCount() { return user2UnreadCount; }
    public void setUser2UnreadCount(Integer user2UnreadCount) { this.user2UnreadCount = user2UnreadCount; }
}