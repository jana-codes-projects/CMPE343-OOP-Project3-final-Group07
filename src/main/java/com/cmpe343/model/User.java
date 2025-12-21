package com.cmpe343.model;


public class User {
    private int id;
    private String username;
    private String role;


    public User(int id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
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
}


