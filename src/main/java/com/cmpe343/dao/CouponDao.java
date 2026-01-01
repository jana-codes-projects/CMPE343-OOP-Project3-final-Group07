package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Coupon;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CouponDao {

    // coupons: id, code, kind (AMOUNT/PERCENT), value, min_cart_amount, active,
    // expires_at

    public List<Coupon> getAllCoupons() {
        List<Coupon> list = new ArrayList<>();
        String sql = "SELECT * FROM coupons ORDER BY id DESC";
        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            // Table might not exist yet
            System.err.println("Coupon fetch error: " + e.getMessage());
        }
        return list;
    }

    public Coupon getCouponByCode(String code) {
        String sql = "SELECT * FROM coupons WHERE code = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Coupon getCouponById(int id) {
        String sql = "SELECT * FROM coupons WHERE id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean toggleCoupon(int id, boolean active) {
        String sql = "UPDATE coupons SET is_active = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int createCoupon(String code, Coupon.CouponKind kind, double value, double minCart, LocalDateTime expiresAt,
            boolean active) {
        String sql = "INSERT INTO coupons (code, kind, value, min_cart, expires_at, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            // Map FIXED to AMOUNT for database compatibility (DB enum is AMOUNT/PERCENT)
            String kindValue = (kind == Coupon.CouponKind.FIXED) ? "AMOUNT" : kind.name();
            ps.setString(2, kindValue);
            ps.setDouble(3, value);
            ps.setDouble(4, minCart);
            ps.setTimestamp(5, expiresAt != null ? Timestamp.valueOf(expiresAt) : null);
            ps.setBoolean(6, active);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private Coupon mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("expires_at");
        String kindStr = rs.getString("kind");
        Coupon.CouponKind kind;
        try {
            // Map legacy "AMOUNT" to "FIXED" for backward compatibility
            if ("AMOUNT".equalsIgnoreCase(kindStr)) {
                kind = Coupon.CouponKind.FIXED;
            } else {
                kind = Coupon.CouponKind.valueOf(kindStr);
            }
        } catch (IllegalArgumentException e) {
            // Fallback to FIXED if unknown kind
            System.err.println("Unknown coupon kind: " + kindStr + ", defaulting to FIXED");
            kind = Coupon.CouponKind.FIXED;
        }
        return new Coupon(
                rs.getInt("id"),
                rs.getString("code"),
                kind,
                rs.getDouble("value"),
                rs.getDouble("min_cart"),
                rs.getBoolean("is_active"),
                ts != null ? ts.toLocalDateTime() : null);
    }
}
