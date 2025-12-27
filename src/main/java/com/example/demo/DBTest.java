package com.example.demo;

import com.example.demo.db.DBConnection;

public class DBTest {
    public static void main(String[] args) {
        try {
            DBConnection.getConnection();
            System.out.println("Database connection SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
