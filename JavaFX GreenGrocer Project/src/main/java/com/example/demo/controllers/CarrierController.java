package com.example.demo.controllers;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.models.Order;
import com.example.demo.models.OrderStatus;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    @FXML private VBox availableOrdersContainer;
    @FXML private VBox myOrdersContainer;
    @FXML private VBox completedOrdersContainer;
    @FXML private Button logoutButton;
    
    private OrderDAO orderDAO;
    private ProductDAO productDAO;
    
    @FXML
    public void initialize() {
        orderDAO = new OrderDAO();
        productDAO = new ProductDAO();
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
    
    private void loadAvailableOrders() {
        availableOrdersContainer.getChildren().clear();
        List<Order> orders = orderDAO.getAvailableOrders();
        
        for (Order order : orders) {
            VBox orderBox = createOrderDisplay(order);
            Button selectButton = new Button("Select Order");
            selectButton.setOnAction(e -> handleSelectOrder(order));
            orderBox.getChildren().add(selectButton);
            availableOrdersContainer.getChildren().add(orderBox);
        }
    }
    
    private void loadMyOrders() {
        myOrdersContainer.getChildren().clear();
        if (currentUser == null) return;
        
        List<Order> orders = orderDAO.getOrdersByCarrier(currentUser.getId(), OrderStatus.ASSIGNED);
        
        for (Order order : orders) {
            VBox orderBox = createOrderDisplay(order);
            Button completeButton = new Button("Mark as Delivered");
            completeButton.setOnAction(e -> handleCompleteOrder(order));
            orderBox.getChildren().add(completeButton);
            myOrdersContainer.getChildren().add(orderBox);
        }
    }
    
    private void loadCompletedOrders() {
        completedOrdersContainer.getChildren().clear();
        if (currentUser == null) return;
        
        List<Order> orders = orderDAO.getOrdersByCarrier(currentUser.getId(), OrderStatus.DELIVERED);
        
        for (Order order : orders) {
            VBox orderBox = createOrderDisplay(order);
            completedOrdersContainer.getChildren().add(orderBox);
        }
    }
    
    private VBox createOrderDisplay(Order order) {
        VBox box = new VBox(5);
        box.setStyle("-fx-padding: 10; -fx-border-color: gray; -fx-border-width: 1;");
        
        Label idLabel = new Label("Order ID: " + order.getId());
        Label totalLabel = new Label("Total: " + formatPrice(order.getTotalAfterTax()));
        Label deliveryLabel = new Label("Delivery: " + order.getRequestedDeliveryTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        
        box.getChildren().addAll(idLabel, totalLabel, deliveryLabel);
        return box;
    }
    
    @FXML
    private void handleSelectOrder(Order order) {
        if (currentUser == null) return;
        
        boolean success = orderDAO.assignOrderToCarrier(order.getId(), currentUser.getId());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Order Selected", 
                    "Order " + order.getId() + " has been assigned to you.");
            loadOrders();
        } else {
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

