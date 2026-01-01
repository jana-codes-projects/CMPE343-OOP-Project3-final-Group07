package com.cmpe343.fx.controller;

import com.cmpe343.dao.*;
import com.cmpe343.model.*;
import com.cmpe343.model.Order.OrderStatus;
import com.cmpe343.model.Product.ProductType;
import com.cmpe343.fx.Session;
import com.cmpe343.fx.util.ToastService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Separator;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
    private javafx.scene.control.TabPane mainTabPane;
    @FXML
    private javafx.scene.layout.FlowPane dashboardContainer;

    private Message selectedMessage;
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
        // loadLoyaltySettings(); // Loyalty not yet implemented in backend
        if (Session.isLoggedIn()) {
            loadMessages();
        }
    }

    // ==================== DASHBOARD ====================

    private void loadDashboard() {
        if (dashboardContainer == null)
            return;
        dashboardContainer.getChildren().clear();

        String[] modules = {
                "Products", "Carriers", "Orders", "Messages",
                "Coupons", "Ratings", "Loyalty", "Reports"
        };

        for (String module : modules) {
            javafx.scene.layout.VBox card = createDashboardCard(module);
            dashboardContainer.getChildren().add(card);
        }
    }

    private javafx.scene.layout.VBox createDashboardCard(String moduleName) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.setStyle(
                "-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 20; -fx-pref-width: 200; -fx-pref-height: 150;");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #2563eb; -fx-background-radius: 8; -fx-padding: 20; -fx-pref-width: 200; -fx-pref-height: 150; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 20; -fx-pref-width: 200; -fx-pref-height: 150;"));
        card.setOnMouseClicked(e -> openModuleTab(moduleName));

        Label title = new Label(moduleName);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label desc = new Label(getModuleDescription(moduleName));
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        desc.setWrapText(true);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label clickLabel = new Label("Click to open →");
        clickLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(title, desc, spacer, clickLabel);
        return card;
    }

    private String getModuleDescription(String module) {
        return switch (module) {
            case "Products" -> "Manage product inventory, prices, and stock levels";
            case "Carriers" -> "View and manage delivery carriers";
            case "Orders" -> "View all customer orders and their details";
            case "Messages" -> "Read and respond to customer messages";
            case "Coupons" -> "Create and manage discount coupons";
            case "Ratings" -> "View carrier performance ratings";
            case "Loyalty" -> "Configure customer loyalty program settings";
            case "Reports" -> "Generate sales and revenue reports";
            default -> "Module description";
        };
    }

    private void openModuleTab(String moduleName) {
        if (mainTabPane == null)
            return;

        javafx.scene.control.Tab targetTab = switch (moduleName) {
            case "Products" -> getTabByText("Products");
            case "Carriers" -> getTabByText("Carriers");
            case "Orders" -> getTabByText("Orders");
            case "Messages" -> getTabByText("Messages");
            case "Coupons" -> getTabByText("Coupons");
            case "Ratings" -> getTabByText("Carrier Ratings");
            case "Loyalty" -> getTabByText("Loyalty Settings");
            case "Reports" -> getTabByText("Reports");
            default -> null;
        };

        if (targetTab != null) {
            mainTabPane.getSelectionModel().select(targetTab);
        }
    }

    private javafx.scene.control.Tab getTabByText(String text) {
        if (mainTabPane == null)
            return null;
        for (javafx.scene.control.Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals(text)) {
                return tab;
            }
        }
        return null;
    }

    // ==================== PRODUCT MANAGEMENT ====================

    private void loadProducts() {
        productsListContainer.getChildren().clear();
        productDetailContainer.getChildren().clear();

        // Preserve the currently selected product ID before clearing
        Integer selectedProductId = selectedProduct != null ? selectedProduct.getId() : null;

        List<Product> products = productDAO.findAll();
        products.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

        if (products.isEmpty()) {
            productsListContainer.getChildren().add(createPlaceholder("No products in the system."));
            selectedProduct = null;
            return;
        }

        Product productToSelect = null;
        for (Product product : products) {
            HBox listItem = createProductListItem(product);
            if (listItem != null)
                productsListContainer.getChildren().add(listItem);

            // Check if this is the previously selected product
            if (selectedProductId != null && product.getId() == selectedProductId) {
                productToSelect = product;
            }
        }

        // Restore the detail view if the product still exists
        if (productToSelect != null) {
            showProductDetail(productToSelect);
        } else {
            selectedProduct = null;
        }
    }

    private HBox createProductListItem(Product product) {
        HBox item = createListItemBase();
        item.setUserData(product.getId());

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("detail-value");
        nameLabel.setStyle("-fx-font-weight: bold;");
        nameLabel.setPrefWidth(200);

        Label typeLabel = new Label(product.getTypeDisplayName());
        typeLabel.getStyleClass().addAll("badge",
                product.getType() == ProductType.FRUIT ? "badge-warning" : "badge-success");

        Label priceLabel = new Label(formatPrice(product.getPrice()) + "/kg");
        priceLabel.getStyleClass().add("detail-value");
        priceLabel.setPrefWidth(120);

        Label stockLabel = new Label("Stock: " + product.getStockKg() + " kg");
        stockLabel.getStyleClass().addAll("badge", product.isLowStock() ? "badge-danger" : "badge-success");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        item.getChildren().addAll(nameLabel, typeLabel, priceLabel, stockLabel, spacer);
        // Fix lambda capture: create final copy to avoid capturing loop variable by
        // reference
        final Product finalProduct = product;
        item.setOnMouseClicked(e -> showProductDetail(finalProduct));
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
                createDetailRow("Type", product.getTypeDisplayName()),
                createDetailRow("Price", formatPrice(product.getPrice())),
                createDetailRow("Stock", String.format("%.2f kg", product.getStockKg())),
                createDetailRow("Threshold", String.format("%.2f kg", product.getThresholdKg())));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setOnAction(e -> handleEditProduct(product));

        Button deleteBtn = new Button("Remove");
        deleteBtn.getStyleClass().add("btn-outline"); // or danger style if implemented
        deleteBtn.setStyle("-fx-border-color: #ef4444; -fx-text-fill: #ef4444;");
        deleteBtn.setOnAction(e -> handleRemoveProduct(product));

        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(name, meta, actions);
        productDetailContainer.getChildren().add(card);
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        Label l = new Label(label + ":");
        l.getStyleClass().add("detail-label");
        l.setPrefWidth(100);

        Label v = new Label(value);
        v.getStyleClass().add("detail-value");

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
            // Fix lambda capture: create final copy to avoid capturing loop variable by
            // reference
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

        card.getChildren().add(toggleBtn);

        carrierDetailContainer.getChildren().add(card);
    }
    
    @FXML
    private void handleHireCarrier() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Hire New Carrier");
        dialog.setHeaderText("Enter carrier details

        ButtonType hireButtonType = new ButtonType("Hire", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(hireButtonType, ButtonType.CANCEL);
        
        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 20;");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("field");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (min 6 characters)");
        passwordField.getStyleClass().add("field");
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        phoneField.getStyleClass().add("field");
        
        TextField addressField = new TextField();
        addressField.setPromptText("Address");
        addressField.getStyleClass().add("field");
        
        content.getChildren().addAll(
            new Label("Username:"), usernameField,
            new Label("Password:"), passwordField,
            new Label("Phone:"), phoneField,
            new Label("Address:"), addressField
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == hireButtonType) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText();
                String phone = phoneField.getText().trim();
                String address = addressField.getText().trim();
                
                if (username.isEmpty()) {
                    showError("Username is required.");
                    return null;
                }
                if (password.length() < 6) {
                    showError("Password must be at least 6 characters.");
                    return null;
                }
            // 
                if (userDAO.usernameExists(username)) {
                    showError("Username already exists.");
                    return null;

                
                try {
                    int carrierId = userDAO.createCarrier(username, password, phone, address);
                    if (carrierId > 0) {
         

                        return new User(carrierId, username, "carrier", phone, address, true);
                    }
                } catch (Exception e) {
                    showError("Failed to hire carrier: " + e.getMessage());
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    @FXML
    private void handleRefreshCarriers() {
        loadCarriers();
    }

    // ==================== ORDER MANA

        ate void loadOrders() {
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

        
        if (ordersCountLabel != null)
            ordersCountLabel.setText("All Orders (" + orders.size() + ")");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd HH:mm");

        Order orderToSelect = null;
        for (Order o : orders) {

                .setUserData(o.getId());

            Label id = new Label("#" + o.getId());
            id.getStyleClass().addAll("badge", "badge-i

                
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

         

        
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Label total = new Label(formatPrice(o.getTotalAfterTax()));
            total.getStyleClass().add("detail-value");

            item.getChildren().addAll(id, status,

                    der finalOrder = o;
            item.setOnMouseClicked(e -> showOrderDetail(finalOrder));
            ordersListContainer.getChildren().add(item);
            

                    ctedOrderId != null && o.getId() == selectedOrderId) {
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
        
        /

        orderInfo.getChildren().add(createDetailRow("Status", order.getStatus().name()));
        orderInfo.getChildren().add(createDetailRow("Total", formatPrice(order.getTotalAfterTax())));
        orderInfo.getChildren().add(createDetailRow("Subtotal", formatPrice(order.getTotalBeforeTax())));
        orderInfo.getChildren().add(createDetailRow("VAT (20%)", formatPrice(order.getVat())));
        orderInfo.getChildren().add(creat

            orderInfo.getChildren().add(createDetailRow("Requested Delivery", order.getRequestedDeliveryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }
        if (order.getDeliveredTime() != null) {
            orderInfo.getChildren().add(createDe

            .getChildren().add(orderInfo);
        
        // Customer Information
        try {
                        

                customer != null) {
                Separator sep1 = new Separator();

                card.getChildren().add(sep1);
                
                Label customerHeader = new Label("Customer Information");
                customerHeader.getStyleClass().add("field-label");

                card.getChildren().add(customerHeader);
                

                customerInfo.getChildren().add(createDetailRow("Name", customer.getUsername()));
                if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
                    customerInfo.getChildren().add(createDetailRow("Phone", customer.getPhone()));
                }
                if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                    customerInfo.getChildren().add(createDetailRow("Address", customer.getAddress()));
                }
                card.getChildren().add(customerInfo);
            }

            System.err.println("Error loading customer info: " + e.getMessage());
        }
        
        // Carrier Information (if assigned)

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

                // Recursive call to refresh with items - but only if items was null
                showOrderDetail(order);
                return;

                System.err.println("Er
                    ror loading order items: " + e.getMessage());
            }

            
        // Display items if they exist (even if empty list)
        if (order.getItems() != null && 

            sep3.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
            card.getChildren().add(sep3);
            
            Label itemsHeader = new Label("Order Items");
            itemsHeader.getStyleClass().add("field-label");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            c

            VBox itemsContainer = new VBox(8);
            // 
            for (CartItem item : order.getItems()) {
                VBox itemCard = new VBox(5);
                itemCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 4; -fx-padding: 10;");
                
                Label productName = new Label(item.getProduct().getName());
                productName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                
                HBox itemDetails = new HBox(15);
               

                itemDetails.getChildren().add(new Label("U

                Label lineTotal = new Label("Line Total: " + formatPrice(item.getLineTotal()));
                lineTotal.setStyle("-fx-font-weight: bold;");
                
                itemCard.getChildren().addAll(productName, itemDetails, lineTotal);
         

            card.getChildren().add(itemsContainer);
        } else if (order.getItems() != null && order.getItems().isEmpty()) {
            // Items were loaded but order has no items
            Separator sep3 = new Separator();
            sep3.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
            card.getChildren().add(sep3);
     

            itemsHeader.getStyleClass().add("field-label");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            card.getChildren().add(itemsHeader);
            

            noItemsLabel.getStyleClass().add("muted");
            card.getChildren().add(noItemsLabel);

        
        orderDetailContainer.getChildren().add(card);
    }
                
                =============== MESSAGE MANAGEMENT ====================
                at

        messageDetailContainer.getChildren().clear();
        

        Integer selectedMessageId = selectedMessage != null ? selectedMessage.getId() : null;

        List<Message> msgs = messageDAO.getAllMessages();

            messagesListContainer.getChildren().add(createPlaceholder("No messages."));
            selectedMessage = null;
            return;
        }


        Message messageToSelect = null;

            HBox item = createListItemBase();
            item.setUserData(m.getId());
            
            Label sender = new Label(m.getSender());
            sender.getStyleClass().add("detail-value");

            
            Label content = new Label(m.getContent().length() > 50 ? m.getContent().substring(0, 50) + "..." : m.getContent());
            content.getStyleClass().add("muted");

            Label timestamp = new Label(m.getTimestamp().format(formatter));
            timestamp.getStyleClass().add("muted");
            timestamp.setPrefWidth(150);
            
            if (!m.isRead()) {

                unreadBadge.getStyleClass().addAll("badge", "badge-info");
                item.getChildren().addAll(sender, content, timestamp, unreadBadge);
            } else {
                item.getChildren().addAll(sender, content, timestamp);
            }

            // Fix lambda capture: create final copy to avoid capturing loop variable by reference
            final Message finalMessage = m;
            item.setOnMouseClicked(e -> {

                showMessageDetail(finalMessage);
                if (!finalMessage.isRead()) {
                    messageDAO.markAsRead(finalMessage.getId());
                    loadMessages(); // Refresh to update read status
                }

            
            messagesListContainer.getChildren().add(item);
            
            // Check if this is the previously selected message
            if (selectedMessageId != null && m.getId() == selectedMessageId) {
                messageToSelect = m;
            }
                            
        }
        

                ageToSelect != null) {
            showMessageDetail(messageToSelect);
        } else {
            selectedMessage = null;
                                
        }
    }
    
                                
    private void showMessageDetail(Message message) {
        messageDetailContainer.getChildren().clear();
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-card");
                            
        
        Label h

            
        VBox meta = new VBox(5);
        LocalDateTime messageTime = message.getTimestamp(

            createDetailRow("Sender", message.getSender()),
         

        );
        
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
        
        Label contentLabel = new Label("Message Content:");
        contentLabel.getStyleClass().add("field-label");
        contentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        TextArea contentArea = new TextArea(message.getContent());
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(5);

        
        card.getChildren().addAll(header, meta, sep, contentLabel, contentArea);
        
        // Check if there's already a reply
        String existingReply = messageDAO.getReplyText(message.getId());
        if (existingReply != null && !existingReply.trim().isEmpty()) {

            replySep.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
            
            Label replyHeader = new Label("Your Reply:");
            replyHeader.getStyleClass().add("field-label");
            replyHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #10b981;");
            
            TextArea replyArea = new TextArea(existingReply);
            replyArea.setEditable(false);
            replyArea.setWrapText(true);
            replyArea.setPrefRowCount(4)

            
            card.getChildren().addAll(replySep, replyHeader, replyArea);
        } else {
            // Add reply section fo

            replySep.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");
            
            Label replyLabel = new Label("Reply:");
            replyLabel.getStyleClass().add("field-label");
            replyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            TextArea replyArea = new TextArea();
            replyArea.setPromptText("Type your reply here...");
            replyArea.setWrapText(true)

            replyArea.getStyleClass().add("field");
            
            Button sendReplyBtn = new Button("Send Reply");
            sendReplyBtn.getStyleCla

            sendReplyBtn.setOnAction(e -> {
                String replyText = replyArea.getText().trim();

                    ToastService.show(messageDetailContainer.getScene(), "Reply cannot be empty", ToastService.Type.ERROR);
                    return;
            // 
                }
                
                try {

                    if (success) {
                        ToastService.show(messageDetailContainer.getScene(), "Reply sent successfully!", ToastService.Type.SUCCESS);
                        loadMessages(); // Refresh to show updated status
                    } else {
         

                } catch (Exception ex) {
                    ex.printStackTrace();
                    ToastService.show(messageDetailContainer.getScene(), "Error: " + ex.getMessage(), ToastService.Type.ERROR);
                }
            });
            
     

            replyActions.getChildren().add(sendReplyBtn);
            
            card.getChildren().addAll(replySep, replyLabel, replyArea, replyActions);
        }
        

        


        ate void handleRefreshMessages() {
        loadMessages();
    }

    // ==================== COUPON MANAGEMENT ====================

    priva

        couponDetailContainer.getChildren().clear();
                
                reserve the cu

        
        // Load ALL coupons from database (not just valid ones)
        List<Coupon> allCoupons = couponDAO.getAllCoupons();
        allCoupons.sort((c1, c2) -> c1.getCode().compareToIgnoreCase(c2.getCode()));
        
        if (allCoupons.isEmpty()) {
            couponsListContainer.getChildren().add(createPlaceholder("No coupons ava  

        }

        Coupon couponToSelect = null;
                    pon c : allCoupo
                     item = createListItemBase();item.setUserData(c.getId());
            
                    de = new Label(c.getCode());
                    StyleClass().add("detail-value");
            code.setStyle("-fx-font-weight: bold;");
         

            String discountText;
            if (c.getKind() == Coupon.CouponKind.AMOUNT) {
                discountText = "-" + formatPrice(c.getValue()) + " TL";
            } else {
                discountText = "-" + c.getValue() + "%";
            }
            Label discount = new Label(discountText);
            discount.getStyleClass().addAll("badge", "badge-success");
            discount.setPrefWidth(100);
            
            // Show status badge
            Label status = new Label(c.isActive() ? "Active" : "Inactive");
            status.getStyleClass().addAll("badge", c.isActive() ? "badge-success" : "badge-danger");
            status.setPrefWidth(80);

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            item.getChildren().addAll(code, discount, status, spacer);
            // Fix lambda capture: create final copy to avoid capturing loop variable by reference
            final Coupon finalCoupon = c;
            item.setOnMouseClicked(e -> showCouponDetail(finalCoupon));
            couponsListContainer.getChildren().add(item);
            
            // Check if this is the 

                couponToSelect = c;
            }

        
        // Restore the detail view if the coupon still exists
        if (couponToSelect != null) {
            showCouponDetail(couponToSelect);
        } else {

        }
    }
    
    private void showCouponDetail(Coupon coupon) {
        selectedCoupon = coupon;
        couponDetailContainer.getChildren().clear();
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-card");
        
        Label header = new Label("Coupon: " + coupon.ge

        
        VBox meta = new VBo
                x(5);
        String discountText;
        if (coupon.getKind() == Coupon.CouponKind.

        } else {
                discountText = coupon
                
                
                ng expiryDateText = coupon.
                ? coupon.getExpiresAt().fo
                : "No expir
        meta.getChildren().addAll(
                createDetailRow("Code", coupon.getCode()),
                createDetailRow("Type", coupon.getKind().name()),
                createDetailRow("Discount", discountText),
                createDetailRow("Min Cart", formatPri

                createDetailRow("Status", coupon.isActive() ? "Active" : "Inactive")
        );
        
        // Calculate days until expiry
        if (coupon.getExpiresAt() != null) {
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), 
                coupon.getExpiresAt().toLocalDate()
            );
            String expiryStatus = daysUntilExpiry > 0 
                    ? daysUntilExpiry + " days remaining" 
                    : "Expired";
            meta.getChildren().add(createDetailRow("Validity", expiryStatus));
        }
        
        card.getChildren().addAll(header, meta);
        couponDetailContainer.getChildren().add(card);
    }

    @FXML
    private void handleAddCoupon() {
        Dialog<Coupon> dialog = new Dialog<>();
        dialog.setTitle("Add New Coupon");
        dialog.setHeaderText("Enter coupon details");
        dialog.setResizable(true);

        // Create form fields
        TextField codeField = new TextField();
        codeField.setPromptText("Coupon Code");
        
        ComboBox<Stri

                    tValue("AMOUNT");

        TextField valueField = new TextField();
        valueField.se

                    nCartField = new TextField();
        minCartField.setPromptText("Minimum Cart (TL)");
        minCartField.setText("0.0");
        
        DatePicker expiryPicker = new DatePicker();
        expiryPicker.setValue(LocalDate.now().plusDays(30)); // Default to 30 days from now
        
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(true);

        VBox form = new VBox(10);
        form.setStyle("-fx-padding: 20; -fx-background-color: #0f172a;");
        
        Label codeLabel = new Label("Code:");
        codeLabel.getStyleClass().add("field-label");
        Label kindLabel = new Label("Type:");
        kindLabel.getStyleClass().add("field-label");
        Label valueLabel = new Label("Discount Value:");
        valueLabel.getStyleClass().add("field-label");
        Label minCartLabel = new Label("Minimum Cart (TL):");
        minCartLabel.getStyleClass().add("field-label");
        Label expiryLabel = new Label("Expiry Date:");
        expiryLabel.getStyleClass().add("field-label");
        
        codeField.getStyleClass().add("field");
        kindCombo.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");
        valueField.getStyleClass().add("field");
        minCartField.getStyleClass().add("field");
        
        form.getChildren().addAll(
            codeLabel, codeField,
            kindLabel, kindCombo,
            valueLabel, valueField,
            minCartLabel, minCartField,
            expiryLabel, expiryPicker,
            activeCheck
        );
        form.setPrefWidth(500);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes()

        
        Platform.runLater(() -> {
            javafx.scene.Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (okBtn != null) {
         

        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {

                    String code = codeField.getText().trim().toUpperCase();
                    String kindStr = kindCombo.getValue();
                    double value = Double.parseDouble(valueField.getText().trim())

                    LocalDate expiry = expiryPicker.getValue();
                    boolean active = activeCheck.isSelected();

                    if (code.isEmpty()) {
                        showError("Coupon code cannot be empty.");

                    }


                        showError("Please select an expiry date.");

                    }

                    if (value <= 0) {
                        showError("Discount value must be greater than 0.");
                        return null;
                    }
             

                        showError("Minimum cart value must be non-negative.");
                        return null;
                    }
                    
                    if ("PERCENT".equals(kindStr) && value > 100) {
                        showError("Percentage discount cannot exceed 100%.");
                        return null;
                    }

                    // Check if code already exists
                    if (couponDAO.getCouponByCode(code) != null) {
                        showError("A 
            oupon with this code already exists.");
                        return null;

        
                    Coupon.CouponKind kind = Coupon.CouponKind.valueOf(kindSt

            
                    int couponId = couponDAO.createCoupon(code, kind, value, minCart, expiresAt, active);
                    if (couponId > 0) {
                        showSuccess("Coupon added successfully!");
                        loadCoupons();
                        return new Coupon(couponId, code, kind, value, minCart, active, expiresAt);
             

                    showError("Please enter valid numbers for value and minimum cart.");
                } catch (Exception e) {
                    showError("Failed to add coupon: " + e.getMessage());
                }

            return null;
        });

        dialog.showAndWait();
    }

            
    private void handleRefreshCo
                    upons() {

            


            
                     loadCarrierR
                    ontainer.getChildren().clear();
                    
                    ing> ratings = ratingDAO.getAllRatings();
                    ngs.isEmpty()) {
                            ra

            

            TimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        for (Rating rating : ratings) {
            VBox ratingCard = ne
                    w VBox(8);
            ratingCard.setStyle("-fx-background-color: #1e2

            HBox header = new HBox(10);
            Label carrierLabel = new Label("Carrier ID: " + rating.getCarrierId());
            carrierLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            
            Label scoreLabel = new Label("Score: " + rating.getScore() + "/5");
            scoreLabel.getStyleClass().addAll("badge", "badge-success");
            
            Region spacer = new Region();

                
                        
            Label dateLabel = new Label(rating.getTimestamp().format(formatter));
            d

            header.getChildren().addAll(carrierLabel, scoreLabel, spacer, dateLabel);
            
            if (rating.getComment() != null && !rating.getComment().isEmpty()) {
                Label commentLabel = new Label("Comment: " + rating.getComment());
                commentLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-wrap-text: true;");
                ratingCard.getChildren().addAll(header, commentLabel);
            } else {
     

            
            ldren().add(ratingCard);
        }
    }
                

        L
    private void handleRefreshRatings() {

        


        ate void handleRefreshLoyalty() {
        if (loyaltyContainer == null) return;
        loyaltyContainer.getChildren().clear();

        try {
            List<Object[]> loyaltyStats =
                 orderDAO.getCustomerLoyaltyStats();
                
            if (loyaltyStats.isEmpty()) {

                info.getStyleClass().add("muted");
                info.setStyle("-fx-padding: 20; -fx-font-size: 14px

                return;

            
            // Header
                Label header = new Label("Customer Loyalty Program - Purchas
                header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-tex
                loyaltyContainer.getChildren().add(header);
                
                // Summary statistics
                          

            double totalRevenue = loyaltyStats.stream().mapToDouble(s -> (Double) s[3]).sum();
            double avgOrdersPerCustomer = totalOrders / (double) totalCustomers;
     

            summaryBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16; -fx-margin: 0 0 20 0;");
            
            Label summaryTitle = new Label("Summary Statistics");
            summaryTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            
            summaryBox.getChildren().addAll(
                summaryTitle,
                createLoyaltySummaryRow("Total Active Customers", String.valueOf(totalCustomers)),
                createLoyaltySummaryRow("Total Orders", String.valueOf(totalOrders)),
                createLoyaltySummaryRow("Total Revenue", String.format("%.2f ₺", totalRevenue)),
     

            
            loyaltyContainer.getChildren().add(summaryBox);
            
            // Customer loyalty list
            Label listHeader = new Label("Customer Loyalty Rankings");
            listHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 20 0 10 0;");
            loyaltyContainer.getChildren().add(listHeader);
     

                int customerId = (Integer) stat[0];
                String username = (String) stat[1];
                int orderCount = (Integer)

                long daysSinceFirst = (Long) stat[4];
                double avgDaysBetween = (Double) stat[5];

                
                VBox card = createLoyaltyCard(username, orderCount, totalSpent, daysSinceFirst,

            }
            

        } catch (Exception e) {
            e.printStackTrace();
            Label error = new Label("Error loading loyalty data: " + e.getMessage());
            error.setStyle("-fx-text-fill: #ef4444; -fx-padding: 20;");
            loyaltyContainer.getChildren().add(error);
        }
    }
    
    private VBox createLoyaltyCard(String username, int orderCount, double totalSpent, 
     

        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16; -fx-margin: 0 0 10 0;");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CE

        Label nameLabel = new Label(username);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Determine loyalty tier based on purchase frequency

        String tierColor = getTierColor(loyaltyTier);
        
        Label tierLabel = new Label(loyaltyTier)

            tierColor, tierColor + "20"));
        tierLabel.getStyleClass().add("badge");
        
                
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        header.getChildren().addAll(nameLabel, spacer, tierLabel);
        
        V

            createDetailRow("Total Orders", String.valueOf(orderCount)),
            createDetailRow("Total Spent", String.format("%.2f ₺", totalSpent)),
            createDetailRow("Days Since First Ord

            createDetailRow("Avg Days Between Orders", avgDaysBetween > 0 ? String.format("%.1f days", avgDaysBetween) : "N/A")
        );
        

        return card;
    }
    

        if (ordersPerMonth >= 4 || totalOrders >= 20) {
            return "VIP";

            return "Gold";
        } else if (ordersPerMonth >= 1 || totalOrders >= 5) {
            return "Silver";
        } else {
            return "Bronze";
        }
    }
    
    private String getTierColor(String tier) {
        return switch (tier) {

            case "Gold" -> "#f59e0b"; // Orange
            case "Silver" -> "#94a3b8"; // Gray
            default -> "#10b981"; // Green
        };
    }
    ate HBox createLoyaltySummaryRow(String label, String value) {
        HBox row = new HBox(12);

        
        Label labelLbl = new Label(label + ":");

        
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        row.getChildren().addAll(labelLbl, spacer, valueLbl);
        return row;
    }


        
    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }
    
    private void showProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();

                    sizable(true);
        
        // Style dialog pane to match project theme
        dialog.getDia

                    alogPane().getStylesheets().add(getClass().getResource("/css/owner.css").toExternalForm());
        
        TextField nameField = new TextField(product != null ? product.getName() : "");
        nameField.get

                    
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("VEG", "FRUIT");
        typeCombo.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");
        if (product != null) {
            // Convert enum to database format for display (VEG/FRUIT)
            typeCombo.setValue(product.getTypeAsDbString());
        } else {
            typeCombo.setValue("FRUIT");
        }
        
                                
        TextField priceField = new TextField(product != null ? String.valueOf(product.getPrice()) : "");
        priceField.getStyleClass().add("field");
        priceField.setPromptText("Price per kg");
        
        TextField stockField = new TextField(product != null ? String.valueOf(product.getStockKg()) : "");
        stockField.getStyleClass().add("field");
                                    ("Stock in kg");
                                    
                                     = new TextField(product != null ? String.valueOf(product.getThresholdKg()) : "");
        thresholdField.getStyleClass().add("field");
        thresholdField.setPromptText("Threshold in kg");
        
        VBox content = new VBox(15);
        content.setStyle("-fx-background-color: #0f172a; -fx-padding: 20;");
        
        Label nameLabel = new Label("Name:");
        nameLabel.getStyleClass().add("field-label");
        Label typeLabel = new Label("Type:");
        typeLabel.getStyleClass().add("field-label");
        Label priceLabel = new Label("Price (TL/kg):");
        priceLabel.getStyleClass().add("field-label");
        Label stockLabel = new Label("Stock (kg):");
        stockLabel.getStyleClass().add("field-label");
        Label thresholdLabel = new Label("Threshold (kg):");
        thresholdLabel.getStyleClass().add("field-label");
        
        con

                typeLabel, typeCombo,
                priceLabel, priceField,
                stockLabel, stockField,
                thresholdLabel, thresholdField
        );
        dialog.getDialogPane().setContent(content);
        
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        
        // Style buttons to match project theme
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node saveBtn = dialog.getDialogPane().lookupButton(saveButton);
            if (saveBtn != null) {
                saveBtn.getStyleClass().add("btn-primary");
            }
            javafx.scene.Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelBtn != null) {
                    cancelBtn.getStyleClass().add("btn-outline");
                    

            
        dialog.setResultConverter(buttonType -> {
                    buttonType == saveButton) {
                    try {

                    String typeDbValue = typeCombo.getValue(); // This will be "VEG" or "FRUIT"
                    double price = Double.parseDouble(priceFiel

                    double threshold = Double.parseDouble(thresholdField.getText().trim());
                    
                    if (name.isEmpty()) {
                        showError("Product name cannot be empty.");
                        return null;
                    }
                    

                        showError("Price, stock, and threshold must be non-negative values.");
                        return null;
                    }
                    
                    if (product == null) {
                        // Create new product - typeDbValue is already in database format (VEG/FRUIT)
                        int productId = productDAO.createProduct(name, typeDbValue, price, stock, threshold);
                        if (productId > 0) {
                            showSuccess("Product added successfully!");
                            loadProducts();
                 

                    } else {
                        // Update existing product
                        boolean success = productDAO.updateProduct(product.getId(), name, typeDbValue, price, stock, threshold);
                        if (success) {
                            showSuccess("Product updated successfully!");
                            loadProducts();
                            // Refresh the detail view
                            List<Product> products = productDAO.findAll();
                     

                                .findFirst()
                                .orElse(null);
                            if (updatedProduct
                                    showProductDetail(updatedProduct);
                 

                        } else {
                            showError("Failed to update product.");
                            return null;
                        }
                    }
                } catch (NumberFormatException e) {

                        (Exception e) {
                    showError("Error: " + e.getMessage(
                                tackTrace();
                }
                                
            return null;
        });
        

            

                    
                     handleR

            

    @FXML
    private void handleRefreshOrders() {

            


            void handleGenerateReport() {
        try {
            List<Order> allOrders = orderDAO.getAllOrders();

                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .collect(java.util.stream.Coll
                    
                    alculate summary statistics
                    le totalRevenue = deliveredOrders.stream()
                    .mapToDouble(Order::getTotalAfterTax)
                    .sum();

            long deliveredOrdersCount = deliveredOrders.size();

            // Calculate total items sold (kg) and product sales by revenue and quantity
            double totalItemsSoldKg = 0.0;
            Map<String, Double> productRevenue = n

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

                        continue;
                    }
                    }
                    
                        rack date range
                    if (order.getOrderTime() != null) {
                    LocalDate orderDate = order.getOrd

                        minDate = orderDate;

                    if (maxDate == null || orderDate.isAfter(maxDate)) {
                        maxDate = orderDate;
                    }

                    // Revenue by date
                    String dateKey = orderDate.toString();
                    revenueByDate.put(dateKey, 

                }
                
                // Process items

                    for (CartItem item : order.getItems()) {
                        String productName = item.getProduct().getName();
                        double quantityKg = item.getQuantityKg();
                        double lineTotal = item.getLineTotal();
                        
                        totalItemsSoldKg += quantityKg;
                        productRevenue.put(productName, 
                            productRevenue.ge

                            productQuantityKg.getOrDefault(productName, 0.0) + quantityKg);
                    }
                }
            }
            
            double averageOrderValue = deliveredOrdersCount > 0 ? totalRevenue / deliveredOrdersCount : 0.0;
            String periodStr = (minDate != null && maxDate != null) 
                ? minDate.toString() + " to " + maxDate.toString()
                : "N/A";

            // Create report dialog

            reportDialog.setTitle("Sales Reports & Analytics");
            reportDialog.setResizable(true);
            

            reportContent.setStyle("-fx-padding: 30;");
            
            // Summary Statistics Section

            summaryTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            
                     summaryStats = new VBox(10);
                    aryStats.getChildren().addAll
                    createSummaryRow("T
                        teSummaryRow("Total Orders", String.valueOf(totalOrdersCount)),
                    cre

                createSummaryRow("Period", periodStr)
            );
            
            reportContent.getChildren().addAll(s

            Separator sep1 = new Separator();

            reportContent.getChildren().add(sep1);
            
            // Revenue by Product (Bar Chart)
            Label chartLabel1 = new Label

            
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();

            BarChart<String, Number> revenueChart = new BarChart<>(xAxis, yAxis);
            revenueChart.setTitle("Revenue by Product");
            revenueChart.setLegendVisible(false);
            revenueChart.setPrefSize(700, 400);
            
            XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
            pro

                .forEach(entry -> {

                });
            revenueChart.getData().add(revenueSeries);
            
            reportContent.getChildren().addAll(chartLabel1, revenueChart);
            
     

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
     

            

            reportDialog.setOnShown(e -> {
                javafx.stage.Window window = reportDialog.getDialogPane().getScene().getWindow();
                if (window instanceof Stage) {
                    ((Stage) window).setMaximized(true);
     

            
            reportDialog.showAndWait();
            
            showSuccess("Report generated successfully!");
        } catch (Exception e) {
            showError("Failed to generate report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    p

        Label labelLbl = new Label(label + ":");
        labelLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-pref-width: 180;");
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        row.getChildren().addAll(labelLbl, valueLbl);
        return row;
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== HANDLERS ====================

    @FXML
    private void handleEditProduct(Product product) {
        showProductDialog(product);
    }

    private void handleRemoveProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Product");
        alert.setHeaderText("Confirm Removal");
        alert.setContentText("Are you sure you want to remove " + product.getName() + "?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // productDAO.delete(product.getId()); // Assuming delete method exists or
            // needed
            // For now, just remove from list logic if DAO supported it
            System.out.println("Remove product: " + product.getName());
            loadProducts(); // Reload list
        }
    }

    private HBox createListItemBase() {
        HBox item = new HBox(10);
        item.getStyleClass().add("list-item");
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private Label createPlaceholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setStyle("-fx-padding: 20;");
        return l;
    }

    private String formatPrice(double price) {
        return String.format("%.2f TL", price);
    }
    
    // ==================== HELPER METHODS ====================
    
    private void showSuccess(String message) {
        if (logoutButton != null && logoutButton.getScene() != null) {
            ToastService.show(logoutButton.getScene(), message, ToastService.Type.SUCCESS);
        }
    }
    
    private void showError(String message) {
        if (logoutButton != null && logoutButton.getScene() != null) {
            ToastService.show(logoutButton.getScene(), message, ToastService.Type.ERROR);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
    
    private void showInfo(String message) {
        if (logoutButton != null && logoutButton.getScene() != null) {
            ToastService.show(logoutButton.getScene(), message, ToastService.Type.INFO);
        }
    }
}
