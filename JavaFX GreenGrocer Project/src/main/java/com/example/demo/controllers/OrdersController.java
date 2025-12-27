package com.example.demo.controllers;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.InvoiceDAO;
import com.example.demo.models.Order;
import com.example.demo.models.Order.OrderStatus;
import com.example.demo.models.OrderItem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the My Orders UI.
 * Displays customer order history and allows viewing order details.
 * 
 * @author Group07
 * @version 1.0
 */
public class OrdersController extends BaseController {
    @FXML private VBox ordersContainer;
    @FXML private Button refreshButton;
    @FXML private Button closeButton;
    
    private OrderDAO orderDAO;
    private InvoiceDAO invoiceDAO;
    private int customerId;
    
    @FXML
    public void initialize() {
        orderDAO = new OrderDAO();
        invoiceDAO = new InvoiceDAO();
    }
    
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
        loadOrders();
    }
    
    private void loadOrders() {
        ordersContainer.getChildren().clear();
        
        List<Order> orders = orderDAO.getOrdersByCustomer(customerId);
        
        if (orders.isEmpty()) {
            Label noOrdersLabel = new Label("You have no orders yet.");
            noOrdersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            ordersContainer.getChildren().add(noOrdersLabel);
            return;
        }
        
        for (Order order : orders) {
            VBox orderCard = createOrderCard(order);
            ordersContainer.getChildren().add(orderCard);
        }
    }
    
    private VBox createOrderCard(Order order) {
        VBox card = new VBox(10);
        card.setStyle("-fx-padding: 15; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #f9f9f9; -fx-background-radius: 5;");
        card.setPrefWidth(Double.MAX_VALUE);
        
        // Header with Order ID and Status
        HBox header = new HBox(10);
        Label orderIdLabel = new Label("Order #" + order.getId());
        orderIdLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label statusLabel = new Label(order.getStatus().toString());
        statusLabel.setStyle(getStatusStyle(order.getStatus()));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(orderIdLabel, spacer, statusLabel);
        
        // Order details
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Label dateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        Label deliveryLabel = new Label("Requested Delivery: " + 
                order.getRequestedDeliveryTime().format(formatter));
        
        Label deliveredLabel = null;
        if (order.getDeliveredTime() != null) {
            deliveredLabel = new Label("Delivered: " + order.getDeliveredTime().format(formatter));
            deliveredLabel.setStyle("-fx-text-fill: green;");
        }
        
        // Order items summary
        Label itemsLabel = new Label("Items (" + order.getItems().size() + "):");
        itemsLabel.setStyle("-fx-font-weight: bold;");
        
        VBox itemsBox = new VBox(5);
        itemsBox.setStyle("-fx-padding: 0 0 0 20;");
        for (OrderItem item : order.getItems()) {
            Label itemLabel = new Label(String.format("- %s: %.2f kg @ %.2f TL = %.2f TL",
                    item.getProduct() != null ? item.getProduct().getName() : "Product " + item.getProductId(),
                    item.getQuantityKg().doubleValue(),
                    item.getUnitPriceApplied().doubleValue(),
                    item.getLineTotal().doubleValue()));
            itemsBox.getChildren().add(itemLabel);
        }
        
        // Totals
        Label totalLabel = new Label("Total: " + formatPrice(order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c5aa0;");
        
        // Action buttons
        HBox buttonBox = new HBox(10);
        
        Button detailsButton = new Button("View Details");
        detailsButton.setOnAction(e -> showOrderDetails(order));
        
        Button invoiceButton = new Button("Download Invoice");
        invoiceButton.setOnAction(e -> downloadInvoice(order));
        
        // Cancel button (only for CREATED or ASSIGNED orders)
        if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.ASSIGNED) {
            Button cancelButton = new Button("Cancel Order");
            cancelButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white;");
            cancelButton.setOnAction(e -> handleCancelOrder(order));
            buttonBox.getChildren().add(cancelButton);
        }
        
        buttonBox.getChildren().addAll(detailsButton, invoiceButton);
        
        // Add all children in the correct order (only once - no duplicates!)
        java.util.List<javafx.scene.Node> children = new java.util.ArrayList<>();
        children.add(header);
        children.add(dateLabel);
        children.add(deliveryLabel);
        if (deliveredLabel != null) {
            children.add(deliveredLabel);
        }
        children.add(itemsLabel);
        children.add(itemsBox);
        children.add(totalLabel);
        children.add(buttonBox);
        card.getChildren().addAll(children);
        
        return card;
    }
    
    private String getStatusStyle(OrderStatus status) {
        switch (status) {
            case CREATED:
                return "-fx-text-fill: blue; -fx-font-weight: bold;";
            case ASSIGNED:
                return "-fx-text-fill: orange; -fx-font-weight: bold;";
            case DELIVERED:
                return "-fx-text-fill: green; -fx-font-weight: bold;";
            case CANCELLED:
                return "-fx-text-fill: red; -fx-font-weight: bold;";
            default:
                return "";
        }
    }
    
    private void showOrderDetails(Order order) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Order Details");
        alert.setHeaderText("Order #" + order.getId());
        
        StringBuilder content = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        content.append("Status: ").append(order.getStatus()).append("\n\n");
        content.append("Order Date: ").append(order.getOrderTime().format(formatter)).append("\n");
        content.append("Requested Delivery: ").append(order.getRequestedDeliveryTime().format(formatter)).append("\n");
        if (order.getDeliveredTime() != null) {
            content.append("Delivered: ").append(order.getDeliveredTime().format(formatter)).append("\n");
        }
        content.append("\n");
        
        content.append("Items:\n");
        for (OrderItem item : order.getItems()) {
            content.append(String.format("  - %s: %.2f kg @ %.2f TL = %.2f TL\n",
                    item.getProduct() != null ? item.getProduct().getName() : "Product " + item.getProductId(),
                    item.getQuantityKg().doubleValue(),
                    item.getUnitPriceApplied().doubleValue(),
                    item.getLineTotal().doubleValue()));
        }
        content.append("\n");
        content.append("Subtotal: ").append(formatPrice(order.getTotalBeforeTax())).append("\n");
        content.append("VAT (20%): ").append(formatPrice(order.getVat())).append("\n");
        if (order.getLoyaltyDiscount().compareTo(BigDecimal.ZERO) > 0) {
            content.append("Loyalty Discount: -").append(formatPrice(order.getLoyaltyDiscount())).append("\n");
        }
        if (order.getCoupon() != null) {
            BigDecimal couponDiscount = order.getCoupon().calculateDiscount(
                    order.getTotalBeforeTax().add(order.getVat()));
            content.append("Coupon Discount: -").append(formatPrice(couponDiscount)).append("\n");
        }
        content.append("\n");
        content.append("TOTAL: ").append(formatPrice(order.getTotalAfterTax()));
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    private void downloadInvoice(Order order) {
        try {
            byte[] invoicePDF = invoiceDAO.getInvoice(order.getId());
            if (invoicePDF == null) {
                showAlert(Alert.AlertType.WARNING, "Invoice Not Found", 
                        "Invoice for this order is not available.");
                return;
            }
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Invoice");
            fileChooser.setInitialFileName("Invoice_Order_" + order.getId() + ".pdf");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            
            Stage stage = (Stage) closeButton.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(invoicePDF);
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                            "Invoice downloaded successfully!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to download invoice: " + e.getMessage());
        }
    }
    
    private void handleCancelOrder(Order order) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Order");
        confirmAlert.setHeaderText("Cancel Order #" + order.getId());
        confirmAlert.setContentText("Are you sure you want to cancel this order?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean cancelled = orderDAO.cancelOrder(order.getId());
                    if (cancelled) {
                        showAlert(Alert.AlertType.INFORMATION, "Order Cancelled", 
                                "Order #" + order.getId() + " has been cancelled.");
                        loadOrders(); // Refresh the list
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", 
                                "Failed to cancel order. It may have already been processed.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", 
                            "An error occurred: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void handleRefresh() {
        loadOrders();
    }
    
    @FXML
    private void handleClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
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

