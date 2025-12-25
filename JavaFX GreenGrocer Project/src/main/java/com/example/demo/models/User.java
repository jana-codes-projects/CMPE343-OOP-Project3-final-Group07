package com.example.demo.models;

import java.time.LocalDateTime;

/**
 * Base class representing a user in the system.
 * Implements encapsulation with private fields and public getters/setters.
 * 
 * @author Group07
 * @version 1.0
 */
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private boolean isActive;
    private String address;
    private String phone;
    private LocalDateTime createdAt;

    /**
     * Enumeration of user roles in the system.
     */
    public enum Role {
        CUSTOMER, CARRIER, OWNER
    }

    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Constructor with all parameters.
     * 
     * @param id the user ID
     * @param username the username
     * @param passwordHash the password hash
     * @param role the user role
     * @param isActive whether the user is active
     * @param address the user's address
     * @param phone the user's phone number
     * @param createdAt the creation timestamp
     */
    public User(int id, String username, String passwordHash, Role role, boolean isActive,
                String address, String phone, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
        this.address = address;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}

