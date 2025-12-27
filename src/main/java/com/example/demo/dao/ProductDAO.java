package com.example.demo.dao;

import com.example.demo.database.DatabaseManager;
import com.example.demo.models.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    /**
     * Fetches all products from the database that have stock > 0.
     * Sorted alphabetically as required by the documentation.
     */
    public List<Product> getAllActiveProducts() {
        List<Product> products = new ArrayList<>();
        // Query filters out zero stock and sorts alphabetically by name
        String query = "SELECT * FROM ProductInfo WHERE stock > 0 ORDER BY name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getDouble("price"),
                        rs.getDouble("stock"),
                        rs.getString("imageLocation"),
                        rs.getDouble("threshold")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching products: " + e.getMessage());
        }
        return products;
    }

    /**
     * Updates product stock in the database after a purchase.
     */
    public boolean updateStock(int productId, double newStock) {
        String query = "UPDATE ProductInfo SET stock = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDouble(1, newStock);
            stmt.setInt(2, productId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating stock: " + e.getMessage());
            return false;
        }
    }
}