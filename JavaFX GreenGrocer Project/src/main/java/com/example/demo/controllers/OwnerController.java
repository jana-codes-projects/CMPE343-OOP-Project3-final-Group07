package com.example.demo.controllers;

import com.example.demo.dao.*;
import com.example.demo.models.Order;
import com.example.demo.models.Product;
import com.example.demo.models.User;
import com.example.demo.utils.InputValidator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for the Owner UI.
 * Handles product management, carrier management, orders, messages, and reports.
 * 
 * @author Group07
 * @version 1.0
 */
public class OwnerController extends BaseController {
    @FXML private VBox productsContainer;
    @FXML private VBox carriersContainer;
    @FXML private VBox ordersContainer;
    @FXML private VBox messagesContainer;
    @FXML private Button logoutButton;
    
    private ProductDAO productDAO;
    private UserDAO userDAO;
    private OrderDAO orderDAO;
    private MessageDAO messageDAO;
    
    @FXML
    public void initialize() {
        productDAO = new ProductDAO();
        userDAO = new UserDAO();
        orderDAO = new OrderDAO();
        messageDAO = new MessageDAO();
        loadAllData();
    }
    
    private void loadAllData() {
        loadProducts();
        loadCarriers();
        loadOrders();
        loadMessages();
    }
    
    private void loadProducts() {
        productsContainer.getChildren().clear();
        List<Product> products = productDAO.getAllProducts(null);
        
        for (Product product : products) {
            VBox productBox = new VBox(5);
            productBox.setStyle("-fx-padding: 5; -fx-border-color: gray; -fx-border-width: 1;");
            
            Label nameLabel = new Label(product.getName() + " - " + product.getPrice() + " TL/kg");
            Label stockLabel = new Label("Stock: " + product.getStockKg() + " kg, Threshold: " + product.getThresholdKg() + " kg");
            
            HBox buttonBox = new HBox(10);
            Button editButton = new Button("Edit");
            editButton.setOnAction(e -> handleEditProduct(product));
            Button removeButton = new Button("Remove");
            removeButton.setOnAction(e -> handleRemoveProduct(product));
            
            buttonBox.getChildren().addAll(editButton, removeButton);
            productBox.getChildren().addAll(nameLabel, stockLabel, buttonBox);
            productsContainer.getChildren().add(productBox);
        }
    }
    
    private void loadCarriers() {
        carriersContainer.getChildren().clear();
        List<User> carriers = userDAO.getAllCarriers();
        
        for (User carrier : carriers) {
            HBox carrierBox = new HBox(10);
            carrierBox.setStyle("-fx-padding: 5;");
            
            Label nameLabel = new Label(carrier.getUsername());
            nameLabel.setPrefWidth(200);
            
            Label statusLabel = new Label(carrier.isActive() ? "Active" : "Inactive");
            statusLabel.setPrefWidth(100);
            
            Button toggleButton = new Button(carrier.isActive() ? "Fire" : "Hire");
            toggleButton.setOnAction(e -> handleToggleCarrier(carrier));
            
            carrierBox.getChildren().addAll(nameLabel, statusLabel, toggleButton);
            carriersContainer.getChildren().add(carrierBox);
        }
    }
    
    private void loadOrders() {
        ordersContainer.getChildren().clear();
        List<Order> orders = orderDAO.getAllOrders();
        
        for (Order order : orders) {
            VBox orderBox = new VBox(5);
            orderBox.setStyle("-fx-padding: 5; -fx-border-color: gray; -fx-border-width: 1;");
            
            Label idLabel = new Label("Order ID: " + order.getId() + " - Status: " + order.getStatus());
            Label totalLabel = new Label("Total: " + formatPrice(order.getTotalAfterTax()));
            Label dateLabel = new Label("Date: " + order.getOrderTime());
            
            orderBox.getChildren().addAll(idLabel, totalLabel, dateLabel);
            ordersContainer.getChildren().add(orderBox);
        }
    }
    
    private void loadMessages() {
        messagesContainer.getChildren().clear();
        // Load messages - simplified implementation
        Label label = new Label("Messages feature - to be fully implemented");
        messagesContainer.getChildren().add(label);
    }
    
    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }
    
    @FXML
    private void handleEditProduct(Product product) {
        showProductDialog(product);
    }
    
    private void showProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Add Product" : "Edit Product");
        
        TextField nameField = new TextField(product != null ? product.getName() : "");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("VEG", "FRUIT");
        if (product != null) {
            typeCombo.setValue(product.getType().name());
        }
        TextField priceField = new TextField(product != null ? product.getPrice().toString() : "");
        TextField stockField = new TextField(product != null ? product.getStockKg().toString() : "");
        TextField thresholdField = new TextField(product != null ? product.getThresholdKg().toString() : "");
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Name:"), nameField,
                new Label("Type:"), typeCombo,
                new Label("Price:"), priceField,
                new Label("Stock (kg):"), stockField,
                new Label("Threshold (kg):"), thresholdField
        );
        dialog.getDialogPane().setContent(content);
        
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                try {
                    Product.ProductType type = Product.ProductType.valueOf(typeCombo.getValue());
                    BigDecimal price = new BigDecimal(priceField.getText());
                    BigDecimal stock = new BigDecimal(stockField.getText());
                    BigDecimal threshold = new BigDecimal(thresholdField.getText());
                    
                    if (!InputValidator.isValidPrice(price) || !InputValidator.isValidThreshold(threshold)) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Input", 
                                "Price and threshold must be positive numbers.");
                        return null;
                    }
                    
                    if (product == null) {
                        Product newProduct = new Product(0, nameField.getText(), type, price, stock, threshold, null, true);
                        productDAO.addProduct(newProduct);
                    } else {
                        product.setName(nameField.getText());
                        product.setType(type);
                        product.setPrice(price);
                        product.setStockKg(stock);
                        product.setThresholdKg(threshold);
                        productDAO.updateProduct(product);
                    }
                    loadProducts();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save product: " + e.getMessage());
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    @FXML
    private void handleRemoveProduct(Product product) {
        boolean confirmed = showConfirmation("Remove Product", 
                "Are you sure you want to remove " + product.getName() + "?");
        if (confirmed) {
            productDAO.removeProduct(product.getId());
            loadProducts();
        }
    }
    
    @FXML
    private void handleToggleCarrier(User carrier) {
        boolean success = carrier.isActive() 
                ? userDAO.deactivateCarrier(carrier.getId())
                : userDAO.activateCarrier(carrier.getId());
        
        if (success) {
            loadCarriers();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update carrier status.");
        }
    }
    
    @FXML
    private void handleRefreshProducts() {
        loadProducts();
    }
    
    @FXML
    private void handleGenerateReport() {
        showAlert(Alert.AlertType.INFORMATION, "Reports", 
                "Report generation - to be implemented with charts (product/time/revenue)");
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
    
    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}

