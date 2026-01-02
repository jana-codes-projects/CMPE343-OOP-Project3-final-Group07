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
    
    /**
     * Retrieves the product image as a byte array from the database.
     * 
     * @param productId The ID of the product
     * @return The image as byte array, or null if not found
     */
    public byte[] getProductImageBlob(int productId) {
        String sql = "SELECT image_blob FROM products WHERE id = ?";
        
        try (Connection c = Db.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Blob blob = rs.getBlob("image_blob");
                    if (blob != null && blob.length() > 0) {
                        return blob.getBytes(1, (int) blob.length());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Updates the product image blob from a file path.
     * Reads the image file and stores it as a BLOB in the database.
     * 
     * @param productId The ID of the product
     * @param imagePath The file path to the image file
     * @return true if update was successful, false otherwise
     */
    public boolean updateProductImageFromFile(int productId, String imagePath) {
        try {
            java.io.File imageFile = new java.io.File(imagePath);
            if (!imageFile.exists() || !imageFile.isFile()) {
                System.err.println("Image file not found: " + imagePath);
                return false;
            }
            
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            return updateProductImageBlob(productId, imageBytes, imagePath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Updates the product image blob directly from byte array.
     * 
     * @param productId The ID of the product
     * @param imageBytes The image as byte array
     * @param imagePath Optional image path (can be null)
     * @return true if update was successful, false otherwise
     */
    public boolean updateProductImageBlob(int productId, byte[] imageBytes, String imagePath) {
        // First, try with image_path if provided, otherwise just update image_blob
        String sql = "UPDATE products SET image_blob = ? WHERE id = ?";
        
        try (Connection c = Db.getConnection()) {
            // Check if image_path column exists by attempting to use it
            boolean hasImagePathColumn = false;
            if (imagePath != null) {
                try (java.sql.Statement checkStmt = c.createStatement();
                     java.sql.ResultSet rs = checkStmt.executeQuery(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'image_path'"
                    )) {
                    hasImagePathColumn = rs.next();
                } catch (Exception e) {
                    // Column check failed, assume it doesn't exist
                    hasImagePathColumn = false;
                }
            }
            
            java.sql.PreparedStatement ps;
            if (hasImagePathColumn && imagePath != null) {
                sql = "UPDATE products SET image_blob = ?, image_path = ? WHERE id = ?";
                ps = c.prepareStatement(sql);
                ps.setBytes(1, imageBytes);
                ps.setString(2, imagePath);
                ps.setInt(3, productId);
            } else {
                sql = "UPDATE products SET image_blob = ? WHERE id = ?";
                ps = c.prepareStatement(sql);
                ps.setBytes(1, imageBytes);
                ps.setInt(2, productId);
            }
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Populates product images from the resources/images/products folder.
     * Matches images by product ID (e.g., 1_domates.png for product ID 1).
     * 
     * @param imagesDirectory The directory containing product images
     * @return Number of images successfully updated
     */
    public int populateProductImagesFromResources(String imagesDirectory) {
        int updatedCount = 0;
        
        try {
            java.io.File dir = new java.io.File(imagesDirectory);
            if (!dir.exists() || !dir.isDirectory()) {
                System.err.println("Images directory not found: " + imagesDirectory);
                return 0;
            }
            
            java.io.File[] imageFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (imageFiles == null) {
                return 0;
            }
            
            for (java.io.File imageFile : imageFiles) {
                String fileName = imageFile.getName();
                // Extract product ID from filename (e.g., "1_domates.png" -> 1)
                try {
                    String idStr = fileName.substring(0, fileName.indexOf('_'));
                    int productId = Integer.parseInt(idStr);
                    
                    byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
                    String imagePath = imageFile.getAbsolutePath();
                    
                    if (updateProductImageBlob(productId, imageBytes, imagePath)) {
                        updatedCount++;
                        System.out.println("Updated image for product ID " + productId + " from " + fileName);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to process image file: " + fileName + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return updatedCount;
    }
    
    public int createProduct(String name, String type, double price, double stockKg, double thresholdKg) {
        return createProduct(name, type, price, stockKg, thresholdKg, null);
    }
    
    public int createProduct(String name, String type, double price, double stockKg, double thresholdKg, byte[] imageBytes) {
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
            
            int productId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    productId = keys.getInt(1);
                }
            }
            
            // Update image if provided
            if (productId > 0 && imageBytes != null) {
                updateProductImageBlob(productId, imageBytes, null);
            }
            
            return productId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create product: " + e.getMessage(), e);
        }
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
        return updateProduct(productId, name, type, price, stockKg, thresholdKg, null);
    }
    
    /**
     * Updates an existing product with optional image.
     * 
     * @param productId The ID of the product to update
     * @param name The new product name
     * @param type The new product type (VEG/FRUIT)
     * @param price The new price
     * @param stockKg The new stock in kg
     * @param thresholdKg The new threshold in kg
     * @param imageBytes Optional image bytes to update
     * @return true if the update was successful, false otherwise
     */
    public boolean updateProduct(int productId, String name, String type, double price, double stockKg, double thresholdKg, byte[] imageBytes) {
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
            
            // Update image if provided
            if (rowsAffected > 0 && imageBytes != null) {
                updateProductImageBlob(productId, imageBytes, null);
            }
            
            return rowsAffected > 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
        }
    }
}
