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
                    SELECT id, username, role, phone, address, is_active, wallet_balance
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
                return mapUserExtended(rs);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Login Error", e);
        }
    }

    public List<User> getAllCarriers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, role, phone, address, is_active, wallet_balance FROM users WHERE role = 'CARRIER'";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapUserExtended(rs));
            }
        } catch (Exception e) {
            System.err.println("Error fetching carriers: " + e.getMessage());
        }
        return list;
    }

    public void updateWalletBalance(int userId, double amountToAdd) {
        String sql = "UPDATE users SET wallet_balance = wallet_balance + ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, amountToAdd);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUserById(int id) {
        String sql = "SELECT id, username, role, phone, address, is_active, wallet_balance FROM users WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserExtended(rs);
                }
            }
        } catch (Exception e) {
            return getUserByIdBasic(id);
        }
        return null;
    }

    private User getUserByIdBasic(int id) {
        String sql = "SELECT id, username, role, wallet_balance FROM users WHERE id = ?";
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
        double balance = 0.0;
        try { balance = rs.getDouble("wallet_balance"); } catch (Exception e) {}

        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("role"),
                null,
                null,
                true,
                balance
        );
    }

    private User mapUserExtended(ResultSet rs) throws SQLException {
        String phone = null;
        String address = null;
        boolean active = true;
        double balance = 0.0;

        try { phone = rs.getString("phone"); } catch (Exception e) {}
        try { address = rs.getString("address"); } catch (Exception e) {}
        try { active = rs.getBoolean("is_active"); } catch (Exception e) {}
        try { balance = rs.getDouble("wallet_balance"); } catch (Exception e) {}

        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("role"),
                phone,
                address,
                active,
                balance);
    }

    public void activateCarrier(int userId) {
        String sql = "UPDATE users SET is_active = 1 WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deactivateCarrier(int userId) {
        String sql = "UPDATE users SET is_active = 0 WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int getOwnerId() {
        String sql = "SELECT id FROM users WHERE role = 'OWNER' LIMIT 1";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) { return rs.getInt("id"); }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public int createCustomer(String username, String password, String phone, String address) {
        String sql = """
            INSERT INTO users (username, password_hash, role, phone, address, is_active, wallet_balance)
            VALUES (?, SHA2(?, 256), 'customer', ?, ?, 1, 0.0)
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
                    if (rs.next()) { return rs.getInt(1); }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
        return -1;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { return rs.getInt(1) > 0; }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}