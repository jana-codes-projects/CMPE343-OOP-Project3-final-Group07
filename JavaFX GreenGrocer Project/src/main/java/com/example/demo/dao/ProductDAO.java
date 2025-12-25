package com.example.demo.dao;

import com.example.demo.db.DatabaseAdapter;
import com.example.demo.models.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Product operations.
 * Handles all database operations related to products.
 * 
 * @author Group07
 * @version 1.0
 */
public class ProductDAO {
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public ProductDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Gets all active products, filtered by type if specified.
     * 
     * @param type the product type (VEG or FRUIT), or null for all
     * @return list of products
     */
    public List<Product> getAllProducts(Product.ProductType type) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = 1";
        if (type != null) {
            sql += " AND type = ?";
        }
        sql += " ORDER BY name";
        
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (type != null) {
                stmt.setString(1, type.name());
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product product = mapResultSetToProduct(rs);
                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Gets all products with stock greater than zero, filtered by type.
     * 
     * @param type the product type (VEG or FRUIT), or null for all
     * @return list of products with stock > 0
     */
    public List<Product> getAvailableProducts(Product.ProductType type) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = 1 AND stock_kg > 0";
        if (type != null) {
            sql += " AND type = ?";
        }
        sql += " ORDER BY name";
        
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (type != null) {
                stmt.setString(1, type.name());
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product product = mapResultSetToProduct(rs);
                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Gets a product by ID.
     * 
     * @param id the product ID
     * @return the product, or null if not found
     */
    public Product getProductById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Searches products by keyword (name).
     * 
     * @param keyword the search keyword
     * @return list of matching products
     */
    public List<Product> searchProducts(String keyword) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = 1 AND name LIKE ? ORDER BY name";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Adds a new product.
     * 
     * @param product the product to add
     * @return the created product with ID, or null if creation fails
     */
    public Product addProduct(Product product) {
        String sql = "INSERT INTO products (name, type, price, stock_kg, threshold_kg, image_blob, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getType().name());
            stmt.setBigDecimal(3, product.getPrice());
            stmt.setBigDecimal(4, product.getStockKg());
            stmt.setBigDecimal(5, product.getThresholdKg());
            stmt.setBytes(6, product.getImageBlob());
            stmt.setBoolean(7, product.isActive());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    product.setId(rs.getInt(1));
                    return product;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates a product.
     * 
     * @param product the product to update
     * @return true if update is successful
     */
    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, type = ?, price = ?, stock_kg = ?, threshold_kg = ?, image_blob = ?, is_active = ? WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getType().name());
            stmt.setBigDecimal(3, product.getPrice());
            stmt.setBigDecimal(4, product.getStockKg());
            stmt.setBigDecimal(5, product.getThresholdKg());
            stmt.setBytes(6, product.getImageBlob());
            stmt.setBoolean(7, product.isActive());
            stmt.setInt(8, product.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Removes (deactivates) a product.
     * 
     * @param productId the product ID
     * @return true if removal is successful
     */
    public boolean removeProduct(int productId) {
        String sql = "UPDATE products SET is_active = 0 WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates product stock after an order.
     * 
     * @param productId the product ID
     * @param quantityKg the quantity to subtract from stock
     * @return true if update is successful
     */
    public boolean updateStock(int productId, BigDecimal quantityKg) {
        String sql = "UPDATE products SET stock_kg = stock_kg - ? WHERE id = ? AND stock_kg >= ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, quantityKg);
            stmt.setInt(2, productId);
            stmt.setBigDecimal(3, quantityKg);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Maps a ResultSet row to a Product object.
     * 
     * @param rs the ResultSet
     * @return the Product object
     * @throws SQLException if a database error occurs
     */
    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getInt("id"));
        product.setName(rs.getString("name"));
        
        String typeStr = rs.getString("type");
        product.setType(Product.ProductType.valueOf(typeStr));
        
        product.setPrice(rs.getBigDecimal("price"));
        product.setStockKg(rs.getBigDecimal("stock_kg"));
        product.setThresholdKg(rs.getBigDecimal("threshold_kg"));
        product.setImageBlob(rs.getBytes("image_blob"));
        product.setActive(rs.getBoolean("is_active"));
        
        return product;
    }
}

