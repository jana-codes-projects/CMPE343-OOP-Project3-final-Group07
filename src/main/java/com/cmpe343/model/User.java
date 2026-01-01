package com.cmpe343.model;

public class User {
    private int id;
    private String username;
    private String role;
    private String phone;
    private String address;
    private boolean active;

    public User(int id, String username, String role) {
        this(id, username, role, null, null, true);
    }

    public User(int id, String username, String role, String phone, String address, boolean active) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.phone = phone;
        this.address = address;
        this.active = active;
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
}
