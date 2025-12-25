package com.example.demo.controllers;

import com.example.demo.models.User;

/**
 * Base controller class for all role-based controllers.
 * Provides common functionality for setting the current user.
 * 
 * @author Group07
 * @version 1.0
 */
public abstract class BaseController {
    protected User currentUser;

    /**
     * Sets the current user for this controller.
     * 
     * @param user the current user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Gets the current user.
     * 
     * @return the current user
     */
    public User getCurrentUser() {
        return currentUser;
    }
}

