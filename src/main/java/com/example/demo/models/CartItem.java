package com.example.demo.models;

public class CartItem {
    private Product product;
    private double quantity;
    private double priceAtPurchase;

    public CartItem(Product product, double quantity) {
        this.product = product;
        this.quantity = quantity;
        this.priceAtPurchase = product.getCurrentPrice(); // Using the threshold logic
    }

    // Merging logic required by documentation (e.g., 1.25kg + 0.75kg = 2kg)
    public void addQuantity(double amount) {
        this.quantity += amount;
    }

    public double getTotalPrice() {
        return this.quantity * this.priceAtPurchase;
    }

    public Product getProduct() { return product; }
    public double getQuantity() { return quantity; }
}