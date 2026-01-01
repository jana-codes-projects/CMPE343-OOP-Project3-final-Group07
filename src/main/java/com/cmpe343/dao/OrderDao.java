package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.CartItem;
import com.cmpe343.model.Product;
import com.cmpe343.model.Order;
import com.cmpe343.model.Order.OrderStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Order operations.
 * Handles CRUD, stock management, and business analytics.
 */
public class OrderDao {

    private static final double VAT_RATE = 0.20;

    // --- ORDER CREATION LOGIC ---

    /**
     * Creates a new order, calculates totals with VAT/Coupons, and reduces stock.
     */
    public int createOrder(int customerId, List<CartItem> items, LocalDateTime requestedDelivery, Integer couponId) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Cart is empty.");
        if (requestedDelivery == null) throw new IllegalArgumentException("Requested delivery time cannot be empty.");

        Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());
        Timestamp requestedTs = Timestamp.valueOf(requestedDelivery);

        double originalSubtotal = items.stream().mapToDouble(item -> round2(item.getLineTotal())).sum();
        double couponDiscount = 0.0;

        // Apply coupon if exists
        if (couponId != null) {
            Double discount = getCouponDiscount(couponId, originalSubtotal);
            if (discount == null) throw new IllegalArgumentException("Invalid coupon or minimum amount not met.");
            couponDiscount = discount;
        }

        double totalAfterCoupon = Math.max(0, originalSubtotal - couponDiscount);
        double vat = round2(totalAfterCoupon * VAT_RATE);
        double totalAfterTax = round2(totalAfterCoupon + vat);

        String insertOrder = """
                    INSERT INTO orders
                      (customer_id, status, order_time, requested_delivery_time, 
                       total_before_tax, vat, total_after_tax, coupon_id, loyalty_discount)
                    VALUES (?, 'CREATED', ?, ?, ?, ?, ?, ?, 0)
                """;

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false); // Transaction start
            int orderId;
            try (PreparedStatement ps = c.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.setTimestamp(2, nowTs);
                ps.setTimestamp(3, requestedTs);
                ps.setDouble(4, totalAfterCoupon);
                ps.setDouble(5, vat);
                ps.setDouble(6, totalAfterTax);
                if (couponId != null) ps.setInt(7, couponId); else ps.setNull(7, Types.INTEGER);

                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new RuntimeException("Failed to retrieve generated Order ID.");
                orderId = keys.getInt(1);
            }

            // Link items and update inventory
            insertOrderItemsAndReduceStock(c, orderId, items);

            c.commit(); // Transaction success
            return orderId;
        } catch (Exception e) {
            throw new RuntimeException("Order Processing Failed: " + e.getMessage(), e);
        }
    }

    // --- CARRIER & STATUS MANAGEMENT ---

    public boolean assignOrderToCarrier(int orderId, int carrierId) {
        String sql = "UPDATE orders SET carrier_id = ?, status = 'ASSIGNED' WHERE id = ? AND status = 'CREATED'";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carrierId);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean markOrderDelivered(int orderId, LocalDateTime deliveredTime) {
        String sql = "UPDATE orders SET status = 'DELIVERED', delivered_time = ? WHERE id = ? AND status = 'ASSIGNED'";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(deliveredTime));
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // --- DATA RETRIEVAL ---

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_time DESC";
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapOrder(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<CartItem> getOrderItems(int orderId) {
        List<CartItem> items = new ArrayList<>();
        String sql = """
            SELECT oi.*, p.name, p.type, p.price, p.stock_kg, p.threshold_kg 
            FROM order_items oi 
            JOIN products p ON oi.product_id = p.id 
            WHERE oi.order_id = ?
        """;
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Product p = new Product(
                        rs.getInt("product_id"), rs.getString("name"),
                        rs.getString("type"), rs.getDouble("price"),
                        rs.getDouble("stock_kg"), rs.getDouble("threshold_kg")
                );
                items.add(new CartItem(p, rs.getDouble("kg"), rs.getDouble("unit_price_applied"), rs.getDouble("line_total")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return items;
    }

    // --- BUSINESS ANALYTICS & REPORTS ---

    /*
     * Fetches top 5 selling products based on weight sold.
     */
    public List<Object[]> getTopSellingProducts() {
        List<Object[]> report = new ArrayList<>();
        String sql = """
            SELECT p.name, SUM(oi.kg) as total_kg 
            FROM order_items oi 
            JOIN products p ON oi.product_id = p.id 
            GROUP BY p.id, p.name 
            ORDER BY total_kg DESC 
            LIMIT 5
        """;
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                report.add(new Object[]{rs.getString("name"), rs.getDouble("total_kg")});
            }
        } catch (Exception e) { e.printStackTrace(); }
        return report;
    }

    /*
     * Fetches delivery count per carrier for performance ranking.
     */
    public List<Object[]> getCarrierPerformance() {
        List<Object[]> stats = new ArrayList<>();
        String sql = """
            SELECT u.username, COUNT(o.id) as deliveries 
            FROM orders o 
            JOIN users u ON o.carrier_id = u.id 
            WHERE o.status = 'DELIVERED' 
            GROUP BY u.id, u.username 
            ORDER BY deliveries DESC
        """;
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stats.add(new Object[]{rs.getString("username"), rs.getInt("deliveries")});
            }
        } catch (Exception e) { e.printStackTrace(); }
        return stats;
    }

    /*
     * Retrieves customer spending and order count for Loyalty Tiering.
     */
    public List<Object[]> getCustomerLoyaltyStats() {
        List<Object[]> stats = new ArrayList<>();
        String sql = """
            SELECT u.id, u.username, COUNT(o.id) as cnt, SUM(o.total_after_tax) as spent 
            FROM users u 
            JOIN orders o ON u.id = o.customer_id 
            GROUP BY u.id, u.username 
            ORDER BY cnt DESC
        """;
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                // Returns: ID, Username, Order Count, Total Spent, and placeholders for tiers
                stats.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getDouble(4), 1L, 1.0, 1.0});
            }
        } catch (Exception e) { e.printStackTrace(); }
        return stats;
    }

    // --- INTERNAL HELPERS ---

    private void insertOrderItemsAndReduceStock(Connection c, int orderId, List<CartItem> items) throws SQLException {
        String itemSql = "INSERT INTO order_items (order_id, product_id, kg, unit_price_applied, line_total) VALUES (?,?,?,?,?)";
        String stockSql = "UPDATE products SET stock_kg = stock_kg - ? WHERE id = ? AND stock_kg >= ?";

        for (CartItem it : items) {
            // Check and update inventory
            try (PreparedStatement ps = c.prepareStatement(stockSql)) {
                ps.setDouble(1, it.getQuantityKg());
                ps.setInt(2, it.getProduct().getId());
                ps.setDouble(3, it.getQuantityKg());
                if (ps.executeUpdate() == 0) throw new SQLException("Insufficient stock for: " + it.getProduct().getName());
            }
            // Insert item record
            try (PreparedStatement ps = c.prepareStatement(itemSql)) {
                ps.setInt(1, orderId);
                ps.setInt(2, it.getProduct().getId());
                ps.setDouble(3, it.getQuantityKg());
                ps.setDouble(4, it.getUnitPrice());
                ps.setDouble(5, it.getLineTotal());
                ps.executeUpdate();
            }
        }
    }

    private Double getCouponDiscount(int couponId, double cartTotal) {
        String sql = "SELECT kind, value, min_cart FROM coupons WHERE id = ? AND is_active = 1";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && cartTotal >= rs.getDouble("min_cart")) {
                String kind = rs.getString("kind");
                double val = rs.getDouble("value");
                return "PERCENT".equals(kind) ? cartTotal * (val / 100.0) : val;
            }
        } catch (Exception e) {}
        return null;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        int carrierId = rs.getInt("carrier_id");
        return new Order(
                rs.getInt("id"), rs.getInt("customer_id"), rs.wasNull() ? null : carrierId,
                OrderStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("order_time").toLocalDateTime(),
                rs.getTimestamp("requested_delivery_time").toLocalDateTime(),
                rs.getTimestamp("delivered_time") != null ? rs.getTimestamp("delivered_time").toLocalDateTime() : null,
                rs.getDouble("total_before_tax"), rs.getDouble("vat"), rs.getDouble("total_after_tax")
        );
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    public List<Order> getOrdersForCustomer(int customerId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY order_time DESC";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Order o = mapOrder(rs);
                o.setItems(getOrderItems(o.getId()));
                list.add(o);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<Order> getAvailableOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = 'CREATED' AND carrier_id IS NULL ORDER BY order_time ASC";
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Order o = mapOrder(rs);
                o.setItems(getOrderItems(o.getId()));
                list.add(o);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /*
     * Cancels an order, restores the stock levels for each product, and updates status.
     */
    public boolean cancelOrderAndRestoreStock(int orderId) {
        String updateStatusSql = "UPDATE orders SET status = 'CANCELLED' WHERE id = ? AND status NOT IN ('DELIVERED', 'CANCELLED')";
        String getItemsSql = "SELECT product_id, kg FROM order_items WHERE order_id = ?";
        String restoreStockSql = "UPDATE products SET stock_kg = stock_kg + ? WHERE id = ?";
        // ADDED: Refund query
        String refundSql = "UPDATE users u JOIN orders o ON u.id = o.customer_id SET u.wallet_balance = u.wallet_balance + o.total_after_tax WHERE o.id = ?";

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false); // Start Transaction

            // 1. Update Order Status
            try (PreparedStatement ps = c.prepareStatement(updateStatusSql)) {
                ps.setInt(1, orderId);
                if (ps.executeUpdate() == 0) {
                    c.rollback();
                    return false;
                }
            }

            // 2. Restore Stock Levels
            List<CartItem> itemsToRestore = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(getItemsSql)) {
                ps.setInt(1, orderId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Product p = new Product(rs.getInt("product_id"), "", "", 0.0, 0.0, 0.0);
                    itemsToRestore.add(new CartItem(p, rs.getDouble("kg"), 0.0, 0.0));
                }
            }

            try (PreparedStatement ps = c.prepareStatement(restoreStockSql)) {
                for (CartItem item : itemsToRestore) {
                    ps.setDouble(1, item.getQuantityKg());
                    ps.setInt(2, item.getProduct().getId());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 3. REFUND MONEY TO WALLET (Crucial Step)
            try (PreparedStatement ps = c.prepareStatement(refundSql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            c.commit(); // Save everything
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateOrderStatus(int orderId, Order.OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<Order> getOrdersByCarrier(int carrierId, OrderStatus status) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE carrier_id = ? AND status = ? ORDER BY order_time DESC";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carrierId);
            ps.setString(2, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Order o = mapOrder(rs);
                o.setItems(getOrderItems(o.getId()));
                list.add(o);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public double getCouponDiscountForOrder(int orderId) {
        String sql = "SELECT c.kind, c.value, o.total_before_tax FROM orders o JOIN coupons c ON o.coupon_id = c.id WHERE o.id = ?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String kind = rs.getString("kind");
                double val = rs.getDouble("value");
                return "PERCENT".equals(kind) ? rs.getDouble("total_before_tax") * (val / 100.0) : val;
            }
        } catch (Exception e) {}
        return 0.0;
    }
}