package com.cmpe343.model;

public class User {
    private int id;
    private String username;
    private String role;
    private String phone;
    private String address;
    private boolean active;
    private double balance;

    public User(int id, String username, String role) {
        this(id, username, role, null, null, true, 0.0);
    }

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
