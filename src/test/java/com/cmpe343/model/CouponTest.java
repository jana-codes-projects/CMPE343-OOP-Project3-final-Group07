package com.cmpe343.model;

import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;

/**
 * Unit tests for the Coupon model class.
 * 
 * @author Group07
 */
public class CouponTest {

    @Test
    public void testAmountCouponDiscount() {
        Coupon coupon = new Coupon(1, "FLAT50", Coupon.CouponKind.AMOUNT, 50.0, 100.0, true, null);

        // Cart meets minimum, get full discount
        assertEquals(50.0, coupon.calculateDiscount(150.0), 0.001);

        // Cart equals minimum, get full discount
        assertEquals(50.0, coupon.calculateDiscount(100.0), 0.001);
    }

    @Test
    public void testAmountCouponMinCartNotMet() {
        Coupon coupon = new Coupon(1, "FLAT50", Coupon.CouponKind.AMOUNT, 50.0, 100.0, true, null);

        // Cart below minimum, no discount
        assertEquals(0.0, coupon.calculateDiscount(80.0), 0.001);
    }

    @Test
    public void testPercentCouponDiscount() {
        Coupon coupon = new Coupon(2, "OFF10", Coupon.CouponKind.PERCENT, 10.0, 50.0, true, null);

        // 10% of 200 = 20
        assertEquals(20.0, coupon.calculateDiscount(200.0), 0.001);

        // 10% of 100 = 10
        assertEquals(10.0, coupon.calculateDiscount(100.0), 0.001);
    }

    @Test
    public void testPercentCouponMinCartNotMet() {
        Coupon coupon = new Coupon(2, "OFF10", Coupon.CouponKind.PERCENT, 10.0, 50.0, true, null);

        // Cart below minimum, no discount
        assertEquals(0.0, coupon.calculateDiscount(40.0), 0.001);
    }

    @Test
    public void testCouponGetters() {
        LocalDateTime expiry = LocalDateTime.of(2026, 12, 31, 23, 59);
        Coupon coupon = new Coupon(3, "NEWYEAR", Coupon.CouponKind.PERCENT, 15.0, 75.0, true, expiry);

        assertEquals(3, coupon.getId());
        assertEquals("NEWYEAR", coupon.getCode());
        assertEquals(Coupon.CouponKind.PERCENT, coupon.getKind());
        assertEquals(15.0, coupon.getValue(), 0.001);
        assertEquals(75.0, coupon.getMinCart(), 0.001);
        assertTrue(coupon.isActive());
        assertEquals(expiry, coupon.getExpiresAt());
    }

    @Test
    public void testCouponKindEnum() {
        assertEquals(2, Coupon.CouponKind.values().length);
        assertEquals(Coupon.CouponKind.AMOUNT, Coupon.CouponKind.valueOf("AMOUNT"));
        assertEquals(Coupon.CouponKind.PERCENT, Coupon.CouponKind.valueOf("PERCENT"));
    }
}
