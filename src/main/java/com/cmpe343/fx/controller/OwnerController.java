package com.cmpe343.fx.controller;

import com.cmpe343.dao.*;
import com.cmpe343.model.*;
import com.cmpe343.model.Order.OrderStatus;
import com.cmpe343.model.Product.ProductType;
import com.cmpe343.fx.Session;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import com.cmpe343.fx.util.ToastService;
import javafx.util.Duration;

public class OwnerController {
    @FXML
    private VBox productsListContainer;
    @FXML
    private VBox productDetailContainer;
    @FXML
    private VBox carriersListContainer;
    @FXML
    private VBox carrierDetailContainer;
    @FXML
    private VBox ordersListContainer;
    @FXML
    private VBox orderDetailContainer;
    @FXML
    private VBox messagesListContainer;
    @FXML
    private VBox messageDetailContainer;
    @FXML
    private VBox couponsListContainer;
    @FXML
    private VBox couponDetailContainer;
    @FXML
    private VBox ratingsContainer;
    @FXML
    private VBox loyaltyContainer;
    @FXML
    private Label ordersCountLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private javafx.scene.layout.StackPane chartContainer;
    @FXML
    private FlowPane dashboardContainer;

    private MessageDao.Conversation selectedConversation;
    private Order selectedOrder;
    private Product selectedProduct;
    private User selectedCarrier;
    private Coupon selectedCoupon;

    private ProductDao productDAO;
    private UserDao userDAO;
    private OrderDao orderDAO;
    private MessageDao messageDAO;
    private CouponDao couponDAO;
    private RatingDao ratingDAO;

    @FXML
    public void initialize() {
        productDAO = new ProductDao();
        userDAO = new UserDao();
        orderDAO = new OrderDao();
        messageDAO = new MessageDao();
        couponDAO = new CouponDao();
        ratingDAO = new RatingDao();

        if (ordersListContainer != null)
            ordersListContainer.setFillWidth(true);

        loadDashboard();
        loadProducts();
        loadCarriers();
        loadOrders();
        loadCoupons();
        loadCarrierRatings();
        handleRefreshLoyalty();
        if (Session.isLoggedIn()) {
            loadConversations();
        }
    }

    // ==================== PRODUCT MANAGEMENT ====================

    private void loadProducts() {
        productsListContainer.getChildren().clear();
        productDetailContainer.getChildren().clear();
        selectedProduct = null;

        List<Product> products = productDAO.findAll();
        products.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

        if (products.isEmpty()) {
            productsListContainer.getChildren().add(createPlaceholder("No products in the system."));
            return;
        }

        for (Product product : products) {
            HBox listItem = createProductListItem(product);
            if (listItem != null)
                productsListContainer.getChildren().add(listItem);
        }
    }

    private HBox createProductListItem(Product product) {
        HBox item = createListItemBase();
        item.setUserData(product.getId());

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("detail-value");
        nameLabel.setStyle("-fx-font-weight: bold;");
        nameLabel.setPrefWidth(200);

        Label typeLabel = new Label(product.getType().name());
        typeLabel.getStyleClass().addAll("badge",
                product.getType() == ProductType.FRUIT ? "badge-warning" : "badge-success");

        Label priceLabel = new Label(formatPrice(product.getPrice()) + "/kg");
        priceLabel.getStyleClass().add("detail-value");
        priceLabel.setPrefWidth(120);

        Label stockLabel = new Label("Stock: " + product.getStockKg() + " kg");
        stockLabel.getStyleClass().addAll("badge", product.isLowStock() ? "badge-danger" : "badge-success");
        // Ensure white text for danger badge if not covered by class
        if (product.isLowStock())
            stockLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        item.getChildren().addAll(nameLabel, typeLabel, priceLabel, stockLabel, spacer);
        item.setOnMouseClicked(e -> showProductDetail(product));
        return item;
    }

    private void showProductDetail(Product product) {
        selectedProduct = product;
        productDetailContainer.getChildren().clear();

        VBox card = new VBox(12);
        card.getStyleClass().add("detail-card");

        Label name = new Label(product.getName());
        name.getStyleClass().add("detail-header");

        VBox meta = new VBox(5);
        meta.getChildren().addAll(
                createDetailRow("Type", product.getType().name()),
                createDetailRow("Price", formatPrice(product.getPrice())),
                createDetailRow("Stock", String.format("%.2f kg", product.getStockKg())),
                createDetailRow("Threshold", String.format("%.2f kg", product.getThresholdKg())));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setOnAction(e -> handleEditProduct(product));

        Button deleteBtn = new Button("Remove");
        deleteBtn.getStyleClass().add("btn-outline");
        deleteBtn.setStyle("-fx-border-color: #ef4444; -fx-text-fill: #ef4444;");
        deleteBtn.setOnAction(e -> handleRemoveProduct(product));

        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(name, meta, actions);
        productDetailContainer.getChildren().add(card);
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label + ":");
        l.getStyleClass().add("detail-label");
        l.setPrefWidth(120);
        l.setStyle("-fx-font-weight: 600;");

        Label v = new Label(value);
        v.getStyleClass().add("detail-value");
        v.setStyle("-fx-font-weight: 500;");

        row.getChildren().addAll(l, v);
        return row;
    }

    // ==================== CARRIER MANAGEMENT ====================

    private void loadCarriers() {
        carriersListContainer.getChildren().clear();
        carrierDetailContainer.getChildren().clear();
        
        // Preserve the currently selected carrier ID before clearing
        Integer selectedCarrierId = selectedCarrier != null ? selectedCarrier.getId() : null;

        List<User> carriers = userDAO.getAllCarriers();
        if (carriers.isEmpty()) {
            carriersListContainer.getChildren().add(createPlaceholder("No carriers found."));
            selectedCarrier = null;
            return;
        }

        User carrierToSelect = null;
        for (User c : carriers) {
            HBox item = createListItemBase();
            item.setUserData(c.getId()); // Support selection highlighting

            Label name = new Label(c.getUsername());
            name.getStyleClass().add("detail-value");
            name.setPrefWidth(200);

            Label status = new Label(c.isActive() ? "Active" : "Inactive");
            status.getStyleClass().addAll("badge", c.isActive() ? "badge-success" : "badge-danger");

            item.getChildren().addAll(name, status);
            // Fix lambda capture: create final copy to avoid capturing loop variable by reference
            final User finalCarrier = c;
            item.setOnMouseClicked(e -> showCarrierDetail(finalCarrier));
            carriersListContainer.getChildren().add(item);
            
            // Check if this is the previously selected carrier
            if (selectedCarrierId != null && c.getId() == selectedCarrierId) {
                carrierToSelect = c;
            }
        }
        
        // Restore the detail view if the carrier still exists
        if (carrierToSelect != null) {
            showCarrierDetail(carrierToSelect);
        } else {
            selectedCarrier = null;
        }
    }

    private void showCarrierDetail(User carrier) {
        selectedCarrier = carrier;
        carrierDetailContainer.getChildren().clear();
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-card");

        Label header = new Label(carrier.getUsername());
        header.getStyleClass().add("detail-header");

        card.getChildren().add(header);
        card.getChildren().add(createDetailRow("Phone", carrier.getPhone() != null ? carrier.getPhone() : "-"));
        card.getChildren().add(createDetailRow("Address", carrier.getAddress() != null ? carrier.getAddress() : "-"));
        
        // Add status display
        Label statusLabel = new Label(carrier.isActive() ? "Active" : "Inactive");
        statusLabel.getStyleClass().addAll("badge", carrier.isActive() ? "badge-success" : "badge-danger");
        HBox statusRow = new HBox(10);
        Label statusHeader = new Label("Status:");
        statusHeader.getStyleClass().add("detail-label");
        statusRow.getChildren().addAll(statusHeader, statusLabel);
        card.getChildren().add(statusRow);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 16 0 0 0;");
        
        Button toggleBtn = new Button(carrier.isActive() ? "Deactivate" : "Activate");
        toggleBtn.getStyleClass().add("btn-outline");
        toggleBtn.setOnAction(e -> {
            // carrier is guaranteed to be non-null since it's used throughout this method
            if (carrier.isActive())
                userDAO.deactivateCarrier(carrier.getId());
            else
                userDAO.activateCarrier(carrier.getId());
            loadCarriers(); // Reload - will preserve selection
        });

        Button fireBtn = new Button("Fire Carrier");
        fireBtn.getStyleClass().add("btn-outline");
        fireBtn.setStyle("-fx-border-color: #ef4444; -fx-text-fill: #f87171;");
        fireBtn.setOnAction(e -> handleFireCarrier(carrier));

        actions.getChildren().addAll(toggleBtn, fireBtn);
        card.getChildren().add(actions);

        carrierDetailContainer.getChildren().add(card);
    }
    
    private void handleFireCarrier(User carrier) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Fire Carrier");
        confirmDialog.setHeaderText("Fire " + carrier.getUsername() + "?");
        confirmDialog.setContentText("Are you sure you want to fire this carrier? This action cannot be undone.");
        
        java.util.Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Note: deleteCarrier method doesn't exist in UserDao yet
                // For now, we'll deactivate the carrier instead
                userDAO.deactivateCarrier(carrier.getId());
                ToastService.show(logoutButton.getScene(), 
                    "Carrier deactivated successfully.", 
                    ToastService.Type.SUCCESS,
                    ToastService.Position.BOTTOM_CENTER, 
                    Duration.seconds(3));
                loadCarriers();
            } catch (Exception e) {
                e.printStackTrace();
                ToastService.show(logoutButton.getScene(), 
                    "Error: " + e.getMessage(), 
                    ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, 
                    Duration.seconds(3));
            }
        }
    }

    // ==================== ORDER MANAGEMENT ====================

    private void loadOrders() {
        ordersListContainer.getChildren().clear();
        orderDetailContainer.getChildren().clear();
        
        // Preserve the currently selected order ID before clearing
        Integer selectedOrderId = selectedOrder != null ? selectedOrder.getId() : null;

        List<Order> orders = orderDAO.getAllOrders();
        if (orders.isEmpty()) {
            ordersListContainer.getChildren().add(createPlaceholder("No orders found."));
            if (ordersCountLabel != null)
                ordersCountLabel.setText("All Orders (0)");
            selectedOrder = null;
            return;
        }

        if (ordersCountLabel != null)
            ordersCountLabel.setText("All Orders (" + orders.size() + ")");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd HH:mm");

        Order orderToSelect = null;
        for (Order o : orders) {
            HBox item = createListItemBase();
            item.setUserData(o.getId());

            Label id = new Label("#" + o.getId());
            id.getStyleClass().addAll("badge", "badge-info");
            id.setPrefWidth(60);

            Label status = new Label(o.getStatus().name());
            String badgeClass = switch (o.getStatus()) {
                case CREATED -> "badge-info";
                case ASSIGNED -> "badge-warning";
                case DELIVERED -> "badge-success";
                case CANCELLED -> "badge-danger";
                default -> "badge-secondary"; // Fallback for any new statuses
            };
            status.getStyleClass().addAll("badge", badgeClass);
            status.setPrefWidth(100);

            Label date = new Label(o.getOrderTime().format(fmt));
            date.getStyleClass().add("muted");

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Label total = new Label(formatPrice(o.getTotalAfterTax()));
            total.getStyleClass().add("detail-value");

            item.getChildren().addAll(id, status, date, spacer, total);
            // Fix lambda capture: create final copy to avoid capturing loop variable by reference
            final Order finalOrder = o;
            item.setOnMouseClicked(e -> showOrderDetail(finalOrder));
            ordersListContainer.getChildren().add(item);
            
            // Check if this is the previously selected order
            if (selectedOrderId != null && o.getId() == selectedOrderId) {
                orderToSelect = o;
            }
        }
        
        // Restore the detail view if the order still exists
        if (orderToSelect != null) {
            showOrderDetail(orderToSelect);
        } else {
            selectedOrder = null;
        }
    }

    private void showOrderDetail(Order order) {
        selectedOrder = order;
        orderDetailContainer.getChildren().clear();
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-card");

        Label title = new Label("Order #" + order.getId());
        title.getStyleClass().add("detail-header");

        card.getChildren().add(title);
        
        // Order Basic Info
        VBox orderInfo = new VBox(5);
        orderInfo.getChildren().add(createDetailRow("Status", order.getStatus().name()));
        orderInfo.getChildren().add(createDetailRow("Total", formatPrice(order.getTotalAfterTax())));
        orderInfo.getChildren().add(createDetailRow("Subtotal", formatPrice(order.getTotalBeforeTax())));
        orderInfo.getChildren().add(createDetailRow("VAT (20%)", formatPrice(order.getVat())));
        orderInfo.getChildren().add(createDetailRow("Order Date", order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        if (order.getRequestedDeliveryTime() != null) {
            orderInfo.getChildren().add(createDetailRow("Requested Delivery", order.getRequestedDeliveryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }
        if (order.getDeliveredTime() != null) {
            orderInfo.getChildren().add(createDetailRow("Delivered", order.getDeliveredTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }
        card.getChildren().add(orderInfo);
        
        // Customer Information
        try {
            User customer = userDAO.getUserById(order.getCustomerId());
            if (customer != null) {
                Separator sep1 = new Separator();
                sep1.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
                card.getChildren().add(sep1);
                
                Label customerHeader = new Label("Customer Information");
                customerHeader.getStyleClass().add("field-label");
                customerHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                card.getChildren().add(customerHeader);
                
                VBox customerInfo = new VBox(5);
                customerInfo.getChildren().add(createDetailRow("Name", customer.getUsername()));
                if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
                    customerInfo.getChildren().add(createDetailRow("Phone", customer.getPhone()));
                }
                if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                    customerInfo.getChildren().add(createDetailRow("Address", customer.getAddress()));
                }
                card.getChildren().add(customerInfo);
            }
        } catch (Exception e) {
            System.err.println("Error loading customer info: " + e.getMessage());
        }
        
        // Carrier Information (if assigned)
        if (order.getCarrierId() != null) {
            try {
                User carrier = userDAO.getUserById(order.getCarrierId());
                if (carrier != null) {
                    Separator sep2 = new Separator();
                    sep2.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
                    card.getChildren().add(sep2);
                    
                    Label carrierHeader = new Label("Carrier Information");
                    carrierHeader.getStyleClass().add("field-label");
                    carrierHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    card.getChildren().add(carrierHeader);
                    
                    VBox carrierInfo = new VBox(5);
                    carrierInfo.getChildren().add(createDetailRow("Name", carrier.getUsername()));
                    if (carrier.getPhone() != null && !carrier.getPhone().isEmpty()) {
                        carrierInfo.getChildren().add(createDetailRow("Phone", carrier.getPhone()));
                    }
                    card.getChildren().add(carrierInfo);
                }
            } catch (Exception e) {
                System.err.println("Error loading carrier info: " + e.getMessage());
            }
        }

        // Order Items with Product Details
        // Check if items are already loaded (not null) to avoid infinite recursion
        if (order.getItems() == null) {
            // Load items if not already loaded
            try {
                order.setItems(orderDAO.getOrderItems(order.getId()));
                // Recursive call to refresh with items - but only if items was null
                showOrderDetail(order);
                return;
            } catch (Exception e) {
                System.err.println("Error loading order items: " + e.getMessage());
            }
        }
        
        // Display items if they exist (even if empty list)
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Separator sep3 = new Separator();
            sep3.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
            card.getChildren().add(sep3);
            
            Label itemsHeader = new Label("Order Items");
            itemsHeader.getStyleClass().add("field-label");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            card.getChildren().add(itemsHeader);
            
            VBox itemsContainer = new VBox(8);
            for (CartItem item : order.getItems()) {
                VBox itemCard = new VBox(5);
                itemCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 4; -fx-padding: 10;");
                
                Label productName = new Label(item.getProduct().getName());
                productName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                
                HBox itemDetails = new HBox(15);
                itemDetails.getChildren().add(new Label("Type: " + item.getProduct().getType().name()));
                itemDetails.getChildren().add(new Label("Quantity: " + String.format("%.2f kg", item.getQuantityKg())));
                itemDetails.getChildren().add(new Label("Unit Price: " + formatPrice(item.getUnitPrice())));
                
                Label lineTotal = new Label("Line Total: " + formatPrice(item.getLineTotal()));
                lineTotal.setStyle("-fx-font-weight: bold;");
                
                itemCard.getChildren().addAll(productName, itemDetails, lineTotal);
                itemsContainer.getChildren().add(itemCard);
            }
            card.getChildren().add(itemsContainer);
        } else if (order.getItems() != null && order.getItems().isEmpty()) {
            // Items were loaded but order has no items
            Separator sep3 = new Separator();
            sep3.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
            card.getChildren().add(sep3);
            
            Label itemsHeader = new Label("Order Items");
            itemsHeader.getStyleClass().add("field-label");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            card.getChildren().add(itemsHeader);
            
            Label noItemsLabel = new Label("This order has no items.");
            noItemsLabel.getStyleClass().add("muted");
            card.getChildren().add(noItemsLabel);
        }
        
        // Add cancel button for CREATED and ASSIGNED orders
        if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.ASSIGNED) {
            Separator actionSep = new Separator();
            actionSep.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
            card.getChildren().add(actionSep);
            
            HBox actionsBox = new HBox(10);
            actionsBox.setAlignment(Pos.CENTER_LEFT);
            
            Button cancelBtn = new Button("Cancel Order");
            cancelBtn.getStyleClass().add("btn-danger");
            cancelBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 4;");
            cancelBtn.setOnAction(e -> handleCancelOrder(order));
            actionsBox.getChildren().add(cancelBtn);
            
            card.getChildren().add(actionsBox);
        }

        orderDetailContainer.getChildren().add(card);
    }
    
    private void handleCancelOrder(Order order) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Cancel Order");
        confirmDialog.setHeaderText("Cancel Order #" + order.getId() + "?");
        confirmDialog.setContentText("Are you sure you want to cancel this order? The products will be restocked and the customer will receive a refund.");
        
        java.util.Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean cancelled = orderDAO.cancelOrder(order.getId());
                
                if (cancelled) {
                    ToastService.show(logoutButton.getScene(), 
                        "Order cancelled successfully. Products have been restocked.", 
                        ToastService.Type.SUCCESS,
                        ToastService.Position.BOTTOM_CENTER, 
                        Duration.seconds(3));
                    // Refresh the orders list and detail
                    loadOrders();
                } else {
                    ToastService.show(logoutButton.getScene(), 
                        "Failed to cancel order", 
                        ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, 
                        Duration.seconds(3));
                }
            } catch (RuntimeException e) {
                ToastService.show(logoutButton.getScene(), 
                    "Error: " + e.getMessage(), 
                    ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, 
                    Duration.seconds(3));
            } catch (Exception e) {
                e.printStackTrace();
                ToastService.show(logoutButton.getScene(), 
                    "Failed to cancel order: " + e.getMessage(), 
                    ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, 
                    Duration.seconds(3));
            }
        }
    }


    // Helper method defining list item style
    private HBox createListItemBase() {
        HBox item = new HBox(12);
        item.getStyleClass().addAll("list-item-base", "card");
        item.setStyle("-fx-padding: 14 18; -fx-cursor: hand; -fx-alignment: center-left; -fx-background-radius: 10; -fx-border-radius: 10;");
        item.setPrefWidth(Double.MAX_VALUE);

        item.setOnMouseEntered(e -> {
            if (item.getProperties().get("selected") == null) {
                item.setStyle("-fx-background-color: rgba(99, 102, 241, 0.2); -fx-padding: 14 18; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #6366f1; -fx-alignment: center-left; -fx-translate-x: 4;");
            }
        });
        item.setOnMouseExited(e -> {
            if (item.getProperties().get("selected") == null) {
                item.setStyle("-fx-padding: 14 18; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-radius: 10; -fx-alignment: center-left; -fx-translate-x: 0;");
            }
        });
        return item;
    }

    private Label createPlaceholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("muted", "placeholder");
        l.setStyle("-fx-padding: 40; -fx-font-style: italic; -fx-alignment: center;");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private String formatPrice(double price) {
        return String.format("%.2f ₺", price);
    }

    // ==================== MESSAGE MANAGEMENT ====================

    // ==================== MESSAGE MANAGEMENT ====================

    private void loadConversations() {
        messagesListContainer.getChildren().clear();
        messageDetailContainer.getChildren().clear();
        selectedConversation = null;

        int ownerId = Session.getUser().getId();
        List<MessageDao.Conversation> conversations = messageDAO.getConversationsForOwner(ownerId);

        if (conversations.isEmpty()) {
            messagesListContainer.getChildren().add(createPlaceholder("No messages."));
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

        for (MessageDao.Conversation conv : conversations) {
            HBox item = createListItemBase();
            item.setUserData(conv);

            VBox info = new VBox(2);
            Label userLbl = new Label(conv.getUsername());
            userLbl.getStyleClass().add("detail-value");
            userLbl.setStyle("-fx-font-weight: bold;");

            Label msgPreview = new Label(
                    conv.getLastMessage().length() > 30 ? conv.getLastMessage().substring(0, 30) + "..."
                            : conv.getLastMessage());
            msgPreview.getStyleClass().add("muted");
            msgPreview.setStyle("-fx-font-size: 11px;");

            info.getChildren().addAll(userLbl, msgPreview);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox meta = new VBox(2);
            meta.setAlignment(Pos.CENTER_RIGHT);

            Label timeLbl = new Label(conv.getLastTimestamp().format(formatter));
            timeLbl.getStyleClass().add("muted");
            timeLbl.setStyle("-fx-font-size: 10px;");

            meta.getChildren().add(timeLbl);

            if (conv.hasUnread()) {
                Label badge = new Label("NEW");
                badge.getStyleClass().addAll("badge", "badge-info");
                badge.setStyle("-fx-font-size: 9px;");
                meta.getChildren().add(badge);
            }

            item.getChildren().addAll(info, spacer, meta);

            item.setOnMouseClicked(e -> {
                selectedConversation = conv;
                showConversationDetail(conv);
            });

            messagesListContainer.getChildren().add(item);
        }
    }

    private void showConversationDetail(MessageDao.Conversation conv) {
        messageDetailContainer.getChildren().clear();
        int ownerId = Session.getUser().getId();

        VBox chatView = new VBox(0);
        chatView.getStyleClass().add("chat-view");
        VBox.setVgrow(chatView, Priority.ALWAYS);

        // Header
        HBox header = new HBox(12);
        header.setStyle("-fx-padding: 16; -fx-background-color: rgba(30, 41, 59, 0.8); -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Avatar
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: #6366f1; -fx-background-radius: 50%; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
        Label avatarLetter = new Label(conv.getUsername().substring(0, 1).toUpperCase());
        avatarLetter.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        avatar.getChildren().add(avatarLetter);
        
        VBox headerInfo = new VBox(2);
        Label headerLbl = new Label(conv.getUsername());
        headerLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #f8fafc;");
        Label statusLbl = new Label("Online");
        statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #10b981;");
        headerInfo.getChildren().addAll(headerLbl, statusLbl);
        
        header.getChildren().addAll(avatar, headerInfo);

        chatView.getChildren().add(header);

        // Messages Area
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox msgsBox = new VBox(12);
        msgsBox.setStyle("-fx-padding: 20; -fx-background-color: rgba(15, 23, 42, 0.5);");

        List<Message> messages = messageDAO.getMessagesBetween(ownerId, conv.getUserId());
        for (Message m : messages) {
            // NEW LOGIC: use senderId to detect if it's ME (Owner).
            boolean isOwnerMsg = (m.getSenderId() == ownerId);

            msgsBox.getChildren().add(createChatBubble(m.getSender(), m.getContent(), m.getTimestamp(), isOwnerMsg));

            // Legacy logic: if message has reply text, show as bubble
            String replyText = messageDAO.getReplyText(m.getId());
            if (replyText != null && !replyText.isEmpty()) {
                LocalDateTime replyTime = m.getTimestamp().plusMinutes(1);
                msgsBox.getChildren().add(createChatBubble("You", replyText, replyTime, true));
            }
        }

        scroll.setContent(msgsBox);

        // Scroll to bottom logic (robust)
        Platform.runLater(() -> {
            msgsBox.applyCss();
            msgsBox.layout();
            scroll.applyCss();
            scroll.layout();
            scroll.setVvalue(1.0);
            Platform.runLater(() -> scroll.setVvalue(1.0));
        });

        chatView.getChildren().add(scroll);

        Message lastMsg = messages.isEmpty() ? null : messages.get(messages.size() - 1);

        HBox inputArea = new HBox(10);
        inputArea.setStyle("-fx-padding: 16; -fx-background-color: rgba(30, 41, 59, 0.8); -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1 0 0 0;");
        inputArea.setAlignment(Pos.CENTER_LEFT);

        TextField replyField = new TextField();
        replyField.setPromptText("Type a message...");
        replyField.setStyle("-fx-background-color: rgba(15, 23, 42, 0.8); -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-padding: 10 16;");
        HBox.setHgrow(replyField, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setStyle("-fx-background-color: #6366f1; -fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: 600; -fx-padding: 10 24; -fx-cursor: hand;");
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle("-fx-background-color: #4f46e5; -fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: 600; -fx-padding: 10 24; -fx-cursor: hand;"));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle("-fx-background-color: #6366f1; -fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: 600; -fx-padding: 10 24; -fx-cursor: hand;"));

        // Bind Enter key
        replyField.setOnAction(e -> sendBtn.fire());

        sendBtn.setOnAction(e -> {
            String txt = replyField.getText().trim();
            if (txt.isEmpty())
                return;

            if (lastMsg == null) {
                // If is empty, we can still start conversation if user is known
                // But normally we reply to someone.
            }

            // New Logic: Create a new message row where sender_id = ownerId.
            // receiver is the customer.
            int currentOwnerId = Session.getUser().getId();

            // We need to know customerId. `conv.getUserId()` is the customer.
            // createMessage(customerId, ownerId, senderId, text)

            int msgId = messageDAO.createMessage(conv.getUserId(), currentOwnerId, currentOwnerId, txt);

            if (msgId > 0) {
                replyField.clear();
                showConversationDetail(conv); // Refresh
            } else {
                com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Failed to send",
                        com.cmpe343.fx.util.ToastService.Type.ERROR);
            }
        });

        inputArea.getChildren().addAll(replyField, sendBtn);
        chatView.getChildren().add(inputArea);

        messageDetailContainer.getChildren().add(chatView);
    }

    private void loadCarrierRatings() {
        ratingsContainer.getChildren().clear();
        
        List<Rating> ratings = ratingDAO.getAllRatings();
        if (ratings.isEmpty()) {
            ratingsContainer.getChildren().add(createPlaceholder("No ratings available."));
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        for (Rating rating : ratings) {
            VBox ratingCard = new VBox(8);
            ratingCard.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 15;");
            
            HBox header = new HBox(10);
            Label carrierLabel = new Label("Carrier ID: " + rating.getCarrierId());
            carrierLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            
            Label scoreLabel = new Label("Score: " + rating.getScore() + "/5");
            scoreLabel.getStyleClass().addAll("badge", "badge-success");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            Label dateLabel = new Label(rating.getTimestamp().format(formatter));
            dateLabel.getStyleClass().add("muted");
            
            header.getChildren().addAll(carrierLabel, scoreLabel, spacer, dateLabel);
            
            if (rating.getComment() != null && !rating.getComment().isEmpty()) {
                Label commentLabel = new Label("Comment: " + rating.getComment());
                commentLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-wrap-text: true;");
                ratingCard.getChildren().addAll(header, commentLabel);
            } else {
                ratingCard.getChildren().add(header);
            }
            
            ratingsContainer.getChildren().add(ratingCard);
        }
    }

    @FXML
    private void handleRefreshRatings() {
        loadCarrierRatings();
    }

    @FXML
    private void handleRefreshLoyalty() {
        if (loyaltyContainer == null) return;
        loyaltyContainer.getChildren().clear();
        
        try {
            List<Object[]> loyaltyStats = orderDAO.getCustomerLoyaltyStats();
            
            if (loyaltyStats.isEmpty()) {
                Label info = new Label("No customer purchase data available.");
                info.getStyleClass().add("muted");
                info.setStyle("-fx-padding: 20; -fx-font-size: 14px;");
                loyaltyContainer.getChildren().add(info);
                return;
            }
            
            // Header
            Label header = new Label("Customer Loyalty Program - Purchase Frequency");
            header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 0 0 20 0;");
            loyaltyContainer.getChildren().add(header);
            
            // Summary statistics
            int totalCustomers = loyaltyStats.size();
            int totalOrders = loyaltyStats.stream().mapToInt(s -> (Integer) s[2]).sum();
            double totalRevenue = loyaltyStats.stream().mapToDouble(s -> (Double) s[3]).sum();
            double avgOrdersPerCustomer = totalOrders / (double) totalCustomers;
            
            VBox summaryBox = new VBox(8);
            summaryBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16; -fx-margin: 0 0 20 0;");
            
            Label summaryTitle = new Label("Summary Statistics");
            summaryTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            
            summaryBox.getChildren().addAll(
                summaryTitle,
                createLoyaltySummaryRow("Total Active Customers", String.valueOf(totalCustomers)),
                createLoyaltySummaryRow("Total Orders", String.valueOf(totalOrders)),
                createLoyaltySummaryRow("Total Revenue", String.format("%.2f ₺", totalRevenue)),
                createLoyaltySummaryRow("Average Orders per Customer", String.format("%.2f", avgOrdersPerCustomer))
            );
            
            loyaltyContainer.getChildren().add(summaryBox);
            
            // Customer loyalty list
            Label listHeader = new Label("Customer Loyalty Rankings");
            listHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 20 0 10 0;");
            loyaltyContainer.getChildren().add(listHeader);
            
            for (Object[] stat : loyaltyStats) {
                int customerId = (Integer) stat[0];
                String username = (String) stat[1];
                int orderCount = (Integer) stat[2];
                double totalSpent = (Double) stat[3];
                long daysSinceFirst = (Long) stat[4];
                double avgDaysBetween = (Double) stat[5];
                double ordersPerMonth = (Double) stat[6];
                
                VBox card = createLoyaltyCard(username, orderCount, totalSpent, daysSinceFirst, avgDaysBetween, ordersPerMonth);
                loyaltyContainer.getChildren().add(card);
            }
            
            ToastService.show(logoutButton.getScene(), 
                "Loyalty data refreshed successfully.", 
                ToastService.Type.SUCCESS,
                ToastService.Position.BOTTOM_CENTER, 
                Duration.seconds(3));
        } catch (Exception e) {
            e.printStackTrace();
            Label error = new Label("Error loading loyalty data: " + e.getMessage());
            error.setStyle("-fx-text-fill: #ef4444; -fx-padding: 20;");
            loyaltyContainer.getChildren().add(error);
            ToastService.show(logoutButton.getScene(), 
                "Error loading loyalty data: " + e.getMessage(), 
                ToastService.Type.ERROR,
                ToastService.Position.BOTTOM_CENTER, 
                Duration.seconds(3));
        }
    }
    
    private VBox createLoyaltyCard(String username, int orderCount, double totalSpent, 
                                   long daysSinceFirst, double avgDaysBetween, double ordersPerMonth) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16; -fx-margin: 0 0 10 0;");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(username);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Determine loyalty tier based on purchase frequency
        String loyaltyTier = getLoyaltyTier(ordersPerMonth, orderCount);
        String tierColor = getTierColor(loyaltyTier);
        
        Label tierLabel = new Label(loyaltyTier);
        tierLabel.setStyle(String.format("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-padding: 4 12; -fx-background-color: %s; -fx-background-radius: 12;", 
            tierColor, tierColor + "20"));
        tierLabel.getStyleClass().add("badge");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        header.getChildren().addAll(nameLabel, spacer, tierLabel);
        
        VBox details = new VBox(6);
        details.getChildren().addAll(
            createDetailRow("Total Orders", String.valueOf(orderCount)),
            createDetailRow("Total Spent", String.format("%.2f ₺", totalSpent)),
            createDetailRow("Days Since First Order", String.valueOf(daysSinceFirst)),
            createDetailRow("Purchase Frequency", String.format("%.2f orders/month", ordersPerMonth)),
            createDetailRow("Avg Days Between Orders", avgDaysBetween > 0 ? String.format("%.1f days", avgDaysBetween) : "N/A")
        );
        
        card.getChildren().addAll(header, new Separator(), details);
        return card;
    }
    
    private String getLoyaltyTier(double ordersPerMonth, int totalOrders) {
        if (ordersPerMonth >= 4 || totalOrders >= 20) {
            return "VIP";
        } else if (ordersPerMonth >= 2 || totalOrders >= 10) {
            return "Gold";
        } else if (ordersPerMonth >= 1 || totalOrders >= 5) {
            return "Silver";
        } else {
            return "Bronze";
        }
    }
    
    private String getTierColor(String tier) {
        return switch (tier) {
            case "VIP" -> "#fbbf24"; // Gold
            case "Gold" -> "#f59e0b"; // Orange
            case "Silver" -> "#94a3b8"; // Gray
            default -> "#10b981"; // Green
        };
    }
    
    private HBox createLoyaltySummaryRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label labelLbl = new Label(label + ":");
        labelLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        row.getChildren().addAll(labelLbl, spacer, valueLbl);
        return row;
    }

    @FXML
    private void handleGenerateReport() {
        try {
            List<Order> allOrders = orderDAO.getAllOrders();
            List<Order> deliveredOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .collect(java.util.stream.Collectors.toList());
            
            // Calculate summary statistics
            double totalRevenue = deliveredOrders.stream()
                .mapToDouble(Order::getTotalAfterTax)
                .sum();
            
            long totalOrdersCount = allOrders.size();
            long deliveredOrdersCount = deliveredOrders.size();
            
            // Calculate total items sold (kg) and product sales by revenue and quantity
            double totalItemsSoldKg = 0.0;
            Map<String, Double> productRevenue = new java.util.HashMap<>();
            Map<String, Double> productQuantityKg = new java.util.HashMap<>();
            Map<String, Double> revenueByDate = new java.util.HashMap<>();
            LocalDate minDate = null;
            LocalDate maxDate = null;
            
            // Process all delivered orders
            for (Order order : deliveredOrders) {
                // Load order items if not already loaded
                if (order.getItems() == null) {
                    try {
                        order.setItems(orderDAO.getOrderItems(order.getId()));
                    } catch (Exception e) {
                        System.err.println("Error loading items for order " + order.getId() + ": " + e.getMessage());
                        continue;
                    }
                }
                
                // Track date range
                if (order.getOrderTime() != null) {
                    LocalDate orderDate = order.getOrderTime().toLocalDate();
                    if (minDate == null || orderDate.isBefore(minDate)) {
                        minDate = orderDate;
                    }
                    if (maxDate == null || orderDate.isAfter(maxDate)) {
                        maxDate = orderDate;
                    }
                    
                    // Revenue by date
                    String dateKey = orderDate.toString();
                    revenueByDate.put(dateKey, 
                        revenueByDate.getOrDefault(dateKey, 0.0) + order.getTotalAfterTax());
                }
                
                // Process items
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    for (CartItem item : order.getItems()) {
                        String productName = item.getProduct().getName();
                        double quantityKg = item.getQuantityKg();
                        double lineTotal = item.getLineTotal();
                        
                        totalItemsSoldKg += quantityKg;
                        productRevenue.put(productName, 
                            productRevenue.getOrDefault(productName, 0.0) + lineTotal);
                        productQuantityKg.put(productName, 
                            productQuantityKg.getOrDefault(productName, 0.0) + quantityKg);
                    }
                }
            }
            
            double averageOrderValue = deliveredOrdersCount > 0 ? totalRevenue / deliveredOrdersCount : 0.0;
            String periodStr = (minDate != null && maxDate != null) 
                ? minDate.toString() + " to " + maxDate.toString()
                : "N/A";
            
            // Create report dialog
            Dialog<Void> reportDialog = new Dialog<>();
            reportDialog.setTitle("Sales Reports & Analytics");
            reportDialog.setResizable(true);
            
            VBox reportContent = new VBox(25);
            reportContent.setStyle("-fx-padding: 30;");
            
            // Summary Statistics Section
            Label summaryTitle = new Label("Summary Statistics");
            summaryTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            
            VBox summaryStats = new VBox(10);
            summaryStats.getChildren().addAll(
                createSummaryRow("Total Revenue", formatPrice(totalRevenue) + " TL"),
                createSummaryRow("Total Orders", String.valueOf(totalOrdersCount)),
                createSummaryRow("Average Order Value", formatPrice(averageOrderValue) + " TL"),
                createSummaryRow("Total Items Sold", String.format("%.2f kg", totalItemsSoldKg)),
                createSummaryRow("Period", periodStr)
            );
            
            reportContent.getChildren().addAll(summaryTitle, summaryStats);
            
            Separator sep1 = new Separator();
            sep1.setStyle("-fx-opacity: 0.3; -fx-padding: 15 0;");
            reportContent.getChildren().add(sep1);
            
            // Revenue by Product (Bar Chart)
            Label chartLabel1 = new Label("Revenue by Product");
            chartLabel1.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e293b;");
            
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Revenue (TL)");
            BarChart<String, Number> revenueChart = new BarChart<>(xAxis, yAxis);
            revenueChart.setTitle("Revenue by Product");
            revenueChart.setLegendVisible(false);
            revenueChart.setPrefSize(700, 400);
            
            XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
            productRevenue.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    revenueSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                });
            revenueChart.getData().add(revenueSeries);
            
            reportContent.getChildren().addAll(chartLabel1, revenueChart);
            
            Separator sep2 = new Separator();
            sep2.setStyle("-fx-opacity: 0.3; -fx-padding: 15 0;");
            reportContent.getChildren().add(sep2);
            
            // Revenue by Time (Last 30 Days) - Line Chart
            Label chartLabel2 = new Label("Revenue by Time (Last 30 Days)");
            chartLabel2.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e293b;");
            
            // Get last 30 days of data, sorted by date
            LocalDate endDate = maxDate != null ? maxDate : LocalDate.now();
            LocalDate startDate = endDate.minusDays(29);
            
            CategoryAxis xAxisTime = new CategoryAxis();
            NumberAxis yAxisTime = new NumberAxis();
            yAxisTime.setLabel("Revenue (TL)");
            LineChart<String, Number> timeChart = new LineChart<>(xAxisTime, yAxisTime);
            timeChart.setTitle("Daily Revenue (Last 30 Days)");
            timeChart.setLegendVisible(false);
            timeChart.setPrefSize(700, 400);
            timeChart.setCreateSymbols(true);
            
            XYChart.Series<String, Number> timeSeries = new XYChart.Series<>();
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                String dateKey = currentDate.toString();
                double revenue = revenueByDate.getOrDefault(dateKey, 0.0);
                timeSeries.getData().add(new XYChart.Data<>(dateKey, revenue));
                currentDate = currentDate.plusDays(1);
            }
            timeChart.getData().add(timeSeries);
            
            reportContent.getChildren().addAll(chartLabel2, timeChart);
            
            Separator sep3 = new Separator();
            sep3.setStyle("-fx-opacity: 0.3; -fx-padding: 15 0;");
            reportContent.getChildren().add(sep3);
            
            // Product Sales Quantity (Pie Chart)
            Label chartLabel3 = new Label("Product Sales Quantity");
            chartLabel3.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e293b;");
            
            ObservableList<PieChart.Data> quantityPieData = FXCollections.observableArrayList();
            productQuantityKg.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(10) // Top 10 products
                .forEach(entry -> {
                    quantityPieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                });
            
            PieChart quantityChart = new PieChart(quantityPieData);
            quantityChart.setTitle("Top Products by Sales Quantity (kg)");
            quantityChart.setLabelsVisible(true);
            quantityChart.setPrefSize(600, 400);
            
            reportContent.getChildren().addAll(chartLabel3, quantityChart);
            
            ScrollPane scrollPane = new ScrollPane(reportContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(700);
            scrollPane.setPrefWidth(800);
            
            reportDialog.getDialogPane().setContent(scrollPane);
            reportDialog.getDialogPane().setPrefSize(1400, 1000);
            reportDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            // Make report dialog open maximized by getting the stage after showing
            reportDialog.setOnShown(e -> {
                javafx.stage.Window window = reportDialog.getDialogPane().getScene().getWindow();
                if (window instanceof Stage) {
                    ((Stage) window).setMaximized(true);
                }
            });
            
            reportDialog.showAndWait();
            
            ToastService.show(logoutButton.getScene(), 
                "Report generated successfully!", 
                ToastService.Type.SUCCESS,
                ToastService.Position.BOTTOM_CENTER, 
                Duration.seconds(3));
        } catch (Exception e) {
            ToastService.show(logoutButton.getScene(), 
                "Failed to generate report: " + e.getMessage(), 
                ToastService.Type.ERROR,
                ToastService.Position.BOTTOM_CENTER, 
                Duration.seconds(3));
            e.printStackTrace();
        }
    }
    
    private HBox createSummaryRow(String label, String value) {
        HBox row = new HBox(15);
        Label labelLbl = new Label(label + ":");
        labelLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-pref-width: 180;");
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        row.getChildren().addAll(labelLbl, valueLbl);
        return row;
    }

    private Node createChatBubble(String sender, String text, LocalDateTime time, boolean isOwner) {
        VBox bubble = new VBox(6);
        bubble.setMaxWidth(400);
        bubble.setStyle("-fx-padding: 12 16; -fx-background-radius: 18;");
        
        if (isOwner) {
            // Sent message (Owner) - Blue
            bubble.setStyle("-fx-padding: 12 16; -fx-background-radius: 18; -fx-background-color: #6366f1;");
        } else {
            // Received message (Customer) - Dark gray
            bubble.setStyle("-fx-padding: 12 16; -fx-background-radius: 18; -fx-background-color: rgba(30, 41, 59, 0.8); -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1;");
        }

        Label txt = new Label(text);
        txt.setWrapText(true);
        txt.setStyle("-fx-text-fill: " + (isOwner ? "white" : "#f8fafc") + "; -fx-font-size: 14px; -fx-line-spacing: 2px;");
        txt.setMaxWidth(350);

        HBox footer = new HBox(6);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        
        Label timeLbl = new Label(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLbl.setStyle("-fx-text-fill: " + (isOwner ? "rgba(255,255,255,0.7)" : "#94a3b8") + "; -fx-font-size: 11px;");
        
        if (isOwner) {
            // Add checkmark icon for sent messages
            Label checkmark = new Label("✓");
            checkmark.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 11px;");
            footer.getChildren().addAll(timeLbl, checkmark);
        } else {
            footer.getChildren().add(timeLbl);
        }

        bubble.getChildren().addAll(txt, footer);

        HBox row = new HBox(8);
        row.setAlignment(isOwner ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.getChildren().add(bubble);
        return row;
    }

    @FXML
    private void handleRefreshMessages() {
        loadConversations();
    }

    // ==================== COUPON MANAGEMENT ====================

    private void loadCoupons() {
        couponsListContainer.getChildren().clear();
        couponDetailContainer.getChildren().clear();

        List<Coupon> coupons = couponDAO.getAllCoupons();
        if (coupons.isEmpty()) {
            couponsListContainer.getChildren().add(createPlaceholder("No coupons."));
            return;
        }

        for (Coupon c : coupons) {
            HBox item = createListItemBase();
            item.setUserData(c.getId());
            
            Label code = new Label(c.getCode());
            code.getStyleClass().add("detail-value");
            code.setStyle("-fx-font-weight: bold;");
            code.setPrefWidth(150);
            
            Label kind = new Label(c.getKind() == Coupon.CouponKind.PERCENT ? "PERCENT" : "FIXED");
            kind.getStyleClass().addAll("badge", c.getKind() == Coupon.CouponKind.PERCENT ? "badge-info" : "badge-warning");
            
            Label value = new Label(c.getKind() == Coupon.CouponKind.PERCENT ? 
                String.format("%.0f%%", c.getValue()) : 
                String.format("%.2f ₺", c.getValue()));
            value.getStyleClass().add("detail-value");
            value.setPrefWidth(100);
            
            Label active = new Label(c.isActive() ? "Active" : "Inactive");
            active.getStyleClass().addAll("badge", c.isActive() ? "badge-success" : "badge-danger");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            item.getChildren().addAll(code, kind, value, active, spacer);
            item.setOnMouseClicked(e -> showCouponDetail(c));
            couponsListContainer.getChildren().add(item);
        }
    }

    @FXML
    private void handleAddCoupon() {
        showCouponDialog(null);
    }
    
    private void showCouponDetail(Coupon coupon) {
        selectedCoupon = coupon;
        couponDetailContainer.getChildren().clear();
        
        VBox card = new VBox(16);
        card.getStyleClass().addAll("detail-card", "card");
        card.setStyle("-fx-padding: 24;");
        
        Label code = new Label(coupon.getCode());
        code.getStyleClass().add("detail-header");
        code.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: white; -fx-padding: 0 0 8 0;");
        
        Separator separator = new Separator();
        separator.setStyle("-fx-opacity: 0.1; -fx-padding: 8 0;");
        
        VBox meta = new VBox(10);
        meta.getChildren().addAll(
            createDetailRow("Type", coupon.getKind().name()),
            createDetailRow("Value", coupon.getKind() == Coupon.CouponKind.PERCENT ? 
                String.format("%.0f%%", coupon.getValue()) : 
                String.format("%.2f ₺", coupon.getValue())),
            createDetailRow("Min Cart", String.format("%.2f ₺", coupon.getMinCart())),
            createDetailRow("Status", coupon.isActive() ? "✅ Active" : "❌ Inactive"),
            createDetailRow("Expires", coupon.getExpiresAt() != null ? 
                coupon.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Never")
        );
        
        HBox actions = new HBox(12);
        actions.getStyleClass().add("actions-container");
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setStyle("-fx-padding: 10 20;");
        editBtn.setOnAction(e -> showCouponDialog(coupon));
        
        Button toggleBtn = new Button(coupon.isActive() ? "Deactivate" : "Activate");
        toggleBtn.getStyleClass().add(coupon.isActive() ? "btn-outline" : "btn-primary");
        toggleBtn.setStyle(coupon.isActive() ? 
            "-fx-border-color: #ef4444; -fx-text-fill: #f87171; -fx-padding: 10 20;" : 
            "-fx-padding: 10 20;");
        toggleBtn.setOnAction(e -> {
            try {
                couponDAO.toggleCoupon(coupon.getId(), !coupon.isActive());
                loadCoupons();
                Coupon updated = couponDAO.getCouponById(coupon.getId());
                if (updated != null) {
                    showCouponDetail(updated);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        actions.getChildren().addAll(editBtn, toggleBtn);
        card.getChildren().addAll(code, separator, meta, actions);
        couponDetailContainer.getChildren().add(card);
    }
    
    private void showCouponDialog(Coupon existing) {
        Dialog<Coupon> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Coupon" : "Edit Coupon");
        dialog.setHeaderText(existing == null ? "Create New Coupon" : "Edit " + existing.getCode());
        
        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        TextField codeField = new TextField();
        codeField.setPromptText("Coupon Code");
        ComboBox<Coupon.CouponKind> kindCombo = new ComboBox<>();
        kindCombo.getItems().addAll(Coupon.CouponKind.values());
        kindCombo.setValue(Coupon.CouponKind.PERCENT);
        TextField valueField = new TextField();
        valueField.setPromptText("Value");
        TextField minCartField = new TextField();
        minCartField.setPromptText("Min Cart Amount");
        DatePicker expiresPicker = new DatePicker();
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(true);
        
        if (existing != null) {
            codeField.setText(existing.getCode());
            kindCombo.setValue(existing.getKind());
            valueField.setText(String.valueOf(existing.getValue()));
            minCartField.setText(String.valueOf(existing.getMinCart()));
            if (existing.getExpiresAt() != null) {
                expiresPicker.setValue(existing.getExpiresAt().toLocalDate());
            }
            activeCheck.setSelected(existing.isActive());
        }
        
        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(kindCombo, 1, 1);
        grid.add(new Label("Value:"), 0, 2);
        grid.add(valueField, 1, 2);
        grid.add(new Label("Min Cart:"), 0, 3);
        grid.add(minCartField, 1, 3);
        grid.add(new Label("Expires:"), 0, 4);
        grid.add(expiresPicker, 1, 4);
        grid.add(activeCheck, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveBtnType) {
                try {
                    String code = codeField.getText().trim();
                    Coupon.CouponKind kind = kindCombo.getValue();
                    double value = Double.parseDouble(valueField.getText());
                    double minCart = Double.parseDouble(minCartField.getText());
                    LocalDateTime expires = expiresPicker.getValue() != null ? 
                        expiresPicker.getValue().atTime(23, 59) : null;
                    boolean active = activeCheck.isSelected();
                    
                    if (existing == null) {
                        int id = couponDAO.createCoupon(code, kind, value, minCart, expires, active);
                        return new Coupon(id, code, kind, value, minCart, active, expires);
                    } else {
                        // Update would need update method in DAO
                        return existing;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        });
        
        java.util.Optional<Coupon> result = dialog.showAndWait();
        result.ifPresent(coupon -> {
            try {
                if (existing == null) {
                    com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Coupon Added",
                            com.cmpe343.fx.util.ToastService.Type.SUCCESS);
                } else {
                    com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Coupon Updated",
                            com.cmpe343.fx.util.ToastService.Type.SUCCESS);
                }
                loadCoupons();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleRefreshCoupons() {
        loadCoupons();
    }


    private void loadDashboard() {
        if (dashboardContainer == null) return;
        dashboardContainer.getChildren().clear();
        
        // Get statistics
        List<Product> products = productDAO.findAll();
        List<Order> orders = orderDAO.getAllOrders();
        List<User> carriers = userDAO.getAllCarriers();
        List<Coupon> coupons = couponDAO.getAllCoupons();
        
        long activeProducts = products.stream().filter(p -> p.getStockKg() > 0).count();
        long lowStockProducts = products.stream().filter(Product::isLowStock).count();
        long pendingOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.CREATED || o.getStatus() == OrderStatus.ASSIGNED).count();
        long activeCoupons = coupons.stream().filter(Coupon::isActive).count();
        
        // Create dashboard cards
        dashboardContainer.getChildren().addAll(
            createDashboardCard("Total Products", String.valueOf(products.size()), "#6366f1", "📦"),
            createDashboardCard("Active Products", String.valueOf(activeProducts), "#10b981", "✅"),
            createDashboardCard("Low Stock", String.valueOf(lowStockProducts), "#ef4444", "⚠️"),
            createDashboardCard("Total Orders", String.valueOf(orders.size()), "#3b82f6", "📋"),
            createDashboardCard("Pending Orders", String.valueOf(pendingOrders), "#f59e0b", "⏳"),
            createDashboardCard("Carriers", String.valueOf(carriers.size()), "#8b5cf6", "🚚"),
            createDashboardCard("Active Coupons", String.valueOf(activeCoupons), "#ec4899", "🎫")
        );
    }
    
    private VBox createDashboardCard(String title, String value, String color, String icon) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 24; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 16;");
        card.setPrefWidth(200);
        card.setPrefHeight(150);
        
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px;");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("detail-label");
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        header.getChildren().addAll(iconLabel, titleLabel);
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 800; -fx-text-fill: " + color + ";");
        
        card.getChildren().addAll(header, valueLabel);
        return card;
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            stage.setScene(new Scene(loader.load(), 640, 480));
            stage.setTitle("Gr7Project3 - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== COMMON ====================

    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }

    private void handleEditProduct(Product product) {
        showProductDialog(product);
    }

    private void showProductDialog(Product existing) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Product" : "Edit Product");
        dialog.setHeaderText(existing == null ? "Create New Product" : "Edit " + existing.getName());

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");
        ComboBox<ProductType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(ProductType.values());
        typeCombo.setValue(ProductType.VEG);
        TextField priceField = new TextField();
        priceField.setPromptText("Price");
        TextField stockField = new TextField();
        stockField.setPromptText("Stock (kg)");
        TextField thresholdField = new TextField();
        thresholdField.setPromptText("Threshold (kg)");

        // Image Handling
        Label imageLabel = new Label("No Image Selected");
        Button uploadBtn = new Button("Choose Image");
        final byte[][] imageBlobHolder = new byte[1][1]; // Mutable container for lambda

        if (existing != null) {
            nameField.setText(existing.getName());
            typeCombo.setValue(existing.getType());
            priceField.setText(String.valueOf(existing.getPrice()));
            stockField.setText(String.valueOf(existing.getStockKg()));
            thresholdField.setText(String.valueOf(existing.getThresholdKg()));
            if (existing.getImageBlob() != null) {
                imageLabel.setText("Current Image Loaded (" + existing.getImageBlob().length + " bytes)");
                imageBlobHolder[0] = existing.getImageBlob();
            }
        }

        uploadBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Product Image");
            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            java.io.File selectedFile = fileChooser.showOpenDialog(uploadBtn.getScene().getWindow());
            if (selectedFile != null) {
                try {
                    imageBlobHolder[0] = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                    imageLabel.setText(selectedFile.getName() + " (" + imageBlobHolder[0].length + " bytes)");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    imageLabel.setText("Error reading file");
                }
            }
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("Threshold:"), 0, 4);
        grid.add(thresholdField, 1, 4);
        grid.add(new Label("Image:"), 0, 5);
        grid.add(new HBox(10, uploadBtn, imageLabel), 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                try {
                    String name = nameField.getText();
                    if (name.isEmpty())
                        return null;
                    double price = Double.parseDouble(priceField.getText());
                    double stock = Double.parseDouble(stockField.getText());
                    double threshold = Double.parseDouble(thresholdField.getText());

                    if (existing == null) {
                        return new Product(0, name, typeCombo.getValue(), price, stock, threshold, imageBlobHolder[0]);
                    } else {
                        return new Product(existing.getId(), name, typeCombo.getValue(), price, stock, threshold,
                                imageBlobHolder[0]);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        java.util.Optional<Product> result = dialog.showAndWait();

        result.ifPresent(product -> {
            try {
                if (existing == null) {
                    productDAO.insert(product);
                    com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Product Added",
                            com.cmpe343.fx.util.ToastService.Type.SUCCESS);
                } else {
                    productDAO.update(product);
                    com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Product Updated",
                            com.cmpe343.fx.util.ToastService.Type.SUCCESS);
                }
                loadProducts();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Operation failed: " + e.getMessage());
                alert.showAndWait();
            }
        });
    }

    private void handleRemoveProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Product");
        alert.setHeaderText("Confirm Removal");
        alert.setContentText("Are you sure you want to remove " + product.getName() + "?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                productDAO.delete(product.getId());
                com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Product Removed",
                        com.cmpe343.fx.util.ToastService.Type.SUCCESS);
                loadProducts();
            } catch (Exception e) {
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setContentText("Failed to remove: " + e.getMessage());
                err.showAndWait();
            }
        }
    }

    // Helper methods moved to appropriate sections.
}
