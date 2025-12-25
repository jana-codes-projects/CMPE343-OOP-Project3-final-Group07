package com.example.demo.models;

import java.time.LocalDateTime;

/**
 * Represents an owner user in the system.
 * Extends User to demonstrate inheritance.
 * 
 * @author Group07
 * @version 1.0
 */
public class Owner extends User {
    /**
     * Default constructor.
     */
    public Owner() {
        super();
        setRole(User.Role.OWNER);
    }

    /**
     * Constructor with all parameters.
     * 
     * @param id the owner ID
     * @param username the username
     * @param passwordHash the password hash
     * @param isActive whether the owner is active
     * @param address the owner's address
     * @param phone the owner's phone number
     * @param createdAt the creation timestamp
     */
    public Owner(int id, String username, String passwordHash, boolean isActive,
                 String address, String phone, LocalDateTime createdAt) {
        super(id, username, passwordHash, User.Role.OWNER, isActive, address, phone, createdAt);
    }
}

