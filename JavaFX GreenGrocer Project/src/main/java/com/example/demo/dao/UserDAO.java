package com.example.demo.dao;

import com.example.demo.db.DatabaseAdapter;
import com.example.demo.models.Customer;
import com.example.demo.models.User;
import com.example.demo.utils.PasswordHasher;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations.
 * Handles all database operations related to users.
 * 
 * @author Group07
 * @version 1.0
 */
public class UserDAO {
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public UserDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Authenticates a user by username and password.
     * 
     * @param username the username
     * @param password the password
     * @return the authenticated user, or null if authentication fails
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = 1";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                // Trim whitespace since password_hash is CHAR(64) and might have padding
                if (storedHash != null) {
                    storedHash = storedHash.trim();
                    // MySQL SHA2() returns uppercase hex, ensure consistent comparison
                    if (PasswordHasher.verifyPassword(password, storedHash)) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during authentication: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if a username already exists.
     * 
     * @param username the username to check
     * @return true if the username exists
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Creates a new customer user.
     * 
     * @param username the username
     * @param password the password
     * @param address the address
     * @param phone the phone number
     * @return the created user, or null if creation fails
     */
    public User createCustomer(String username, String password, String address, String phone) {
        String sql = "INSERT INTO users (username, password_hash, role, is_active, address, phone) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, username);
            stmt.setString(2, PasswordHasher.hashPassword(password));
            stmt.setString(3, "customer");
            stmt.setBoolean(4, true);
            stmt.setString(5, address);
            stmt.setString(6, phone);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int userId = rs.getInt(1);
                    return getUserById(userId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets a user by ID.
     * 
     * @param id the user ID
     * @return the user, or null if not found
     */
    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets a customer by ID with loyalty information.
     * 
     * @param id the customer ID
     * @return the customer
     */
    public Customer getCustomerById(int id) {
        User user = getUserById(id);
        if (user != null && user.getRole() == User.Role.CUSTOMER) {
            Customer customer = new Customer();
            customer.setId(user.getId());
            customer.setUsername(user.getUsername());
            customer.setPasswordHash(user.getPasswordHash());
            customer.setRole(user.getRole());
            customer.setActive(user.isActive());
            customer.setAddress(user.getAddress());
            customer.setPhone(user.getPhone());
            customer.setCreatedAt(user.getCreatedAt());
            
            // Get completed orders count
            String sql = "SELECT COUNT(*) FROM orders WHERE customer_id = ? AND status = 'DELIVERED'";
            try (Connection conn = dbAdapter.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    customer.setCompletedOrders(rs.getInt(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return customer;
        }
        return null;
    }

    /**
     * Updates a user's profile information.
     * 
     * @param user the user to update
     * @return true if update is successful
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET address = ?, phone = ? WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getAddress());
            stmt.setString(2, user.getPhone());
            stmt.setInt(3, user.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets all carriers.
     * 
     * @return list of carrier users
     */
    public List<User> getAllCarriers() {
        List<User> carriers = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'carrier' AND is_active = 1";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                carriers.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return carriers;
    }

    /**
     * Deactivates (fires) a carrier.
     * 
     * @param carrierId the carrier ID
     * @return true if deactivation is successful
     */
    public boolean deactivateCarrier(int carrierId) {
        String sql = "UPDATE users SET is_active = 0 WHERE id = ? AND role = 'carrier'";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, carrierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Activates (hires) a carrier.
     * 
     * @param carrierId the carrier ID
     * @return true if activation is successful
     */
    public boolean activateCarrier(int carrierId) {
        String sql = "UPDATE users SET is_active = 1 WHERE id = ? AND role = 'carrier'";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, carrierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Maps a ResultSet row to a User object.
     * 
     * @param rs the ResultSet
     * @return the User object
     * @throws SQLException if a database error occurs
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        
        String roleStr = rs.getString("role");
        user.setRole(User.Role.valueOf(roleStr.toUpperCase()));
        
        user.setActive(rs.getBoolean("is_active"));
        user.setAddress(rs.getString("address"));
        user.setPhone(rs.getString("phone"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            LocalDateTime createdDateTime = createdAt.toLocalDateTime();
            user.setCreatedAt(createdDateTime);
        }
        
        return user;
    }
}

