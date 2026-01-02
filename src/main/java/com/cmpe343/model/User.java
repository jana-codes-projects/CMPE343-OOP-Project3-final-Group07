package com.cmpe343.model;

/**
 * Represents a user in the GreenGrocer system.
 * Users can have different roles: customer, carrier, or owner.
 * Each role has different permissions and access levels within the application.
 * 
 * @author Group07
 * @version 1.0
 */
public class User {
    private int id;
    private String username;
    private String role;
    private String phone;
    private String address;
    private boolean active;
    private double balance;

    /**
     * Creates a new User with minimal information.
     * 
     * @param id       The unique identifier for the user
     * @param username The username for login
     * @param role     The user's role (customer, carrier, or owner)
     */
    public User(int id, String username, String role) {
        this(id, username, role, null, null, true, 0.0);
    }

    /**
     * Creates a new User with complete information.
     * 
     * @param id       The unique identifier for the user
     * @param username The username for login
     * @param role     The user's role (customer, carrier, or owner)
     * @param phone    The user's phone number
     * @param address  The user's address for delivery
     * @param active   Whether the user account is active
     * @param balance  The user's wallet balance
     */
    public User(int id, String username, String role, String phone, String address, boolean active, double balance) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.phone = phone;
        this.address = address;
        this.active = active;
        this.balance = balance;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
