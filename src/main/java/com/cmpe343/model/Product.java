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
    private boolean active = true; // Added for OwnerController compatibility

    public Product(int id, String name, ProductType type,
            double price, double stockKg, double thresholdKg) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.stockKg = stockKg;
        this.thresholdKg = thresholdKg;
    }

    // Constructor for DB reading of existing string types
    public Product(int id, String name, String typeStr,
            double price, double stockKg, double thresholdKg) {
        this(id, name, parseType(typeStr), price, stockKg, thresholdKg);
    }

    public Product(int id, String name, String typeStr, double price, double stockKg) {
        this(id, name, parseType(typeStr), price, stockKg, 0);
    }

    private static ProductType parseType(String typeStr) {
        if (typeStr == null)
            return ProductType.OTHER;
        String upper = typeStr.toUpperCase();
        // Map database values to enum (database uses VEG, code uses VEGETABLE)
        if (upper.equals("VEG"))
            return ProductType.VEGETABLE;
        if (upper.equals("FRUIT"))
            return ProductType.FRUIT;
        try {
            return ProductType.valueOf(upper);
        } catch (IllegalArgumentException e) {
            // Mapping for existing "Fruit" / "Vegetable" strings
            if (upper.equals("FRUIT"))
                return ProductType.FRUIT;
            if (upper.equals("VEGETABLE"))
                return ProductType.VEGETABLE;
            return ProductType.OTHER;
        }
    }
    
    /**
     * Converts ProductType enum to database string format (VEG/FRUIT).
     */
    public String getTypeAsDbString() {
        return switch (type) {
            case VEGETABLE -> "VEG";
            case FRUIT -> "FRUIT";
            case OTHER -> "VEG"; // Default fallback
        };
    }
    
    /**
     * Gets display name for product type (for UI).
     */
    public String getTypeDisplayName() {
        return switch (type) {
            case VEGETABLE -> "Vegetable";
            case FRUIT -> "Fruit";
            case OTHER -> "Other";
        };
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
