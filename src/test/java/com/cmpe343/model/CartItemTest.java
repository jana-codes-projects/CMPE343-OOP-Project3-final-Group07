package com.cmpe343.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the CartItem model class.
 * 
 * @author Group07
 */
public class CartItemTest {

    @Test
    public void testCartItemCreation() {
        Product product = new Product(1, "Apple", Product.ProductType.FRUIT, 10.0, 100.0, 20.0);
        CartItem item = new CartItem(product, 2.5);

        assertEquals(product, item.getProduct());
        assertEquals(2.5, item.getQuantityKg(), 0.001);
        // Should use product price for current cart items
        assertEquals(10.0, item.getUnitPrice(), 0.001);
        assertEquals(25.0, item.getLineTotal(), 0.001);
    }

    @Test
    public void testCartItemWithHistoricalPricing() {
        Product product = new Product(1, "Apple", Product.ProductType.FRUIT, 10.0, 100.0, 20.0);
        // Historical price was 12.0 (doubled due to threshold)
        CartItem item = new CartItem(product, 2.0, 12.0, 24.0);

        // Should use historical price, not current product price
        assertEquals(12.0, item.getUnitPrice(), 0.001);
        assertEquals(24.0, item.getLineTotal(), 0.001);
    }

    @Test
    public void testCartItemSetQuantity() {
        Product product = new Product(1, "Banana", Product.ProductType.FRUIT, 8.0, 50.0, 10.0);
        CartItem item = new CartItem(product, 1.0);

        assertEquals(1.0, item.getQuantityKg(), 0.001);

        item.setQuantityKg(3.5);
        assertEquals(3.5, item.getQuantityKg(), 0.001);

        // Line total should update with new quantity
        assertEquals(28.0, item.getLineTotal(), 0.001);
    }

    @Test
    public void testCartItemLineTotalCalculation() {
        Product product = new Product(1, "Carrot", Product.ProductType.VEGETABLE, 5.0, 80.0, 15.0);

        CartItem item1 = new CartItem(product, 1.0);
        assertEquals(5.0, item1.getLineTotal(), 0.001);

        CartItem item2 = new CartItem(product, 4.0);
        assertEquals(20.0, item2.getLineTotal(), 0.001);

        CartItem item3 = new CartItem(product, 0.5);
        assertEquals(2.5, item3.getLineTotal(), 0.001);
    }
}
