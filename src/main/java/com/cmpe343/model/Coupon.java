package com.cmpe343.model;

import java.time.LocalDateTime;

public class Coupon {

    public enum CouponKind {
        PERCENT, FIXED
    }

    private int id;
    private String code;
    private CouponKind kind;
    private double value;
    private double minCart;
    private boolean active;
    private LocalDateTime expiresAt;

    public Coupon(int id, String code, CouponKind kind, double value, double minCart, boolean active,
            LocalDateTime expiresAt) {
        this.id = id;
        this.code = code;
        this.kind = kind;
        this.value = value;
        this.minCart = minCart;
        this.active = active;
        this.expiresAt = expiresAt;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public CouponKind getKind() {
        return kind;
    }

    public double getValue() {
        return value;
    }

    public double getMinCart() {
        return minCart;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
