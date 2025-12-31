package com.cmpe343.model;

import java.time.LocalDate;

public class Coupon {
    private int id;
    private String code;
    private double discountAmount;
    private LocalDate expiryDate;
    private boolean active;

    public Coupon(int id, String code, double discountAmount, LocalDate expiryDate, boolean active) {
        this.id = id;
        this.code = code;
        this.discountAmount = discountAmount;
        this.expiryDate = expiryDate;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public boolean isActive() {
        return active;
    }
}
