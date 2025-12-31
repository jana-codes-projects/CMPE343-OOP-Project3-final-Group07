package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Coupon;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CouponDao {
    
    public List<Coupon> getAllCoupons() {
        List<Coupon> list = new ArrayList<>();
        String sql = "SELECT id, code, discount_amount, expiry_date, active FROM coupons ORDER BY code";
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                // Add null check to prevent NullPointerException if expiry_date is NULL in database
                java.sql.Date expiryDate = rs.getDate("expiry_date");
                LocalDate expiry = expiryDate != null 
                    ? expiryDate.toLocalDate() 
                    : LocalDate.now().plusYears(1); // Fallback to 1 year from now if NULL
                list.add(new Coupon(
                    rs.getInt("id"),
                    rs.getString("code"),
                    rs.getDouble("discount_amount"),
                    expiry,
                    rs.getBoolean("active")
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
            SELECT id, code, discount_amount, expiry_date, active 
            FROM coupons 
            WHERE active = 1 AND expiry_date >= CURDATE()
            ORDER BY code
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                // Add null check to prevent NullPointerException if expiry_date is NULL in database
                java.sql.Date expiryDate = rs.getDate("expiry_date");
                LocalDate expiry = expiryDate != null 
                    ? expiryDate.toLocalDate() 
                    : LocalDate.now().plusYears(1); // Fallback to 1 year from now if NULL
                list.add(new Coupon(
                    rs.getInt("id"),
                    rs.getString("code"),
                    rs.getDouble("discount_amount"),
                    expiry,
                    rs.getBoolean("active")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public Coupon getCouponByCode(String code) {
        String sql = """
            SELECT id, code, discount_amount, expiry_date, active 
            FROM coupons 
            WHERE code = ? AND active = 1 AND expiry_date >= CURDATE()
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Add null check to prevent NullPointerException if expiry_date is NULL in database
                    java.sql.Date expiryDate = rs.getDate("expiry_date");
                    LocalDate expiry = expiryDate != null 
                        ? expiryDate.toLocalDate() 
                        : LocalDate.now().plusYears(1); // Fallback to 1 year from now if NULL
                    return new Coupon(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getDouble("discount_amount"),
                        expiry,
                        rs.getBoolean("active")
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
            SELECT id, code, discount_amount, expiry_date, active 
            FROM coupons 
            WHERE id = ? AND active = 1 AND expiry_date >= CURDATE()
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Add null check to prevent NullPointerException if expiry_date is NULL in database
                    java.sql.Date expiryDate = rs.getDate("expiry_date");
                    LocalDate expiry = expiryDate != null 
                        ? expiryDate.toLocalDate() 
                        : LocalDate.now().plusYears(1); // Fallback to 1 year from now if NULL
                    return new Coupon(rs.getInt("id"), rs.getString("code"), rs.getDouble("discount_amount"), expiry, rs.getBoolean("active"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public int createCoupon(String code, double discountAmount, LocalDate expiryDate, boolean active) {
        String sql = """
            INSERT INTO coupons (code, discount_amount, expiry_date, active)
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setDouble(2, discountAmount);
            ps.setDate(3, java.sql.Date.valueOf(expiryDate));
            ps.setBoolean(4, active);
            
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
