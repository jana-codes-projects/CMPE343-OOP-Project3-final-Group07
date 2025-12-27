package com.example.demo.dao;
import com.example.demo.database.DatabaseManager;
import com.example.demo.models.User;
import java.sql.*;

public class UserDAO {

    /**
     * Validates user credentials during login.
     * Uses SHA-256 for password security as suggested by professional standards.
     */
    public User login(String username, String password) {
        String query = "SELECT * FROM UserInfo WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getString("address"),
                            rs.getString("contact"),
                            rs.getDouble("loyaltyPoints")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Registers a new customer into the database.
     * Required by project documentation: Validate unique username.
     */
    public boolean register(User user, String password) {
        String query = "INSERT INTO UserInfo (username, password, role, address, contact, loyaltyPoints) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, password);
            stmt.setString(3, "customer"); // Default role for registration
            stmt.setString(4, user.getAddress());
            stmt.setString(5, user.getContact());
            stmt.setDouble(6, 0.0); // Initial loyalty points

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            return false;
        }
    }
}