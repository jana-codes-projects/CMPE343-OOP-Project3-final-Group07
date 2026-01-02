package com.cmpe343.model;

import java.time.LocalDateTime;

public class Message {

    private int id;
    private int senderId;
    private String sender;
    private String content;
    private LocalDateTime timestamp;
    private boolean read;

    public Message(int id, String sender, String content, LocalDateTime timestamp, boolean read) {
        this(id, -1, sender, content, timestamp, read);
    }

    public Message(int id, int senderId, String sender, String content, LocalDateTime timestamp, boolean read) {
        this.id = id;
        this.senderId = senderId;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.read = read;
    }

    public int getId() {
        return id;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
