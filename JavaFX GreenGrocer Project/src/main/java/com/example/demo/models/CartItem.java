package com.example.demo.models;

import java.math.BigDecimal;

/**
 * Represents an item in the shopping cart.
 * 
 * @author Group07
 * @version 1.0
 */
public class CartItem {
    private Product product;
    private BigDecimal quantityKg;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    /**
     * Default constructor.
     */
    public CartItem() {
    }

    /**
     * Constructor with product and quantity.
     * 
     * @param product the product
     * @param quantityKg the quantity in kg
     */
    public CartItem(Product product, BigDecimal quantityKg) {
        this.product = product;
        this.quantityKg = quantityKg;
        this.unitPrice = product.getEffectivePrice();
        calculateLineTotal();
    }

    /**
     * Calculates the line total for this item.
     */
    private void calculateLineTotal() {
        this.lineTotal = unitPrice.multiply(quantityKg);
    }

    /**
     * Adds quantity to this cart item.
     * Used when merging duplicate products in cart.
     * 
     * @param additionalKg the additional quantity in kg
     */
    public void addQuantity(BigDecimal additionalKg) {
        this.quantityKg = this.quantityKg.add(additionalKg);
        calculateLineTotal();
    }

    /**
     * Gets the product.
     * 
     * @return the product
     */
    public Product getProduct() {
        return product;
    }

    /**
     * Sets the product.
     * 
     * @param product the product to set
     */
    public void setProduct(Product product) {
        this.product = product;
        this.unitPrice = product.getEffectivePrice();
        calculateLineTotal();
    }

    /**
     * Gets the quantity in kg.
     * 
     * @return the quantity in kg
     */
    public BigDecimal getQuantityKg() {
        return quantityKg;
    }

    /**
     * Sets the quantity in kg.
     * 
     * @param quantityKg the quantity to set
     */
    public void setQuantityKg(BigDecimal quantityKg) {
        this.quantityKg = quantityKg;
        calculateLineTotal();
    }

    /**
     * Gets the unit price.
     * 
     * @return the unit price
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * Sets the unit price.
     * 
     * @param unitPrice the unit price to set
     */
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateLineTotal();
    }

    /**
     * Gets the line total.
     * 
     * @return the line total
     */
    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    /**
     * Sets the line total.
     * 
     * @param lineTotal the line total to set
     */
    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return product.getId() == cartItem.product.getId();
    }

    @Override
    public int hashCode() {
        return product.getId();
    }
}

