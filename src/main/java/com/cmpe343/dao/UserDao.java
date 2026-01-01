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
        String sql = "SELECT id, username, role, phone, address, is_active FROM users WHERE role = 'carrier'"; // Assuming
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

    public boolean registerCustomer(String username, String password, String address, String phone) {
        // First check if username exists
        String checkSql = "SELECT id FROM users WHERE username = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(checkSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return false; // Username taken
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking username: " + e.getMessage(), e);
        }

        String sql = "INSERT INTO users (username, password_hash, role, address, phone, is_active) VALUES (?, SHA2(?, 256), 'customer', ?, ?, 1)";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, address);
            ps.setString(4, phone);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error registering user: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the owner user ID (first user with role 'owner').
     * 
     * @return The owner ID, or -1 if not found
     */
    public int getOwnerId() {
        String sql = "SELECT id FROM users WHERE role = 'owner' LIMIT 1";
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
}