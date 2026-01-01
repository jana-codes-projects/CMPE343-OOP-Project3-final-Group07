package com.cmpe343.db;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
    public static boolean updateUserBalance(int userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amount);
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("SQL Error during balance update: " + e.getMessage());
            return false;
        }
    }
}
