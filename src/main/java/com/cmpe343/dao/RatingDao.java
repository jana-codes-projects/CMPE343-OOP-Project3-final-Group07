package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Rating;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RatingDao {

    public record RatingStats(double average, int count) {
    }

    // ratings: id, order_id, carrier_id, customer_id, rating, comment, created_at

    public List<Rating> getAllRatings() {
        List<Rating> list = new ArrayList<>();
        String sql = "SELECT * FROM ratings ORDER BY created_at DESC";
        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Rating fetch error: " + e.getMessage());
        }
        return list;
    }

    public Rating getRatingByOrderId(int orderId) {
        String sql = "SELECT * FROM ratings WHERE order_id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveRating(int orderId, int carrierId, int customerId, int rating, String comment) {
        // Check if exists
        Rating existing = getRatingByOrderId(orderId);
        if (existing == null) {
            // Insert
            String sql = "INSERT INTO ratings (order_id, carrier_id, customer_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection c = Db.getConnection();
                    PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.setInt(2, carrierId);
                ps.setInt(3, customerId);
                ps.setInt(4, rating);
                ps.setString(5, comment);
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // Update
            String sql = "UPDATE ratings SET rating = ?, comment = ?, created_at = ? WHERE id = ?";
            try (Connection c = Db.getConnection();
                    PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, rating);
                ps.setString(2, comment);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(4, existing.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Rating(
                rs.getInt("id"),
                rs.getInt("order_id"),
                rs.getInt("carrier_id"),
                rs.getInt("customer_id"),
                rs.getInt("rating"),
                rs.getString("comment"),
                ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
    }

    public RatingStats getCarrierStats(int carrierId) {
        String sql = "SELECT AVG(rating) as avg_score, COUNT(*) as count FROM ratings WHERE carrier_id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carrierId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RatingStats(rs.getDouble("avg_score"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new RatingStats(0.0, 0);
    }
}
