package com.cmpe343.model;

import java.time.LocalDateTime;

/**
 * Represents a message between a customer and the owner.
 * Messages support owner replies and read status tracking.
 * Used for customer inquiries and owner responses.
 * 
 * @author Group07
 * @version 1.0
 */
public class Message {

    private int id;
    private String sender;
    private String content;
    private LocalDateTime timestamp;
    private boolean read;
    private LocalDateTime repliedAt;

    /**
     * Creates a new Message without reply information.
     * 
     * @param id        The unique message identifier
     * @param sender    The username of the sender
     * @param content   The message content
     * @param timestamp When the message was sent
     * @param read      Whether the message has been read
     */
    public Message(int id, String sender, String content, LocalDateTime timestamp, boolean read) {
        this(id, sender, content, timestamp, read, null);
    }

    /**
     * Creates a new Message with reply information.
     * 
     * @param id        The unique message identifier
     * @param sender    The username of the sender
     * @param content   The message content
     * @param timestamp When the message was sent
     * @param read      Whether the message has been read
     * @param repliedAt When the owner replied (null if no reply)
     */
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
