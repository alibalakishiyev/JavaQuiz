package com.ali.chatBot;


public class ChatMessage {
    private String senderId;
    private String text;
    private String type;
    private long timestamp;
    private boolean isUserMessage;

    public ChatMessage() {
    }

    // Full constructor with all parameters
    public ChatMessage(String senderId, String text, String type, long timestamp, boolean isUserMessage) {
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.timestamp = timestamp;
        this.isUserMessage = isUserMessage;
    }
    // Simplified constructor (without type)
    public ChatMessage(String senderId, String text, long timestamp, boolean isUserMessage) {
        this(senderId, text, "text", timestamp, isUserMessage); // Default type to "text"
    }


    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }

    public void setUserMessage(boolean userMessage) {
        isUserMessage = userMessage;
    }
}