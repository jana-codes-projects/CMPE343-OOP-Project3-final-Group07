package com.cmpe343.model;

import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;

/**
 * Unit tests for the Order model class.
 * 
 * @author Group07
 */
public class OrderTest {

    @Test
    public void testOrderCreation() {
        LocalDateTime orderTime = LocalDateTime.now();
        LocalDateTime requestedTime = orderTime.plusHours(2);

        Order order = new Order(1, 100, 200, Order.OrderStatus.CREATED,
                orderTime, requestedTime, null, 80.0, 16.0, 96.0);

        assertEquals(1, order.getId());
        assertEquals(100, order.getCustomerId());
        assertEquals(Integer.valueOf(200), order.getCarrierId());
        assertEquals(Order.OrderStatus.CREATED, order.getStatus());
        assertEquals(orderTime, order.getOrderTime());
        assertEquals(requestedTime, order.getRequestedDeliveryTime());
        assertNull(order.getDeliveredTime());
        assertEquals(80.0, order.getTotalBeforeTax(), 0.001);
        assertEquals(16.0, order.getVat(), 0.001);
        assertEquals(96.0, order.getTotalAfterTax(), 0.001);
    }

    @Test
    public void testOrderWithNullCarrier() {
        Order order = new Order(1, 100, null, Order.OrderStatus.CREATED,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2), null,
                100.0, 20.0, 120.0);

        assertNull(order.getCarrierId());
    }

    @Test
    public void testOrderStatusTransitions() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.CREATED);
        assertEquals(Order.OrderStatus.CREATED, order.getStatus());

        order.setStatus(Order.OrderStatus.ASSIGNED);
        assertEquals(Order.OrderStatus.ASSIGNED, order.getStatus());

        order.setStatus(Order.OrderStatus.DELIVERED);
        assertEquals(Order.OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    public void testOrderSetters() {
        Order order = new Order();

        order.setId(5);
        assertEquals(5, order.getId());

        order.setCustomerId(10);
        assertEquals(10, order.getCustomerId());

        order.setCarrierId(15);
        assertEquals(Integer.valueOf(15), order.getCarrierId());

        order.setTotalBeforeTax(100.0);
        assertEquals(100.0, order.getTotalBeforeTax(), 0.001);

        order.setVat(20.0);
        assertEquals(20.0, order.getVat(), 0.001);

        order.setTotalAfterTax(120.0);
        assertEquals(120.0, order.getTotalAfterTax(), 0.001);
    }

    @Test
    public void testOrderStatusEnum() {
        // Test all enum values exist
        assertEquals(4, Order.OrderStatus.values().length);
        assertEquals(Order.OrderStatus.CREATED, Order.OrderStatus.valueOf("CREATED"));
        assertEquals(Order.OrderStatus.ASSIGNED, Order.OrderStatus.valueOf("ASSIGNED"));
        assertEquals(Order.OrderStatus.DELIVERED, Order.OrderStatus.valueOf("DELIVERED"));
        assertEquals(Order.OrderStatus.CANCELLED, Order.OrderStatus.valueOf("CANCELLED"));
    }
}
