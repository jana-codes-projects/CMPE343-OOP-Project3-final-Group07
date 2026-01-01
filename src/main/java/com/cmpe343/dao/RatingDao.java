package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Rating;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RatingDao {

    // ratings: id, carrier_id, score, comment, timestamp

    public List<Rating> getAllRatings() {
        List<Rating> list = new ArrayList<>();
        String sql = "SELECT * FROM ratings ORDER BY timestamp DESC";
        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            // Table might not exist yet
            System.err.println("Rating fetch error: " + e.getMessage());
        }
        return list;
    }

    public void addRating(int carrierId, int score, String comment) {
        String sql = "INSERT INTO ratings (carrier_id, score, comment, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carrierId);
            ps.setInt(2, score);
            ps.setString(3, comment);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("timestamp");
        return new Rating(
                rs.getInt("id"),
                rs.getInt("carrier_id"),
                rs.getInt("score"),
                rs.getString("comment"),
                ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
    }
}
