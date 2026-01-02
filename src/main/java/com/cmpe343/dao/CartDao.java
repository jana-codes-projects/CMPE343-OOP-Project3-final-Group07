package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.CartItem;
import com.cmpe343.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDao {

    public void addToCart(int userId, int productId, double kg) {
        // Upsert logic: If exists, add to current quantity
        String checkSql = "SELECT quantity_kg FROM cart_items WHERE user_id=? AND product_id=?";
        String insertSql = "INSERT INTO cart_items (user_id, product_id, quantity_kg) VALUES (?, ?, ?)";
        String updateSql = "UPDATE cart_items SET quantity_kg = quantity_kg + ? WHERE user_id=? AND product_id=?";

        try (Connection c = Db.getConnection()) {
            // Check existence
            boolean exists = false;
            try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        exists = true;
                }
            }

            if (exists) {
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setDouble(1, kg);
                    ps.setInt(2, userId);
                    ps.setInt(3, productId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ps.setDouble(3, kg);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error adding to cart: " + e.getMessage(), e);
        }
    }

    public void remove(int userId, int productId) {
        String sql = "DELETE FROM cart_items WHERE user_id=? AND product_id=?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error removing item: " + e.getMessage(), e);
        }
    }

    public void clear(int userId) {
        String sql = "DELETE FROM cart_items WHERE user_id=?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error clearing cart: " + e.getMessage(), e);
        }
    }

    public static class CartLoadResult {
        public List<CartItem> items = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
    }

    /**
     * Loads cart items and validates stock.
     */
    public CartLoadResult getCartItemsWithStockCheck(int userId) {
        CartLoadResult result = new CartLoadResult();
        String sql = """
                    SELECT ci.product_id, ci.quantity_kg, p.name, p.type, p.price, p.stock_kg, p.threshold_kg, p.discount_threshold, p.discount_percentage, p.image_blob
                    FROM cart_items ci
                    JOIN products p ON ci.product_id = p.id
                    WHERE ci.user_id = ?
                """;

        String updateQtySql = "UPDATE cart_items SET quantity_kg = ? WHERE user_id = ? AND product_id = ?";
        String deleteItemSql = "DELETE FROM cart_items WHERE user_id = ? AND product_id = ?";

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int pId = rs.getInt("product_id");
                    double cartKg = rs.getDouble("quantity_kg");
                    double stockKg = rs.getDouble("stock_kg");

                    String pName = rs.getString("name");
                    String pType = rs.getString("type");
                    double pPrice = rs.getDouble("price");
                    double thresholdKg = rs.getDouble("threshold_kg");
                    double discThreshold = rs.getDouble("discount_threshold");
                    double discPercent = rs.getDouble("discount_percentage");
                    byte[] pImageBlob = rs.getBytes("image_blob");

                    // STOCK CHECK
                    if (stockKg <= 0) {
                        // Out of stock -> remove
                        try (PreparedStatement del = c.prepareStatement(deleteItemSql)) {
                            del.setInt(1, userId);
                            del.setInt(2, pId);
                            del.executeUpdate();
                        }
                        result.warnings.add("Ürün stoğu bittiği için sepetten çıkarıldı: " + pName);
                        continue;
                    }

                    if (cartKg > stockKg) {
                        // Adjustment needed
                        try (PreparedStatement upd = c.prepareStatement(updateQtySql)) {
                            upd.setDouble(1, stockKg);
                            upd.setInt(2, userId);
                            upd.setInt(3, pId);
                            upd.executeUpdate();
                        }
                        cartKg = stockKg;
                        result.warnings.add("Stok yetersizliği nedeniyle miktar güncellendi: " + pName);
                    }

                    // Build Product & CartItem
                    // Pass actual threshold logic
                    Product p = new Product(pId, pName, pType, pPrice, stockKg, thresholdKg, discThreshold, discPercent,
                            pImageBlob);
                    CartItem item = new CartItem(p, cartKg);
                    result.items.add(item);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading cart: " + e.getMessage(), e);
        }
        return result;
    }

    public int getCartItemCount(int userId) {
        String sql = "SELECT COUNT(*) FROM cart_items WHERE user_id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void updateQuantity(int userId, int productId, double newQuantityKg) {
        String sql = "UPDATE cart_items SET quantity_kg = ? WHERE user_id = ? AND product_id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, newQuantityKg);
            ps.setInt(2, userId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating quantity: " + e.getMessage(), e);
        }
    }
}
