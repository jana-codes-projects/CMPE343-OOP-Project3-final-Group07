package com.example.demo.dao;

import com.example.demo.db.DatabaseAdapter;
import com.example.demo.models.Order;
import com.example.demo.models.OrderItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Order operations.
 * Handles all database operations related to orders.
 * 
 * @author Group07
 * @version 1.0
 */
public class OrderDAO {
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public OrderDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Creates a new order with items (transactional).
     * 
     * @param order the order to create
     * @return the created order with ID, or null if creation fails
     */
    public Order createOrder(Order order) {
        Connection conn = null;
        try {
            conn = dbAdapter.getConnection();
            conn.setAutoCommit(false);
            
            // Insert order
            String orderSql = "INSERT INTO orders (customer_id, carrier_id, status, order_time, requested_delivery_time, " +
                    "total_before_tax, vat, total_after_tax, coupon_id, loyalty_discount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setInt(1, order.getCustomerId());
                if (order.getCarrierId() != null) {
                    orderStmt.setInt(2, order.getCarrierId());
                } else {
                    orderStmt.setNull(2, Types.INTEGER);
                }
                orderStmt.setString(3, order.getStatus().name());
                orderStmt.setTimestamp(4, Timestamp.valueOf(order.getOrderTime()));
                orderStmt.setTimestamp(5, Timestamp.valueOf(order.getRequestedDeliveryTime()));
                BigDecimal totalBeforeTax = order.getTotalBeforeTax();
                BigDecimal vat = order.getVat();
                BigDecimal totalAfterTax = order.getTotalAfterTax();
                BigDecimal loyaltyDiscount = order.getLoyaltyDiscount();
                orderStmt.setBigDecimal(6, totalBeforeTax);
                orderStmt.setBigDecimal(7, vat);
                orderStmt.setBigDecimal(8, totalAfterTax);
                if (order.getCouponId() != null) {
                    orderStmt.setInt(9, order.getCouponId());
                } else {
                    orderStmt.setNull(9, Types.INTEGER);
                }
                orderStmt.setBigDecimal(10, loyaltyDiscount);
                
                int rowsAffected = orderStmt.executeUpdate();
                if (rowsAffected > 0) {
                    ResultSet rs = orderStmt.getGeneratedKeys();
                    if (rs.next()) {
                        int orderId = rs.getInt(1);
                        order.setId(orderId);
                        
                        // Insert order items and update stock
                        String itemSql = "INSERT INTO order_items (order_id, product_id, kg, unit_price_applied, line_total) VALUES (?, ?, ?, ?, ?)";
                        String stockSql = "UPDATE products SET stock_kg = stock_kg - ? WHERE id = ? AND stock_kg >= ?";
                        try (PreparedStatement itemStmt = conn.prepareStatement(itemSql);
                             PreparedStatement stockStmt = conn.prepareStatement(stockSql)) {
                            for (OrderItem item : order.getItems()) {
                                itemStmt.setInt(1, orderId);
                                itemStmt.setInt(2, item.getProductId());
                                itemStmt.setBigDecimal(3, item.getQuantityKg());
                                itemStmt.setBigDecimal(4, item.getUnitPriceApplied());
                                itemStmt.setBigDecimal(5, item.getLineTotal());
                                itemStmt.addBatch();
                                
                                // Update stock within transaction
                                stockStmt.setBigDecimal(1, item.getQuantityKg());
                                stockStmt.setInt(2, item.getProductId());
                                stockStmt.setBigDecimal(3, item.getQuantityKg());
                                stockStmt.addBatch();
                            }
                            itemStmt.executeBatch();
                            stockStmt.executeBatch();
                        }
                        
                        conn.commit();
                        return order;
                    }
                }
            }
            conn.rollback();
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Gets an order by ID with all items.
     * 
     * @param orderId the order ID
     * @return the order, or null if not found
     */
    public Order getOrderById(int orderId) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(orderId));
                return order;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets all orders for a customer.
     * 
     * @param customerId the customer ID
     * @return list of orders
     */
    public List<Order> getOrdersByCustomer(int customerId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY order_time DESC";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * Gets all orders available for carriers (status = CREATED).
     * 
     * @return list of available orders
     */
    public List<Order> getAvailableOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = 'CREATED' ORDER BY order_time ASC";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * Gets orders assigned to a carrier.
     * 
     * @param carrierId the carrier ID
     * @param status the order status, or null for all statuses
     * @return list of orders
     */
    public List<Order> getOrdersByCarrier(int carrierId, Order.OrderStatus status) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE carrier_id = ?";
        if (status != null) {
            sql += " AND status = ?";
        }
        sql += " ORDER BY order_time DESC";
        
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, carrierId);
            if (status != null) {
                stmt.setString(2, status.name());
            }
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * Gets all orders (for owner).
     * 
     * @return list of all orders
     */
    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_time DESC";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * Assigns an order to a carrier.
     * 
     * @param orderId the order ID
     * @param carrierId the carrier ID
     * @return true if assignment is successful
     */
    public boolean assignOrderToCarrier(int orderId, int carrierId) {
        String sql = "UPDATE orders SET carrier_id = ?, status = 'ASSIGNED' WHERE id = ? AND status = 'CREATED'";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, carrierId);
            stmt.setInt(2, orderId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Marks an order as delivered.
     * 
     * @param orderId the order ID
     * @param deliveredTime the delivery time
     * @return true if update is successful
     */
    public boolean markOrderDelivered(int orderId, LocalDateTime deliveredTime) {
        String sql = "UPDATE orders SET status = 'DELIVERED', delivered_time = ? WHERE id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(deliveredTime));
            stmt.setInt(2, orderId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Cancels an order (within allowed timeframe).
     * 
     * @param orderId the order ID
     * @return true if cancellation is successful
     */
    public boolean cancelOrder(int orderId) {
        String sql = "UPDATE orders SET status = 'CANCELLED' WHERE id = ? AND status IN ('CREATED', 'ASSIGNED')";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets order items for an order.
     * 
     * @param orderId the order ID
     * @return list of order items
     */
    private List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getInt("id"));
                item.setOrderId(rs.getInt("order_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setQuantityKg(rs.getBigDecimal("kg"));
                item.setUnitPriceApplied(rs.getBigDecimal("unit_price_applied"));
                item.setLineTotal(rs.getBigDecimal("line_total"));
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Maps a ResultSet row to an Order object.
     * 
     * @param rs the ResultSet
     * @return the Order object
     * @throws SQLException if a database error occurs
     */
    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setCustomerId(rs.getInt("customer_id"));
        
        int carrierId = rs.getInt("carrier_id");
        if (!rs.wasNull()) {
            order.setCarrierId(carrierId);
        }
        
        order.setStatus(Order.OrderStatus.valueOf(rs.getString("status")));
        
        Timestamp orderTime = rs.getTimestamp("order_time");
        if (orderTime != null) {
            LocalDateTime orderDateTime = orderTime.toLocalDateTime();
            order.setOrderTime(orderDateTime);
        }
        
        Timestamp reqTime = rs.getTimestamp("requested_delivery_time");
        if (reqTime != null) {
            LocalDateTime reqDateTime = reqTime.toLocalDateTime();
            order.setRequestedDeliveryTime(reqDateTime);
        }
        
        Timestamp delTime = rs.getTimestamp("delivered_time");
        if (delTime != null) {
            LocalDateTime delDateTime = delTime.toLocalDateTime();
            order.setDeliveredTime(delDateTime);
        }
        
        order.setTotalBeforeTax(rs.getBigDecimal("total_before_tax"));
        order.setVat(rs.getBigDecimal("vat"));
        order.setTotalAfterTax(rs.getBigDecimal("total_after_tax"));
        
        int couponId = rs.getInt("coupon_id");
        if (!rs.wasNull()) {
            order.setCouponId(couponId);
        }
        
        order.setLoyaltyDiscount(rs.getBigDecimal("loyalty_discount"));
        
        return order;
    }
}

