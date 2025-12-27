package com.example.demo.controllers;

import com.example.demo.dao.OrderDAO;
import com.example.demo.models.Order;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Carrier UI.
 * Handles viewing and managing delivery orders.
 * 
 * @author Group07
 * @version 1.0
 */
public class CarrierController extends BaseController {
    @FXML private Label usernameLabel;
    @FXML private VBox availableOrdersListContainer;
    @FXML private VBox availableOrderDetailContainer;
    @FXML private VBox myOrdersListContainer;
    @FXML private VBox myOrderDetailContainer;
    @FXML private VBox completedOrdersListContainer;
    @FXML private VBox completedOrderDetailContainer;
    @FXML private Button logoutButton;
    
    private OrderDAO orderDAO;
    private Order selectedAvailableOrder;
    private Order selectedMyOrder;
    private Order selectedCompletedOrder;
    
    @FXML
    public void initialize() {
        orderDAO = new OrderDAO();
        loadOrders();
    }
    
    @Override
    public void setCurrentUser(com.example.demo.models.User user) {
        super.setCurrentUser(user);
        if (usernameLabel != null && user != null) {
            usernameLabel.setText("Carrier: " + user.getUsername());
        }
        loadOrders();
    }
    
    private void loadOrders() {
        loadAvailableOrders();
        loadMyOrders();
        loadCompletedOrders();
    }
    
    @FXML
    private void handleRefresh() {
        loadOrders();
    }
    
    private void loadAvailableOrders() {
        availableOrdersListContainer.getChildren().clear();
        availableOrderDetailContainer.getChildren().clear();
        selectedAvailableOrder = null;
        
        List<Order> orders = orderDAO.getAvailableOrders();
        
        if (orders.isEmpty()) {
            Label noOrdersLabel = new Label("No available orders at this time.");
            noOrdersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            availableOrdersListContainer.getChildren().add(noOrdersLabel);
            return;
        }
        
        // Sort by order ID (newest first)
        orders.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        for (Order order : orders) {
            HBox listItem = createOrderListItem(order, formatter, "#2196f3");
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                availableOrdersListContainer.getChildren().add(listItem);
            }
        }
        
        availableOrdersListContainer.requestLayout();
        showAvailableOrderDetailPlaceholder();
    }
    
    private void loadMyOrders() {
        myOrdersListContainer.getChildren().clear();
        myOrderDetailContainer.getChildren().clear();
        selectedMyOrder = null;
        
        if (currentUser == null) {
            Label noUserLabel = new Label("Carrier not logged in.");
            noUserLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            myOrdersListContainer.getChildren().add(noUserLabel);
            return;
        }
        
        List<Order> orders = orderDAO.getOrdersByCarrier(currentUser.getId(), Order.OrderStatus.ASSIGNED);
        
        if (orders.isEmpty()) {
            Label noOrdersLabel = new Label("No assigned orders. Select orders from the 'Available Orders' tab.");
            noOrdersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            myOrdersListContainer.getChildren().add(noOrdersLabel);
            return;
        }
        
        // Sort by order ID (newest first)
        orders.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        for (Order order : orders) {
            HBox listItem = createOrderListItem(order, formatter, "#ff9800");
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                myOrdersListContainer.getChildren().add(listItem);
            }
        }

        myOrdersListContainer.requestLayout();
        showMyOrderDetailPlaceholder();
    }
    
    /**
     * Creates a clickable list item for an order.
     */
    private HBox createOrderListItem(Order order, DateTimeFormatter formatter, String statusColor) {
        HBox item = new HBox(10);
        item.setStyle("-fx-padding: 10 12; -fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; " +
                      "-fx-cursor: hand;");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefWidth(Double.MAX_VALUE);
        item.setUserData(order.getId());
        
        // Add hover effect
        item.setOnMouseEntered(e -> {
            if (!item.getStyle().contains("-fx-background-color: #e3f2fd;")) {
                item.setStyle(item.getStyle() + " -fx-background-color: #f5f5f5;");
            }
        });
        item.setOnMouseExited(e -> {
            if (!item.getStyle().contains("-fx-background-color: #e3f2fd;")) {
                item.setStyle(item.getStyle().replace(" -fx-background-color: #f5f5f5;", ""));
            }
        });
        
        // Order ID
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1976d2; -fx-cursor: hand;");
        idLabel.setPrefWidth(100);
        
        // Status badge
        String statusText = order.getStatus().name();
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + "; " +
                            "-fx-padding: 4 8; -fx-background-color: " + statusColor + "20; -fx-background-radius: 4;");
        
        // Date
        Label dateLabel = new Label(order.getOrderTime().format(formatter));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        dateLabel.setPrefWidth(150);
        
        // Customer name
        com.example.demo.models.User customer = null;
        try {
            customer = new com.example.demo.dao.UserDAO().getUserById(order.getCustomerId());
        } catch (Exception e) {
            // Continue without customer info
        }
        String customerName = customer != null ? customer.getUsername() : "Customer #" + order.getCustomerId();
        Label customerLabel = new Label(customerName);
        customerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
        
        // Total amount
        Label totalLabel = new Label(formatPrice(order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        totalLabel.setPrefWidth(100);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        item.getChildren().addAll(idLabel, statusLabel, dateLabel, customerLabel, spacer, totalLabel);
        
        // Store reference to order in item for click handler
        item.setUserData(order);
        
        // Make clickable - determine which detail method to call based on which container we're adding to
        item.setOnMouseClicked(e -> {
            if (item.getParent() == availableOrdersListContainer) {
                showAvailableOrderDetail(order, formatter);
            } else if (item.getParent() == myOrdersListContainer) {
                showMyOrderDetail(order, formatter);
            } else if (item.getParent() == completedOrdersListContainer) {
                showCompletedOrderDetail(order, formatter);
            }
        });
        idLabel.setOnMouseClicked(e -> item.getOnMouseClicked().handle(e));
        
        return item;
    }
    
    /**
     * Shows the detail view for an available order.
     */
    private void showAvailableOrderDetail(Order order, DateTimeFormatter formatter) {
        selectedAvailableOrder = order;
        availableOrderDetailContainer.getChildren().clear();
        
        VBox detailCard = createAvailableOrderDetailCard(order, formatter);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            availableOrderDetailContainer.getChildren().add(detailCard);
        }
        
        updateOrderListSelection(order, availableOrdersListContainer);
    }
    
    /**
     * Shows the detail view for a my order.
     */
    private void showMyOrderDetail(Order order, DateTimeFormatter formatter) {
        selectedMyOrder = order;
        myOrderDetailContainer.getChildren().clear();
        
        VBox detailCard = createMyOrderDetailCard(order, formatter);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            myOrderDetailContainer.getChildren().add(detailCard);
        }
        
        updateOrderListSelection(order, myOrdersListContainer);
    }
    
    /**
     * Shows the detail view for a completed order.
     */
    private void showCompletedOrderDetail(Order order, DateTimeFormatter formatter) {
        selectedCompletedOrder = order;
        completedOrderDetailContainer.getChildren().clear();
        
        VBox detailCard = createCompletedOrderDetailCard(order, formatter);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            completedOrderDetailContainer.getChildren().add(detailCard);
        }
        
        updateOrderListSelection(order, completedOrdersListContainer);
    }
    
    /**
     * Updates the visual selection in the order list.
     */
    private void updateOrderListSelection(Order selected, VBox listContainer) {
        for (javafx.scene.Node node : listContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                String currentStyle = item.getStyle();
                if (currentStyle.contains("-fx-background-color: #e3f2fd;")) {
                    item.setStyle(currentStyle.replace(" -fx-background-color: #e3f2fd;", "")
                                             .replace("-fx-background-color: #e3f2fd;", "-fx-background-color: #ffffff;"));
                }
                if (item.getUserData() instanceof Order) {
                    Order order = (Order) item.getUserData();
                    if (order.getId() == selected.getId()) {
                        item.setStyle(item.getStyle() + " -fx-background-color: #e3f2fd;");
                    }
                }
            }
        }
    }
    
    /**
     * Shows placeholder text in the detail view.
     */
    private void showAvailableOrderDetailPlaceholder() {
        availableOrderDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select an order from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
        availableOrderDetailContainer.getChildren().add(placeholder);
    }
    
    private void showMyOrderDetailPlaceholder() {
        myOrderDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select an order from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
        myOrderDetailContainer.getChildren().add(placeholder);
    }
    
    private void showCompletedOrderDetailPlaceholder() {
        completedOrderDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select an order from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
        completedOrderDetailContainer.getChildren().add(placeholder);
    }
    
    /**
     * Creates a detail card for an available order (used in detail view).
     */
    private VBox createAvailableOrderDetailCard(Order order, DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #2196f3; -fx-border-width: 2; -fx-border-radius: 8; " +
                      "-fx-background-color: #e3f2fd; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #1976d2;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statusLabel = new Label("AVAILABLE");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1976d2; " +
                            "-fx-padding: 6 12; -fx-background-color: #2196f320; -fx-background-radius: 4;");
        
        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        // Details
        VBox detailsBox = new VBox(8);
        
        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        orderDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        
        Label requestedDeliveryLabel = new Label("Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
        requestedDeliveryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        
        // Customer info
        com.example.demo.models.User customer = null;
        try {
            customer = new com.example.demo.dao.UserDAO().getUserById(order.getCustomerId());
        } catch (Exception e) {
            // Continue without customer info
        }
        
        if (customer != null) {
            Label customerLabel = new Label("Customer: " + customer.getUsername());
            customerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
            detailsBox.getChildren().add(customerLabel);
            
            if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                Label addressLabel = new Label("Address: " + customer.getAddress());
                addressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #757575; -fx-wrap-text: true;");
                detailsBox.getChildren().add(addressLabel);
            }
        }
        
        // Order items
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsHeader = new Label("Items:");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            VBox itemsBox = new VBox(3);
            itemsBox.setStyle("-fx-padding: 0 0 0 20;");
            for (com.example.demo.models.OrderItem item : order.getItems()) {
                String itemName = item.getProduct() != null 
                    ? item.getProduct().getName() 
                    : "Product #" + item.getProductId();
                Label itemLabel = new Label(String.format("• %s: %.2f kg @ %.2f TL = %.2f TL",
                        itemName,
                        item.getQuantityKg().doubleValue(),
                        item.getUnitPriceApplied().doubleValue(),
                        item.getLineTotal().doubleValue()));
                itemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #424242;");
                itemsBox.getChildren().add(itemLabel);
            }
            
            detailsBox.getChildren().addAll(itemsHeader, itemsBox);
        }
        
        // Total
        Label totalLabel = new Label("Total: " + formatPrice(order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        detailsBox.getChildren().add(totalLabel);
        
        // Action button
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button selectButton = new Button("Select Order");
        selectButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; " +
                             "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
        selectButton.setOnAction(e -> {
            handleSelectOrder(order);
            loadOrders(); // Refresh all tabs
        });
        
        buttonBox.getChildren().add(selectButton);
        
        card.getChildren().addAll(header, detailsBox, buttonBox);
        return card;
    }
    
    /**
     * Creates a detail card for a my order (used in detail view).
     */
    private VBox createMyOrderDetailCard(Order order, DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #ff9800; -fx-border-width: 2; -fx-border-radius: 8; " +
                      "-fx-background-color: #fff3e0; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #f57c00;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statusLabel = new Label("ASSIGNED");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f57c00; " +
                            "-fx-padding: 6 12; -fx-background-color: #ff980020; -fx-background-radius: 4;");
        
        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        // Details
        VBox detailsBox = new VBox(8);
        
        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        orderDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        
        Label requestedDeliveryLabel = new Label("Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
        requestedDeliveryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        
        // Order items
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsHeader = new Label("Items:");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            VBox itemsBox = new VBox(3);
            itemsBox.setStyle("-fx-padding: 0 0 0 20;");
            for (com.example.demo.models.OrderItem item : order.getItems()) {
                String itemName = item.getProduct() != null 
                    ? item.getProduct().getName() 
                    : "Product #" + item.getProductId();
                Label itemLabel = new Label(String.format("• %s: %.2f kg @ %.2f TL = %.2f TL",
                        itemName,
                        item.getQuantityKg().doubleValue(),
                        item.getUnitPriceApplied().doubleValue(),
                        item.getLineTotal().doubleValue()));
                itemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #424242;");
                itemsBox.getChildren().add(itemLabel);
            }
            
            detailsBox.getChildren().addAll(itemsHeader, itemsBox);
        }
        
        // Total
        Label totalLabel = new Label("Total: " + formatPrice(order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        detailsBox.getChildren().add(totalLabel);
        
        // Action button
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button completeButton = new Button("Mark as Delivered");
        completeButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; " +
                               "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
        completeButton.setOnAction(e -> {
            handleCompleteOrder(order);
            loadOrders(); // Refresh all tabs
        });
        
        buttonBox.getChildren().add(completeButton);
        
        card.getChildren().addAll(header, detailsBox, buttonBox);
        return card;
    }
    
    private void loadCompletedOrders() {
        completedOrdersListContainer.getChildren().clear();
        completedOrderDetailContainer.getChildren().clear();
        selectedCompletedOrder = null;
        
        if (currentUser == null) {
            Label noUserLabel = new Label("Carrier not logged in.");
            noUserLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            completedOrdersListContainer.getChildren().add(noUserLabel);
            return;
        }
        
        List<Order> orders = orderDAO.getOrdersByCarrier(currentUser.getId(), Order.OrderStatus.DELIVERED);
        
        if (orders.isEmpty()) {
            Label noOrdersLabel = new Label("No completed orders yet.");
            noOrdersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            completedOrdersListContainer.getChildren().add(noOrdersLabel);
            return;
        }
        
        // Sort by order ID (newest first)
        orders.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        for (Order order : orders) {
            HBox listItem = createOrderListItem(order, formatter, "#4caf50");
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                completedOrdersListContainer.getChildren().add(listItem);
            }
        }
        
        completedOrdersListContainer.requestLayout();
        showCompletedOrderDetailPlaceholder();
    }
    
    /**
     * Creates a detail card for a completed order (used in detail view).
     */
    private VBox createCompletedOrderDetailCard(Order order, DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #4caf50; -fx-border-width: 2; -fx-border-radius: 8; " +
                      "-fx-background-color: #f1f8f4; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2e7d32;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statusLabel = new Label("✓ DELIVERED");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2e7d32; " +
                            "-fx-padding: 6 12; -fx-background-color: #4caf5020; -fx-background-radius: 4;");
        
        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        // Details
        VBox detailsBox = new VBox(8);
        
        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        orderDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        
        Label requestedDeliveryLabel = new Label("Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
        requestedDeliveryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        
        if (order.getDeliveredTime() != null) {
            Label deliveredLabel = new Label("Delivered: " + order.getDeliveredTime().format(formatter));
            deliveredLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            detailsBox.getChildren().add(deliveredLabel);
        }
        
        // Order items
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsHeader = new Label("Items:");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            VBox itemsBox = new VBox(3);
            itemsBox.setStyle("-fx-padding: 0 0 0 20;");
            for (com.example.demo.models.OrderItem item : order.getItems()) {
                String itemName = item.getProduct() != null 
                    ? item.getProduct().getName() 
                    : "Product #" + item.getProductId();
                Label itemLabel = new Label(String.format("• %s: %.2f kg @ %.2f TL = %.2f TL",
                        itemName,
                        item.getQuantityKg().doubleValue(),
                        item.getUnitPriceApplied().doubleValue(),
                        item.getLineTotal().doubleValue()));
                itemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #424242;");
                itemsBox.getChildren().add(itemLabel);
            }
            
            detailsBox.getChildren().addAll(itemsHeader, itemsBox);
        }
        
        // Total
        Label totalLabel = new Label("Total: " + formatPrice(order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        detailsBox.getChildren().add(totalLabel);
        
        card.getChildren().addAll(header, detailsBox);
        return card;
    }
    
    @FXML
    private void handleSelectOrder(Order order) {
        if (currentUser == null) return;
        
        System.out.println("DEBUG: Attempting to assign order #" + order.getId() + " to carrier ID: " + currentUser.getId());
        boolean success = orderDAO.assignOrderToCarrier(order.getId(), currentUser.getId());
        if (success) {
            System.out.println("DEBUG: Successfully assigned order #" + order.getId() + " to carrier ID: " + currentUser.getId());
            showAlert(Alert.AlertType.INFORMATION, "Order Selected", 
                    "Order " + order.getId() + " has been assigned to you.");
            loadOrders(); // This refreshes all tabs including "My Orders"
        } else {
            System.out.println("DEBUG: Failed to assign order #" + order.getId() + " - may have been selected by another carrier");
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to select order. It may have been selected by another carrier.");
        }
    }
    
    @FXML
    private void handleCompleteOrder(Order order) {
        boolean success = orderDAO.markOrderDelivered(order.getId(), LocalDateTime.now());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Order Completed", 
                    "Order " + order.getId() + " has been marked as delivered.");
            loadOrders();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to mark order as delivered.");
        }
    }
    
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/login.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 500, 400));
            stage.setTitle("Login - Group07 GreenGrocer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String formatPrice(java.math.BigDecimal price) {
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

