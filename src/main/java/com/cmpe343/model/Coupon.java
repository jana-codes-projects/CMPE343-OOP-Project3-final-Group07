package com.cmpe343.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a discount coupon in the GreenGrocer system.
 * Coupons can provide either a fixed amount discount or a percentage discount.
 * They can have minimum cart requirements and expiration dates.
 * 
 * @author Group07
 * @version 1.0
 */
public class Coupon {
    /**
     * Types of discount coupons.
     */
    public enum CouponKind {
        /** Fixed amount discount (e.g., 25 TL off) */
        AMOUNT,
        /** Percentage discount (e.g., 10% off) */
        PERCENT
    }

    private int id;
    private String code;
    private CouponKind kind;
    private double value;
    private double minCart;
    private boolean isActive;
    private LocalDateTime expiresAt;

    public Coupon(int id, String code, CouponKind kind, double value, double minCart, boolean isActive,
            LocalDateTime expiresAt) {
        this.id = id;
        this.code = code;
        this.kind = kind;
        this.value = value;
        this.minCart = minCart;
        this.isActive = isActive;
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
        return isActive;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Calculates the discount amount based on the coupon type and cart total.
     * For AMOUNT coupons: returns the fixed value
     * For PERCENT coupons: returns the percentage of cartTotal
     * 
     * @param cartTotal The cart total before discount
     * @return The discount amount to apply
     */
    public double calculateDiscount(double cartTotal) {
        if (cartTotal < minCart) {
            return 0.0; // Cart doesn't meet minimum requirement
        }

        if (kind == CouponKind.AMOUNT) {
            return Math.min(value, cartTotal); // Don't discount more than cart total
        } else { // PERCENT
            return cartTotal * (value / 100.0);
        }
    }

    /**
     * Legacy method for backward compatibility.
     * Returns the value directly (for AMOUNT) or 0 (for PERCENT, which needs cart
     * total).
     * Use calculateDiscount(cartTotal) instead for accurate discount calculation.
     */
    @Deprecated
    public double getDiscountAmount() {
        return kind == CouponKind.AMOUNT ? value : 0.0;
    }

    /**
     * Legacy method for backward compatibility.
     */
    @Deprecated
    public LocalDate getExpiryDate() {
        return expiresAt != null ? expiresAt.toLocalDate() : null;
    }
}
