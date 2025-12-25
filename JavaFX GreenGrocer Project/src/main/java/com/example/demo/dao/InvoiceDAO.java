package com.example.demo.dao;

import com.example.demo.db.DatabaseAdapter;

import java.sql.*;

/**
 * Data Access Object for Invoice operations.
 * Handles storing and retrieving PDF invoices.
 * 
 * @author Group07
 * @version 1.0
 */
public class InvoiceDAO {
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public InvoiceDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Saves an invoice PDF to the database.
     * 
     * @param orderId the order ID
     * @param pdfBytes the PDF as byte array
     * @return true if save is successful
     */
    public boolean saveInvoice(int orderId, byte[] pdfBytes) {
        String sql = "INSERT INTO invoices (order_id, pdf_blob) VALUES (?, ?) ON DUPLICATE KEY UPDATE pdf_blob = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            stmt.setBytes(2, pdfBytes);
            stmt.setBytes(3, pdfBytes);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves an invoice PDF from the database.
     * 
     * @param orderId the order ID
     * @return the PDF as byte array, or null if not found
     */
    public byte[] getInvoice(int orderId) {
        String sql = "SELECT pdf_blob FROM invoices WHERE order_id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBytes("pdf_blob");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

