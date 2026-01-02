package com.cmpe343.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a customer order in the GreenGrocer system.
 * An order contains cart items, pricing information, and delivery details.
 * Orders progress through different statuses: CREATED → ASSIGNED →
 * DELIVERED/CANCELLED.
 * 
 * @author Group07
 * @version 1.0
 */
public class Order {

    /**
     * Represents the possible states of an order in the system.
     */
    public enum OrderStatus {
        /** Order has been created but not yet assigned to a carrier */
        CREATED,
        /** Order has been assigned to a carrier for delivery */
        ASSIGNED,
        /** Order has been successfully delivered to the customer */
        DELIVERED,
        /** Order has been cancelled by the customer */
        CANCELLED
    }

    private int id;
    private int customerId;
    private Integer carrierId;
    private OrderStatus status;
    private LocalDateTime orderTime;
    private LocalDateTime requestedDeliveryTime;
    private LocalDateTime deliveredTime;
    private double totalBeforeTax;
    private double vat;
    private double totalAfterTax;
    private List<CartItem> items;

    /**
     * Default constructor for Order.
     */
    public Order() {
    }

    /**
     * Creates a new Order with complete information.
     * 
     * @param id                    The unique identifier for the order
     * @param customerId            The ID of the customer who placed the order
     * @param carrierId             The ID of the assigned carrier (null if not
     *                              assigned)
     * @param status                The current status of the order
     * @param orderTime             The timestamp when the order was placed
     * @param requestedDeliveryTime The customer's requested delivery time
     * @param deliveredTime         The actual delivery time (null if not delivered)
     * @param totalBeforeTax        The subtotal before VAT
     * @param vat                   The VAT amount (20%)
     * @param totalAfterTax         The final total after VAT
     */
    public Order(int id, int customerId, Integer carrierId, OrderStatus status,
            LocalDateTime orderTime, LocalDateTime requestedDeliveryTime, LocalDateTime deliveredTime,
            double totalBeforeTax, double vat, double totalAfterTax) {
        this.id = id;
        this.customerId = customerId;
        this.carrierId = carrierId;
        this.status = status;
        this.orderTime = orderTime;
        this.requestedDeliveryTime = requestedDeliveryTime;
        this.deliveredTime = deliveredTime;
        this.totalBeforeTax = totalBeforeTax;
        this.vat = vat;
        this.totalAfterTax = totalAfterTax;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public void setCarrierId(Integer carrierId) {
        this.carrierId = carrierId;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public void setRequestedDeliveryTime(LocalDateTime requestedDeliveryTime) {
        this.requestedDeliveryTime = requestedDeliveryTime;
    }

    public void setDeliveredTime(LocalDateTime deliveredTime) {
        this.deliveredTime = deliveredTime;
    }

    public void setTotalBeforeTax(double totalBeforeTax) {
        this.totalBeforeTax = totalBeforeTax;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public void setTotalAfterTax(double totalAfterTax) {
        this.totalAfterTax = totalAfterTax;
    }

    public int getId() {
        return id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public Integer getCarrierId() {
        return carrierId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public LocalDateTime getRequestedDeliveryTime() {
        return requestedDeliveryTime;
    }

    public LocalDateTime getDeliveredTime() {
        return deliveredTime;
    }

    public double getTotalBeforeTax() {
        return totalBeforeTax;
    }

    public double getVat() {
        return vat;
    }

    public double getTotalAfterTax() {
        return totalAfterTax;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
