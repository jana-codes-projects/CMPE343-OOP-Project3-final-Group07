package com.cmpe343.model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {

    public enum OrderStatus {
        CREATED, ASSIGNED, DELIVERED, CANCELLED
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
