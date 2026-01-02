package com.cmpe343.model;

import java.time.LocalDateTime;

/**
 * Represents a customer rating for a carrier's delivery service.
 * Ratings are on a 1-5 scale and can include optional comments.
 * Each rating is associated with a specific order delivery.
 * 
 * @author Group07
 * @version 1.0
 */
public class Rating {
    private int id;
    private int carrierId;
    private int customerId;
    private int score; // 1-5
    private String comment;
    private LocalDateTime timestamp;

    /**
     * Creates a new Rating.
     * 
     * @param id         The unique rating identifier
     * @param carrierId  The ID of the carrier being rated
     * @param customerId The ID of the customer giving the rating
     * @param score      The rating score (1-5)
     * @param comment    Optional comment about the delivery
     * @param timestamp  When the rating was submitted
     */
    public Rating(int id, int carrierId, int customerId, int score, String comment, LocalDateTime timestamp) {
        this.id = id;
        this.carrierId = carrierId;
        this.customerId = customerId;
        this.score = score;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public int getCarrierId() {
        return carrierId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getScore() {
        return score;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
