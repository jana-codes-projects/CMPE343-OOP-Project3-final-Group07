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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
    @FXML
    private VBox reportsContainer;
    @FXML
    private ListView<String> topProductsList;
    @FXML
    private ListView<String> carrierPerformanceList;

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

        // Thumbnail
        Node thumbNode;
        try {
            byte[] bytes = productDAO.getProductImageBlob(product.getId());
            if (bytes != null) {
                javafx.scene.image.Image img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes));
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(30);
                iv.setFitHeight(30);
                iv.setPreserveRatio(true);
                thumbNode = iv;
            } else {
                Label l = new Label(product.getName().substring(0, 1).toUpperCase());
                l.setStyle(
                        "-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
                thumbNode = l;
            }
        } catch (Exception e) {
            thumbNode = new Label("?");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        item.getChildren().addAll(thumbNode, nameLabel, typeLabel, priceLabel, stockLabel, spacer);
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

        // Image display
        Node imageNode;
        try {
            byte[] imageBytes = productDAO.getProductImageBlob(product.getId());
            if (imageBytes != null) {
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                        new java.io.ByteArrayInputStream(imageBytes));
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(150);
                iv.setFitHeight(150);
                iv.setPreserveRatio(true);
                imageNode = iv;
            } else {
                Label placeholder = new Label("No Image");
                placeholder.getStyleClass().add("muted");
                imageNode = placeholder;
            }
        } catch (Exception e) {
            imageNode = new Label("Error loading image");
        }
        VBox imgBox = new VBox(imageNode);
        imgBox.setAlignment(Pos.CENTER);
        imgBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8;");

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

        card.getChildren().addAll(name, imgBox, meta, actions);
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
            // Fix lambda capture: create final copy to avoid capturing loop variable by
            // reference
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

    // ==================== REPORTS & ANALYTICS ====================
    private void loadAnalytics() {
        // 1. Safety Check: Ensure FXML components are properly injected
        if (topProductsList == null || carrierPerformanceList == null) {
            System.err.println("Warning: ListView components are not initialized. Check FXML fx:id.");
            return;
        }

        // 2. Clear existing data to avoid duplication during refresh
        topProductsList.getItems().clear();
        carrierPerformanceList.getItems().clear();

        try {
            // 3. Fetch Top Selling Products via DAO
            // Expected Object array format: [String productName, Double totalQuantity]
            List<Object[]> topProducts = orderDAO.getTopSellingProducts();

            if (topProducts.isEmpty()) {
                topProductsList.getItems().add("No sales data available yet.");
            } else {
                for (Object[] row : topProducts) {
                    String productName = (String) row[0];
                    Double totalKg = (Double) row[1];
                    topProductsList.getItems().add(String.format("📦 %-15s | %6.2f kg sold", productName, totalKg));
                }
            }

            // 4. Fetch Carrier Performance Stats via DAO
            // Expected Object array format: [String carrierName, Integer deliveryCount]
            List<Object[]> carrierStats = orderDAO.getCarrierPerformance();

            if (carrierStats.isEmpty()) {
                carrierPerformanceList.getItems().add("No delivery records found.");
            } else {
                for (Object[] row : carrierStats) {
                    String carrierName = (String) row[0];
                    Integer deliveries = (Integer) row[1];
                    carrierPerformanceList.getItems()
                            .add(String.format("🚚 %-15s | %d deliveries", carrierName, deliveries));
                }
            }

        } catch (Exception e) {
            System.err.println("Critical error while loading analytics: " + e.getMessage());
            topProductsList.getItems().add("Error: Could not retrieve data.");
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
        orderInfo.getChildren().add(createDetailRow("Order Date",
                order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        if (order.getRequestedDeliveryTime() != null) {
            orderInfo.getChildren().add(createDetailRow("Requested Delivery",
                    order.getRequestedDeliveryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }
        if (order.getDeliveredTime() != null) {
            orderInfo.getChildren().add(createDetailRow("Delivered",
                    order.getDeliveredTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
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
                itemCard.setStyle(
                        "-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 4; -fx-padding: 10;");

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

        orderDetailContainer.getChildren().add(card);
    }

    // ==================== MESSAGE MANAGEMENT ====================

    private void loadMessages() {
        messagesListContainer.getChildren().clear();
        messageDetailContainer.getChildren().clear();

        // Preserve the currently selected message ID before clearing
        Integer selectedMessageId = selectedMessage != null ? selectedMessage.getId() : null;

        List<Message> msgs = messageDAO.getAllMessages();
        if (msgs.isEmpty()) {
            messagesListContainer.getChildren().add(createPlaceholder("No messages."));
            selectedMessage = null;
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        Message messageToSelect = null;
        for (Message m : msgs) {
            HBox item = createListItemBase();
            item.setUserData(m.getId());

            Label sender = new Label(m.getSender());
            sender.getStyleClass().add("detail-value");
            sender.setPrefWidth(150);

            Label content = new Label(
                    m.getContent().length() > 50 ? m.getContent().substring(0, 50) + "..." : m.getContent());
            content.getStyleClass().add("muted");

            Label timestamp = new Label(m.getTimestamp().format(formatter));
            timestamp.getStyleClass().add("muted");
            timestamp.setPrefWidth(150);

            if (!m.isRead()) {
                Label unreadBadge = new Label("NEW");
                unreadBadge.getStyleClass().addAll("badge", "badge-info");
                item.getChildren().addAll(sender, content, timestamp, unreadBadge);
            } else {
                item.getChildren().addAll(sender, content, timestamp);
            }

            // Fix lambda capture: create final copy to avoid capturing loop variable by
            // reference
            final Message finalMessage = m;
            item.setOnMouseClicked(e -> {
                selectedMessage = finalMessage;
                showMessageDetail(finalMessage);
                if (!finalMessage.isRead()) {
                    messageDAO.markAsRead(finalMessage.getId());
                    loadMessages(); // Refresh to update read status
                }
            });

            messagesListContainer.getChildren().add(item);

            // Check if this is the previously selected message
            if (selectedMessageId != null && m.getId() == selectedMessageId) {
                messageToSelect = m;
            }
        }

        // Restore the detail view if the message still exists
        if (messageToSelect != null) {
            showMessageDetail(messageToSelect);
        } else {
            selectedMessage = null;
        }
    }

    private void showMessageDetail(Message message) {
        messageDetailContainer.getChildren().clear();
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-card");

        Label header = new Label("Message from: " + message.getSender());
        header.getStyleClass().add("detail-header");

        VBox meta = new VBox(5);
        LocalDateTime messageTime = message.getTimestamp();
        meta.getChildren().addAll(
                createDetailRow("Sender", message.getSender()),
                createDetailRow("Timestamp", messageTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                createDetailRow("Status",
                        message.getRepliedAt() != null ? "Replied" : (message.isRead() ? "Read" : "New")));

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");

        Label contentLabel = new Label("Message Content:");
        contentLabel.getStyleClass().add("field-label");
        contentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea contentArea = new TextArea(message.getContent());
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(5);
        contentArea.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-text-fill: white;");

        card.getChildren().addAll(header, meta, sep, contentLabel, contentArea);

        // Check if there's already a reply
        String existingReply = messageDAO.getReplyText(message.getId());
        if (existingReply != null && !existingReply.trim().isEmpty()) {
            Separator replySep = new Separator();
            replySep.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");

            Label replyHeader = new Label("Your Reply:");
            replyHeader.getStyleClass().add("field-label");
            replyHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #10b981;");

            TextArea replyArea = new TextArea(existingReply);
            replyArea.setEditable(false);
            replyArea.setWrapText(true);
            replyArea.setPrefRowCount(4);
            replyArea.setStyle("-fx-background-color: rgba(16, 185, 129, 0.1); -fx-text-fill: #94a3b8;");

            card.getChildren().addAll(replySep, replyHeader, replyArea);
        } else {
            // Add reply section for new replies
            Separator replySep = new Separator();
            replySep.setStyle("-fx-opacity: 0.3; -fx-padding: 10 0;");

            Label replyLabel = new Label("Reply:");
            replyLabel.getStyleClass().add("field-label");
            replyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            TextArea replyArea = new TextArea();
            replyArea.setPromptText("Type your reply here...");
            replyArea.setWrapText(true);
            replyArea.setPrefRowCount(4);
            replyArea.getStyleClass().add("field");

            Button sendReplyBtn = new Button("Send Reply");
            sendReplyBtn.getStyleClass().add("btn-primary");
            sendReplyBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
            sendReplyBtn.setOnAction(e -> {
                String replyText = replyArea.getText().trim();
                if (replyText.isEmpty()) {
                    ToastService.show(messageDetailContainer.getScene(), "Reply cannot be empty",
                            ToastService.Type.ERROR);
                    return;
                }

                try {
                    boolean success = messageDAO.replyToMessage(message.getId(), replyText);
                    if (success) {
                        ToastService.show(messageDetailContainer.getScene(), "Reply sent successfully!",
                                ToastService.Type.SUCCESS);
                        loadMessages(); // Refresh to show updated status
                    } else {
                        ToastService.show(messageDetailContainer.getScene(), "Failed to send reply",
                                ToastService.Type.ERROR);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ToastService.show(messageDetailContainer.getScene(), "Error: " + ex.getMessage(),
                            ToastService.Type.ERROR);
                }
            });

            HBox replyActions = new HBox(10);
            replyActions.setAlignment(Pos.CENTER_RIGHT);
            replyActions.getChildren().add(sendReplyBtn);

            card.getChildren().addAll(replySep, replyLabel, replyArea, replyActions);
        }

        messageDetailContainer.getChildren().add(card);
    }

    @FXML
    private void handleRefreshMessages() {
        loadMessages();
    }

    // ==================== COUPON MANAGEMENT ====================

    private void loadCoupons() {
        couponsListContainer.getChildren().clear();
        couponDetailContainer.getChildren().clear();

        // Preserve the currently selected coupon ID before clearing
        Integer selectedCouponId = selectedCoupon != null ? selectedCoupon.getId() : null;

        // Load ALL coupons from database (not just valid ones)
        List<Coupon> allCoupons = couponDAO.getAllCoupons();
        allCoupons.sort((c1, c2) -> c1.getCode().compareToIgnoreCase(c2.getCode()));

        if (allCoupons.isEmpty()) {
            couponsListContainer.getChildren().add(createPlaceholder("No coupons available."));
            selectedCoupon = null;
            return;
        }

        Coupon couponToSelect = null;
        for (Coupon c : allCoupons) {
            HBox item = createListItemBase();
            item.setUserData(c.getId());

            Label code = new Label(c.getCode());
            code.getStyleClass().add("detail-value");
            code.setStyle("-fx-font-weight: bold;");
            code.setPrefWidth(150);

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
            // Fix lambda capture: create final copy to avoid capturing loop variable by
            // reference
            final Coupon finalCoupon = c;
            item.setOnMouseClicked(e -> showCouponDetail(finalCoupon));
            couponsListContainer.getChildren().add(item);

            // Check if this is the previously selected coupon
            if (selectedCouponId != null && c.getId() == selectedCouponId) {
                couponToSelect = c;
            }
        }

        // Restore the detail view if the coupon still exists
        if (couponToSelect != null) {
            showCouponDetail(couponToSelect);
        } else {
            selectedCoupon = null;
        }
    }

    private void showCouponDetail(Coupon coupon) {
        selectedCoupon = coupon;
        couponDetailContainer.getChildren().clear();
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-card");

        Label header = new Label("Coupon: " + coupon.getCode());
        header.getStyleClass().add("detail-header");

        VBox meta = new VBox(5);
        String discountText;
        if (coupon.getKind() == Coupon.CouponKind.AMOUNT) {
            discountText = formatPrice(coupon.getValue()) + " TL (Fixed Amount)";
        } else {
            discountText = coupon.getValue() + "% (Percentage)";
        }

        String expiryDateText = coupon.getExpiresAt() != null
                ? coupon.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "No expiry";

        meta.getChildren().addAll(
                createDetailRow("Code", coupon.getCode()),
                createDetailRow("Type", coupon.getKind().name()),
                createDetailRow("Discount", discountText),
                createDetailRow("Min Cart", formatPrice(coupon.getMinCart()) + " TL"),
                createDetailRow("Expiry Date", expiryDateText),
                createDetailRow("Status", coupon.isActive() ? "Active" : "Inactive"));

        // Calculate days until expiry
        if (coupon.getExpiresAt() != null) {
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    coupon.getExpiresAt().toLocalDate());
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

        ComboBox<String> kindCombo = new ComboBox<>();
        kindCombo.getItems().addAll("AMOUNT", "PERCENT");
        kindCombo.setValue("AMOUNT");

        TextField valueField = new TextField();
        valueField.setPromptText("Discount Value");

        TextField minCartField = new TextField();
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
        kindCombo.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");
        valueField.getStyleClass().add("field");
        minCartField.getStyleClass().add("field");

        form.getChildren().addAll(
                codeLabel, codeField,
                kindLabel, kindCombo,
                valueLabel, valueField,
                minCartLabel, minCartField,
                expiryLabel, expiryPicker,
                activeCheck);
        form.setPrefWidth(500);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(600, 550);

        Platform.runLater(() -> {
            javafx.scene.Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (okBtn != null) {
                okBtn.getStyleClass().add("btn-primary");
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    String code = codeField.getText().trim().toUpperCase();
                    String kindStr = kindCombo.getValue();
                    double value = Double.parseDouble(valueField.getText().trim());
                    double minCart = Double.parseDouble(minCartField.getText().trim());
                    LocalDate expiry = expiryPicker.getValue();
                    boolean active = activeCheck.isSelected();

                    if (code.isEmpty()) {
                        showError("Coupon code cannot be empty.");
                        return null;
                    }

                    if (expiry == null) {
                        showError("Please select an expiry date.");
                        return null;
                    }

                    if (value <= 0) {
                        showError("Discount value must be greater than 0.");
                        return null;
                    }

                    if (minCart < 0) {
                        showError("Minimum cart value must be non-negative.");
                        return null;
                    }

                    if ("PERCENT".equals(kindStr) && value > 100) {
                        showError("Percentage discount cannot exceed 100%.");
                        return null;
                    }

                    // Check if code already exists
                    if (couponDAO.getCouponByCode(code) != null) {
                        showError("A coupon with this code already exists.");
                        return null;
                    }

                    Coupon.CouponKind kind = Coupon.CouponKind.valueOf(kindStr);
                    LocalDateTime expiresAt = expiry.atStartOfDay();

                    int couponId = couponDAO.createCoupon(code, kind, value, minCart, expiresAt, active);
                    if (couponId > 0) {
                        showSuccess("Coupon added successfully!");
                        loadCoupons();
                        return new Coupon(couponId, code, kind, value, minCart, active, expiresAt);
                    }
                } catch (NumberFormatException e) {
                    showError("Please enter valid numbers for value and minimum cart.");
                } catch (Exception e) {
                    showError("Failed to add coupon: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleRefreshCoupons() {
        loadCoupons();
    }

    // ==================== RATINGS ====================

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
        if (loyaltyContainer == null)
            return;
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
            summaryBox.setStyle(
                    "-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16; -fx-margin: 0 0 20 0;");

            Label summaryTitle = new Label("Summary Statistics");
            summaryTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

            summaryBox.getChildren().addAll(
                    summaryTitle,
                    createLoyaltySummaryRow("Total Active Customers", String.valueOf(totalCustomers)),
                    createLoyaltySummaryRow("Total Orders", String.valueOf(totalOrders)),
                    createLoyaltySummaryRow("Total Revenue", String.format("%.2f ₺", totalRevenue)),
                    createLoyaltySummaryRow("Average Orders per Customer",
                            String.format("%.2f", avgOrdersPerCustomer)));

            loyaltyContainer.getChildren().add(summaryBox);

            // Customer loyalty list
            Label listHeader = new Label("Customer Loyalty Rankings");
            listHeader.setStyle(
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 20 0 10 0;");
            loyaltyContainer.getChildren().add(listHeader);

            for (Object[] stat : loyaltyStats) {
                int customerId = (Integer) stat[0];
                String username = (String) stat[1];
                int orderCount = (Integer) stat[2];
                double totalSpent = (Double) stat[3];
                long daysSinceFirst = (Long) stat[4];
                double avgDaysBetween = (Double) stat[5];
                double ordersPerMonth = (Double) stat[6];

                VBox card = createLoyaltyCard(username, orderCount, totalSpent, daysSinceFirst, avgDaysBetween,
                        ordersPerMonth);
                loyaltyContainer.getChildren().add(card);
            }

            showInfo("Loyalty data refreshed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            Label error = new Label("Error loading loyalty data: " + e.getMessage());
            error.setStyle("-fx-text-fill: #ef4444; -fx-padding: 20;");
            loyaltyContainer.getChildren().add(error);
        }
    }

    private VBox createLoyaltyCard(String username, int orderCount, double totalSpent,
            long daysSinceFirst, double avgDaysBetween, double ordersPerMonth) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16; -fx-margin: 0 0 10 0;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(username);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Determine loyalty tier based on purchase frequency
        String loyaltyTier = getLoyaltyTier(ordersPerMonth, orderCount);
        String tierColor = getTierColor(loyaltyTier);

        Label tierLabel = new Label(loyaltyTier);
        tierLabel.setStyle(String.format(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-padding: 4 12; -fx-background-color: %s; -fx-background-radius: 12;",
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
                createDetailRow("Avg Days Between Orders",
                        avgDaysBetween > 0 ? String.format("%.1f days", avgDaysBetween) : "N/A"));

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

    // ==================== COMMON ====================

    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }

    private void showProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Add Product" : "Edit Product");
        dialog.setResizable(true);

        // Style dialog pane to match project theme
        dialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/owner.css").toExternalForm());

        TextField nameField = new TextField(product != null ? product.getName() : "");
        nameField.getStyleClass().add("field");
        nameField.setPromptText("Product name");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("VEG", "FRUIT");
        typeCombo.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");
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
        stockField.setPromptText("Stock in kg");

        TextField thresholdField = new TextField(product != null ? String.valueOf(product.getThresholdKg()) : "");
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

        content.getChildren().addAll(
                nameLabel, nameField,
                typeLabel, typeCombo,
                priceLabel, priceField,
                stockLabel, stockField,
                thresholdLabel, thresholdField);

        // Image Selection Section
        Label imageLabel = new Label("Product Image:");
        imageLabel.getStyleClass().add("field-label");

        Button chooseImageBtn = new Button("Choose Image...");
        chooseImageBtn.getStyleClass().add("btn-outline");
        chooseImageBtn.setMaxWidth(Double.MAX_VALUE);

        Label selectedFileLabel = new Label("No file selected");
        selectedFileLabel.getStyleClass().add("muted");
        selectedFileLabel.setStyle("-fx-font-size: 11px;");

        final byte[][] imageBytesContainer = new byte[1][0];
        imageBytesContainer[0] = null;

        chooseImageBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Product Image");
            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            java.io.File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
            if (selectedFile != null) {
                try {
                    imageBytesContainer[0] = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                    selectedFileLabel.setText(selectedFile.getName() + " (" + (imageBytesContainer[0].length / 1024) + " KB)");
                } catch (java.io.IOException ex) {
                    showError("Could not read image file: " + ex.getMessage());
                }
            }
        });

        content.getChildren().addAll(imageLabel, chooseImageBtn, selectedFileLabel);

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
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                try {
                    String name = nameField.getText().trim();
                    String typeDbValue = typeCombo.getValue(); // This will be "VEG" or "FRUIT"
                    double price = Double.parseDouble(priceField.getText().trim());
                    double stock = Double.parseDouble(stockField.getText().trim());
                    double threshold = Double.parseDouble(thresholdField.getText().trim());

                    if (name.isEmpty()) {
                        showError("Product name cannot be empty.");
                        return null;
                    }

                    if (price < 0 || stock < 0 || threshold < 0) {
                        showError("Price, stock, and threshold must be non-negative values.");
                        return null;
                    }

                    if (product == null) {
                        // Create new product
                        int productId = productDAO.createProduct(name, typeDbValue, price, stock, threshold, imageBytesContainer[0]);
                        if (productId > 0) {
                            showSuccess("Product added successfully!");
                            loadProducts();
                            return null;
                        }
                    } else {
                        // Update existing product
                        boolean success = productDAO.updateProduct(product.getId(), name, typeDbValue, price, stock,
                                threshold, imageBytesContainer[0]);
                        if (success) {
                            showSuccess("Product updated successfully!");
                            loadProducts();
                            // Refresh the detail view
                            List<Product> products = productDAO.findAll();
                            Product updatedProduct = products.stream()
                                    .filter(p -> p.getId() == product.getId())
                                    .findFirst()
                                    .orElse(null);
                            if (updatedProduct != null) {
                                showProductDetail(updatedProduct);
                            }
                            return null;
                        } else {
                            showError("Failed to update product.");
                            return null;
                        }
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid number format.");
                    return null;
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleHireCarrier() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Hire New Carrier");
        dialog.setHeaderText("Enter details for the new carrier");

        // CSS
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/owner.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("field");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        phoneField.getStyleClass().add("field");

        TextArea addressField = new TextArea();
        addressField.setPromptText("Address");
        addressField.setPrefRowCount(3);
        addressField.getStyleClass().add("field");

        VBox content = new VBox(15);
        content.setStyle("-fx-background-color: #0f172a; -fx-padding: 20;");

        Label userLabel = new Label("Username:");
        userLabel.getStyleClass().add("field-label");
        Label passLabel = new Label("Password:");
        passLabel.getStyleClass().add("field-label");
        Label phoneLabel = new Label("Phone:");
        phoneLabel.getStyleClass().add("field-label");
        Label addrLabel = new Label("Address:");
        addrLabel.getStyleClass().add("field-label");

        content.getChildren().addAll(
                userLabel, usernameField,
                passLabel, passwordField,
                phoneLabel, phoneField,
                addrLabel, addressField);

        dialogPane.setContent(content);

        ButtonType hireButton = new ButtonType("Hire", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(hireButton, ButtonType.CANCEL);

        // Style buttons
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node hireBtn = dialogPane.lookupButton(hireButton);
            if (hireBtn != null)
                hireBtn.getStyleClass().add("btn-primary");

            javafx.scene.Node cancelBtn = dialogPane.lookupButton(ButtonType.CANCEL);
            if (cancelBtn != null)
                cancelBtn.getStyleClass().add("btn-outline");
        });

        dialog.setResultConverter(btn -> {
            if (btn == hireButton) {
                String u = usernameField.getText().trim();
                String p = passwordField.getText().trim();
                String ph = phoneField.getText().trim();
                String a = addressField.getText().trim();

                if (u.isEmpty() || p.isEmpty()) {
                    showError("Username and Password are required.");
                    return null;
                }

                // Check if username exists
                if (userDAO.usernameExists(u)) {
                    showError("Username '" + u + "' is already taken.");
                    return null;
                }

                int newId = userDAO.createCarrier(u, p, ph, a);
                if (newId > 0) {
                    showSuccess("Carrier '" + u + "' hired successfully!");
                    loadCarriers(); // Refresh list
                } else {
                    showError("Failed to create carrier.");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleRefreshCarriers() {
        loadCarriers();
        toast("Refreshed carrier list", ToastService.Type.INFO);
    }

    @FXML
    private void handleRefreshProducts() {
        loadProducts();
    }

    @FXML
    private void handleRefreshOrders() {
        loadOrders();
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
            double averageOrderValue = deliveredOrdersCount > 0 ? totalRevenue / deliveredOrdersCount : 0.0;

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

                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    for (CartItem item : order.getItems()) {
                        String productName = item.getProduct().getName();
                        double quantityKg = item.getQuantityKg();
                        double lineTotal = item.getUnitPrice() * quantityKg; // Approximate line total

                        totalItemsSoldKg += quantityKg;
                        productRevenue.put(productName,
                                productRevenue.getOrDefault(productName, 0.0) + lineTotal);
                        productQuantityKg.put(productName,
                                productQuantityKg.getOrDefault(productName, 0.0) + quantityKg);
                    }
                }
            }

            String periodStr = (minDate != null && maxDate != null)
                    ? minDate.format(DateTimeFormatter.ISO_DATE) + " to " + maxDate.format(DateTimeFormatter.ISO_DATE)
                    : "N/A";

            // Create report dialog
            Dialog<Void> reportDialog = new Dialog<>();
            reportDialog.setTitle("Sales Report");
            reportDialog.setHeaderText("Performance Summary");
            reportDialog.setResizable(true);

            // Style dialog
            DialogPane pane = reportDialog.getDialogPane();
            pane.getStylesheets().add(getClass().getResource("/css/owner.css").toExternalForm());
            pane.setPrefSize(800, 600);

            VBox reportContent = new VBox(25);
            reportContent.setStyle("-fx-padding: 30;");

            // Summary Statistics Section
            Label summaryTitle = new Label("Summary Statistics");
            summaryTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            VBox summaryStats = new VBox(10);
            summaryStats.getChildren().addAll(
                    createSummaryRow("Total Revenue", formatPrice(totalRevenue) + " TL"),
                    createSummaryRow("Total Orders", String.valueOf(totalOrdersCount)),
                    createSummaryRow("Delivered Orders", String.valueOf(deliveredOrdersCount)),
                    createSummaryRow("Average Order Value", formatPrice(averageOrderValue) + " TL"),
                    createSummaryRow("Total Items Sold", String.format("%.2f kg", totalItemsSoldKg)),
                    createSummaryRow("Period", periodStr));

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
                    .limit(10) // Top 10 products
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

            ScrollPane scrollPane = new ScrollPane(reportContent);
            scrollPane.setFitToWidth(true);
            pane.setContent(scrollPane);

            ButtonType closeButton = ButtonType.CLOSE;
            pane.getButtonTypes().add(closeButton);

            reportDialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error generating report: " + e.getMessage());
        }
    }

    private HBox createSummaryRow(String label, String value) {
        HBox row = new HBox(20);
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-font-weight: bold; -fx-min-width: 150;");
        Label val = new Label(value);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private String formatPrice(double price) {
        return String.format("%.2f", price);
    }

    private void showError(String message) {
        toast(message, ToastService.Type.ERROR);
    }

    private void showSuccess(String message) {
        toast(message, ToastService.Type.SUCCESS);
    }

    private void showInfo(String message) {
        toast(message, ToastService.Type.INFO);
    }

    private void toast(String message, ToastService.Type type) {
        if (dashboardContainer != null && dashboardContainer.getScene() != null) {
            ToastService.show(dashboardContainer.getScene(), message, type);
        }
    }

    private javafx.scene.layout.HBox createListItemBase() {
        javafx.scene.layout.HBox item = new javafx.scene.layout.HBox(15);
        item.getStyleClass().add("list-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle(
                "-fx-padding: 12; -fx-background-color: rgba(30, 41, 59, 0.4); -fx-background-radius: 8; -fx-cursor: hand;");
        item.setOnMouseEntered(e -> item.setStyle(
                "-fx-padding: 12; -fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 8; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle(
                "-fx-padding: 12; -fx-background-color: rgba(30, 41, 59, 0.4); -fx-background-radius: 8; -fx-cursor: hand;"));
        return item;
    }

    private javafx.scene.layout.VBox createPlaceholder(String message) {
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox();
        box.setAlignment(Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(40));
        Label label = new Label(message);
        label.getStyleClass().add("muted");
        label.setStyle("-fx-font-style: italic;");
        box.getChildren().add(label);
        return box;
    }

    private void handleEditProduct(Product product) {
        showProductDialog(product);
    }

    private void handleRemoveProduct(Product product) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Product");
        confirm.setHeaderText("Remove " + product.getName());
        confirm.setContentText("Choose how to remove this product:");

        ButtonType deactivateBtn = new ButtonType("Deactivate");
        ButtonType deleteBtn = new ButtonType("Delete", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deactivateBtn, deleteBtn, ButtonType.CANCEL);

        confirm.showAndWait().ifPresent(res -> {
            if (res == deactivateBtn) {
                if (productDAO.deactivateProduct(product.getId())) {
                    showSuccess("Product deactivated successfully.");
                    loadProducts();
                } else {
                    showError("Failed to deactivate product.");
                }
            } else if (res == deleteBtn) {
                if (productDAO.deleteProduct(product.getId())) {
                    showSuccess("Product deleted successfully.");
                    loadProducts();
                } else {
                    showError("Failed to delete product.");
                }
            }
        });
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
            showError("Error during logout: " + e.getMessage());
        }
    }
}
