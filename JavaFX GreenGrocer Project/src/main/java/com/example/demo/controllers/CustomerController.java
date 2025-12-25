package com.example.demo.controllers;

import com.example.demo.dao.*;
import com.example.demo.models.*;
import com.example.demo.utils.InputValidator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Customer UI.
 * Handles product browsing, shopping cart, orders, and messaging.
 * 
 * @author Group07
 * @version 1.0
 */
public class CustomerController extends BaseController {
    @FXML private Label usernameLabel;
    @FXML private TextField searchField;
    @FXML private VBox vegetablesContainer;
    @FXML private VBox fruitsContainer;
    @FXML private Button cartButton;
    @FXML private Button logoutButton;
    
    private ProductDAO productDAO;
    private List<CartItem> cart;
    
    @FXML
    public void initialize() {
        productDAO = new ProductDAO();
        cart = new ArrayList<>();
        loadProducts();
    }
    
    @Override
    public void setCurrentUser(User user) {
        super.setCurrentUser(user);
        if (usernameLabel != null && user != null) {
            usernameLabel.setText("User: " + user.getUsername());
        }
    }
    
    private void loadProducts() {
        loadProductsByType(Product.ProductType.VEG, vegetablesContainer);
        loadProductsByType(Product.ProductType.FRUIT, fruitsContainer);
    }
    
    private void loadProductsByType(Product.ProductType type, VBox container) {
        container.getChildren().clear();
        List<Product> products = productDAO.getAvailableProducts(type);
        
        for (Product product : products) {
            HBox productRow = createProductRow(product);
            container.getChildren().add(productRow);
        }
    }
    
    private HBox createProductRow(Product product) {
        HBox productRow = new HBox(10);
        productRow.setStyle("-fx-padding: 5; -fx-border-color: lightgray; -fx-border-width: 1;");
        
        Label nameLabel = new Label(product.getName());
        nameLabel.setPrefWidth(150);
        
        Label priceLabel = new Label(formatPrice(product.getEffectivePrice()));
        priceLabel.setPrefWidth(100);
        if (product.isLowStock()) {
            priceLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
        
        Label stockLabel = new Label("Stock: " + product.getStockKg() + " kg");
        stockLabel.setPrefWidth(100);
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("kg");
        quantityField.setPrefWidth(80);
        
        Button addButton = new Button("Add to Cart");
        Product finalProduct = product; // final variable for lambda
        addButton.setOnAction(e -> handleAddToCart(finalProduct, quantityField));
        
        productRow.getChildren().addAll(nameLabel, priceLabel, stockLabel, quantityField, addButton);
        return productRow;
    }
    
    @FXML
    private void handleAddToCart(Product product, TextField quantityField) {
        String input = quantityField.getText().trim();
        if (!InputValidator.isValidQuantity(input)) {
            showAlert(Alert.AlertType.ERROR, "Invalid Quantity", 
                    "Please enter a positive number for quantity.");
            return;
        }
        
        BigDecimal quantity = InputValidator.parseQuantity(input);
        if (quantity.compareTo(product.getStockKg()) > 0) {
            showAlert(Alert.AlertType.WARNING, "Insufficient Stock", 
                    "Stock available: " + product.getStockKg() + " kg");
            return;
        }
        
        // Check if product already in cart (merge quantities)
        CartItem existingItem = cart.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst()
                .orElse(null);
        
        if (existingItem != null) {
            BigDecimal newTotal = existingItem.getQuantityKg().add(quantity);
            if (newTotal.compareTo(product.getStockKg()) > 0) {
                showAlert(Alert.AlertType.WARNING, "Insufficient Stock", 
                        "Total quantity exceeds available stock.");
                return;
            }
            existingItem.addQuantity(quantity);
        } else {
            cart.add(new CartItem(product, quantity));
        }
        
        quantityField.clear();
        showAlert(Alert.AlertType.INFORMATION, "Added to Cart", 
                quantity + " kg of " + product.getName() + " added to cart.");
    }
    
    @FXML
    private void handleViewCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/cart.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.setTitle("Shopping Cart - Group07 GreenGrocer");
            
            CartController controller = loader.getController();
            controller.setCart(cart);
            controller.setCustomerId(currentUser.getId());
            controller.setCurrentUser(currentUser);
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open shopping cart.");
        }
    }
    
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadProducts();
            return;
        }
        
        List<Product> results = productDAO.searchProducts(keyword);
        
        if (results.isEmpty()) {
            loadProducts(); // If no results, show all products
        } else {
            // Display filtered search results
            vegetablesContainer.getChildren().clear();
            fruitsContainer.getChildren().clear();
            for (Product product : results) {
                if (product.getStockKg().compareTo(BigDecimal.ZERO) <= 0) continue;
                VBox container = product.getType() == Product.ProductType.VEG ? vegetablesContainer : fruitsContainer;
                HBox productRow = createProductRow(product);
                container.getChildren().add(productRow);
            }
        }
    }
    
    @FXML
    private void handleViewOrders() {
        showAlert(Alert.AlertType.INFORMATION, "My Orders", 
                "Order history feature - to be implemented with full UI");
    }
    
    @FXML
    private void handleMessages() {
        showAlert(Alert.AlertType.INFORMATION, "Messages", 
                "Messaging feature - to be implemented with full UI");
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
}

