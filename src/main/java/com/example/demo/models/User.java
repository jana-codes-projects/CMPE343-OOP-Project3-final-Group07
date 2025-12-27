package com.example.demo.models;

/**
 * User model representing the UserInfo table in the database.
 */
public class User {
    private int id;
    private String username;
    private String password;
    private String role;
    private String address;
    private String contact;
    private double loyaltyPoints;

    public User() {}

    public User(int id, String username, String role, String address, String contact, double loyaltyPoints) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.address = address;
        this.contact = contact;
        this.loyaltyPoints = loyaltyPoints;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public double getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(double loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
}