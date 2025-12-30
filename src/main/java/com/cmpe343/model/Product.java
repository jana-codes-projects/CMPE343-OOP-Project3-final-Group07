package com.cmpe343.model;

public class Product {

    public enum ProductType {
        FRUIT, VEGETABLE, OTHER
    }

    private int id;
    private String name;
    private ProductType type;
    private double price;
    private double stockKg;
    private double thresholdKg;
    private String imagePath;
    private boolean active = true; // Added for OwnerController compatibility

    public Product(int id, String name, ProductType type,
            double price, double stockKg, double thresholdKg, String imagePath) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.stockKg = stockKg;
        this.thresholdKg = thresholdKg;
        this.imagePath = imagePath;
    }

    // Constructor for DB reading of existing string types
    public Product(int id, String name, String typeStr,
            double price, double stockKg, double thresholdKg, String imagePath) {
        this(id, name, parseType(typeStr), price, stockKg, thresholdKg, imagePath);
    }

    public Product(int id, String name, String typeStr, double price, double stockKg) {
        this(id, name, parseType(typeStr), price, stockKg, 0, null);
    }

    private static ProductType parseType(String typeStr) {
        if (typeStr == null)
            return ProductType.OTHER;
        try {
            return ProductType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Mapping for existing "Fruit" / "Vegetable" strings
            if (typeStr.equalsIgnoreCase("Fruit"))
                return ProductType.FRUIT;
            if (typeStr.equalsIgnoreCase("Vegetable"))
                return ProductType.VEGETABLE;
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

    public String getImagePath() {
        return imagePath;
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
}
