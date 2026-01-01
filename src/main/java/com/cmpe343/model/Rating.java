package com.cmpe343.model;

import java.time.LocalDateTime;

public class Rating {
    private int id;
    private int carrierId;
    private int score;
    private String comment;
    private LocalDateTime timestamp;

    public Rating(int id, int carrierId, int score, String comment, LocalDateTime timestamp) {
        this.id = id;
        this.carrierId = carrierId;
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
