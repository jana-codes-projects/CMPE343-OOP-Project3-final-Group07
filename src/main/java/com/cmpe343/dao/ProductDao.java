package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Product;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();

        // Get image from BLOB only (no image_path column in database)
        String sql = """
                    SELECT id, name, type, price, stock_kg, threshold_kg
                    FROM products
                    ORDER BY name
                """;

        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                // Images are stored in BLOB, accessed via getProductImageBlob(productId)
                list.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getDouble("price"),
                        rs.getDouble("stock_kg"),
                        rs.getDouble("threshold_kg")));
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException("Could not fetch product list", e);
        }
    }
    
//    public byte[] getProductImageBlob(int productId) {
//        String sql = "SELECT image_blob FROM products WHERE id = ?";
//
//        try (Connection c = Db.getConnection();
//                java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setInt(1, productId);
//
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    Blob blob = rs.getBlob("image_blob");
//                    if (blob != null && blob.length() > 0) {
//                        return blob.getBytes(1, (int) blob.length());
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
    
    public int createProduct(String name, String type, double price, double stockKg, double thresholdKg) {
        String sql = """
            INSERT INTO products (name, type, price, stock_kg, threshold_kg)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection c = Db.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setDouble(3, price);
            ps.setDouble(4, stockKg);
            ps.setDouble(5, thresholdKg);
            
            ps.executeUpdate();
            
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create product: " + e.getMessage(), e);
        }
        return -1;
    }
    
    /**
     * Updates an existing product.
     * 
     * @param productId The ID of the product to update
     * @param name The new product name
     * @param type The new product type (VEG/FRUIT)
     * @param price The new price
     * @param stockKg The new stock in kg
     * @param thresholdKg The new threshold in kg
     * @return true if the update was successful, false otherwise
     */
    public boolean updateProduct(int productId, String name, String type, double price, double stockKg, double thresholdKg) {
        String sql = """
            UPDATE products
            SET name = ?, type = ?, price = ?, stock_kg = ?, threshold_kg = ?
            WHERE id = ?
        """;
        
        try (Connection c = Db.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setDouble(3, price);
            ps.setDouble(4, stockKg);
            ps.setDouble(5, thresholdKg);
            ps.setInt(6, productId);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
        }
    }
}
