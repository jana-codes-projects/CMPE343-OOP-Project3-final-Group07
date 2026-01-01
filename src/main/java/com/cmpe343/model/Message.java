package com.cmpe343.model;

import java.time.LocalDateTime;

public class Message {

    private int id;
    private String sender;
    private String content;
    private LocalDateTime timestamp;
    private boolean read;
    private LocalDateTime repliedAt;

    public Message(int id, String sender, String content, LocalDateTime timestamp, boolean read) {
        this(id, sender, content, timestamp, read, null);
    }

    public Message(int id, String sender, String content, LocalDateTime timestamp, boolean read,
            LocalDateTime repliedAt) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.read = read;
        this.repliedAt = repliedAt;
    }

    public int getId() {
        return id;
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

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }
}
