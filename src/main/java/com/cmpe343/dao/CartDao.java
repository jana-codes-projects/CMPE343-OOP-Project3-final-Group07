package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.CartItem;
import com.cmpe343.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDao {

    public CartDao() {
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = """
                    CREATE TABLE IF NOT EXISTS cart_items (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        product_id INT NOT NULL,
                        quantity_kg DOUBLE DEFAULT 1.0,
                        unit_price_applied DECIMAL(10,2) NOT NULL COMMENT 'Price at time of adding to cart',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY unique_cart_item (user_id, product_id),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
                    )
                """;
        try (Connection c = Db.getConnection();
                Statement s = c.createStatement()) {
            s.execute(sql);
            // Add unit_price_applied column if it doesn't exist (for existing tables)
            try {
                s.execute("ALTER TABLE cart_items ADD COLUMN unit_price_applied DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Price at time of adding to cart'");
            } catch (Exception e) {
                // Column already exists, ignore
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Cart table creation failed: " + e.getMessage());
        }
    }

    public void addToCart(int userId, int productId, double kg) {
        // Get current product price to store it
        double currentPrice = getProductPrice(productId);
        
        // Upsert logic: If exists, add to current quantity (keep original price)
        String checkSql = "SELECT quantity_kg FROM cart_items WHERE user_id=? AND product_id=?";
        String insertSql = "INSERT INTO cart_items (user_id, product_id, quantity_kg, unit_price_applied) VALUES (?, ?, ?, ?)";
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
                // Update quantity only, keep original price
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setDouble(1, kg);
                    ps.setInt(2, userId);
                    ps.setInt(3, productId);
                    ps.executeUpdate();
                }
            } else {
                // Insert with current price
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ps.setDouble(3, kg);
                    ps.setDouble(4, currentPrice);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error adding to cart: " + e.getMessage(), e);
        }
    }
    
    private double getProductPrice(int productId) {
        String sql = "SELECT price FROM products WHERE id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
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
                    SELECT ci.product_id, ci.quantity_kg, ci.unit_price_applied, 
                           p.name, p.type, p.price, p.stock_kg, p.threshold_kg, p.image_blob
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
                    double thresholdKg = rs.getDouble("threshold_kg");

                    String pName = rs.getString("name");
                    String pType = rs.getString("type");
                    // Use stored price from cart_items (price at time of addition), not current product price
                    double pPrice = rs.getDouble("unit_price_applied");
                    // Fallback to current price if unit_price_applied is 0 (for legacy data)
                    if (pPrice <= 0) {
                        pPrice = rs.getDouble("price");
                    }

                    // STOCK CHECK
                    if (stockKg <= 0) {
                        // Out of stock -> remove
                        try (PreparedStatement del = c.prepareStatement(deleteItemSql)) {
                            del.setInt(1, userId);
                            del.setInt(2, pId);
                            del.executeUpdate();
                        }
                        result.warnings.add("Product removed from cart due to out of stock: " + pName);
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
                        result.warnings.add("Quantity updated due to insufficient stock: " + pName);
                    }

                    // THRESHOLD CHECK - warn if stock is at or below threshold
                    if (stockKg <= thresholdKg) {
                        result.warnings.add("Low stock warning: " + pName + " has only " + stockKg + " kg remaining (threshold: " + thresholdKg + " kg)");
                    }

                    // Build Product & CartItem
                    // Use current product price for Product object (data integrity)
                    double currentProductPrice = rs.getDouble("price");
                    Product p = new Product(pId, pName, pType, currentProductPrice, stockKg, thresholdKg);
                    // Use stored price from cart_items (price at time of addition) for CartItem pricing
                    // This ensures pricing consistency even if product price changes
                    double storedPrice = pPrice; // This is unit_price_applied from the query
                    // Round line total to match order_items precision (2 decimal places)
                    double lineTotal = Math.round((storedPrice * cartKg) * 100.0) / 100.0;
                    CartItem item = new CartItem(p, cartKg, storedPrice, lineTotal);
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
    
    /**
     * Gets the quantity of a specific product in the user's cart.
     * 
     * @param userId The user ID
     * @param productId The product ID
     * @return The quantity in kg, or 0 if not in cart
     */
    public double getCartQuantity(int userId, int productId) {
        String sql = "SELECT quantity_kg FROM cart_items WHERE user_id = ? AND product_id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("quantity_kg");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
