package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Coupon;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CouponDao {
    
    public List<Coupon> getAllCoupons() {
        List<Coupon> list = new ArrayList<>();
        String sql = "SELECT id, code, kind, value, min_cart, is_active, expires_at FROM coupons ORDER BY code";
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Coupon.CouponKind kind = Coupon.CouponKind.valueOf(rs.getString("kind"));
                java.sql.Timestamp expiresAt = rs.getTimestamp("expires_at");
                LocalDateTime expiry = expiresAt != null 
                    ? expiresAt.toLocalDateTime() 
                    : null;
                list.add(new Coupon(
                    rs.getInt("id"),
                    rs.getString("code"),
                    kind,
                    rs.getDouble("value"),
                    rs.getDouble("min_cart"),
                    rs.getBoolean("is_active"),
                    expiry
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    /**
     * Gets all active coupons that are valid (not expired).
     * Note: Coupons are global and not customer-specific in this implementation.
     * The customerId parameter is kept for API compatibility but not used in the query.
     * 
     * @param customerId Customer ID (currently unused - coupons are global)
     * @return List of active, non-expired coupons
     */
    public List<Coupon> getActiveCouponsForCustomer(int customerId) {
        List<Coupon> list = new ArrayList<>();
        // Note: customerId parameter is not used - coupons are global in this system
        // If customer-specific coupons are needed, add: WHERE customer_id = ? OR customer_id IS NULL
        String sql = """
            SELECT id, code, kind, value, min_cart, is_active, expires_at 
            FROM coupons 
            WHERE is_active = 1 AND (expires_at IS NULL OR expires_at >= NOW())
            ORDER BY code
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Coupon.CouponKind kind = Coupon.CouponKind.valueOf(rs.getString("kind"));
                java.sql.Timestamp expiresAt = rs.getTimestamp("expires_at");
                LocalDateTime expiry = expiresAt != null 
                    ? expiresAt.toLocalDateTime() 
                    : null;
                list.add(new Coupon(
                    rs.getInt("id"),
                    rs.getString("code"),
                    kind,
                    rs.getDouble("value"),
                    rs.getDouble("min_cart"),
                    rs.getBoolean("is_active"),
                    expiry
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public Coupon getCouponByCode(String code) {
        String sql = """
            SELECT id, code, kind, value, min_cart, is_active, expires_at 
            FROM coupons 
            WHERE code = ? AND is_active = 1 AND (expires_at IS NULL OR expires_at >= NOW())
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Coupon.CouponKind kind = Coupon.CouponKind.valueOf(rs.getString("kind"));
                    java.sql.Timestamp expiresAt = rs.getTimestamp("expires_at");
                    LocalDateTime expiry = expiresAt != null 
                        ? expiresAt.toLocalDateTime() 
                        : null;
                    return new Coupon(
                        rs.getInt("id"),
                        rs.getString("code"),
                        kind,
                        rs.getDouble("value"),
                        rs.getDouble("min_cart"),
                        rs.getBoolean("is_active"),
                        expiry
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public Coupon getCouponById(int id) {
        String sql = """
            SELECT id, code, kind, value, min_cart, is_active, expires_at 
            FROM coupons 
            WHERE id = ? AND is_active = 1 AND (expires_at IS NULL OR expires_at >= NOW())
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Coupon.CouponKind kind = Coupon.CouponKind.valueOf(rs.getString("kind"));
                    java.sql.Timestamp expiresAt = rs.getTimestamp("expires_at");
                    LocalDateTime expiry = expiresAt != null 
                        ? expiresAt.toLocalDateTime() 
                        : null;
                    return new Coupon(
                        rs.getInt("id"),
                        rs.getString("code"),
                        kind,
                        rs.getDouble("value"),
                        rs.getDouble("min_cart"),
                        rs.getBoolean("is_active"),
                        expiry
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public int createCoupon(String code, Coupon.CouponKind kind, double value, double minCart, LocalDateTime expiresAt, boolean isActive) {
        String sql = """
            INSERT INTO coupons (code, kind, value, min_cart, expires_at, is_active)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, kind.name());
            ps.setDouble(3, value);
            ps.setDouble(4, minCart);
            if (expiresAt != null) {
                ps.setTimestamp(5, java.sql.Timestamp.valueOf(expiresAt));
            } else {
                ps.setNull(5, java.sql.Types.TIMESTAMP);
            }
            ps.setBoolean(6, isActive);
            
            ps.executeUpdate();
            
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create coupon: " + e.getMessage(), e);
        }
        return -1;
    }
}
