package com.example.demo.models;

import java.time.LocalDateTime;

/**
 * Represents a customer user in the system.
 * Extends User to demonstrate inheritance.
 * 
 * @author Group07
 * @version 1.0
 */
public class Customer extends User {
    private int loyaltyPoints;
    private int completedOrders;

    /**
     * Default constructor.
     */
    public Customer() {
        super();
        setRole(User.Role.CUSTOMER);
    }

    /**
     * Constructor with all parameters.
     * 
     * @param id the customer ID
     * @param username the username
     * @param passwordHash the password hash
     * @param isActive whether the customer is active
     * @param address the customer's address
     * @param phone the customer's phone number
     * @param createdAt the creation timestamp
     * @param loyaltyPoints the loyalty points
     * @param completedOrders the number of completed orders
     */
    public Customer(int id, String username, String passwordHash, boolean isActive,
                    String address, String phone, LocalDateTime createdAt,
                    int loyaltyPoints, int completedOrders) {
        super(id, username, passwordHash, User.Role.CUSTOMER, isActive, address, phone, createdAt);
        this.loyaltyPoints = loyaltyPoints;
        this.completedOrders = completedOrders;
    }

    /**
     * Gets the loyalty points.
     * 
     * @return the loyalty points
     */
    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    /**
     * Sets the loyalty points.
     * 
     * @param loyaltyPoints the loyalty points to set
     */
    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    /**
     * Gets the number of completed orders.
     * 
     * @return the number of completed orders
     */
    public int getCompletedOrders() {
        return completedOrders;
    }

    /**
     * Sets the number of completed orders.
     * 
     * @param completedOrders the number of completed orders to set
     */
    public void setCompletedOrders(int completedOrders) {
        this.completedOrders = completedOrders;
    }

    /**
     * Adds loyalty points.
     * 
     * @param points the points to add
     */
    public void addLoyaltyPoints(int points) {
        this.loyaltyPoints += points;
    }

    /**
     * Calculates the loyalty discount based on completed orders.
     * Rule: 5% discount for 10+ orders, 10% for 25+ orders, 15% for 50+ orders.
     * 
     * @param totalAmount the total amount before discount
     * @return the discount amount
     */
    public double calculateLoyaltyDiscount(double totalAmount) {
        if (completedOrders >= 50) {
            return totalAmount * 0.15; // 15% discount
        } else if (completedOrders >= 25) {
            return totalAmount * 0.10; // 10% discount
        } else if (completedOrders >= 10) {
            return totalAmount * 0.05; // 5% discount
        }
        return 0.0;
    }
}

