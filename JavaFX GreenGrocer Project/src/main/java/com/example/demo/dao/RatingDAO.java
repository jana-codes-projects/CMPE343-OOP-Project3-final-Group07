package com.example.demo.dao;

import com.example.demo.db.DatabaseAdapter;
import com.example.demo.models.Rating;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Rating operations.
 * Handles all database operations related to ratings.
 * 
 * @author Group07
 * @version 1.0
 */
public class RatingDAO {
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public RatingDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Creates a new rating.
     * 
     * @param rating the rating to create
     * @return the created rating with ID, or null if creation fails
     */
    public Rating createRating(Rating rating) {
        String sql = "INSERT INTO ratings (order_id, carrier_id, customer_id, rating, comment) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, rating.getOrderId());
            stmt.setInt(2, rating.getCarrierId());
            stmt.setInt(3, rating.getCustomerId());
            stmt.setInt(4, rating.getRating());
            stmt.setString(5, rating.getComment());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    rating.setId(rs.getInt(1));
                    updateCarrierAverageRating(rating.getCarrierId());
                    return rating;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets ratings for a carrier.
     * 
     * @param carrierId the carrier ID
     * @return list of ratings
     */
    public List<Rating> getRatingsByCarrier(int carrierId) {
        List<Rating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM ratings WHERE carrier_id = ? ORDER BY created_at DESC";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, carrierId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ratings.add(mapResultSetToRating(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ratings;
    }

    /**
     * Gets average rating for a carrier.
     * 
     * @param carrierId the carrier ID
     * @return the average rating
     */
    public double getAverageRating(int carrierId) {
        String sql = "SELECT AVG(rating) as avg_rating FROM ratings WHERE carrier_id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, carrierId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Checks if an order has been rated.
     * 
     * @param orderId the order ID
     * @return true if the order has been rated
     */
    public boolean orderHasRating(int orderId) {
        String sql = "SELECT COUNT(*) FROM ratings WHERE order_id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates the carrier's average rating (would be stored in a separate field or calculated on demand).
     * This is a placeholder - in a real system, you might want to store this in a carriers table.
     * 
     * @param carrierId the carrier ID
     */
    private void updateCarrierAverageRating(int carrierId) {
        // This could update a cached average rating field if such exists
        // For now, it's calculated on demand via getAverageRating
    }

    /**
     * Maps a ResultSet row to a Rating object.
     * 
     * @param rs the ResultSet
     * @return the Rating object
     * @throws SQLException if a database error occurs
     */
    private Rating mapResultSetToRating(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setId(rs.getInt("id"));
        rating.setOrderId(rs.getInt("order_id"));
        rating.setCarrierId(rs.getInt("carrier_id"));
        rating.setCustomerId(rs.getInt("customer_id"));
        rating.setRating(rs.getInt("rating"));
        rating.setComment(rs.getString("comment"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            LocalDateTime createdDateTime = createdAt.toLocalDateTime();
            rating.setCreatedAt(createdDateTime);
        }
        
        return rating;
    }
}

