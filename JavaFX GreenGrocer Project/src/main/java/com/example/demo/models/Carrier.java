package com.example.demo.models;

import java.time.LocalDateTime;

/**
 * Represents a carrier user in the system.
 * Extends User to demonstrate inheritance.
 * 
 * @author Group07
 * @version 1.0
 */
public class Carrier extends User {
    private double averageRating;
    private int totalDeliveries;

    /**
     * Default constructor.
     */
    public Carrier() {
        super();
        setRole(User.Role.CARRIER);
    }

    /**
     * Constructor with all parameters.
     * 
     * @param id the carrier ID
     * @param username the username
     * @param passwordHash the password hash
     * @param isActive whether the carrier is active
     * @param address the carrier's address
     * @param phone the carrier's phone number
     * @param createdAt the creation timestamp
     * @param averageRating the average rating
     * @param totalDeliveries the total number of deliveries
     */
    public Carrier(int id, String username, String passwordHash, boolean isActive,
                   String address, String phone, LocalDateTime createdAt,
                   double averageRating, int totalDeliveries) {
        super(id, username, passwordHash, User.Role.CARRIER, isActive, address, phone, createdAt);
        this.averageRating = averageRating;
        this.totalDeliveries = totalDeliveries;
    }

    /**
     * Gets the average rating.
     * 
     * @return the average rating
     */
    public double getAverageRating() {
        return averageRating;
    }

    /**
     * Sets the average rating.
     * 
     * @param averageRating the average rating to set
     */
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    /**
     * Gets the total number of deliveries.
     * 
     * @return the total number of deliveries
     */
    public int getTotalDeliveries() {
        return totalDeliveries;
    }

    /**
     * Sets the total number of deliveries.
     * 
     * @param totalDeliveries the total number of deliveries to set
     */
    public void setTotalDeliveries(int totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    /**
     * Increments the total deliveries count.
     */
    public void incrementDeliveries() {
        this.totalDeliveries++;
    }
}

