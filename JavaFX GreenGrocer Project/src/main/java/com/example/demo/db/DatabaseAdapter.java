package com.example.demo.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database adapter class for managing database connections.
 * Implements singleton pattern to ensure a single connection pool.
 * 
 * @author Group07
 * @version 1.0
 */
public class DatabaseAdapter {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/greengrocer_db";
    private static final String DB_USER = "myuser@localhost";
    private static final String DB_PASSWORD = "1234";
    
    private static DatabaseAdapter instance;
    private Connection connection;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private DatabaseAdapter() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    /**
     * Gets the singleton instance of DatabaseAdapter.
     * 
     * @return the DatabaseAdapter instance
     */
    public static synchronized DatabaseAdapter getInstance() {
        if (instance == null) {
            instance = new DatabaseAdapter();
        }
        return instance;
    }

    /**
     * Gets a database connection.
     * Creates a new connection if none exists or if the existing one is closed.
     * 
     * @return the database connection
     * @throws SQLException if a database error occurs
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    /**
     * Closes the database connection.
     * 
     * @throws SQLException if a database error occurs
     */
    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Tests the database connection.
     * 
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

