package com.cmpe343.service;

import com.cmpe343.dao.UserDao;
import com.cmpe343.model.User;

/**
 * Service for user authentication.
 * Provides login functionality using password hashing verification.
 * 
 * @author Group07
 * @version 1.0
 */
public class AuthService {

    private final UserDao userDao = new UserDao();

    public User login(String username, String password) {
        return userDao.login(username, password);
    }
}