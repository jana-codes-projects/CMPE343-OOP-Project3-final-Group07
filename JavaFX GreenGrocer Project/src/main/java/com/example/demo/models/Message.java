package com.example.demo.models;

import java.time.LocalDateTime;

/**
 * Represents a message between customer and owner.
 * 
 * @author Group07
 * @version 1.0
 */
public class Message {
    private int id;
    private int customerId;
    private int ownerId;
    private String text;
    private LocalDateTime createdAt;
    private String reply;
    private LocalDateTime repliedAt;

    /**
     * Default constructor.
     */
    public Message() {
    }

    /**
     * Constructor with parameters.
     * 
     * @param customerId the customer ID
     * @param ownerId the owner ID
     * @param text the message text
     */
    public Message(int customerId, int ownerId, String text) {
        this.customerId = customerId;
        this.ownerId = ownerId;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Checks if the message has been replied to.
     * 
     * @return true if the message has been replied to
     */
    public boolean hasReply() {
        return reply != null && !reply.isEmpty();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
        this.repliedAt = LocalDateTime.now();
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }
}

