package edu.cuhk.csci3310.buddyconnect.chatbot;

/*
 * Chat mssage model
 */
public class ChatMessage {
    public static final int TYPE_USER = 1;
    public static final int TYPE_BOT = 2;

    private String message;
    private int type;
    private long timestamp;

    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 