package com.cmpe343.dao;

import com.cmpe343.db.Db;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.nio.file.Files;
import java.io.File;

/**
 * Data Access Object for invoice operations.
 * Handles saving and retrieving PDF invoices as BLOBs and invoice text as
 * CLOBs.
 * Supports upsert functionality for invoice updates.
 * 
 * @author Group07
 * @version 1.0
 */
public class InvoiceDao {

    public void saveInvoice(int orderId, File pdfFile, String invoiceText) {
        String sql = "INSERT INTO invoices (order_id, pdf_blob, invoice_text, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                +
                "ON DUPLICATE KEY UPDATE pdf_blob = VALUES(pdf_blob), invoice_text = VALUES(invoice_text)";

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());

            ps.setInt(1, orderId);
            ps.setBytes(2, pdfBytes);
            ps.setString(3, invoiceText);

            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save invoice to database", e);
        }
    }
}
