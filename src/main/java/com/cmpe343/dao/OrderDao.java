package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.CartItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDao {

    private static final double VAT_RATE = 0.20; // %20

    public int createOrder(int customerId, List<CartItem> items, LocalDateTime requestedDelivery) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Sepet boş.");
        }
        if (requestedDelivery == null) {
            throw new IllegalArgumentException("Requested delivery time boş olamaz.");
        }

        double totalBeforeTax = items.stream().mapToDouble(CartItem::getLineTotal).sum();
        double vat = round2(totalBeforeTax * VAT_RATE);
        double totalAfterTax = round2(totalBeforeTax + vat);

        Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());
        Timestamp requestedTs = Timestamp.valueOf(requestedDelivery);

        String insertOrder = """
                    INSERT INTO orders
                      (customer_id, carrier_id, status, order_time, requested_delivery_time, delivered_time,
                       total_before_tax, vat, total_after_tax, coupon_id, loyalty_discount)
                    VALUES
                      (?, NULL, 'CREATED', ?, ?, NULL,
                       ?, ?, ?, NULL, 0)
                """;

        // ✅ SENİN TABLOYA GÖRE:
        // kg ve unit_price_applied
        String insertItem = """
                    INSERT INTO order_items (order_id, product_id, kg, unit_price_applied, line_total)
                    VALUES (?, ?, ?, ?, ?)
                """;

        String updateStock = """
                    UPDATE products
                    SET stock_kg = stock_kg - ?
                    WHERE id = ? AND stock_kg >= ?
                """;

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);

            int orderId;

            // 1) Order insert
            try (PreparedStatement ps = c.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.setTimestamp(2, nowTs);
                ps.setTimestamp(3, requestedTs);
                ps.setDouble(4, round2(totalBeforeTax));
                ps.setDouble(5, vat);
                ps.setDouble(6, totalAfterTax);

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        c.rollback();
                        throw new RuntimeException("Order id alınamadı.");
                    }
                    orderId = keys.getInt(1);
                }
            }

            // 2) Items + stock update
            for (CartItem it : items) {
                // stok düş
                try (PreparedStatement ps = c.prepareStatement(updateStock)) {
                    ps.setDouble(1, it.getQuantityKg());
                    ps.setInt(2, it.getProduct().getId());
                    ps.setDouble(3, it.getQuantityKg());
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        c.rollback();
                        throw new RuntimeException("Yetersiz stok: " + it.getProduct().getName());
                    }
                }

                // ✅ order_item insert (kg + unit_price_applied)
                try (PreparedStatement ps = c.prepareStatement(insertItem)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, it.getProduct().getId());
                    ps.setDouble(3, round2(it.getQuantityKg()));
                    ps.setDouble(4, round2(it.getUnitPrice()));
                    ps.setDouble(5, round2(it.getLineTotal()));
                    ps.executeUpdate();
                }
            }

            c.commit();
            return orderId;

        } catch (Exception e) {
            throw new RuntimeException("Order oluşturulamadı: " + e.getMessage(), e);
        }
    }

    public List<com.cmpe343.model.Order> getAllOrders() {
        List<com.cmpe343.model.Order> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_time DESC";

        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapOrder(rs));
            }

        } catch (Exception e) {
            System.err.println("Error fetching orders: " + e.getMessage());
        }
        return list;
    }

    private com.cmpe343.model.Order mapOrder(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int customerId = rs.getInt("customer_id");
        int carrierId = rs.getInt("carrier_id");
        if (rs.wasNull())
            carrierId = 0; // or null logic
        String statusStr = rs.getString("status");
        com.cmpe343.model.Order.OrderStatus status = com.cmpe343.model.Order.OrderStatus.CREATED;
        try {
            status = com.cmpe343.model.Order.OrderStatus.valueOf(statusStr);
        } catch (Exception e) {
        }

        LocalDateTime orderTime = rs.getTimestamp("order_time").toLocalDateTime();
        LocalDateTime requested = rs.getTimestamp("requested_delivery_time") != null
                ? rs.getTimestamp("requested_delivery_time").toLocalDateTime()
                : null;
        LocalDateTime delivered = rs.getTimestamp("delivered_time") != null
                ? rs.getTimestamp("delivered_time").toLocalDateTime()
                : null;

        return new com.cmpe343.model.Order(
                id,
                customerId,
                carrierId == 0 ? null : carrierId,
                status,
                orderTime,
                requested,
                delivered,
                rs.getDouble("total_before_tax"),
                rs.getDouble("vat"),
                rs.getDouble("total_after_tax"));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
