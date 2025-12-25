package com.example.demo.dao;

import com.example.demo.db.DatabaseAdapter;
import com.example.demo.models.Coupon;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Coupon operations.
 * Handles all database operations related to coupons.
 * 
 * @author Group07
 * @version 1.0
 */
public class CouponDAO {
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public CouponDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Gets a coupon by code.
     * 
     * @param code the coupon code
     * @return the coupon, or null if not found
     */
    public Coupon getCouponByCode(String code) {
        String sql = "SELECT * FROM coupons WHERE code = ? AND is_active = 1";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToCoupon(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets a coupon by ID.
     * 
     * @param id the coupon ID
     * @return the coupon, or null if not found
     */
    public Coupon getCouponById(int id) {
        String sql = "SELECT * FROM coupons WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToCoupon(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets all active coupons.
     * 
     * @return list of active coupons
     */
    public List<Coupon> getAllCoupons() {
        List<Coupon> coupons = new ArrayList<>();
        String sql = "SELECT * FROM coupons ORDER BY code";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                coupons.add(mapResultSetToCoupon(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return coupons;
    }

    /**
     * Creates a new coupon.
     * 
     * @param coupon the coupon to create
     * @return the created coupon with ID, or null if creation fails
     */
    public Coupon createCoupon(Coupon coupon) {
        String sql = "INSERT INTO coupons (code, kind, value, min_cart, is_active, expires_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, coupon.getCode());
            stmt.setString(2, coupon.getType().name());
            stmt.setBigDecimal(3, coupon.getValue());
            stmt.setBigDecimal(4, coupon.getMinCartValue());
            stmt.setBoolean(5, coupon.isActive());
            if (coupon.getExpiresAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(coupon.getExpiresAt()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    coupon.setId(rs.getInt(1));
                    return coupon;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates a coupon.
     * 
     * @param coupon the coupon to update
     * @return true if update is successful
     */
    public boolean updateCoupon(Coupon coupon) {
        String sql = "UPDATE coupons SET code = ?, kind = ?, value = ?, min_cart = ?, is_active = ?, expires_at = ? WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, coupon.getCode());
            stmt.setString(2, coupon.getType().name());
            stmt.setBigDecimal(3, coupon.getValue());
            stmt.setBigDecimal(4, coupon.getMinCartValue());
            stmt.setBoolean(5, coupon.isActive());
            if (coupon.getExpiresAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(coupon.getExpiresAt()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            stmt.setInt(7, coupon.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Maps a ResultSet row to a Coupon object.
     * 
     * @param rs the ResultSet
     * @return the Coupon object
     * @throws SQLException if a database error occurs
     */
    private Coupon mapResultSetToCoupon(ResultSet rs) throws SQLException {
        Coupon coupon = new Coupon();
        coupon.setId(rs.getInt("id"));
        coupon.setCode(rs.getString("code"));
        
        String kindStr = rs.getString("kind");
        coupon.setType(Coupon.CouponType.valueOf(kindStr));
        
        BigDecimal value = rs.getBigDecimal("value");
        BigDecimal minCart = rs.getBigDecimal("min_cart");
        coupon.setValue(value);
        coupon.setMinCartValue(minCart);
        coupon.setActive(rs.getBoolean("is_active"));
        
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            LocalDateTime expiresDateTime = expiresAt.toLocalDateTime();
            coupon.setExpiresAt(expiresDateTime);
        }
        
        return coupon;
    }
}

