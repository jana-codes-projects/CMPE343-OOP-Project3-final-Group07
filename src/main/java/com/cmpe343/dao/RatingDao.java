package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Rating;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RatingDao {
    
    public List<Rating> getAllRatings() {
        List<Rating> list = new ArrayList<>();
        // Use correct column names: rating (not score) and created_at (not timestamp)
        String sql = "SELECT id, carrier_id, customer_id, rating, comment, created_at FROM ratings ORDER BY created_at DESC";
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                // Add null check to prevent NullPointerException if created_at is NULL in database
                java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                LocalDateTime ratingTime = timestamp != null 
                    ? timestamp.toLocalDateTime() 
                    : LocalDateTime.now(); // Fallback to current time if NULL
                list.add(new Rating(
                    rs.getInt("id"),
                    rs.getInt("carrier_id"),
                    rs.getInt("customer_id"),
                    rs.getInt("rating"), // Use 'rating' column, not 'score'
                    rs.getString("comment"),
                    ratingTime
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public void createRating(int carrierId, int customerId, int score, String comment) {
        // Use correct column names: rating (not score) and created_at (not timestamp)
        // Note: created_at has DEFAULT CURRENT_TIMESTAMP, but we set it explicitly for consistency
        String sql = """
            INSERT INTO ratings (carrier_id, customer_id, rating, comment, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carrierId);
            ps.setInt(2, customerId);
            ps.setInt(3, score); // Store as 'rating' column
            ps.setString(4, comment != null ? comment : "");
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rating: " + e.getMessage(), e);
        }
    }
    
    public boolean hasRatingForOrder(int orderId, int customerId) {
        // Check if there's a rating for the carrier of this order
        String sql = """
            SELECT COUNT(*) 
            FROM ratings r
            JOIN orders o ON r.carrier_id = o.carrier_id
            WHERE o.id = ? AND r.customer_id = ?
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, customerId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
