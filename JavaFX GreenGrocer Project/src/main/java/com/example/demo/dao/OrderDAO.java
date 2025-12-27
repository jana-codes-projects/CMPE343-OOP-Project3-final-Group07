package com.example.demo.dao;

import com.example.demo.db.DatabaseAdapter;
import com.example.demo.models.Order;
import com.example.demo.models.OrderItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            System.out.println("DEBUG OrderDAO: ========== CREATING ORDER ==========");
            System.out.println("DEBUG OrderDAO: Customer ID: " + order.getCustomerId());
            System.out.println("DEBUG OrderDAO: Order Items Count: " + (order.getItems() != null ? order.getItems().size() : 0));
            System.out.println("DEBUG OrderDAO: Total After Tax: " + order.getTotalAfterTax());
            
            if (order.getItems() == null || order.getItems().isEmpty()) {
                System.err.println("ERROR OrderDAO: Cannot create order with no items!");
                return null;
            }
            
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
                orderStmt.setBigDecimal(10, loyaltyDiscount != null ? loyaltyDiscount : BigDecimal.ZERO);
                
                System.out.println("DEBUG OrderDAO: Executing order INSERT statement...");
                int rowsAffected = orderStmt.executeUpdate();
                System.out.println("DEBUG OrderDAO: Order INSERT affected " + rowsAffected + " row(s)");
                
                if (rowsAffected > 0) {
                    ResultSet rs = orderStmt.getGeneratedKeys();
                    if (rs.next()) {
                        int orderId = rs.getInt(1);
                        order.setId(orderId);
                        System.out.println("DEBUG OrderDAO: Order created with ID: " + orderId);
                        
                        // Insert order items
                        // Note: Stock is already decremented when items are added to cart,
                        // so we don't need to decrement again here
                        String itemSql = "INSERT INTO order_items (order_id, product_id, kg, unit_price_applied, line_total) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                            int itemCount = 0;
                            for (OrderItem item : order.getItems()) {
                                itemStmt.setInt(1, orderId);
                                itemStmt.setInt(2, item.getProductId());
                                itemStmt.setBigDecimal(3, item.getQuantityKg());
                                itemStmt.setBigDecimal(4, item.getUnitPriceApplied());
                                itemStmt.setBigDecimal(5, item.getLineTotal());
                                itemStmt.addBatch();
                                itemCount++;
                                System.out.println("DEBUG OrderDAO: Added order item #" + itemCount + " - Product ID: " + item.getProductId() + ", Quantity: " + item.getQuantityKg() + " kg");
                            }
                            int[] itemResults = itemStmt.executeBatch();
                            int successfulItems = 0;
                            for (int result : itemResults) {
                                if (result > 0) successfulItems++;
                            }
                            System.out.println("DEBUG OrderDAO: Inserted " + successfulItems + " out of " + itemCount + " order items");
                            
                            if (successfulItems != itemCount) {
                                System.err.println("ERROR OrderDAO: Not all order items were inserted! Expected: " + itemCount + ", Inserted: " + successfulItems);
                                conn.rollback();
                                return null;
                            }
                        }
                        
                        System.out.println("DEBUG OrderDAO: Committing transaction for order ID: " + orderId);
                        conn.commit();
                        System.out.println("DEBUG OrderDAO: Order #" + orderId + " successfully saved to database");
                        
                        // Verify the order was saved correctly
                        Order verifiedOrder = getOrderById(orderId);
                        if (verifiedOrder != null) {
                            System.out.println("DEBUG OrderDAO: Verification successful - Order #" + orderId + " found in database with " + 
                                             verifiedOrder.getItems().size() + " items");
                        } else {
                            System.err.println("ERROR OrderDAO: Verification failed - Order #" + orderId + " not found after creation!");
                        }
                        
                        return order;
                    } else {
                        System.err.println("ERROR OrderDAO: Failed to get generated order ID");
                        conn.rollback();
                    }
                } else {
                    System.err.println("ERROR OrderDAO: Order INSERT did not affect any rows");
                    conn.rollback();
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR OrderDAO: SQLException while creating order: " + e.getMessage());
            System.err.println("ERROR OrderDAO: SQL State: " + e.getSQLState());
            System.err.println("ERROR OrderDAO: Error Code: " + e.getErrorCode());
            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("DEBUG OrderDAO: Transaction rolled back due to error");
                }
            } catch (SQLException ex) {
                System.err.println("ERROR OrderDAO: Failed to rollback transaction: " + ex.getMessage());
                ex.printStackTrace();
            }
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR OrderDAO: Unexpected exception while creating order: " + e.getMessage());
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
                System.err.println("ERROR OrderDAO: Failed to reset auto-commit: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("DEBUG OrderDAO: ========== ORDER CREATION FAILED ==========");
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
            
            System.out.println("DEBUG OrderDAO: Executing query: " + sql);
            int count = 0;
            while (rs.next()) {
                count++;
                int orderId = rs.getInt("id");
                String status = rs.getString("status");
                System.out.println("DEBUG OrderDAO: Found order #" + orderId + " with status: " + status);
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
            System.out.println("DEBUG OrderDAO: Total orders found: " + count + ", returning list size: " + orders.size());
        } catch (SQLException e) {
            System.err.println("ERROR OrderDAO: Exception while getting available orders: " + e.getMessage());
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
            
            System.out.println("DEBUG OrderDAO: Executing query: " + sql + " with carrierId=" + carrierId + ", status=" + (status != null ? status.name() : "null"));
            ResultSet rs = stmt.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                int orderId = rs.getInt("id");
                String orderStatus = rs.getString("status");
                int orderCarrierId = rs.getInt("carrier_id");
                System.out.println("DEBUG OrderDAO: Found order #" + orderId + " with status: " + orderStatus + ", carrier_id: " + orderCarrierId);
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
            System.out.println("DEBUG OrderDAO: Total orders found: " + count + ", returning list size: " + orders.size());
        } catch (SQLException e) {
            System.err.println("ERROR OrderDAO: Exception while getting orders by carrier: " + e.getMessage());
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
        System.out.println("DEBUG OrderDAO: ========== GETTING ALL ORDERS ==========");
        
        // First, verify total count in database
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) as total FROM orders")) {
            ResultSet countRs = countStmt.executeQuery();
            if (countRs.next()) {
                int totalInDb = countRs.getInt("total");
                System.out.println("DEBUG OrderDAO: Total orders in database: " + totalInDb);
            }
        } catch (SQLException e) {
            System.err.println("ERROR OrderDAO: Failed to count orders: " + e.getMessage());
        }
        
        // Query to get ALL orders from the database - NO FILTERS
        // Use explicit ORDER BY id DESC to ensure we get all orders
        String sql = "SELECT * FROM orders ORDER BY id DESC";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbAdapter.getConnection();
            System.out.println("DEBUG OrderDAO: Got connection: " + (conn != null ? "valid" : "NULL"));
            System.out.println("DEBUG OrderDAO: Connection closed: " + (conn != null ? conn.isClosed() : "N/A"));
            System.out.println("DEBUG OrderDAO: Connection auto-commit: " + (conn != null ? conn.getAutoCommit() : "N/A"));
            
            // Use regular Statement instead of PreparedStatement since we have no parameters
            // This ensures we get all rows without any fetch size limitations
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            System.out.println("DEBUG OrderDAO: Created statement: " + (stmt != null ? "valid" : "NULL"));
            
            System.out.println("DEBUG OrderDAO: Executing getAllOrders query (NO FILTERS - ALL ORDERS): " + sql);
            rs = stmt.executeQuery(sql);
            System.out.println("DEBUG OrderDAO: Got ResultSet: " + (rs != null ? "valid" : "NULL"));
            
            // Check ResultSet type
            if (rs != null) {
                System.out.println("DEBUG OrderDAO: ResultSet type: " + rs.getType());
                System.out.println("DEBUG OrderDAO: ResultSet concurrency: " + rs.getConcurrency());
            }
            
            int count = 0;
            int processedCount = 0;
            int failedCount = 0;
            Map<Order.OrderStatus, Integer> statusCounts = new java.util.HashMap<>();
            List<Integer> orderIdsFound = new ArrayList<>();
            
            System.out.println("DEBUG OrderDAO: Starting to iterate ResultSet...");
            while (rs.next()) {
                count++; // Count all rows found
                int orderId = rs.getInt("id");
                orderIdsFound.add(orderId);
                
                System.out.println("DEBUG OrderDAO: >>> Iterating row #" + count + " - Order ID: " + orderId);
                
                try {
                    String status = rs.getString("status");
                    Order.OrderStatus orderStatus = null;
                    
                    // Safely parse status
                    try {
                        orderStatus = Order.OrderStatus.valueOf(status);
                        statusCounts.put(orderStatus, statusCounts.getOrDefault(orderStatus, 0) + 1);
                    } catch (IllegalArgumentException e) {
                        System.err.println("ERROR OrderDAO: Invalid status '" + status + "' for order #" + orderId + " - Skipping status count");
                    }
                    
                    System.out.println("DEBUG OrderDAO: Processing order #" + orderId + " - Status: " + status + 
                                     ", Customer ID: " + rs.getInt("customer_id") + 
                                     ", Total: " + rs.getBigDecimal("total_after_tax"));
                    
                    // Map ResultSet to Order object
                    Order order = mapResultSetToOrder(rs);
                    
                    // Get order items
                    List<OrderItem> items = getOrderItems(order.getId());
                    order.setItems(items);
                    
                    System.out.println("DEBUG OrderDAO: Order #" + orderId + " loaded with " + items.size() + " items");
                    
                    if (items.isEmpty()) {
                        System.err.println("WARNING OrderDAO: Order #" + orderId + " has no items! (Still adding to list)");
                    }
                    
                    orders.add(order);
                    processedCount++;
                    System.out.println("DEBUG OrderDAO: Successfully processed order #" + orderId + " (Total processed: " + processedCount + ")");
                    
                } catch (Exception e) {
                    failedCount++;
                    System.err.println("ERROR OrderDAO: Failed to process order #" + orderId + ": " + e.getMessage());
                    System.err.println("ERROR OrderDAO: Exception type: " + e.getClass().getSimpleName());
                    e.printStackTrace();
                    // Continue processing other orders
                }
            }
            
            System.out.println("DEBUG OrderDAO: ========== ORDER RETRIEVAL SUMMARY ==========");
            System.out.println("DEBUG OrderDAO: Total rows in ResultSet: " + count);
            System.out.println("DEBUG OrderDAO: Successfully processed: " + processedCount);
            System.out.println("DEBUG OrderDAO: Failed to process: " + failedCount);
            System.out.println("DEBUG OrderDAO: Orders by status: " + statusCounts);
            System.out.println("DEBUG OrderDAO: Order IDs found: " + orderIdsFound);
            System.out.println("DEBUG OrderDAO: Returning list with " + orders.size() + " orders");
            if (processedCount != count) {
                System.err.println("WARNING OrderDAO: Not all orders were processed! Expected: " + count + ", Processed: " + processedCount + ", Failed: " + failedCount);
            }
            if (count == 0) {
                System.err.println("ERROR OrderDAO: NO ORDERS FOUND IN DATABASE! Check if orders table has data.");
            }
            System.out.println("DEBUG OrderDAO: ===============================================");
            
        } catch (SQLException e) {
            System.err.println("ERROR OrderDAO: SQLException while getting all orders: " + e.getMessage());
            System.err.println("ERROR OrderDAO: SQL State: " + e.getSQLState());
            System.err.println("ERROR OrderDAO: Error Code: " + e.getErrorCode());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR OrderDAO: Unexpected exception while getting all orders: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close ResultSet and Statement explicitly (Connection is managed by DatabaseAdapter)
            try {
                if (rs != null) {
                    rs.close();
                    System.out.println("DEBUG OrderDAO: Closed ResultSet");
                }
                if (stmt != null) {
                    stmt.close();
                    System.out.println("DEBUG OrderDAO: Closed Statement");
                }
            } catch (SQLException e) {
                System.err.println("ERROR OrderDAO: Error closing ResultSet/Statement: " + e.getMessage());
            }
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
            
            System.out.println("DEBUG OrderDAO: Marking order #" + orderId + " as DELIVERED at " + deliveredTime);
            stmt.setTimestamp(1, Timestamp.valueOf(deliveredTime));
            stmt.setInt(2, orderId);
            
            int rowsAffected = stmt.executeUpdate();
            System.out.println("DEBUG OrderDAO: markOrderDelivered affected " + rowsAffected + " row(s)");
            
            // Verify the update
            if (rowsAffected > 0) {
                String verifySql = "SELECT status FROM orders WHERE id = ?";
                try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                    verifyStmt.setInt(1, orderId);
                    ResultSet rs = verifyStmt.executeQuery();
                    if (rs.next()) {
                        String status = rs.getString("status");
                        System.out.println("DEBUG OrderDAO: Verified order #" + orderId + " status is now: " + status);
                    }
                }
            }
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("ERROR OrderDAO: Exception while marking order as delivered: " + e.getMessage());
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
        String sql = "SELECT * FROM order_items WHERE order_id = ? ORDER BY id ASC";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            int itemCount = 0;
            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getInt("id"));
                item.setOrderId(rs.getInt("order_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setQuantityKg(rs.getBigDecimal("kg"));
                item.setUnitPriceApplied(rs.getBigDecimal("unit_price_applied"));
                item.setLineTotal(rs.getBigDecimal("line_total"));
                items.add(item);
                itemCount++;
            }
            
            if (itemCount == 0) {
                System.err.println("WARNING OrderDAO: No items found for order ID: " + orderId);
            }
        } catch (SQLException e) {
            System.err.println("ERROR OrderDAO: Failed to get order items for order ID " + orderId + ": " + e.getMessage());
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

