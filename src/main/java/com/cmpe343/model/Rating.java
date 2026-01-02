package com.cmpe343.model;

import java.time.LocalDateTime;

public class Rating {
    private int id;
    private int orderId;
    private int carrierId;
    private int customerId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public Rating(int id, int orderId, int carrierId, int customerId, int rating, String comment,
            LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.carrierId = carrierId;
        this.customerId = customerId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getCarrierId() {
        return carrierId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
