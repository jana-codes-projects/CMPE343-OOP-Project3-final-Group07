package com.cmpe343.model;

public class Product {

    private int id;
    private String name;
    private String type;
    private double price;
    private double stockKg;
    private double thresholdKg;

    public Product(int id, String name, String type,
            double price, double stockKg, double thresholdKg) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.stockKg = stockKg;
        this.thresholdKg = thresholdKg;
    }

    public Product(int id, String name, String type, double price, double stockKg) {
        this(id, name, type, price, stockKg, 0);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
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
}
