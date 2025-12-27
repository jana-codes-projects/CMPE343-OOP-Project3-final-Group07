package com.example.demo.dao;

import com.example.demo.database.DatabaseManager;
import java.sql.*;

public class OrderDAO {

    /**
     * Saves the order to OrderInfo table.
     * Note: invoicePDF is stored as a BLOB as per requirements.
     */
    public boolean saveOrder(int userId, String productsSummary, double totalCost, byte[] pdfData) {
        String query = "INSERT INTO OrderInfo (userId, products, totalCost, orderTime, isDelivered, invoicePDF) VALUES (?, ?, ?, NOW(), ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setString(2, productsSummary);
            stmt.setDouble(3, totalCost);
            stmt.setBoolean(4, false); // Initial delivery status
            stmt.setBytes(5, pdfData); // Saving PDF as BLOB

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Order saving error: " + e.getMessage());
            return false;
        }
    }
}