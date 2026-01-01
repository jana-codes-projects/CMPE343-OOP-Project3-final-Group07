package com.cmpe343.model;

import java.io.InputStream;

public class Product {

    public enum ProductType {
        VEG, FRUIT, OTHER
    }

    private int id;
    private String name;
    private ProductType type;
    private double price;
    private double stockKg;
    private double thresholdKg;
    private byte[] imageBlob; // For holding image data
    private boolean active = true;

    public Product(int id, String name, ProductType type,
            double price, double stockKg, double thresholdKg, byte[] imageBlob) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.stockKg = stockKg;
        this.thresholdKg = thresholdKg;
        this.imageBlob = imageBlob;
    }

    // Constructor for DB reading of existing string types
    public Product(int id, String name, String typeStr,
            double price, double stockKg, double thresholdKg, byte[] imageBlob) {
        this(id, name, parseType(typeStr), price, stockKg, thresholdKg, imageBlob);
    }

    private static ProductType parseType(String typeStr) {
        if (typeStr == null)
            return ProductType.OTHER;
        try {
            return ProductType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ProductType.OTHER;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ProductType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public double getStockKg() {
        return stockKg;
    }

    public double getThresholdKg() {
        return thresholdKg;
    }

    public byte[] getImageBlob() {
        return imageBlob;
    }

    public boolean isLowStock() {
        return stockKg <= thresholdKg;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getEffectivePrice() {
        return isLowStock() ? price * 2 : price;
    }

    public String getTypeDisplayName() {
        return type == ProductType.VEG ? "VEG" : (type == ProductType.FRUIT ? "FRUIT" : "OTHER");
    }

    public String getTypeAsDbString() {
        return type == ProductType.VEG ? "VEG" : (type == ProductType.FRUIT ? "FRUIT" : "OTHER");
    }
}
