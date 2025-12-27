package com.example.demo.models;
/**
 * Product model representing the ProductInfo table in the database.
 */
public class Product {
    private int id;
    private String name;
    private String type; // Options: fruit / vegetable
    private double price;
    private double stock;
    private String imageLocation;
    private double threshold;

    public Product() {}

    public Product(int id, String name, String type, double price, double stock, String imageLocation, double threshold) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.stock = stock;
        this.imageLocation = imageLocation;
        this.threshold = threshold;
    }

    /**
     * Logic: If stock is less than or equal to threshold, the price doubles.
     * Required by project documentation.
     */
    public double getCurrentPrice() {
        if (this.stock <= this.threshold) {
            return this.price * 2.0;
        }
        return this.price;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getStock() { return stock; }
    public void setStock(double stock) { this.stock = stock; }

    public String getImageLocation() { return imageLocation; }
    public void setImageLocation(String imageLocation) { this.imageLocation = imageLocation; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
}