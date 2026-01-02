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

                // order_item insert (kg + unit_price_applied)
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

    // New method for fetching order items - critical for Order Details view
    public List<CartItem> getOrderItems(int orderId) {
        List<CartItem> list = new java.util.ArrayList<>();
        String sql = """
                    SELECT oi.*, p.id as p_id, p.name, p.type, p.price, p.stock_kg, p.threshold_kg, p.image_blob, p.is_active
                    FROM order_items oi
                    JOIN products p ON oi.product_id = p.id
                    WHERE oi.order_id = ?
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Create minimal product from result set
                    // We need basic details for display
                    com.cmpe343.model.Product p = new com.cmpe343.model.Product(
                            rs.getInt("p_id"),
                            rs.getString("name"),
                            com.cmpe343.model.Product.ProductType.valueOf(rs.getString("type")),
                            rs.getDouble("price"),
                            rs.getDouble("stock_kg"),
                            rs.getDouble("threshold_kg"),
                            rs.getBytes("image_blob"));
                    p.setActive(rs.getBoolean("is_active"));

                    // Create cart item
                    // Note: In CartItem logic, we usually set current price.
                    // But here we might want to preserve the unit_price_applied from history.
                    // For display purposes, we can use the stored line_total and reconstruct if
                    // needed
                    double quantity = rs.getDouble("kg");
                    double unitPriceApplied = rs.getDouble("unit_price_applied");

                    CartItem item = new CartItem(p, quantity);
                    // We ideally should allow setting the effective price directly if we want
                    // historical accuracy
                    // but CartItem calculates it from Product.
                    // For now this is sufficient for basic display relative to product.
                    // If strict historical accuracy is needed, CartItem needs a way to override
                    // price.
                    // For this project scope, reloading product info is acceptable.

                    list.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching order items: " + e.getMessage());
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

    public List<com.cmpe343.model.Order> getAvailableOrders() {
        List<com.cmpe343.model.Order> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = 'CREATED' AND carrier_id IS NULL ORDER BY order_time DESC";

        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        } catch (Exception e) {
            System.err.println("Error fetching available orders: " + e.getMessage());
        }
        return list;
    }

    public List<com.cmpe343.model.Order> getOrdersByCarrier(int carrierId, com.cmpe343.model.Order.OrderStatus status) {
        List<com.cmpe343.model.Order> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM orders WHERE carrier_id = ? AND status = ? ORDER BY order_time DESC";

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carrierId);
            ps.setString(2, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching carrier orders: " + e.getMessage());
        }
        return list;
    }

    public boolean assignOrderToCarrier(int orderId, int carrierId) {
        String sql = "UPDATE orders SET carrier_id = ?, status = 'ASSIGNED' WHERE id = ? AND carrier_id IS NULL";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carrierId);
            ps.setInt(2, orderId);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<com.cmpe343.model.Order> getOrdersByUserId(int userId) {
        List<com.cmpe343.model.Order> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY order_time DESC";

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching user orders: " + e.getMessage());
        }
        return list;
    }

    public boolean cancelOrder(int orderId) {
        String updateStatus = "UPDATE orders SET status = 'CANCELLED' WHERE id = ? AND status != 'DELIVERED' AND status != 'CANCELLED'";
        String restoreStock = """
                    UPDATE products p
                    JOIN order_items oi ON p.id = oi.product_id
                    SET p.stock_kg = p.stock_kg + oi.kg
                    WHERE oi.order_id = ?
                """;

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(updateStatus)) {
                ps.setInt(1, orderId);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    try (PreparedStatement ps2 = c.prepareStatement(restoreStock)) {
                        ps2.setInt(1, orderId);
                        ps2.executeUpdate();
                    }
                    c.commit();
                    return true;
                } else {
                    c.rollback();
                    return false;
                }
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markOrderDelivered(int orderId, LocalDateTime time) {
        String sql = "UPDATE orders SET status = 'DELIVERED', delivered_time = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(time));
            ps.setInt(2, orderId);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Object[]> getCustomerLoyaltyStats() {
        String sql = """
                    SELECT
                        u.id,
                        u.username,
                        COUNT(o.id) as order_count,
                        SUM(o.total_after_tax) as total_spent,
                        DATEDIFF(NOW(), MIN(o.order_time)) as days_since_first,
                        DATEDIFF(NOW(), MIN(o.order_time)) / NULLIF(COUNT(o.id)-1, 0) as avg_days_between,
                        COUNT(o.id) / (DATEDIFF(NOW(), MIN(o.order_time)) / 30.0) as orders_per_month
                    FROM users u
                    JOIN orders o ON u.id = o.customer_id
                    WHERE u.role = 'customer' AND o.status = 'DELIVERED'
                    GROUP BY u.id, u.username
                    ORDER BY total_spent DESC
                """;

        List<Object[]> stats = new java.util.ArrayList<>();
        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stats.add(new Object[] {
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getInt("order_count"),
                        rs.getDouble("total_spent"),
                        rs.getLong("days_since_first"),
                        rs.getDouble("avg_days_between"),
                        rs.getDouble("orders_per_month")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public void createInvoice(int orderId, List<CartItem> items) {
        // 1. Generate Invoice Text
        StringBuilder sb = new StringBuilder();
        sb.append("--------------------------------------------------\n");
        sb.append("                  INVOICE / RECEIPT               \n");
        sb.append("--------------------------------------------------\n");
        sb.append("Order ID: ").append(orderId).append("\n");
        sb.append("Date:     ").append(LocalDateTime.now()).append("\n");
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("%-20s %10s %10s %10s\n", "Item", "Qty", "Price", "Total"));
        sb.append("--------------------------------------------------\n");

        double subtotal = 0;
        for (CartItem item : items) {
            String name = item.getProduct().getName();
            if (name.length() > 20)
                name = name.substring(0, 17) + "...";
            sb.append(String.format("%-20s %9.2f %10.2f %10.2f\n",
                    name,
                    item.getQuantityKg(),
                    item.getUnitPrice(),
                    item.getLineTotal()));
            subtotal += item.getLineTotal();
        }
        sb.append("--------------------------------------------------\n");
        double vat = round2(subtotal * VAT_RATE);
        double total = round2(subtotal + vat);

        sb.append(String.format("Subtotal: %34.2f\n", subtotal));
        sb.append(String.format("VAT (20%%): %33.2f\n", vat));
        sb.append(String.format("TOTAL:    %34.2f\n", total));
        sb.append("--------------------------------------------------\n");

        String invoiceText = sb.toString();
        byte[] pdfBlob = invoiceText.getBytes(); // Mock PDF as text bytes

        // 2. Insert into DB
        String sql = "INSERT INTO invoices (order_id, pdf_blob, invoice_text) VALUES (?, ?, ?)";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setBytes(2, pdfBlob);
            ps.setString(3, invoiceText);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error generating invoice: " + e.getMessage());
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public double getCouponDiscountForOrder(int orderId) {
        String sql = """
                    SELECT c.kind, c.value, c.min_cart_amount, o.total_before_tax
                    FROM orders o
                    JOIN coupons c ON o.coupon_id = c.id
                    WHERE o.id = ?
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String kind = rs.getString("kind");
                    double value = rs.getDouble("value");
                    // We don't strictly need these for calculation if the discount was already
                    // applied to total_after_tax
                    // BUT, typically we want to know how much was discounted.
                    // If the DB doesn't store the discount amount directly, we verify against the
                    // kind.
                    // The schema has 'coupon_id', but not 'discount_amount'.
                    // However, we can calculate it.

                    double totalBefore = rs.getDouble("total_before_tax");

                    if ("PERCENT".equalsIgnoreCase(kind)) {
                        return round2(totalBefore * (value / 100.0));
                    } else {
                        // FIXED
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
