package com.cmpe343.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Product model class.
 * 
 * @author Group07
 */
public class ProductTest {

    @Test
    public void testProductCreationWithEnumType() {
        Product product = new Product(1, "Apple", Product.ProductType.FRUIT, 10.50, 100.0, 20.0);

        assertEquals(1, product.getId());
        assertEquals("Apple", product.getName());
        assertEquals(Product.ProductType.FRUIT, product.getType());
        assertEquals(10.50, product.getPrice(), 0.001);
        assertEquals(100.0, product.getStockKg(), 0.001);
        assertEquals(20.0, product.getThresholdKg(), 0.001);
    }

    @Test
    public void testProductCreationWithStringType() {
        Product product = new Product(2, "Carrot", "VEG", 8.00, 50.0, 10.0);

        assertEquals(2, product.getId());
        assertEquals("Carrot", product.getName());
        assertEquals(Product.ProductType.VEGETABLE, product.getType());
        assertEquals(8.00, product.getPrice(), 0.001);
        assertEquals(50.0, product.getStockKg(), 0.001);
    }

    @Test
    public void testProductCreationWithFruitString() {
        Product product = new Product(3, "Banana", "FRUIT", 12.00, 30.0, 5.0);

        assertEquals(Product.ProductType.FRUIT, product.getType());
    }

    @Test
    public void testProductTypeAsDbString() {
        Product fruit = new Product(1, "Apple", Product.ProductType.FRUIT, 10.0, 100.0, 20.0);
        Product veg = new Product(2, "Carrot", Product.ProductType.VEGETABLE, 8.0, 50.0, 10.0);

        assertEquals("FRUIT", fruit.getTypeAsDbString());
        assertEquals("VEG", veg.getTypeAsDbString());
    }

    @Test
    public void testIsLowStock() {
        // Stock = 100, Threshold = 20 -> Not low stock
        Product product1 = new Product(1, "Apple", Product.ProductType.FRUIT, 10.0, 100.0, 20.0);
        assertFalse(product1.isLowStock());

        // Stock = 15, Threshold = 20 -> Low stock
        Product product2 = new Product(2, "Banana", Product.ProductType.FRUIT, 12.0, 15.0, 20.0);
        assertTrue(product2.isLowStock());

        // Stock exactly at threshold -> Low stock
        Product product3 = new Product(3, "Orange", Product.ProductType.FRUIT, 11.0, 20.0, 20.0);
        assertTrue(product3.isLowStock());
    }

    @Test
    public void testIsActiveDefault() {
        Product product = new Product(1, "Apple", Product.ProductType.FRUIT, 10.0, 100.0, 20.0);
        assertTrue(product.isActive());
    }
}
