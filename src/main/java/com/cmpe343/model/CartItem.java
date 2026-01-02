package com.cmpe343.model;

public class CartItem {
    private final Product product;
    private double quantityKg;

    public CartItem(Product product, double quantityKg) {
        this.product = product;
        this.quantityKg = quantityKg;
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

    public double getUnitPrice() {
        return product.getPrice();
    }

    public double getLineTotal() {
        if (hasDiscount()) {
            double multiplier = 1.0 - (product.getDiscountPercentage() / 100.0);
            return product.getPrice() * quantityKg * multiplier;
        }
        return product.getPrice() * quantityKg;
    }

    public boolean hasDiscount() {
        return quantityKg > product.getDiscountThreshold();
    }
}
