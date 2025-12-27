package com.example.demo.controllers;

import com.example.demo.dao.*;
import com.example.demo.models.Order;
import com.example.demo.models.Product;
import com.example.demo.models.User;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for the Owner UI.
 * Handles product management, carrier management, orders, messages, and reports.
 * 
 * @author Group07
 * @version 1.0
 */
public class OwnerController extends BaseController {
    @FXML private VBox productsListContainer;
    @FXML private VBox productDetailContainer;
    @FXML private VBox carriersListContainer;
    @FXML private VBox carrierDetailContainer;
    @FXML private VBox ordersListContainer;
    @FXML private VBox orderDetailContainer;
    @FXML private VBox messagesListContainer;
    @FXML private VBox messageDetailContainer;
    @FXML private Label ordersCountLabel;
    @FXML private Button logoutButton;
    
    private com.example.demo.models.Message selectedMessage;
    private Order selectedOrder;
    private Product selectedProduct;
    private User selectedCarrier;
    
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
        
        // Ensure containers are properly configured for multiple items
        if (ordersListContainer != null) {
            ordersListContainer.setFillWidth(true);
        }
        
        // Don't load messages here - wait for setCurrentUser to be called
        loadProducts();
        loadCarriers();
        loadOrders();
    }
    
    @Override
    public void setCurrentUser(User user) {
        super.setCurrentUser(user);
        System.out.println("DEBUG OwnerController: setCurrentUser called with user: " + (user != null ? user.getUsername() + " (ID: " + user.getId() + ")" : "null"));
        // Now that we have the user, load messages
        loadMessages();
    }
    
    private void loadProducts() {
        productsListContainer.getChildren().clear();
        productDetailContainer.getChildren().clear();
        selectedProduct = null;
        
        List<Product> products = productDAO.getAllProducts(null);
        products.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        
        if (products.isEmpty()) {
            Label noProductsLabel = new Label("No products in the system.");
            noProductsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            productsListContainer.getChildren().add(noProductsLabel);
            return;
        }
        
        for (Product product : products) {
            HBox listItem = createProductListItem(product);
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                productsListContainer.getChildren().add(listItem);
            }
        }
        
        productsListContainer.requestLayout();
        showProductDetailPlaceholder();
    }
    
    private HBox createProductListItem(Product product) {
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
        
        // Product name
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1976d2; -fx-cursor: hand;");
        nameLabel.setPrefWidth(200);
        
        // Type badge
        String typeColor = product.getType() == Product.ProductType.FRUIT ? "#ff9800" : "#4caf50";
        Label typeLabel = new Label(product.getType().name());
        typeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + typeColor + "; " +
                          "-fx-padding: 4 8; -fx-background-color: " + typeColor + "20; -fx-background-radius: 4;");
        
        // Price
        Label priceLabel = new Label(formatPrice(product.getPrice()) + " /kg");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
        priceLabel.setPrefWidth(120);
        
        // Stock
        String stockColor = product.isLowStock() ? "#f44336" : "#4caf50";
        Label stockLabel = new Label("Stock: " + product.getStockKg() + " kg");
        stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + stockColor + "; -fx-font-weight: " + 
                           (product.isLowStock() ? "bold;" : "normal;"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Status indicator
        Label statusLabel = new Label(product.isActive() ? "Active" : "Inactive");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (product.isActive() ? "#4caf50" : "#757575") + ";");
        
        item.getChildren().addAll(nameLabel, typeLabel, priceLabel, stockLabel, spacer, statusLabel);
        item.setOnMouseClicked(e -> showProductDetail(product));
        nameLabel.setOnMouseClicked(e -> showProductDetail(product));
        
        return item;
    }
    
    private void showProductDetail(Product product) {
        selectedProduct = product;
        productDetailContainer.getChildren().clear();
        
        VBox detailCard = createProductDetailCard(product);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            productDetailContainer.getChildren().add(detailCard);
        }
        
        updateProductListSelection(product);
    }
    
    private VBox createProductDetailCard(Product product) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #e0e0e0; -fx-border-width: 1.5; -fx-border-radius: 8; " +
                      "-fx-background-color: #ffffff; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #1976d2;");
        
        String typeColor = product.getType() == Product.ProductType.FRUIT ? "#ff9800" : "#4caf50";
        Label typeLabel = new Label(product.getType().name());
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + typeColor + "; " +
                          "-fx-padding: 6 12; -fx-background-color: " + typeColor + "20; -fx-background-radius: 4;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statusLabel = new Label(product.isActive() ? "ACTIVE" : "INACTIVE");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + 
                            (product.isActive() ? "#4caf50" : "#f44336") + ";");
        
        header.getChildren().addAll(nameLabel, typeLabel, spacer, statusLabel);
        
        // Details
        VBox detailsBox = new VBox(8);
        
        Label priceLabel = new Label("Price: " + formatPrice(product.getPrice()) + " TL/kg");
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        
        String stockColor = product.isLowStock() ? "#f44336" : "#4caf50";
        Label stockLabel = new Label("Stock: " + product.getStockKg() + " kg");
        stockLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + stockColor + "; " +
                           "-fx-font-weight: " + (product.isLowStock() ? "bold;" : "normal;"));
        
        Label thresholdLabel = new Label("Threshold: " + product.getThresholdKg() + " kg");
        thresholdLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #757575;");
        
        if (product.isLowStock()) {
            Label lowStockWarning = new Label("⚠ Low Stock Warning");
            lowStockWarning.setStyle("-fx-font-size: 13px; -fx-text-fill: #f44336; -fx-font-weight: bold; -fx-padding: 8; " +
                                    "-fx-background-color: #ffebee; -fx-background-radius: 4;");
            detailsBox.getChildren().add(lowStockWarning);
        }
        
        detailsBox.getChildren().addAll(priceLabel, stockLabel, thresholdLabel);
        
        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button editButton = new Button("Edit Product");
        editButton.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-font-weight: bold; " +
                           "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
        editButton.setOnAction(e -> handleEditProduct(product));
        
        Button removeButton = new Button("Remove Product");
        removeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; " +
                             "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
        removeButton.setOnAction(e -> handleRemoveProduct(product));
        
        buttonBox.getChildren().addAll(editButton, removeButton);
        
        card.getChildren().addAll(header, detailsBox, buttonBox);
        return card;
    }
    
    private void updateProductListSelection(Product selected) {
        for (javafx.scene.Node node : productsListContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                String currentStyle = item.getStyle();
                if (currentStyle.contains("-fx-background-color: #e3f2fd;")) {
                    item.setStyle(currentStyle.replace(" -fx-background-color: #e3f2fd;", "")
                                             .replace("-fx-background-color: #e3f2fd;", "-fx-background-color: #ffffff;"));
                }
                if (item.getUserData() != null && item.getUserData().equals(selected.getId())) {
                    item.setStyle(item.getStyle() + " -fx-background-color: #e3f2fd;");
                }
            }
        }
    }
    
    private void showProductDetailPlaceholder() {
        productDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select a product from the list to view/edit details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
        productDetailContainer.getChildren().add(placeholder);
    }
    
    private void loadCarriers() {
        carriersListContainer.getChildren().clear();
        carrierDetailContainer.getChildren().clear();
        selectedCarrier = null;
        
        List<User> carriers = userDAO.getAllCarriers();
        carriers.sort((c1, c2) -> c1.getUsername().compareToIgnoreCase(c2.getUsername()));
        
        if (carriers.isEmpty()) {
            Label noCarriersLabel = new Label("No carriers in the system.");
            noCarriersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            carriersListContainer.getChildren().add(noCarriersLabel);
            return;
        }
        
        for (User carrier : carriers) {
            HBox listItem = createCarrierListItem(carrier);
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                carriersListContainer.getChildren().add(listItem);
            }
        }
        
        carriersListContainer.requestLayout();
        showCarrierDetailPlaceholder();
    }
    
    private HBox createCarrierListItem(User carrier) {
        HBox item = new HBox(10);
        item.setStyle("-fx-padding: 10 12; -fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; " +
                      "-fx-cursor: hand;");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefWidth(Double.MAX_VALUE);
        item.setUserData(carrier.getId());
        
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
        
        // Carrier name
        Label nameLabel = new Label(carrier.getUsername());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1976d2; -fx-cursor: hand;");
        nameLabel.setPrefWidth(200);
        
        // Status badge
        String statusColor = carrier.isActive() ? "#4caf50" : "#757575";
        Label statusLabel = new Label(carrier.isActive() ? "ACTIVE" : "INACTIVE");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + "; " +
                            "-fx-padding: 4 8; -fx-background-color: " + statusColor + "20; -fx-background-radius: 4;");
        
        // Phone
        Label phoneLabel = new Label(carrier.getPhone() != null ? carrier.getPhone() : "No phone");
        phoneLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
        phoneLabel.setPrefWidth(150);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        item.getChildren().addAll(nameLabel, statusLabel, phoneLabel, spacer);
        item.setOnMouseClicked(e -> showCarrierDetail(carrier));
        nameLabel.setOnMouseClicked(e -> showCarrierDetail(carrier));
        
        return item;
    }
    
    private void showCarrierDetail(User carrier) {
        selectedCarrier = carrier;
        carrierDetailContainer.getChildren().clear();
        
        VBox detailCard = createCarrierDetailCard(carrier);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            carrierDetailContainer.getChildren().add(detailCard);
        }
        
        updateCarrierListSelection(carrier);
    }
    
    private VBox createCarrierDetailCard(User carrier) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #e0e0e0; -fx-border-width: 1.5; -fx-border-radius: 8; " +
                      "-fx-background-color: #ffffff; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(carrier.getUsername());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #1976d2;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        String statusColor = carrier.isActive() ? "#4caf50" : "#757575";
        Label statusLabel = new Label(carrier.isActive() ? "ACTIVE" : "INACTIVE");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + "; " +
                            "-fx-padding: 6 12; -fx-background-color: " + statusColor + "20; -fx-background-radius: 4;");
        
        header.getChildren().addAll(nameLabel, spacer, statusLabel);
        
        // Details
        VBox detailsBox = new VBox(8);
        
        Label phoneLabel = new Label("Phone: " + (carrier.getPhone() != null ? carrier.getPhone() : "Not provided"));
        phoneLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        
        Label addressLabel = new Label("Address: " + (carrier.getAddress() != null ? carrier.getAddress() : "Not provided"));
        addressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121;");
        addressLabel.setWrapText(true);
        
        detailsBox.getChildren().addAll(phoneLabel, addressLabel);
        
        // Action button
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button toggleButton = new Button(carrier.isActive() ? "Fire Carrier" : "Hire Carrier");
        String buttonColor = carrier.isActive() ? "#f44336" : "#4caf50";
        toggleButton.setStyle("-fx-background-color: " + buttonColor + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                             "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
        toggleButton.setOnAction(e -> {
            handleToggleCarrier(carrier);
            loadCarriers(); // Reload to show updated status
            if (selectedCarrier != null && selectedCarrier.getId() == carrier.getId()) {
                // Update selected carrier after status change
                User updatedCarrier = userDAO.getUserById(carrier.getId());
                if (updatedCarrier != null) {
                    showCarrierDetail(updatedCarrier);
                }
            }
        });
        
        buttonBox.getChildren().add(toggleButton);
        
        card.getChildren().addAll(header, detailsBox, buttonBox);
        return card;
    }
    
    private void updateCarrierListSelection(User selected) {
        for (javafx.scene.Node node : carriersListContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                String currentStyle = item.getStyle();
                if (currentStyle.contains("-fx-background-color: #e3f2fd;")) {
                    item.setStyle(currentStyle.replace(" -fx-background-color: #e3f2fd;", "")
                                             .replace("-fx-background-color: #e3f2fd;", "-fx-background-color: #ffffff;"));
                }
                if (item.getUserData() != null && item.getUserData().equals(selected.getId())) {
                    item.setStyle(item.getStyle() + " -fx-background-color: #e3f2fd;");
                }
            }
        }
    }
    
    private void showCarrierDetailPlaceholder() {
        carrierDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select a carrier from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
        carrierDetailContainer.getChildren().add(placeholder);
    }
    
    private void loadOrders() {
        ordersListContainer.getChildren().clear();
        orderDetailContainer.getChildren().clear();
        selectedOrder = null;
        
        // Ensure container is properly configured
        ordersListContainer.setFillWidth(true);
        
        System.out.println("DEBUG OwnerController: ========== LOADING ORDERS ==========");
        System.out.println("DEBUG OwnerController: Calling orderDAO.getAllOrders()...");
        
        List<Order> orders = orderDAO.getAllOrders();
        System.out.println("DEBUG OwnerController: Received " + orders.size() + " total orders from OrderDAO");
        
        if (orders.isEmpty()) {
            Label noOrdersLabel = new Label("No orders in the system yet.");
            noOrdersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            ordersListContainer.getChildren().add(noOrdersLabel);
            System.out.println("DEBUG OwnerController: No orders found in database");
            if (ordersCountLabel != null) {
                ordersCountLabel.setText("All Orders (0 total)");
            }
            return;
        }
        
        // Count orders by status
        Map<Order.OrderStatus, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        System.out.println("DEBUG OwnerController: Order status breakdown: " + statusCounts);
        
        // Verify all orders have items (but still display orders without items)
        int ordersWithoutItems = 0;
        for (Order order : orders) {
            if (order.getItems() == null || order.getItems().isEmpty()) {
                ordersWithoutItems++;
                System.err.println("WARNING OwnerController: Order #" + order.getId() + " has no items!");
            }
        }
        if (ordersWithoutItems > 0) {
            System.err.println("WARNING OwnerController: Found " + ordersWithoutItems + " order(s) without items! (Still displaying them)");
        }
        
        // Sort orders by ID descending (newest first)
        orders.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        
        System.out.println("DEBUG OwnerController: Creating list items for " + orders.size() + " orders...");
        
        // Create clickable list items
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        int itemsCreated = 0;
        int itemsFailed = 0;
        for (Order order : orders) {
            try {
                System.out.println("DEBUG OwnerController: Creating list item for order #" + order.getId());
                HBox listItem = createOrderListItem(order, formatter);
                if (listItem != null) {
                    listItem.setPrefWidth(Double.MAX_VALUE);
                    listItem.setMaxWidth(Double.MAX_VALUE);
                    listItem.setMinWidth(0); // Ensure items can shrink if needed
                    ordersListContainer.getChildren().add(listItem);
                    itemsCreated++;
                    System.out.println("DEBUG OwnerController: Successfully added list item for order #" + order.getId() + " (Total items in container: " + ordersListContainer.getChildren().size() + ")");
                } else {
                    System.err.println("ERROR OwnerController: Failed to create list item for order #" + order.getId() + " - createOrderListItem returned null");
                    itemsFailed++;
                }
            } catch (Exception e) {
                System.err.println("ERROR OwnerController: Exception while creating list item for order #" + order.getId() + ": " + e.getMessage());
                e.printStackTrace();
                itemsFailed++;
            }
        }
        
        System.out.println("DEBUG OwnerController: ========== ORDER DISPLAY SUMMARY ==========");
        System.out.println("DEBUG OwnerController: Total orders retrieved from database: " + orders.size());
        System.out.println("DEBUG OwnerController: List items created successfully: " + itemsCreated);
        System.out.println("DEBUG OwnerController: List items failed: " + itemsFailed);
        System.out.println("DEBUG OwnerController: UI container children count: " + ordersListContainer.getChildren().size());
        System.out.println("DEBUG OwnerController: Orders by status: " + statusCounts);
        System.out.println("DEBUG OwnerController: ============================================");
        
        if (itemsCreated != orders.size()) {
            System.err.println("ERROR OwnerController: Mismatch! Expected " + orders.size() + " items but created " + itemsCreated + ", failed: " + itemsFailed);
            System.err.println("ERROR OwnerController: Some orders may not be displayed!");
        } else {
            System.out.println("DEBUG OwnerController: ✓ SUCCESS - All " + orders.size() + " orders are displayed in the UI");
        }
        
        // Update the orders count label
        if (ordersCountLabel != null) {
            ordersCountLabel.setText("All Orders (" + orders.size() + " total)");
        }
        
        // Force layout update
        ordersListContainer.requestLayout();
        
        // Also trigger a layout pass on the parent ScrollPane if it exists
        javafx.application.Platform.runLater(() -> {
            ordersListContainer.requestLayout();
            if (ordersListContainer.getParent() != null) {
                ordersListContainer.getParent().requestLayout();
            }
        });
        
        // Show placeholder in detail view
        showOrderDetailPlaceholder();
    }
    
    /**
     * Creates a clickable list item for an order.
     */
    private HBox createOrderListItem(Order order, java.time.format.DateTimeFormatter formatter) {
        try {
            HBox item = new HBox(10);
            item.setStyle("-fx-padding: 10 12; -fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; " +
                          "-fx-cursor: hand;");
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPrefWidth(Double.MAX_VALUE);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setMinWidth(0);
            item.setUserData(order.getId()); // Store order ID for selection
        
        // Determine status color
        String statusColor;
        switch (order.getStatus()) {
            case DELIVERED:
                statusColor = "#4caf50"; // Green
                break;
            case ASSIGNED:
                statusColor = "#ff9800"; // Orange
                break;
            case CREATED:
                statusColor = "#2196f3"; // Blue
                break;
            case CANCELLED:
                statusColor = "#f44336"; // Red
                break;
            default:
                statusColor = "#757575"; // Gray
        }
        
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
        Label statusLabel = new Label(order.getStatus().name());
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + "; " +
                            "-fx-padding: 4 8; -fx-background-color: " + statusColor + "20; -fx-background-radius: 4;");
        
        // Date
        Label dateLabel = new Label(order.getOrderTime().format(formatter));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        dateLabel.setPrefWidth(150);
        
        // Customer name
        User customer = null;
        try {
            customer = userDAO.getUserById(order.getCustomerId());
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
            
            // Make entire item clickable
            item.setOnMouseClicked(e -> showOrderDetail(order, formatter));
            idLabel.setOnMouseClicked(e -> showOrderDetail(order, formatter));
            
            return item;
        } catch (Exception e) {
            System.err.println("ERROR OwnerController: Exception in createOrderListItem for order #" + order.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Shows the detail view for a selected order.
     */
    private void showOrderDetail(Order order, java.time.format.DateTimeFormatter formatter) {
        selectedOrder = order;
        orderDetailContainer.getChildren().clear();
        
        System.out.println("DEBUG OwnerController: Showing detail for order ID: " + order.getId());
        
        VBox detailCard = createOrderDisplay(order);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            orderDetailContainer.getChildren().add(detailCard);
        }
        
        // Highlight selected item in list
        updateOrderListSelection(order);
    }
    
    /**
     * Updates the visual selection in the order list.
     */
    private void updateOrderListSelection(Order selected) {
        for (javafx.scene.Node node : ordersListContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                // Remove selection style from all items
                String currentStyle = item.getStyle();
                if (currentStyle.contains("-fx-background-color: #e3f2fd;")) {
                    item.setStyle(currentStyle.replace(" -fx-background-color: #e3f2fd;", "")
                                             .replace("-fx-background-color: #e3f2fd;", "-fx-background-color: #ffffff;"));
                }
                // Check if this item corresponds to the selected order
                if (item.getUserData() != null && item.getUserData().equals(selected.getId())) {
                    item.setStyle(item.getStyle() + " -fx-background-color: #e3f2fd;");
                }
            }
        }
    }
    
    /**
     * Shows placeholder text in the detail view.
     */
    private void showOrderDetailPlaceholder() {
        orderDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select an order from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
        orderDetailContainer.getChildren().add(placeholder);
    }
    
    /**
     * Creates a detailed order display card (used in detail view).
     */
    private VBox createOrderDisplay(Order order) {
        VBox box = new VBox(10);
        
        // Different styling based on order status
        String borderColor;
        String bgColor;
        switch (order.getStatus()) {
            case DELIVERED:
                borderColor = "#4caf50"; // Green
                bgColor = "#f1f8f4";
                break;
            case ASSIGNED:
                borderColor = "#ff9800"; // Orange
                bgColor = "#fff3e0";
                break;
            case CREATED:
                borderColor = "#2196f3"; // Blue
                bgColor = "#e3f2fd";
                break;
            case CANCELLED:
                borderColor = "#f44336"; // Red
                bgColor = "#ffebee";
                break;
            default:
                borderColor = "#cccccc"; // Gray
                bgColor = "#f9f9f9";
        }
        
        box.setStyle("-fx-padding: 18 20; -fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: " + bgColor + "; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        box.setPrefWidth(Double.MAX_VALUE);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMinWidth(0);
        box.setSpacing(12);
        
        // Header with Order ID and Status
        HBox header = new HBox(10);
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label statusLabel = new Label(order.getStatus().name());
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + borderColor + ";");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        // Order details
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        Label requestedDeliveryLabel = new Label("Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
        
        // Build children list in correct order
        java.util.List<javafx.scene.Node> children = new java.util.ArrayList<>();
        children.add(header);
        children.add(orderDateLabel);
        children.add(requestedDeliveryLabel);
        
        // Customer info
        try {
            User customer = userDAO.getUserById(order.getCustomerId());
            if (customer != null) {
                Label customerLabel = new Label("Customer: " + customer.getUsername());
                children.add(customerLabel);
            }
        } catch (Exception e) {
            // Continue without customer info
        }
        
        // Carrier info if assigned
        if (order.getCarrierId() != null && order.getCarrierId() > 0) {
            try {
                User carrier = userDAO.getUserById(order.getCarrierId());
                if (carrier != null) {
                    Label carrierLabel = new Label("Carrier: " + carrier.getUsername());
                    children.add(carrierLabel);
                }
            } catch (Exception e) {
                // Continue without carrier info
            }
        }
        
        // Delivered time if delivered
        if (order.getDeliveredTime() != null) {
            Label deliveredLabel = new Label("Delivered: " + order.getDeliveredTime().format(formatter));
            deliveredLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            children.add(deliveredLabel);
        }
        
        // Order items summary
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsHeader = new Label("Items:");
            itemsHeader.setStyle("-fx-font-weight: bold;");
            
            VBox itemsBox = new VBox(3);
            itemsBox.setStyle("-fx-padding: 0 0 0 20;");
            for (com.example.demo.models.OrderItem item : order.getItems()) {
                String itemName = item.getProduct() != null 
                    ? item.getProduct().getName() 
                    : "Product #" + item.getProductId();
                Label itemLabel = new Label(String.format("  • %s: %.2f kg @ %.2f TL = %.2f TL",
                        itemName,
                        item.getQuantityKg().doubleValue(),
                        item.getUnitPriceApplied().doubleValue(),
                        item.getLineTotal().doubleValue()));
                itemsBox.getChildren().add(itemLabel);
            }
            
            children.add(itemsHeader);
            children.add(itemsBox);
        }
        
        // Total
        Label totalLabel = new Label("Total: " + formatPrice(order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        children.add(totalLabel);
        
        // Add all children at once
        box.getChildren().addAll(children);
        
        return box;
    }
    
    private void loadMessages() {
        messagesListContainer.getChildren().clear();
        messageDetailContainer.getChildren().clear();
        selectedMessage = null;
        
        System.out.println("DEBUG OwnerController: loadMessages called. currentUser: " + (currentUser != null ? currentUser.getUsername() + " (ID: " + currentUser.getId() + ")" : "null"));
        
        if (currentUser == null || currentUser.getId() == 0) {
            Label label = new Label("Owner not logged in. Please refresh after logging in.");
            label.setStyle("-fx-font-size: 14px; -fx-text-fill: red;");
            messagesListContainer.getChildren().add(label);
            System.out.println("DEBUG OwnerController: Owner not logged in, showing message");
            return;
        }
        
        // Fetch messages for all active owners
        System.out.println("DEBUG OwnerController: Fetching messages for all active owners");
        List<com.example.demo.models.Message> allMessages = messageDAO.getMessagesForAllOwners();
        System.out.println("DEBUG OwnerController: Total messages retrieved: " + allMessages.size());
        
        // Filter to only English messages
        List<com.example.demo.models.Message> messages = allMessages.stream()
                .filter(msg -> isEnglishText(msg.getText()))
                .collect(java.util.stream.Collectors.toList());
        
        System.out.println("DEBUG OwnerController: After filtering for English: " + messages.size() + " messages remain");
        
        if (messages.isEmpty()) {
            Label label = new Label("No English messages received yet.");
            label.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
            messagesListContainer.getChildren().add(label);
            System.out.println("DEBUG OwnerController: No English messages found");
            return;
        }
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        // Create clickable list items
        for (com.example.demo.models.Message message : messages) {
            HBox listItem = createMessageListItem(message, formatter);
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                messagesListContainer.getChildren().add(listItem);
            }
        }
        
        System.out.println("DEBUG OwnerController: FINAL - Total message list items: " + messagesListContainer.getChildren().size());
        messagesListContainer.requestLayout();
        
        // Show placeholder in detail view
        showMessageDetailPlaceholder();
    }
    
    /**
     * Creates a clickable list item for a message.
     */
    private HBox createMessageListItem(com.example.demo.models.Message message, java.time.format.DateTimeFormatter formatter) {
        HBox item = new HBox(10);
        item.setStyle("-fx-padding: 10 12; -fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; " +
                      "-fx-cursor: hand;");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefWidth(Double.MAX_VALUE);
        item.setUserData(message.getId()); // Store message ID for selection
        
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
        
        // Customer name - clickable
        User customer = userDAO.getUserById(message.getCustomerId());
        String customerName = customer != null ? customer.getUsername() : "Customer #" + message.getCustomerId();
        Label nameLabel = new Label("From: " + customerName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1976d2; -fx-cursor: hand;");
        nameLabel.setPrefWidth(180);
        
        // Date
        Label dateLabel = new Label(message.getCreatedAt().format(formatter));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        
        // Preview of message (first 40 chars)
        String preview = message.getText();
        if (preview.length() > 40) {
            preview = preview.substring(0, 40) + "...";
        }
        Label previewLabel = new Label(preview);
        previewLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
        previewLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(previewLabel, javafx.scene.layout.Priority.ALWAYS);
        
        // Reply indicator
        if (message.hasReply()) {
            Label repliedLabel = new Label("✓");
            repliedLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4caf50; -fx-font-weight: bold;");
            item.getChildren().addAll(nameLabel, dateLabel, previewLabel, repliedLabel);
        } else {
            item.getChildren().addAll(nameLabel, dateLabel, previewLabel);
        }
        
        // Make entire item clickable
        item.setOnMouseClicked(e -> showMessageDetail(message, formatter));
        // Also make name label clickable
        nameLabel.setOnMouseClicked(e -> showMessageDetail(message, formatter));
        
        return item;
    }
    
    /**
     * Shows the detail view for a selected message.
     */
    private void showMessageDetail(com.example.demo.models.Message message, java.time.format.DateTimeFormatter formatter) {
        selectedMessage = message;
        messageDetailContainer.getChildren().clear();
        
        System.out.println("DEBUG OwnerController: Showing detail for message ID: " + message.getId() + ", hasReply: " + message.hasReply());
        
        VBox detailCard = createMessageCard(message, formatter);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            messageDetailContainer.getChildren().add(detailCard);
        }
        
        // Highlight selected item in list
        updateListSelection(message);
    }
    
    /**
     * Updates the visual selection in the message list.
     */
    private void updateListSelection(com.example.demo.models.Message selected) {
        for (javafx.scene.Node node : messagesListContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                // Remove selection style from all items
                String currentStyle = item.getStyle();
                if (currentStyle.contains("-fx-background-color: #e3f2fd;")) {
                    item.setStyle(currentStyle.replace(" -fx-background-color: #e3f2fd;", "")
                                             .replace("-fx-background-color: #e3f2fd;", "-fx-background-color: #ffffff;"));
                }
                // Check if this item corresponds to the selected message
                if (item.getUserData() != null && item.getUserData().equals(selected.getId())) {
                    item.setStyle(item.getStyle() + " -fx-background-color: #e3f2fd;");
                }
            }
        }
    }
    
    /**
     * Shows placeholder text in the detail view.
     */
    private void showMessageDetailPlaceholder() {
        messageDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select a message from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20;");
        messageDetailContainer.getChildren().add(placeholder);
    }
    
    /**
     * Checks if text is in English (contains primarily ASCII characters and common English patterns).
     * 
     * @param text the text to check
     * @return true if text appears to be English
     */
    private boolean isEnglishText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Remove common punctuation and whitespace for analysis
        String cleaned = text.replaceAll("[^\\p{ASCII}]", "");
        
        // If more than 30% of characters are non-ASCII, likely not English
        if (text.length() > 0 && (double) cleaned.length() / text.length() < 0.7) {
            return false;
        }
        
        // Check for common non-English characters (Turkish, Arabic, etc.)
        // Turkish: ç, ğ, ı, ö, ş, ü
        // Other common non-ASCII: é, è, ñ, etc.
        String nonEnglishPattern = "[çÇğĞıİöÖşŞüÜáÁéÉíÍóÓúÚñÑàÀèÈìÌòÒùÙ]";
        if (text.matches(".*" + nonEnglishPattern + ".*")) {
            return false;
        }
        
        return true;
    }
    
    private VBox createMessageCard(com.example.demo.models.Message message, java.time.format.DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #e0e0e0; -fx-border-width: 1.5; -fx-border-radius: 8; " +
                      "-fx-background-color: #ffffff; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(0);
        card.setSpacing(12);
        
        // Header with customer info and timestamp
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        User customer = userDAO.getUserById(message.getCustomerId());
        String customerName = customer != null ? customer.getUsername() : "Customer #" + message.getCustomerId();
        
        Label customerLabel = new Label(customerName);
        customerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1976d2;");
        
        Label dateLabel = new Label(" • " + message.getCreatedAt().format(formatter));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        header.getChildren().addAll(customerLabel, dateLabel, spacer);
        
        // Message text - use Label for better appearance
        Label messageLabel = new Label(message.getText());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #212121; -fx-padding: 12; " +
                              "-fx-background-color: #f5f5f5; -fx-background-radius: 6;");
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        
        // Reply section
        VBox replySection = new VBox(8);
        
        if (message.hasReply()) {
            Label replyHeader = new Label("Your Reply:");
            replyHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #388e3c;");
            
            Label replyLabel = new Label(message.getReply());
            replyLabel.setWrapText(true);
            replyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2e7d32; -fx-padding: 10; " +
                                "-fx-background-color: #e8f5e9; -fx-background-radius: 6;");
            replyLabel.setMaxWidth(Double.MAX_VALUE);
            
            Label replyDateLabel = new Label("Replied: " + message.getRepliedAt().format(formatter));
            replyDateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575; -fx-padding: 4 0 0 0;");
            
            replySection.getChildren().addAll(replyHeader, replyLabel, replyDateLabel);
        } else {
            TextArea replyTextArea = new TextArea();
            replyTextArea.setPromptText("Type your reply here...");
            replyTextArea.setWrapText(true);
            replyTextArea.setPrefRowCount(3);
            replyTextArea.setMaxHeight(120);
            replyTextArea.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; " +
                                   "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 4;");
            
            Button replyButton = new Button("Send Reply");
            replyButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; " +
                                 "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
            replyButton.setOnAction(e -> handleReplyToMessage(message, replyTextArea));
            
            HBox replyButtonBox = new HBox(10);
            replyButtonBox.setAlignment(Pos.CENTER_RIGHT);
            replyButtonBox.getChildren().add(replyButton);
            
            Label replyLabel = new Label("Reply:");
            replyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #424242;");
            
            replySection.getChildren().addAll(replyLabel, replyTextArea, replyButtonBox);
        }
        
        card.getChildren().addAll(header, messageLabel, replySection);
        
        return card;
    }
    
    private void handleReplyToMessage(com.example.demo.models.Message message, TextArea replyTextArea) {
        String replyText = replyTextArea.getText().trim();
        
        if (replyText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Reply", "Please enter a reply before sending.");
            return;
        }
        
        System.out.println("DEBUG OwnerController: ========== REPLY ATTEMPT START ==========");
        System.out.println("DEBUG OwnerController: Message object: " + message);
        System.out.println("DEBUG OwnerController: Message ID: " + message.getId());
        System.out.println("DEBUG OwnerController: Message Customer ID: " + message.getCustomerId());
        System.out.println("DEBUG OwnerController: Message Owner ID: " + message.getOwnerId());
        System.out.println("DEBUG OwnerController: Reply text length: " + replyText.length());
        System.out.println("DEBUG OwnerController: Reply text: " + replyText);
        
        // Verify message exists in database before attempting update
        try {
            com.example.demo.models.Message dbMessage = messageDAO.getMessagesForAllOwners().stream()
                    .filter(m -> m.getId() == message.getId())
                    .findFirst()
                    .orElse(null);
            
            if (dbMessage == null) {
                System.err.println("ERROR OwnerController: Message ID " + message.getId() + " not found in database!");
                showAlert(Alert.AlertType.ERROR, "Error", "Message not found in database. Please refresh and try again.");
                return;
            } else {
                System.out.println("DEBUG OwnerController: Verified message exists in database");
            }
        } catch (Exception e) {
            System.err.println("ERROR OwnerController: Exception while verifying message: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            boolean success = messageDAO.replyToMessage(message.getId(), replyText);
            System.out.println("DEBUG OwnerController: Reply operation result: " + success);
            
            if (success) {
                System.out.println("DEBUG OwnerController: Reply was successful, reloading messages...");
                showAlert(Alert.AlertType.INFORMATION, "Reply Sent", "Your reply has been sent successfully!");
                
                // Store the message ID before reloading
                int messageId = message.getId();
                
                // Small delay to ensure database commit completes
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Reload messages
                loadMessages();
                
                // Reselect the message to show the updated reply
                reselectMessageById(messageId);
                System.out.println("DEBUG OwnerController: ========== REPLY SUCCESS ==========");
            } else {
                System.err.println("ERROR OwnerController: Reply operation returned false");
                System.err.println("ERROR OwnerController: Please check MessageDAO debug output above for details");
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to send reply. Please check the console for details and try again.");
                System.out.println("DEBUG OwnerController: ========== REPLY FAILED ==========");
            }
        } catch (Exception e) {
            System.err.println("ERROR OwnerController: Exception while sending reply: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while sending reply: " + e.getMessage());
            System.out.println("DEBUG OwnerController: ========== REPLY EXCEPTION ==========");
        }
    }
    
    /**
     * Reselects a message by ID after reloading the message list.
     */
    private void reselectMessageById(int messageId) {
        // Get all messages again to find the updated one
        List<com.example.demo.models.Message> allMessages = messageDAO.getMessagesForAllOwners();
        List<com.example.demo.models.Message> messages = allMessages.stream()
                .filter(msg -> isEnglishText(msg.getText()))
                .collect(java.util.stream.Collectors.toList());
        
        // Find the message with the matching ID
        com.example.demo.models.Message foundMessage = null;
        for (com.example.demo.models.Message msg : messages) {
            if (msg.getId() == messageId) {
                foundMessage = msg;
                break;
            }
        }
        
        if (foundMessage != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            showMessageDetail(foundMessage, formatter);
            System.out.println("DEBUG OwnerController: Reselected message ID: " + messageId);
        } else {
            System.out.println("DEBUG OwnerController: Could not find message ID: " + messageId + " after reload");
        }
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
                        loadProducts();
                    } else {
                        product.setName(nameField.getText());
                        product.setType(type);
                        product.setPrice(price);
                        product.setStockKg(stock);
                        product.setThresholdKg(threshold);
                        productDAO.updateProduct(product);
                        // Reload products and reselect the edited product
                        loadProducts();
                        // Reselect the product if it still exists
                        Product updatedProduct = productDAO.getProductById(product.getId());
                        if (updatedProduct != null) {
                            showProductDetail(updatedProduct);
                        }
                    }
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
            loadProducts(); // This will clear the detail view and reset selection
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
    private void handleRefreshMessages() {
        loadMessages();
    }
    
    @FXML
    private void handleRefreshOrders() {
        System.out.println("DEBUG OwnerController: Refresh button clicked - reloading all orders");
        loadOrders();
    }
    
    @FXML
    private void handleGenerateReport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/reports.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 900, 700));
            stage.setTitle("Reports - Group07 GreenGrocer");
            
            ReportsController controller = loader.getController();
            controller.setOrderDAO(orderDAO);
            controller.setProductDAO(productDAO);
            controller.loadReports();
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open reports window: " + e.getMessage());
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
    
    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}

