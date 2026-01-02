package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.CartItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDao {

    private static final double VAT_RATE = 0.20; // %20

    public int createOrder(int customerId, List<CartItem> items, LocalDateTime requestedDelivery) {
        return createOrder(customerId, items, requestedDelivery, null);
    }

    public int createOrder(int customerId, List<CartItem> items, LocalDateTime requestedDelivery, Integer couponId) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        if (requestedDelivery == null) {
            throw new IllegalArgumentException("Requested delivery time cannot be empty.");
        }

        Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());
        Timestamp requestedTs = Timestamp.valueOf(requestedDelivery);

        // Calculate original subtotal (before coupon)
        // Round each line total before summing to match order_items precision
        double originalSubtotal = items.stream()
            .mapToDouble(item -> round2(item.getLineTotal()))
            .sum();
        
        // Apply coupon discount if provided
        // Validate coupon at order placement time to prevent race conditions
        double couponDiscount = 0.0;
        if (couponId != null) {
            Double discount = getCouponDiscount(couponId, originalSubtotal);
            if (discount == null) {
                // Coupon is invalid (expired/deactivated/not found/min cart not met) - throw exception to inform user
                throw new IllegalArgumentException("The selected coupon is no longer valid. Please remove it and try again.");
            }
            couponDiscount = discount; // discount can be 0.0 for valid coupons with zero discount
        }
        
        // Calculate post-coupon subtotal (this is what VAT is calculated on)
        double totalAfterCoupon = Math.max(0, originalSubtotal - couponDiscount);
        double vat = round2(totalAfterCoupon * VAT_RATE);
        double totalAfterTax = round2(totalAfterCoupon + vat);
        
        // Store the post-coupon subtotal in totalBeforeTax (since VAT is calculated on this)
        // This ensures consistency: totalBeforeTax + VAT = totalAfterTax
        double totalBeforeTax = totalAfterCoupon;

        String insertOrder = """
                    INSERT INTO orders
                      (customer_id, carrier_id, status, order_time, requested_delivery_time, delivered_time,
                       total_before_tax, vat, total_after_tax, coupon_id, loyalty_discount)
                    VALUES
                      (?, NULL, 'CREATED', ?, ?, NULL,
                       ?, ?, ?, ?, 0)
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
                // Set coupon_id (position 7)
                if (couponId != null) {
                    ps.setInt(7, couponId);
                } else {
                    ps.setNull(7, Types.INTEGER);
                }

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        c.rollback();
                        throw new RuntimeException("Could not retrieve order id.");
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
                        throw new RuntimeException("Insufficient stock: " + it.getProduct().getName());
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
            throw new RuntimeException("Could not create order: " + e.getMessage(), e);
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
    
    public List<com.cmpe343.model.Order> getOrdersForCustomer(int customerId) {
        List<com.cmpe343.model.Order> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY order_time DESC";

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.cmpe343.model.Order order = mapOrder(rs);
                    // Load order items
                    order.setItems(getOrderItems(order.getId()));
                    list.add(order);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching customer orders: " + e.getMessage());
        }
        return list;
    }
    
    public List<com.cmpe343.model.CartItem> getOrderItems(int orderId) {
        List<com.cmpe343.model.CartItem> items = new java.util.ArrayList<>();
        String sql = """
            SELECT oi.product_id, oi.kg, oi.unit_price_applied, oi.line_total,
                   p.name, p.type, p.price, p.stock_kg, p.threshold_kg, p.image_blob
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            WHERE oi.order_id = ?
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Use current product price (not historical) to maintain data integrity
                    // Historical pricing is stored separately in CartItem
                    // Images are stored in BLOB, accessed via ProductDao.getProductImageBlob(productId)
                    com.cmpe343.model.Product product = new com.cmpe343.model.Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getDouble("price"), // Current product price from products table
                        rs.getDouble("stock_kg"),
                        rs.getDouble("threshold_kg")
                    );
                    // Store historical pricing separately to preserve order integrity
                    // This ensures CartItem.getUnitPrice() and getLineTotal() return the values
                    // that were applied at order creation time, not the current product price
                    double historicalUnitPrice = rs.getDouble("unit_price_applied");
                    double historicalLineTotal = rs.getDouble("line_total");
                    com.cmpe343.model.CartItem item = new com.cmpe343.model.CartItem(
                        product, 
                        rs.getDouble("kg"),
                        historicalUnitPrice,
                        historicalLineTotal
                    );
                    items.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching order items: " + e.getMessage());
        }
        return items;
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

        // Add null check to prevent NullPointerException if order_time is NULL in database
        java.sql.Timestamp orderTimeStamp = rs.getTimestamp("order_time");
        LocalDateTime orderTime = orderTimeStamp != null 
            ? orderTimeStamp.toLocalDateTime() 
            : LocalDateTime.now(); // Fallback to current time if NULL
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

    /**
     * Gets the discount amount for a coupon.
     * Returns null if the coupon is invalid (not found, expired, or inactive).
     * Returns the discount amount (which may be 0.0) if the coupon is valid.
     * 
     * @param couponId The coupon ID
     * @return The discount amount if coupon is valid, null if invalid
     */
    private Double getCouponDiscount(int couponId, double cartTotal) {
        String sql = "SELECT kind, value, min_cart FROM coupons WHERE id = ? AND is_active = 1 AND (expires_at IS NULL OR expires_at >= NOW())";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Coupon found and valid
                    String kind = rs.getString("kind");
                    double value = rs.getDouble("value");
                    double minCart = rs.getDouble("min_cart");
                    
                    // Check minimum cart requirement
                    if (cartTotal < minCart) {
                        return null; // Cart doesn't meet minimum requirement
                    }
                    
                    // Calculate discount based on type
                    if ("AMOUNT".equals(kind)) {
                        return Math.min(value, cartTotal); // Don't discount more than cart total
                    } else if ("PERCENT".equals(kind)) {
                        return cartTotal * (value / 100.0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching coupon: " + e.getMessage());
        }
        // Coupon not found or invalid - return null to distinguish from valid coupon with 0 discount
        return null;
    }
    
    public double getCouponDiscountForOrder(int orderId) {
        // Get the order's total_before_tax to calculate percentage discount correctly
        String sql = """
            SELECT c.kind, c.value, o.total_before_tax
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
                    double totalBeforeTax = rs.getDouble("total_before_tax");
                    
                    if ("AMOUNT".equals(kind)) {
                        return Math.min(value, totalBeforeTax);
                    } else if ("PERCENT".equals(kind)) {
                        return totalBeforeTax * (value / 100.0);
                    }
                }
            }
        } catch (Exception e) {
            // Order might not have a coupon, return 0
        }
        return 0.0;
    }

    public List<com.cmpe343.model.Order> getAvailableOrders() {
        List<com.cmpe343.model.Order> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = 'CREATED' AND carrier_id IS NULL ORDER BY order_time DESC";
        
        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                com.cmpe343.model.Order order = mapOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                list.add(order);
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
                    com.cmpe343.model.Order order = mapOrder(rs);
                    order.setItems(getOrderItems(order.getId()));
                    list.add(order);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching carrier orders: " + e.getMessage());
        }
        return list;
    }
    
    public boolean assignOrderToCarrier(int orderId, int carrierId) {
        String sql = """
            UPDATE orders 
            SET carrier_id = ?, status = 'ASSIGNED' 
            WHERE id = ? AND status = 'CREATED' AND carrier_id IS NULL
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carrierId);
            ps.setInt(2, orderId);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (Exception e) {
            System.err.println("Error assigning order to carrier: " + e.getMessage());
            return false;
        }
    }
    
    public boolean markOrderDelivered(int orderId, LocalDateTime deliveredTime) {
        String sql = """
            UPDATE orders 
            SET status = 'DELIVERED', delivered_time = ? 
            WHERE id = ? AND status = 'ASSIGNED'
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(deliveredTime));
            ps.setInt(2, orderId);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (Exception e) {
            System.err.println("Error marking order as delivered: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets customer loyalty statistics including order count, total spent, and purchase frequency.
     * 
     * @return A list of customer loyalty data as Object arrays: [customerId, username, orderCount, totalSpent, daysSinceFirstOrder, avgDaysBetweenOrders]
     */
    public List<Object[]> getCustomerLoyaltyStats() {
        List<Object[]> stats = new java.util.ArrayList<>();
        String sql = """
            SELECT 
                u.id as customer_id,
                u.username,
                COUNT(o.id) as order_count,
                COALESCE(SUM(o.total_after_tax), 0) as total_spent,
                MIN(o.order_time) as first_order_date,
                MAX(o.order_time) as last_order_date
            FROM users u
            LEFT JOIN orders o ON u.id = o.customer_id
            WHERE u.role = 'customer' AND u.is_active = 1
            GROUP BY u.id, u.username
            HAVING order_count > 0
            ORDER BY order_count DESC, total_spent DESC
        """;
        
        try (Connection c = Db.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                int customerId = rs.getInt("customer_id");
                String username = rs.getString("username");
                int orderCount = rs.getInt("order_count");
                double totalSpent = rs.getDouble("total_spent");
                
                java.sql.Timestamp firstOrder = rs.getTimestamp("first_order_date");
                java.sql.Timestamp lastOrder = rs.getTimestamp("last_order_date");
                
                // Calculate days since first order
                long daysSinceFirstOrder = 0;
                if (firstOrder != null) {
                    daysSinceFirstOrder = java.time.temporal.ChronoUnit.DAYS.between(
                        firstOrder.toLocalDateTime().toLocalDate(),
                        java.time.LocalDate.now()
                    );
                    if (daysSinceFirstOrder == 0) daysSinceFirstOrder = 1; // Avoid division by zero
                }
                
                // Calculate average days between orders
                double avgDaysBetweenOrders = 0.0;
                if (orderCount > 1 && firstOrder != null && lastOrder != null) {
                    long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                        firstOrder.toLocalDateTime().toLocalDate(),
                        lastOrder.toLocalDateTime().toLocalDate()
                    );
                    if (totalDays > 0) {
                        avgDaysBetweenOrders = (double) totalDays / (orderCount - 1);
                    }
                }
                
                // Calculate purchase frequency (orders per month)
                double ordersPerMonth = 0.0;
                if (daysSinceFirstOrder > 0) {
                    double months = daysSinceFirstOrder / 30.0;
                    ordersPerMonth = orderCount / Math.max(months, 0.1); // Avoid division by zero
                }
                
                stats.add(new Object[]{
                    customerId,
                    username,
                    orderCount,
                    totalSpent,
                    daysSinceFirstOrder,
                    avgDaysBetweenOrders,
                    ordersPerMonth
                });
            }
        } catch (Exception e) {
            System.err.println("Error fetching customer loyalty stats: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }
    
    /**
     * Cancels an order, restocks products, and updates order status.
     * This method:
     * 1. Updates the order status to CANCELLED (only if order is CREATED or ASSIGNED)
     * 2. Restocks all products from the order items
     * 3. Uses a transaction to ensure data consistency
     * 
     * Note: Refunds are typically handled externally through payment processors.
     * This method only handles the inventory restocking and status update.
     * 
     * @param orderId The ID of the order to cancel
     * @return true if the order was successfully cancelled, false otherwise
     * @throws RuntimeException if the order cannot be cancelled (e.g., already DELIVERED or CANCELLED)
     */
    public boolean cancelOrder(int orderId) {
        // First, get the order to check its status and items
        com.cmpe343.model.Order order = null;
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM orders WHERE id = ?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    order = mapOrder(rs);
                } else {
                    return false; // Order not found
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching order: " + e.getMessage());
            return false;
        }
        
        // Check if order can be cancelled (only CREATED or ASSIGNED orders can be cancelled)
        if (order.getStatus() == com.cmpe343.model.Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel a delivered order.");
        }
        if (order.getStatus() == com.cmpe343.model.Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled.");
        }
        
        // Load order items
        List<com.cmpe343.model.CartItem> items = getOrderItems(orderId);
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Order has no items to restock.");
        }
        
        String updateOrderStatus = """
            UPDATE orders 
            SET status = 'CANCELLED' 
            WHERE id = ? AND status IN ('CREATED', 'ASSIGNED')
        """;
        
        String restockProduct = """
            UPDATE products
            SET stock_kg = stock_kg + ?
            WHERE id = ?
        """;
        
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            
            try {
                // 1) Update order status
                try (PreparedStatement ps = c.prepareStatement(updateOrderStatus)) {
                    ps.setInt(1, orderId);
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        c.rollback();
                        throw new RuntimeException("Could not update order status. Order may have already been cancelled or delivered.");
                    }
                }
                
                // 2) Restock all products
                for (com.cmpe343.model.CartItem item : items) {
                    try (PreparedStatement ps = c.prepareStatement(restockProduct)) {
                        ps.setDouble(1, item.getQuantityKg());
                        ps.setInt(2, item.getProduct().getId());
                        ps.executeUpdate();
                    }
                }
                
                c.commit();
                return true;
                
            } catch (Exception e) {
                c.rollback();
                throw new RuntimeException("Could not cancel order: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            System.err.println("Error cancelling order: " + e.getMessage());
            throw new RuntimeException("Could not cancel order: " + e.getMessage(), e);
        }
    }
    
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
