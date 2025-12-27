package com.example.demo.database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the connection between the Java application and the MySQL database.
 * Following the CMPE343 Project requirements.
 */
public class DatabaseManager {

    // Connection URL: Protocol + Host (localhost) + Port (3306) + Database Name
    private static final String URL = "jdbc:mysql://localhost:3306/greengrocer_db?useSSL=false&serverTimezone=UTC";

    // Project specific credentials provided in requirements
    private static final String USER = "myuser";
    private static final String PASSWORD = "1234";

    /**
     * Establishes and returns a connection to the database.
     * @return Connection object
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load the MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect using the driver manager
            return DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver Error: " + e.getMessage());
            throw new SQLException("MySQL JDBC Driver not found!");
        } catch (SQLException e) {
            System.err.println("Database Connection Error: " + e.getMessage());
            throw e;
        }
    }
}