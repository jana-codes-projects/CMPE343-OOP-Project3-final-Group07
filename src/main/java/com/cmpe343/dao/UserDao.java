package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User login(String username, String password) {
        String sql = """
                    SELECT id, username, role
                    FROM users
                    WHERE username = ?
                      AND password_hash = SHA2(?, 256)
                      AND is_active = 1
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Login Error", e);
        }
    }

    public List<User> getAllCarriers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, role, phone, address, is_active FROM users WHERE role = 'CARRIER'"; // Assuming
                                                                                                               // new
                                                                                                               // fields

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapUserExtended(rs));
            }
        } catch (Exception e) {
            // For now, return empty if table structure mismatch or error
            System.err.println("Error fething carriers: " + e.getMessage());
        }
        return list;
    }

    public User getUserById(int id) {
        String sql = "SELECT id, username, role, phone, address, is_active FROM users WHERE id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserExtended(rs);
                }
            }
        } catch (Exception e) {
            // Fallback to basic if extended query fails
            return getUserByIdBasic(id);
        }
        return null;
    }

    private User getUserByIdBasic(int id) {
        String sql = "SELECT id, username, role FROM users WHERE id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user", e);
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws Exception {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("role"));
    }

    public void activateCarrier(int userId) {
        String sql = "UPDATE users SET is_active = 1 WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deactivateCarrier(int userId) {
        String sql = "UPDATE users SET is_active = 0 WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User mapUserExtended(ResultSet rs) throws SQLException {
        // Try to get extended fields, default to null/true if column missing
        String phone = null;
        String address = null;
        boolean active = true;

        try {
            phone = rs.getString("phone");
        } catch (Exception e) {
        }
        try {
            address = rs.getString("address");
        } catch (Exception e) {
        }
        try {
            active = rs.getBoolean("is_active");
        } catch (Exception e) {
        }

        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("role"),
                phone,
                address,
                active);
    }

    /**
     * Gets the owner user ID (first user with role 'OWNER').
     * 
     * @return The owner ID, or -1 if not found
     */
    public int getOwnerId() {
        String sql = "SELECT id FROM users WHERE role = 'OWNER' LIMIT 1";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Creates a new customer user.
     * 
     * @param username The username
     * @param password The plain text password (will be hashed)
     * @param phone    The phone number
     * @param address  The address
     * @return The created user ID, or -1 if creation fails (e.g., username already
     *         exists)
     */
    public int createCustomer(String username, String password, String phone, String address) {
        String sql = """
                    INSERT INTO users (username, password_hash, role, phone, address, is_active)
                    VALUES (?, SHA2(?, 256), 'customer', ?, ?, 1)
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, phone);
            ps.setString(4, address);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            // Username already exists
            throw new RuntimeException("Username already exists", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Checks if a username already exists.
     * 
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Creates a new carrier user (hire carrier).
     * 
     * @param username The username
     * @param password The plain text password (will be hashed)
     * @param phone    The phone number
     * @param address  The address
     * @return The created carrier ID, or -1 if creation fails
     */
    public int createCarrier(String username, String password, String phone, String address) {
        String sql = """
                    INSERT INTO users (username, password_hash, role, phone, address, is_active)
                    VALUES (?, SHA2(?, 256), 'carrier', ?, ?, 1)
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, phone);
            ps.setString(4, address);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new RuntimeException("Username already exists", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create carrier: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Updates a user's profile (phone and address).
     */
    public boolean updateUserProfile(int userId, String phone, String address) {
        String sql = "UPDATE users SET phone = ?, address = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, address);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}