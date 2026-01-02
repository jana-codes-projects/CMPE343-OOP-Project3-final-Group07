package com.cmpe343.db;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Db {
    private static Properties props;

    private static Properties props() {
        if (props != null) return props;
        try {
            props = new Properties();
            props.load(new FileInputStream("app.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("app.properties okunamadı (proje kökünde olmalı).", e);
        }
    }

    public static Connection getConnection() {
        try {
            Properties p = props();
            return DriverManager.getConnection(
                    p.getProperty("db.url"),
                    p.getProperty("db.user"),
                    p.getProperty("db.password")
            );
        } catch (Exception e) {
            throw new RuntimeException("DB bağlantısı kurulamadı.", e);
        }
    }
    
    /**
     * Updates the user's wallet balance by adding the specified amount.
     * Note: This assumes the users table has a wallet_balance column.
     * If it doesn't exist, add it with: ALTER TABLE users ADD COLUMN wallet_balance DECIMAL(10,2) DEFAULT 0.0;
     * 
     * @param userId The ID of the user
     * @param amount The amount to add to the wallet (can be positive or negative)
     * @return true if the update was successful, false otherwise
     */
    public static boolean updateUserBalance(int userId, double amount) {
        String sql = "UPDATE users SET wallet_balance = COALESCE(wallet_balance, 0) + ? WHERE id = ?";
        try (Connection c = getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
