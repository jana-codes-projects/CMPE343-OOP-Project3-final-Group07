package com.cmpe343.model;

public class User {
    private int id;
    private String username;
    private String role;
    private String phone;
    private String address;
    private boolean active;

    private int loyaltyLevel;

    public User(int id, String username, String role) {
        this(id, username, role, null, null, true, 0);
    }

    public User(int id, String username, String role, String phone, String address, boolean active) {
        this(id, username, role, phone, address, active, 0);
    }

    public User(int id, String username, String role, String phone, String address, boolean active, int loyaltyLevel) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.phone = phone;
        this.address = address;
        this.active = active;
        this.loyaltyLevel = loyaltyLevel;
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

    public int getLoyaltyLevel() {
        return loyaltyLevel;
    }

    public void setLoyaltyLevel(int loyaltyLevel) {
        this.loyaltyLevel = loyaltyLevel;
    }
}
