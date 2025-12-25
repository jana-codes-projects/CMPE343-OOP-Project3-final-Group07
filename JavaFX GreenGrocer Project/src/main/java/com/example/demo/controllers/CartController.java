package com.example.demo.controllers;

import com.example.demo.dao.*;
import com.example.demo.models.*;
import com.example.demo.services.InvoiceService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Shopping Cart UI.
 * Handles cart management, checkout, and order creation.
 * 
 * @author Group07
 * @version 1.0
 */
public class CartController extends BaseController {
    @FXML private VBox cartItemsContainer;
    @FXML private Label subtotalLabel;
    @FXML private Label vatLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;
    @FXML private TextField couponField;
    @FXML private DatePicker deliveryDatePicker;
    @FXML private TextField deliveryTimeField;
    @FXML private Button checkoutButton;
    
    private List<CartItem> cart;
    private int customerId;
    private Coupon appliedCoupon;
    private BigDecimal loyaltyDiscount;
    
    private OrderDAO orderDAO;
    private CouponDAO couponDAO;
    private UserDAO userDAO;
    private InvoiceService invoiceService;
    private InvoiceDAO invoiceDAO;
    
    @FXML
    public void initialize() {
        orderDAO = new OrderDAO();
        couponDAO = new CouponDAO();
        userDAO = new UserDAO();
        invoiceService = new InvoiceService();
        invoiceDAO = new InvoiceDAO();
        
        deliveryDatePicker.setValue(LocalDate.now().plusDays(1));
        deliveryTimeField.setText("10:00");
    }
    
    public void setCart(List<CartItem> cart) {
        this.cart = cart;
        updateCartDisplay();
    }
    
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
        // Calculate loyalty discount
        Customer customer = userDAO.getCustomerById(customerId);
        if (customer != null) {
            BigDecimal cartTotal = calculateSubtotal();
            loyaltyDiscount = BigDecimal.valueOf(customer.calculateLoyaltyDiscount(cartTotal.doubleValue()));
        } else {
            loyaltyDiscount = BigDecimal.ZERO;
        }
        updateTotals();
    }
    
    private void updateCartDisplay() {
        cartItemsContainer.getChildren().clear();
        
        for (CartItem item : cart) {
            HBox itemRow = new HBox(10);
            itemRow.setStyle("-fx-padding: 5;");
            
            Label nameLabel = new Label(item.getProduct().getName());
            nameLabel.setPrefWidth(200);
            
            Label quantityLabel = new Label(item.getQuantityKg() + " kg");
            quantityLabel.setPrefWidth(100);
            
            Label priceLabel = new Label(formatPrice(item.getUnitPrice()));
            priceLabel.setPrefWidth(100);
            
            Label totalLabel = new Label(formatPrice(item.getLineTotal()));
            totalLabel.setPrefWidth(100);
            
            Button removeButton = new Button("Remove");
            removeButton.setOnAction(e -> handleRemoveItem(item));
            
            itemRow.getChildren().addAll(nameLabel, quantityLabel, priceLabel, totalLabel, removeButton);
            cartItemsContainer.getChildren().add(itemRow);
        }
        
        updateTotals();
    }
    
    private void updateTotals() {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal vat = subtotal.multiply(Order.getVatRate());
        BigDecimal discount = loyaltyDiscount;
        
        if (appliedCoupon != null) {
            discount = discount.add(appliedCoupon.calculateDiscount(subtotal.add(vat)));
        }
        
        BigDecimal total = subtotal.add(vat).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        
        subtotalLabel.setText("Subtotal: " + formatPrice(subtotal));
        vatLabel.setText("VAT (20%): " + formatPrice(vat));
        discountLabel.setText("Discount: " + formatPrice(discount));
        discountLabel.setVisible(discount.compareTo(BigDecimal.ZERO) > 0);
        totalLabel.setText("Total: " + formatPrice(total));
    }
    
    private BigDecimal calculateSubtotal() {
        return cart.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @FXML
    private void handleRemoveItem(CartItem item) {
        cart.remove(item);
        updateCartDisplay();
    }
    
    @FXML
    private void handleApplyCoupon() {
        String code = couponField.getText().trim();
        if (code.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Code", "Please enter a coupon code.");
            return;
        }
        
        Coupon coupon = couponDAO.getCouponByCode(code);
        if (coupon != null) {
            BigDecimal cartTotal = calculateSubtotal().add(calculateSubtotal().multiply(Order.getVatRate()));
            if (coupon.isValidForCart(cartTotal)) {
                appliedCoupon = coupon;
                updateTotals();
                showAlert(Alert.AlertType.INFORMATION, "Coupon Applied", "Coupon " + code + " applied successfully.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid Coupon", 
                        "Coupon does not meet minimum cart value requirement.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Invalid Coupon", "Coupon code not found or expired.");
        }
    }
    
    @FXML
    private void handleCheckout() {
        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Cart", "Your cart is empty.");
            return;
        }
        
        // Validate delivery date (within 48 hours)
        LocalDate deliveryDate = deliveryDatePicker.getValue();
        if (deliveryDate == null) {
            showAlert(Alert.AlertType.ERROR, "Invalid Date", "Please select a delivery date.");
            return;
        }
        
        LocalDateTime deliveryDateTime;
        try {
            LocalTime time = LocalTime.parse(deliveryTimeField.getText(), DateTimeFormatter.ofPattern("HH:mm"));
            deliveryDateTime = LocalDateTime.of(deliveryDate, time);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Time", "Please enter time in HH:mm format.");
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (deliveryDateTime.isBefore(now) || deliveryDateTime.isAfter(now.plusHours(48))) {
            showAlert(Alert.AlertType.ERROR, "Invalid Date", 
                    "Delivery date must be within 48 hours from now.");
            return;
        }
        
        // Check minimum cart value (e.g., 50 TL)
        BigDecimal total = calculateSubtotal().add(calculateSubtotal().multiply(Order.getVatRate()));
        if (total.compareTo(new BigDecimal("50")) < 0) {
            showAlert(Alert.AlertType.WARNING, "Minimum Cart Value", 
                    "Minimum cart value is 50 TL. Current total: " + formatPrice(total));
            return;
        }
        
        // Create order
        try {
            Order order = new Order(customerId, deliveryDateTime);
            List<OrderItem> orderItems = new java.util.ArrayList<>();
            for (CartItem item : cart) {
                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(item.getProduct().getId());
                orderItem.setQuantityKg(item.getQuantityKg());
                orderItem.setUnitPriceApplied(item.getUnitPrice());
                orderItem.setLineTotal(item.getLineTotal());
                orderItems.add(orderItem);
            }
            order.setItems(orderItems);
            
            order.calculateTotalBeforeTax();
            order.calculateVat();
            order.setLoyaltyDiscount(loyaltyDiscount);
            if (appliedCoupon != null) {
                order.setCoupon(appliedCoupon);
                order.setCouponId(appliedCoupon.getId());
            }
            order.calculateTotalAfterTax();
            
            Order createdOrder = orderDAO.createOrder(order);
            if (createdOrder != null) {
                // Generate and save invoice
                byte[] invoicePDF = invoiceService.generateInvoicePDF(createdOrder);
                invoiceDAO.saveInvoice(createdOrder.getId(), invoicePDF);
                
                showAlert(Alert.AlertType.INFORMATION, "Order Placed", 
                        "Your order has been placed successfully! Order ID: " + createdOrder.getId());
                
                // Close cart window
                ((Stage) checkoutButton.getScene().getWindow()).close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Order Failed", "Failed to place order. Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleContinueShopping() {
        ((Stage) checkoutButton.getScene().getWindow()).close();
    }
    
    private String formatPrice(BigDecimal price) {
        return String.format("%.2f TL", price.doubleValue());
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

