package com.example.demo.models;

import java.time.LocalDateTime;

/**
 * Represents a rating given by a customer to a carrier.
 * 
 * @author Group07
 * @version 1.0
 */
public class Rating {
    private int id;
    private int orderId;
    private int carrierId;
    private int customerId;
    private int rating; // 1-5
    private String comment;
    private LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    public Rating() {
    }

    /**
     * Constructor with parameters.
     * 
     * @param orderId the order ID
     * @param carrierId the carrier ID
     * @param customerId the customer ID
     * @param rating the rating (1-5)
     * @param comment the comment
     */
    public Rating(int orderId, int carrierId, int customerId, int rating, String comment) {
        this.orderId = orderId;
        this.carrierId = carrierId;
        this.customerId = customerId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Validates that the rating is between 1 and 5.
     * 
     * @param rating the rating to validate
     * @return true if valid
     */
    public static boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(int carrierId) {
        this.carrierId = carrierId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (isValidRating(rating)) {
            this.rating = rating;
        } else {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

