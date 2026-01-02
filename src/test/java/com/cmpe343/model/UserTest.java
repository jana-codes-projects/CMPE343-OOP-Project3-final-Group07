package com.cmpe343.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the User model class.
 * 
 * @author Group07
 */
public class UserTest {

    @Test
    public void testUserCreationWithMinimalInfo() {
        User user = new User(1, "testuser", "customer");

        assertEquals(1, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("customer", user.getRole());
        assertNull(user.getPhone());
        assertNull(user.getAddress());
        assertTrue(user.isActive());
        assertEquals(0.0, user.getBalance(), 0.001);
    }

    @Test
    public void testUserCreationWithCompleteInfo() {
        User user = new User(2, "fulluser", "carrier", "555-1234", "123 Main St", true, 100.50);

        assertEquals(2, user.getId());
        assertEquals("fulluser", user.getUsername());
        assertEquals("carrier", user.getRole());
        assertEquals("555-1234", user.getPhone());
        assertEquals("123 Main St", user.getAddress());
        assertTrue(user.isActive());
        assertEquals(100.50, user.getBalance(), 0.001);
    }

    @Test
    public void testSetActive() {
        User user = new User(1, "testuser", "customer");
        assertTrue(user.isActive());

        user.setActive(false);
        assertFalse(user.isActive());

        user.setActive(true);
        assertTrue(user.isActive());
    }

    @Test
    public void testSetBalance() {
        User user = new User(1, "testuser", "customer");
        assertEquals(0.0, user.getBalance(), 0.001);

        user.setBalance(250.75);
        assertEquals(250.75, user.getBalance(), 0.001);
    }

    @Test
    public void testSetPhone() {
        User user = new User(1, "testuser", "customer");
        assertNull(user.getPhone());

        user.setPhone("555-9999");
        assertEquals("555-9999", user.getPhone());
    }

    @Test
    public void testSetAddress() {
        User user = new User(1, "testuser", "customer");
        assertNull(user.getAddress());

        user.setAddress("456 Oak Ave");
        assertEquals("456 Oak Ave", user.getAddress());
    }
}
