package com.example.demo.controllers;

import com.example.demo.dao.*;
import com.example.demo.models.*;
import com.example.demo.utils.InputValidator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
    @FXML private VBox vegetablesListContainer;
    @FXML private VBox vegetablesDetailContainer;
    @FXML private VBox fruitsListContainer;
    @FXML private VBox fruitsDetailContainer;
    @FXML private Button cartButton;
    @FXML private Button logoutButton;
    
    private ProductDAO productDAO;
    private List<CartItem> cart;
    private Product selectedVegetable;
    private Product selectedFruit;
    
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
        loadProductsByType(Product.ProductType.VEG, vegetablesListContainer, vegetablesDetailContainer);
        loadProductsByType(Product.ProductType.FRUIT, fruitsListContainer, fruitsDetailContainer);
    }
    
    private void loadProductsByType(Product.ProductType type, VBox listContainer, VBox detailContainer) {
        listContainer.getChildren().clear();
        detailContainer.getChildren().clear();
        
        // Get all products including out-of-stock ones
        List<Product> products = productDAO.getAllProducts(type);
        products.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        
        if (products.isEmpty()) {
            Label noProductsLabel = new Label("No " + type.name().toLowerCase() + " available.");
            noProductsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            listContainer.getChildren().add(noProductsLabel);
            return;
        }
        
        for (Product product : products) {
            HBox listItem = createProductListItem(product, type);
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                listContainer.getChildren().add(listItem);
            }
        }
        
        listContainer.requestLayout();
        showProductDetailPlaceholder(type);
    }
    
    private HBox createProductListItem(Product product, Product.ProductType type) {
        HBox item = new HBox(10);
        item.setStyle("-fx-padding: 10 12; -fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; " +
                      "-fx-cursor: hand;");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefWidth(Double.MAX_VALUE);
        item.setUserData(product.getId());
        
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
        
        // Stock check (needed for styling)
        boolean isOutOfStock = product.getStockKg().compareTo(BigDecimal.ZERO) <= 0;
        
        // Product name
        String nameColor = isOutOfStock ? "#f44336" : "#1976d2";
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + nameColor + "; -fx-cursor: hand;");
        nameLabel.setPrefWidth(180);
        
        // Price
        String priceColor = isOutOfStock ? "#f44336" : (product.isLowStock() ? "#f44336" : "#4caf50");
        Label priceLabel = new Label(formatPrice(product.getEffectivePrice()) + " /kg");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + priceColor + "; " +
                           "-fx-font-weight: " + (isOutOfStock || product.isLowStock() ? "bold;" : "normal;"));
        priceLabel.setPrefWidth(120);
        
        // Stock
        String stockColor = isOutOfStock ? "#f44336" : (product.isLowStock() ? "#f44336" : "#757575");
        String stockText = isOutOfStock ? "OUT OF STOCK" : ("Stock: " + product.getStockKg() + " kg");
        Label stockLabel = new Label(stockText);
        stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + stockColor + "; " +
                           "-fx-font-weight: " + (isOutOfStock || product.isLowStock() ? "bold;" : "normal;"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Stock indicator badges
        if (isOutOfStock) {
            Label outOfStockLabel = new Label("OUT OF STOCK");
            outOfStockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff; -fx-font-weight: bold; " +
                                    "-fx-padding: 4 8; -fx-background-color: #f44336; -fx-background-radius: 4;");
            item.getChildren().addAll(nameLabel, priceLabel, stockLabel, spacer, outOfStockLabel);
            // Make the entire item appear faded/disabled
            item.setStyle(item.getStyle() + " -fx-opacity: 0.7;");
        } else if (product.isLowStock()) {
            Label lowStockLabel = new Label("⚠ Low Stock");
            lowStockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #f44336; -fx-font-weight: bold;");
            item.getChildren().addAll(nameLabel, priceLabel, stockLabel, spacer, lowStockLabel);
        } else {
            item.getChildren().addAll(nameLabel, priceLabel, stockLabel, spacer);
        }
        
        // Store reference to product
        item.setUserData(product);
        
        // Make clickable
        item.setOnMouseClicked(e -> {
            if (type == Product.ProductType.VEG) {
                showVegetableDetail(product);
            } else {
                showFruitDetail(product);
            }
        });
        nameLabel.setOnMouseClicked(e -> item.getOnMouseClicked().handle(e));
        
        return item;
    }
    
    private void showVegetableDetail(Product product) {
        selectedVegetable = product;
        vegetablesDetailContainer.getChildren().clear();
        
        VBox detailCard = createProductDetailCard(product);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            vegetablesDetailContainer.getChildren().add(detailCard);
        }
        
        updateProductListSelection(product, vegetablesListContainer);
    }
    
    private void showFruitDetail(Product product) {
        selectedFruit = product;
        fruitsDetailContainer.getChildren().clear();
        
        VBox detailCard = createProductDetailCard(product);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            fruitsDetailContainer.getChildren().add(detailCard);
        }
        
        updateProductListSelection(product, fruitsListContainer);
    }
    
    private void updateProductListSelection(Product selected, VBox listContainer) {
        for (javafx.scene.Node node : listContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                String currentStyle = item.getStyle();
                if (currentStyle.contains("-fx-background-color: #e3f2fd;")) {
                    item.setStyle(currentStyle.replace(" -fx-background-color: #e3f2fd;", "")
                                             .replace("-fx-background-color: #e3f2fd;", "-fx-background-color: #ffffff;"));
                }
                if (item.getUserData() instanceof Product) {
                    Product product = (Product) item.getUserData();
                    if (product.getId() == selected.getId()) {
                        item.setStyle(item.getStyle() + " -fx-background-color: #e3f2fd;");
                    }
                }
            }
        }
    }
    
    private void showProductDetailPlaceholder(Product.ProductType type) {
        VBox detailContainer = type == Product.ProductType.VEG ? vegetablesDetailContainer : fruitsDetailContainer;
        detailContainer.getChildren().clear();
        Label placeholder = new Label("Select a product from the list to view details and add to cart");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
        detailContainer.getChildren().add(placeholder);
    }
    
    private VBox createProductDetailCard(Product product) {
        VBox card = new VBox(12);
        boolean isOutOfStock = product.getStockKg().compareTo(BigDecimal.ZERO) <= 0;
        
        String borderColor = isOutOfStock ? "#f44336" : "#e0e0e0";
        card.setStyle("-fx-padding: 18 20; -fx-border-color: " + borderColor + "; -fx-border-width: 1.5; -fx-border-radius: 8; " +
                      "-fx-background-color: #ffffff; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        String nameColor = isOutOfStock ? "#f44336" : "#1976d2";
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: " + nameColor + ";");
        
        String typeColor = product.getType() == Product.ProductType.FRUIT ? "#ff9800" : "#4caf50";
        Label typeLabel = new Label(product.getType().name());
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + typeColor + "; " +
                          "-fx-padding: 6 12; -fx-background-color: " + typeColor + "20; -fx-background-radius: 4;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Out of stock badge
        if (isOutOfStock) {
            Label outOfStockBadge = new Label("OUT OF STOCK");
            outOfStockBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ffffff; " +
                                    "-fx-padding: 6 12; -fx-background-color: #f44336; -fx-background-radius: 4;");
            header.getChildren().addAll(nameLabel, typeLabel, spacer, outOfStockBadge);
        } else {
            header.getChildren().addAll(nameLabel, typeLabel, spacer);
        }
        
        // Details
        VBox detailsBox = new VBox(10);
        
        // Price info
        HBox priceBox = new HBox(10);
        Label priceLabel = new Label("Price: " + formatPrice(product.getEffectivePrice()) + " TL/kg");
        priceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + nameColor + ";");
        
        if (product.isLowStock() && !product.getEffectivePrice().equals(product.getPrice())) {
            Label originalPriceLabel = new Label("(Original: " + formatPrice(product.getPrice()) + " TL/kg)");
            originalPriceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575; -fx-strikethrough: true;");
            priceBox.getChildren().addAll(priceLabel, originalPriceLabel);
        } else {
            priceBox.getChildren().add(priceLabel);
        }
        detailsBox.getChildren().add(priceBox);
        
        // Stock info
        String stockColor = isOutOfStock ? "#f44336" : (product.isLowStock() ? "#f44336" : "#4caf50");
        String stockText = isOutOfStock ? "OUT OF STOCK" : ("Available Stock: " + product.getStockKg() + " kg");
        Label stockLabel = new Label(stockText);
        stockLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + stockColor + "; " +
                           "-fx-font-weight: bold;");
        detailsBox.getChildren().add(stockLabel);
        
        if (isOutOfStock) {
            Label outOfStockWarning = new Label("⚠ This product is currently out of stock and cannot be added to cart.");
            outOfStockWarning.setStyle("-fx-font-size: 13px; -fx-text-fill: #f44336; -fx-font-weight: bold; -fx-padding: 10; " +
                                      "-fx-background-color: #ffebee; -fx-background-radius: 4;");
            outOfStockWarning.setWrapText(true);
            detailsBox.getChildren().add(outOfStockWarning);
        } else if (product.isLowStock()) {
            Label thresholdLabel = new Label("Threshold: " + product.getThresholdKg() + " kg");
            thresholdLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #757575;");
            detailsBox.getChildren().add(thresholdLabel);
            
            Label lowStockWarning = new Label("⚠ Low Stock - Price Doubled!");
            lowStockWarning.setStyle("-fx-font-size: 13px; -fx-text-fill: #f44336; -fx-font-weight: bold; -fx-padding: 8; " +
                                    "-fx-background-color: #ffebee; -fx-background-radius: 4;");
            detailsBox.getChildren().add(lowStockWarning);
        }
        
        // Add to cart section - only show if in stock
        if (!isOutOfStock) {
            VBox addToCartBox = new VBox(8);
            Label quantityLabel = new Label("Quantity (kg):");
            quantityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            HBox quantityBox = new HBox(10);
            TextField quantityField = new TextField();
            quantityField.setPromptText("Enter amount in kg (e.g., 1.5)");
            quantityField.setPrefWidth(200);
            quantityField.setStyle("-fx-font-size: 13px;");
            
            Button addToCartButton = new Button("Add to Cart");
            addToCartButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; " +
                                    "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
            
            Product finalProduct = product; // final variable for lambda
            addToCartButton.setOnAction(e -> {
                handleAddToCart(finalProduct, quantityField);
            });
            
            quantityBox.getChildren().addAll(quantityField, addToCartButton);
            quantityBox.setAlignment(Pos.CENTER_LEFT);
            
            addToCartBox.getChildren().addAll(quantityLabel, quantityBox);
            addToCartBox.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5; -fx-background-radius: 6;");
            
            card.getChildren().addAll(header, detailsBox, addToCartBox);
        } else {
            card.getChildren().addAll(header, detailsBox);
        }
        
        return card;
    }
    
    @FXML
    private void handleAddToCart(Product product, TextField quantityField) {
        // Get fresh product data from database to check current stock
        Product freshProduct = productDAO.getProductById(product.getId());
        if (freshProduct == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Product not found.");
            return;
        }
        
        // Check if product is out of stock
        if (freshProduct.getStockKg().compareTo(BigDecimal.ZERO) <= 0) {
            showAlert(Alert.AlertType.WARNING, "Out of Stock", 
                    "This product is currently out of stock and cannot be added to cart.");
            // Refresh products to update display
            loadProducts();
            return;
        }
        
        String input = quantityField.getText().trim();
        if (!InputValidator.isValidQuantity(input)) {
            showAlert(Alert.AlertType.ERROR, "Invalid Quantity", 
                    "Please enter a positive number for quantity.");
            return;
        }
        
        BigDecimal quantity = InputValidator.parseQuantity(input);
        
        if (quantity.compareTo(freshProduct.getStockKg()) > 0) {
            showAlert(Alert.AlertType.WARNING, "Insufficient Stock", 
                    "Stock available: " + freshProduct.getStockKg() + " kg");
            return;
        }
        
        // Check if product already in cart (merge quantities)
        CartItem existingItem = cart.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst()
                .orElse(null);
        
        if (existingItem != null) {
            BigDecimal newTotal = existingItem.getQuantityKg().add(quantity);
            if (newTotal.compareTo(freshProduct.getStockKg()) > 0) {
                showAlert(Alert.AlertType.WARNING, "Insufficient Stock", 
                        "Total quantity exceeds available stock.");
                return;
            }
            existingItem.addQuantity(quantity);
        } else {
            cart.add(new CartItem(freshProduct, quantity));
        }
        
        // Update stock in database
        boolean stockUpdated = productDAO.updateStock(product.getId(), quantity);
        if (!stockUpdated) {
            showAlert(Alert.AlertType.WARNING, "Stock Update Failed", 
                    "Failed to update stock. Please try again.");
            return;
        }
        
        // Refresh product display to show updated stock
        loadProducts();
        
        quantityField.clear();
        showAlert(Alert.AlertType.INFORMATION, "Added to Cart", 
                quantity + " kg of " + product.getName() + " added to cart.");
    }
    
    @FXML
    private void handleViewCart() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "User not logged in.");
            return;
        }
        
        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Empty Cart", "Your shopping cart is empty. Add items to continue.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/cart.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.setTitle("Shopping Cart - Group07 GreenGrocer");
            
            CartController controller = loader.getController();
            controller.setCart(cart);
            controller.setCustomerId(currentUser.getId());
            controller.setCurrentUser(currentUser);
            // Set callback to refresh products when cart changes
            controller.setOnCartChangeCallback(() -> loadProducts());
            
            // Refresh products when cart window closes
            stage.setOnCloseRequest(e -> loadProducts());
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open shopping cart: " + e.getMessage());
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
            // Display filtered search results in both tabs (including out-of-stock products)
            vegetablesListContainer.getChildren().clear();
            fruitsListContainer.getChildren().clear();
            vegetablesDetailContainer.getChildren().clear();
            fruitsDetailContainer.getChildren().clear();
            
            for (Product product : results) {
                // Show all products including out-of-stock ones
                VBox listContainer = product.getType() == Product.ProductType.VEG ? vegetablesListContainer : fruitsListContainer;
                HBox listItem = createProductListItem(product, product.getType());
                if (listItem != null) {
                    listItem.setPrefWidth(Double.MAX_VALUE);
                    listItem.setMaxWidth(Double.MAX_VALUE);
                    listContainer.getChildren().add(listItem);
                }
            }
            
            showProductDetailPlaceholder(Product.ProductType.VEG);
            showProductDetailPlaceholder(Product.ProductType.FRUIT);
        }
    }
    
    @FXML
    private void handleViewOrders() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "User not logged in.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/orders.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 800, 600));
            stage.setTitle("My Orders - Group07 GreenGrocer");
            
            OrdersController controller = loader.getController();
            controller.setCustomerId(currentUser.getId());
            controller.setCurrentUser(currentUser);
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open orders window: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleMessages() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "User not logged in.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/messages.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.setTitle("Messages - Group07 GreenGrocer");
            
            MessagesController controller = loader.getController();
            controller.setCustomerId(currentUser.getId());
            controller.setCurrentUser(currentUser);
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open messages window: " + e.getMessage());
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

