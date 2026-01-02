package com.cmpe343.model;

/**
 * Represents an item in a shopping cart or order.
 * Tracks product, quantity, and pricing information.
 * Supports both current cart items (live pricing) and historical order items
 * (stored pricing).
 * 
 * @author Group07
 * @version 1.0
 */
public class CartItem {
    private final Product product;
    private double quantityKg;
    // Historical pricing for order items (null for current cart items)
    private Double historicalUnitPrice;
    private Double historicalLineTotal;

    public CartItem(Product product, double quantityKg) {
        this.product = product;
        this.quantityKg = quantityKg;
        this.historicalUnitPrice = null;
        this.historicalLineTotal = null;
    }

    /**
     * Constructor for order items with historical pricing.
     * This preserves the price that was applied at order creation time.
     */
    public CartItem(Product product, double quantityKg, double historicalUnitPrice, double historicalLineTotal) {
        this.product = product;
        this.quantityKg = quantityKg;
        this.historicalUnitPrice = historicalUnitPrice;
        this.historicalLineTotal = historicalLineTotal;
    }

    public Product getProduct() {
        return product;
    }

    public double getQuantityKg() {
        return quantityKg;
    }

    public void setQuantityKg(double quantityKg) {
        this.quantityKg = quantityKg;
    }

    /**
     * Returns the unit price. For historical order items, returns the price at
     * order time.
     * For current cart items, returns the current product price.
     */
    public double getUnitPrice() {
        return historicalUnitPrice != null ? historicalUnitPrice : product.getPrice();
    }

    /**
     * Returns the line total. For historical order items, returns the stored line
     * total.
     * For current cart items, calculates from current product price.
     */
    public double getLineTotal() {
        return historicalLineTotal != null ? historicalLineTotal : (product.getPrice() * quantityKg);
    }
}
