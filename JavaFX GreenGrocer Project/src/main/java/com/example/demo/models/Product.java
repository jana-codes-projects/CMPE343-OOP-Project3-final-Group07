package com.example.demo.models;

import javafx.scene.image.Image;

import java.math.BigDecimal;

/**
 * Represents a product (fruit or vegetable) in the system.
 * 
 * @author Group07
 * @version 1.0
 */
public class Product {
    private int id;
    private String name;
    private ProductType type;
    private BigDecimal price;
    private BigDecimal stockKg;
    private BigDecimal thresholdKg;
    private byte[] imageBlob;
    private Image image;
    private boolean isActive;

    /**
     * Enumeration of product types.
     */
    public enum ProductType {
        VEG, FRUIT
    }

    /**
     * Default constructor.
     */
    public Product() {
    }

    /**
     * Constructor with all parameters.
     * 
     * @param id the product ID
     * @param name the product name
     * @param type the product type
     * @param price the price per kg
     * @param stockKg the stock in kg
     * @param thresholdKg the threshold in kg (when stock is below this, price doubles)
     * @param imageBlob the product image as bytes
     * @param isActive whether the product is active
     */
    public Product(int id, String name, ProductType type, BigDecimal price, 
                   BigDecimal stockKg, BigDecimal thresholdKg, byte[] imageBlob, boolean isActive) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.stockKg = stockKg;
        this.thresholdKg = thresholdKg;
        this.imageBlob = imageBlob;
        this.isActive = isActive;
    }

    /**
     * Gets the effective price based on stock and threshold.
     * If stock is less than or equal to threshold, price is doubled.
     * 
     * @return the effective price
     */
    public BigDecimal getEffectivePrice() {
        if (stockKg.compareTo(thresholdKg) <= 0) {
            return price.multiply(new BigDecimal("2.00"));
        }
        return price;
    }

    /**
     * Checks if the product is in low stock (at or below threshold).
     * 
     * @return true if stock is at or below threshold
     */
    public boolean isLowStock() {
        return stockKg.compareTo(thresholdKg) <= 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductType getType() {
        return type;
    }

    public void setType(ProductType type) {
        this.type = type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getStockKg() {
        return stockKg;
    }

    public void setStockKg(BigDecimal stockKg) {
        this.stockKg = stockKg;
    }

    public BigDecimal getThresholdKg() {
        return thresholdKg;
    }

    public void setThresholdKg(BigDecimal thresholdKg) {
        this.thresholdKg = thresholdKg;
    }

    public byte[] getImageBlob() {
        return imageBlob;
    }

    public void setImageBlob(byte[] imageBlob) {
        this.imageBlob = imageBlob;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", price=" + price +
                ", stockKg=" + stockKg +
                ", thresholdKg=" + thresholdKg +
                '}';
    }
}

