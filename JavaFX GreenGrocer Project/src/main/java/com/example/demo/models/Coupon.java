package com.example.demo.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a discount coupon in the system.
 * 
 * @author Group07
 * @version 1.0
 */
public class Coupon {
    private int id;
    private String code;
    private CouponType type;
    private BigDecimal value;
    private BigDecimal minCartValue;
    private boolean isActive;
    private LocalDateTime expiresAt;

    /**
     * Enumeration of coupon types.
     */
    public enum CouponType {
        AMOUNT, PERCENT
    }

    /**
     * Default constructor.
     */
    public Coupon() {
    }

    /**
     * Constructor with all parameters.
     * 
     * @param id the coupon ID
     * @param code the coupon code
     * @param type the coupon type (AMOUNT or PERCENT)
     * @param value the coupon value
     * @param minCartValue the minimum cart value required
     * @param isActive whether the coupon is active
     * @param expiresAt the expiration date/time
     */
    public Coupon(int id, String code, CouponType type, BigDecimal value, 
                  BigDecimal minCartValue, boolean isActive, LocalDateTime expiresAt) {
        this.id = id;
        this.code = code;
        this.type = type;
        this.value = value;
        this.minCartValue = minCartValue;
        this.isActive = isActive;
        this.expiresAt = expiresAt;
    }

    /**
     * Calculates the discount amount based on the coupon type and cart total.
     * 
     * @param cartTotal the cart total amount
     * @return the discount amount
     */
    public BigDecimal calculateDiscount(BigDecimal cartTotal) {
        if (!isActive) {
            return BigDecimal.ZERO;
        }
        
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return BigDecimal.ZERO;
        }
        
        if (cartTotal.compareTo(minCartValue) < 0) {
            return BigDecimal.ZERO;
        }
        
        if (type == CouponType.AMOUNT) {
            return value.compareTo(cartTotal) > 0 ? cartTotal : value;
        } else { // PERCENT
            return cartTotal.multiply(value).divide(new BigDecimal("100"));
        }
    }

    /**
     * Checks if the coupon is valid for the given cart total.
     * 
     * @param cartTotal the cart total amount
     * @return true if the coupon is valid
     */
    public boolean isValidForCart(BigDecimal cartTotal) {
        if (!isActive) {
            return false;
        }
        
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        
        return cartTotal.compareTo(minCartValue) >= 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public CouponType getType() {
        return type;
    }

    public void setType(CouponType type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getMinCartValue() {
        return minCartValue;
    }

    public void setMinCartValue(BigDecimal minCartValue) {
        this.minCartValue = minCartValue;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}

