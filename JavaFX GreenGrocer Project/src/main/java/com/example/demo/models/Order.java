package com.example.demo.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an order in the system.
 * 
 * @author Group07
 * @version 1.0
 */
public class Order {
    private int id;
    private int customerId;
    private Integer carrierId;
    private OrderStatus status;
    private LocalDateTime orderTime;
    private LocalDateTime requestedDeliveryTime;
    private LocalDateTime deliveredTime;
    private BigDecimal totalBeforeTax;
    private BigDecimal vat;
    private BigDecimal totalAfterTax;
    private Integer couponId;
    private BigDecimal loyaltyDiscount;
    private List<OrderItem> items;
    private Customer customer;
    private Carrier carrier;
    private Coupon coupon;

    /**
     * Enumeration of order statuses.
     */
    public enum OrderStatus {
        CREATED, ASSIGNED, DELIVERED, CANCELLED
    }

    /**
     * Default constructor.
     */
    public Order() {
        this.items = new ArrayList<>();
        this.status = OrderStatus.CREATED;
    }

    /**
     * Constructor with basic parameters.
     * 
     * @param customerId the customer ID
     * @param requestedDeliveryTime the requested delivery time
     */
    public Order(int customerId, LocalDateTime requestedDeliveryTime) {
        this();
        this.customerId = customerId;
        this.requestedDeliveryTime = requestedDeliveryTime;
        this.orderTime = LocalDateTime.now();
        this.totalBeforeTax = BigDecimal.ZERO;
        this.vat = BigDecimal.ZERO;
        this.totalAfterTax = BigDecimal.ZERO;
        this.loyaltyDiscount = BigDecimal.ZERO;
    }

    /**
     * Adds an item to the order.
     * 
     * @param item the order item to add
     */
    public void addItem(OrderItem item) {
        items.add(item);
    }

    /**
     * Gets the VAT rate (20%).
     * 
     * @return the VAT rate as a decimal
     */
    public static BigDecimal getVatRate() {
        return new BigDecimal("0.20");
    }

    /**
     * Calculates the total before tax from all items.
     */
    public void calculateTotalBeforeTax() {
        totalBeforeTax = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the VAT amount.
     */
    public void calculateVat() {
        vat = totalBeforeTax.multiply(getVatRate());
    }

    /**
     * Calculates the total after tax, applying discounts.
     */
    public void calculateTotalAfterTax() {
        BigDecimal discount = loyaltyDiscount;
        if (coupon != null) {
            discount = discount.add(coupon.calculateDiscount(totalBeforeTax.add(vat)));
        }
        totalAfterTax = totalBeforeTax.add(vat).subtract(discount);
        if (totalAfterTax.compareTo(BigDecimal.ZERO) < 0) {
            totalAfterTax = BigDecimal.ZERO;
        }
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public Integer getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Integer carrierId) {
        this.carrierId = carrierId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public LocalDateTime getRequestedDeliveryTime() {
        return requestedDeliveryTime;
    }

    public void setRequestedDeliveryTime(LocalDateTime requestedDeliveryTime) {
        this.requestedDeliveryTime = requestedDeliveryTime;
    }

    public LocalDateTime getDeliveredTime() {
        return deliveredTime;
    }

    public void setDeliveredTime(LocalDateTime deliveredTime) {
        this.deliveredTime = deliveredTime;
    }

    public BigDecimal getTotalBeforeTax() {
        return totalBeforeTax;
    }

    public void setTotalBeforeTax(BigDecimal totalBeforeTax) {
        this.totalBeforeTax = totalBeforeTax;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public void setVat(BigDecimal vat) {
        this.vat = vat;
    }

    public BigDecimal getTotalAfterTax() {
        return totalAfterTax;
    }

    public void setTotalAfterTax(BigDecimal totalAfterTax) {
        this.totalAfterTax = totalAfterTax;
    }

    public Integer getCouponId() {
        return couponId;
    }

    public void setCouponId(Integer couponId) {
        this.couponId = couponId;
    }

    public BigDecimal getLoyaltyDiscount() {
        return loyaltyDiscount;
    }

    public void setLoyaltyDiscount(BigDecimal loyaltyDiscount) {
        this.loyaltyDiscount = loyaltyDiscount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }
}

