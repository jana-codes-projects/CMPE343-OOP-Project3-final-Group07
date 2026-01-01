package com.cmpe343.model;

public class User {
    private int id;
    private String username;
    private String role;
    private String phone;
    private String address;
    private boolean active;
    private double walletBalance;

    public User(int id, String username, String role) {
        this(id, username, role, null, null, true, 0.0);
    }

    public User(int id, String username, String role, String phone, String address, boolean active, double walletBalance) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.phone = phone;
        this.address = address;
        this.active = active;
        this.walletBalance = walletBalance;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }
}