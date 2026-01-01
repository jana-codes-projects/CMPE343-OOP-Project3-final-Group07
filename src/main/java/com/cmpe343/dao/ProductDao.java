package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Product;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();

        String sql = """
                    SELECT id, name, type, price, stock_kg, threshold_kg, image_blob
                    FROM products
                    ORDER BY name
                """;

        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getDouble("price"),
                        rs.getDouble("stock_kg"),
                        rs.getDouble("threshold_kg"),
                        rs.getBytes("image_blob")));
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException("Product listesi çekilemedi", e);
        }
    }

    public void insert(Product p) {
        String sql = "INSERT INTO products (name, type, price, stock_kg, threshold_kg, image_blob, is_active) VALUES (?, ?, ?, ?, ?, ?, 1)";
        try (Connection c = Db.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getType().name());
            ps.setDouble(3, p.getPrice());
            ps.setDouble(4, p.getStockKg());
            ps.setDouble(5, p.getThresholdKg());
            ps.setBytes(6, p.getImageBlob());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error inserting product: " + e.getMessage(), e);
        }
    }

    public void update(Product p) {
        String sql = "UPDATE products SET name=?, type=?, price=?, stock_kg=?, threshold_kg=?, image_blob=? WHERE id=?";
        try (Connection c = Db.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getType().name());
            ps.setDouble(3, p.getPrice());
            ps.setDouble(4, p.getStockKg());
            ps.setDouble(5, p.getThresholdKg());
            ps.setBytes(6, p.getImageBlob());
            ps.setInt(7, p.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating product: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        // Soft delete usually better, but for this project requirements imply removal
        // Check foreign keys first? Or just delete. Let's do DELETE.
        String sql = "DELETE FROM products WHERE id=?";
        try (Connection c = Db.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting product: " + e.getMessage(), e);
        }
    }
}
