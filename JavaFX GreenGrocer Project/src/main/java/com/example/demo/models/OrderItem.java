package com.example.demo.models;

import java.math.BigDecimal;

/**
 * Represents an item within an order.
 * 
 * @author Group07
 * @version 1.0
 */
public class OrderItem {
    private int id;
    private int orderId;
    private int productId;
    private BigDecimal quantityKg;
    private BigDecimal unitPriceApplied;
    private BigDecimal lineTotal;
    private Product product;

    /**
     * Default constructor.
     */
    public OrderItem() {
    }

    /**
     * Constructor with parameters.
     * 
     * @param orderId the order ID
     * @param productId the product ID
     * @param quantityKg the quantity in kg
     * @param unitPriceApplied the unit price applied
     */
    public OrderItem(int orderId, int productId, BigDecimal quantityKg, BigDecimal unitPriceApplied) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantityKg = quantityKg;
        this.unitPriceApplied = unitPriceApplied;
        this.lineTotal = unitPriceApplied.multiply(quantityKg);
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

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public BigDecimal getQuantityKg() {
        return quantityKg;
    }

    public void setQuantityKg(BigDecimal quantityKg) {
        this.quantityKg = quantityKg;
        if (unitPriceApplied != null) {
            this.lineTotal = unitPriceApplied.multiply(quantityKg);
        }
    }

    public BigDecimal getUnitPriceApplied() {
        return unitPriceApplied;
    }

    public void setUnitPriceApplied(BigDecimal unitPriceApplied) {
        this.unitPriceApplied = unitPriceApplied;
        if (quantityKg != null) {
            this.lineTotal = unitPriceApplied.multiply(quantityKg);
        }
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}

