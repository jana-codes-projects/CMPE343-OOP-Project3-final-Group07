package com.example.demo.services;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.models.CartItem;
import com.example.demo.models.User;
import java.util.List;
import java.time.LocalDateTime;

public class OrderService {
    private OrderDAO orderDAO = new OrderDAO();
    private ProductDAO productDAO = new ProductDAO();

    /**
     * Processes the entire checkout.
     * Validates stock, updates DB, generates invoice, and saves order.
     */
    public boolean processCheckout(User user, List<CartItem> cart, double finalTotal) {
        // 1. Mandatory Stock Validation
        for (CartItem item : cart) {
            if (item.getProduct().getStock() < item.getQuantity()) {
                return false;
            }
        }

        // 2. Update Database Stocks
        for (CartItem item : cart) {
            double newStock = item.getProduct().getStock() - item.getQuantity();
            productDAO.updateStock(item.getProduct().getId(), newStock);
        }

        // 3. Prepare Summary String
        StringBuilder summary = new StringBuilder();
        for (CartItem item : cart) {
            summary.append(item.getProduct().getName())
                    .append(" (").append(item.getQuantity()).append("kg), ");
        }

        // 4. Generate Invoice (PDF data as BLOB)
        byte[] invoiceData = generateInvoice(user, cart, finalTotal);

        // 5. Final Save to Database
        return orderDAO.saveOrder(user.getId(), summary.toString(), finalTotal, invoiceData);
    }

    /**
     * Generates a text-based invoice converted to bytes for BLOB storage.
     */
    private byte[] generateInvoice(User user, List<CartItem> cart, double total) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- GREEN GROCER INVOICE ---\n");
        sb.append("Customer: ").append(user.getUsername()).append("\n");
        sb.append("Address: ").append(user.getAddress()).append("\n");
        sb.append("Date: ").append(LocalDateTime.now()).append("\n");
        sb.append("---------------------------\n");
        for (CartItem item : cart) {
            sb.append(item.getProduct().getName())
                    .append(" x ").append(item.getQuantity())
                    .append(" = ").append(item.getTotalPrice()).append(" TL\n");
        }
        sb.append("---------------------------\n");
        sb.append("TOTAL COST: ").append(total).append(" TL\n");
        return sb.toString().getBytes();
    }

    public double calculateVAT(double subtotal) {
        return subtotal * 0.18;
    }

    public double getLoyaltyDiscount(User user) {
        return user.getLoyaltyPoints() * 0.01;
    }

    public double getCouponDiscount(String code, double currentTotal) {
        if ("GROUP07".equalsIgnoreCase(code)) {
            return currentTotal * 0.10;
        }
        return 0.0;
    }
}