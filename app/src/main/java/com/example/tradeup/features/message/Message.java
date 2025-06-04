package com.example.tradeup.features.message;

import java.util.Date;

public class Message {
    private String messageId;
    private String chatId;
    private String senderId;
    private String receiverId;
    private String content;
    private String type; // text, image, etc.
    private Date timestamp;

    public Message() {}

    public Message(String messageId, String chatId, String senderId, String receiverId,
                   String content, String type, Date timestamp) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public String getChatId() { return chatId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public Date getTimestamp() { return timestamp; }

    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
