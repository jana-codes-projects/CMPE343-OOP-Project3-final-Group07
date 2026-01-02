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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private Label ordersCountLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private javafx.scene.layout.StackPane chartContainer;
    @FXML
    private FlowPane dashboardContainer;

    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab loyaltyTab;
    @FXML
    private VBox loyaltyReportContainer;
    @FXML
    private Tab carriersTab;
    @FXML
    private Tab productsTab;
    @FXML
    private Tab ordersTab;
    @FXML
    private Tab messagesTab;
    @FXML
    private Tab couponsTab;
    @FXML
    private Tab ratingsTab;
    @FXML
    private Tab reportsTab;
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

        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == loyaltyTab) {
                    loadLoyaltyReport();
                } else if (newTab == carriersTab) {
                    loadCarriers();
                } else if (newTab == ratingsTab) {
                    loadCarrierRatings();
                } else if (newTab == productsTab) {
                    loadProducts();
                } else if (newTab == ordersTab) {
                    loadOrders();
                } else if (newTab == messagesTab && Session.isLoggedIn()) {
                    loadConversations();
                } else if (newTab == couponsTab) {
                    loadCoupons();
                }
            });
        }

        loadDashboard();
        loadProducts();
        loadCarriers();
        loadOrders();
        loadCoupons();
        loadCarrierRatings();
        loadLoyaltyReport();
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
                createDetailRow("Low Stock Thresh", String.format("%.2f kg", product.getThresholdKg())),
                createDetailRow("Disc. Threshold", String.format("%.2f kg", product.getDiscountThreshold())),
                createDetailRow("Disc. Percentage", String.format("%.0f%%", product.getDiscountPercentage())));

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

    private void showProductEditMode(Product product) {
        selectedProduct = product;
        productDetailContainer.getChildren().clear();

        VBox card = new VBox(12);
        card.getStyleClass().add("detail-card");

        Label header = new Label("Edit " + product.getName());
        header.getStyleClass().add("detail-header");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));

        TextField nameField = new TextField(product.getName());
        nameField.setPromptText("Name");

        ComboBox<ProductType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(ProductType.values());
        typeCombo.setValue(product.getType());

        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        priceField.setPromptText("Price");

        TextField stockField = new TextField(String.valueOf(product.getStockKg()));
        stockField.setPromptText("Stock");

        TextField thresholdField = new TextField(String.valueOf(product.getThresholdKg()));
        thresholdField.setPromptText("Low Stock Threshold");

        TextField discThreshField = new TextField(String.valueOf(product.getDiscountThreshold()));
        discThreshField.setPromptText("Discount Threshold");

        TextField discPercentField = new TextField(String.valueOf(product.getDiscountPercentage()));
        discPercentField.setPromptText("Discount %");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("Low Thresh:"), 0, 4);
        grid.add(thresholdField, 1, 4);
        grid.add(new Label("Disc. Thresh:"), 0, 5);
        grid.add(discThreshField, 1, 5);
        grid.add(new Label("Disc. %:"), 0, 6);
        grid.add(discPercentField, 1, 6);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                String n = nameField.getText();
                double p = Double.parseDouble(priceField.getText());
                double s = Double.parseDouble(stockField.getText());
                double t = Double.parseDouble(thresholdField.getText());
                double dt = Double.parseDouble(discThreshField.getText());
                double dp = Double.parseDouble(discPercentField.getText());

                Product updated = new Product(product.getId(), n, typeCombo.getValue(), p, s, t, dt, dp,
                        product.getImageBlob());
                productDAO.update(updated);

                com.cmpe343.fx.util.ToastService.show(productDetailContainer.getScene(), "Product Updated",
                        com.cmpe343.fx.util.ToastService.Type.SUCCESS);
                loadProducts(); // Refresh list to update name/badges
                showProductDetail(updated); // Go back to view mode
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Invalid input: " + ex.getMessage());
                alert.showAndWait();
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-outline");
        cancelBtn.setOnAction(e -> showProductDetail(product));

        actions.getChildren().addAll(saveBtn, cancelBtn);

        card.getChildren().addAll(header, grid, actions);
        productDetailContainer.getChildren().add(card);
    }

    // ==================== LOYALTY MANAGEMENT ====================

    @FXML
    private void handleRefreshLoyalty() {
        loadLoyaltyReport();
    }

    private void loadLoyaltyReport() {
        if (loyaltyReportContainer == null)
            return;
        loyaltyReportContainer.getChildren().clear();

        // Header
        HBox header = new HBox(16);
        header.setStyle(
                "-fx-padding: 12 16; -fx-background-color: #f8fafc; -fx-border-color: transparent transparent #e2e8f0 transparent;");
        Label h1 = new Label("Customer");
        h1.setPrefWidth(150);
        h1.setStyle("-fx-font-weight:bold; -fx-text-fill:#64748b;");
        Label h2 = new Label("Total Spent");
        h2.setPrefWidth(100);
        h2.setStyle("-fx-font-weight:bold; -fx-text-fill:#64748b;");
        Label h3 = new Label("Orders");
        h3.setPrefWidth(80);
        h3.setStyle("-fx-font-weight:bold; -fx-text-fill:#64748b;");
        Label h4 = new Label("Calculated Status");
        h4.setPrefWidth(150);
        h4.setStyle("-fx-font-weight:bold; -fx-text-fill:#64748b;");
        Label h5 = new Label("Assigned Level");
        h5.setPrefWidth(140);
        h5.setStyle("-fx-font-weight:bold; -fx-text-fill:#64748b;");

        header.getChildren().addAll(h1, h2, h3, h4, h5);
        loyaltyReportContainer.getChildren().add(header);

        List<Object[]> stats = orderDAO.getCustomerLoyaltyStats();
        // stats: [id, username, order_count, total_spent... ]

        for (Object[] row : stats) {
            int userId = (int) row[0];
            String username = (String) row[1];
            int count = ((Number) row[2]).intValue();
            double spent = ((Number) row[3]).doubleValue();

            // Check current user level
            User u = userDAO.getUserById(userId);
            int currentLevel = (u != null) ? u.getLoyaltyLevel() : 0;

            // Auto Eligibility
            boolean autoEligible = (spent > 5000 || count > 5);

            HBox item = new HBox(16);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-padding: 12 16; -fx-border-color: transparent transparent #f1f5f9 transparent;");

            Label lName = new Label(username);
            lName.setPrefWidth(150);
            lName.setStyle("-fx-font-weight:bold;");
            Label lSpent = new Label(String.format("%.2f ₺", spent));
            lSpent.setPrefWidth(100);
            Label lCount = new Label(String.valueOf(count));
            lCount.setPrefWidth(80);

            Label lStatus = new Label(autoEligible ? "Likely Eligible" : "Standard");
            lStatus.setPrefWidth(150);
            if (autoEligible)
                lStatus.setStyle("-fx-text-fill: #10b981;");
            else
                lStatus.setStyle("-fx-text-fill: #94a3b8;");

            // Level Selector
            ComboBox<String> levelParams = new ComboBox<>();
            levelParams.getItems().addAll("Level 0 (None)", "Level 1 (Loyal)", "Level 2 (VIP)");
            levelParams.setValue("Level " + currentLevel
                    + (currentLevel == 0 ? " (None)" : (currentLevel == 1 ? " (Loyal)" : " (VIP)")));
            levelParams.setPrefWidth(170);

            levelParams.setOnAction(e -> {
                String val = levelParams.getValue();
                int newLevel = 0;
                if (val.startsWith("Level 1"))
                    newLevel = 1;
                else if (val.startsWith("Level 2"))
                    newLevel = 2;

                if (newLevel != currentLevel) {
                    userDAO.updateLoyaltyLevel(userId, newLevel);
                    com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(),
                            "Updated " + username + " to Level " + newLevel,
                            com.cmpe343.fx.util.ToastService.Type.SUCCESS);
                }
            });

            item.getChildren().addAll(lName, lSpent, lCount, lStatus, levelParams);
            loyaltyReportContainer.getChildren().add(item);
        }
    }

    // ==================== CARRIER MANAGEMENT ====================

    private void loadCarriers() {
        carriersListContainer.getChildren().clear();
        carrierDetailContainer.getChildren().clear();

        List<User> carriers = userDAO.getAllCarriers();
        if (carriers.isEmpty()) {
            carriersListContainer.getChildren().add(createPlaceholder("No carriers found."));
            return;
        }

        for (User c : carriers) {
            HBox item = createListItemBase();
            item.setUserData(c.getId()); // Support selection highlighting

            Label name = new Label(c.getUsername());
            name.getStyleClass().add("detail-value");
            name.setPrefWidth(200);

            Label role = new Label("CARRIER");
            role.getStyleClass().addAll("badge", "badge-info");

            item.getChildren().addAll(name, role);
            item.setOnMouseClicked(e -> showCarrierDetail(c));
            carriersListContainer.getChildren().add(item);
        }
    }

    @FXML
    private void handleAddCarrier() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add Carrier");
        dialog.setHeaderText("Create New Carrier Account");

        ButtonType createType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        TextField phone = new TextField();
        phone.setPromptText("Phone");
        TextField address = new TextField();
        address.setPromptText("Address");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phone, 1, 2);
        grid.add(new Label("Address:"), 0, 3);
        grid.add(address, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == createType) {
                return new User(0, username.getText(), "carrier", phone.getText(), address.getText(), true);
            }
            return null;
        });

        // We need to capture the password too, but User model doesn't hold it.
        // We can just use the fields directly if result is present.

        dialog.showAndWait().ifPresent(u -> {
            String pwd = password.getText();
            if (u.getUsername().isEmpty() || pwd.isEmpty()) {
                com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Username/Password required",
                        com.cmpe343.fx.util.ToastService.Type.ERROR);
                return;
            }
            if (userDAO.createCarrier(u.getUsername(), pwd, u.getAddress(), u.getPhone())) {
                com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Carrier created",
                        com.cmpe343.fx.util.ToastService.Type.SUCCESS);
                loadCarriers();
            } else {
                com.cmpe343.fx.util.ToastService.show(logoutButton.getScene(), "Failed (Username taken?)",
                        com.cmpe343.fx.util.ToastService.Type.ERROR);
            }
        });
    }

    @FXML
    private void handleRefreshCarriers() {
        loadCarriers();
    }

    private void showCarrierDetail(User carrier) {
        carrierDetailContainer.getChildren().clear();
        VBox card = new VBox(12); // Incressed spacing
        card.getStyleClass().add("detail-card");
        // Apply card style manually if detail-card isn't sufficient or if we want the
        // generic card look
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 24;");

        Label header = new Label(carrier.getUsername());
        header.getStyleClass().add("h2"); // Use h2 instead of detail-header

        // Rating Stats
        RatingDao.RatingStats stats = ratingDAO.getCarrierStats(carrier.getId());

        HBox ratingBox = new HBox(8);
        ratingBox.setAlignment(Pos.CENTER_LEFT);

        // Star display
        Label starIcon = new Label("★");
        starIcon.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 20px;");

        Label ratingVal = new Label(String.format("%.1f", stats.average()));
        ratingVal.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label ratingCount = new Label("(" + stats.count() + " ratings)");
        ratingCount.getStyleClass().add("muted");

        ratingBox.getChildren().addAll(starIcon, ratingVal, ratingCount);

        card.getChildren().addAll(header, ratingBox);

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.1; -fx-padding: 8 0;");
        card.getChildren().add(sep);

        card.getChildren().add(createDetailRow("Phone", carrier.getPhone() != null ? carrier.getPhone() : "-"));
        card.getChildren().add(createDetailRow("Address", carrier.getAddress() != null ? carrier.getAddress() : "-"));

        Button toggleBtn = new Button(carrier.isActive() ? "Deactivate" : "Activate");
        toggleBtn.getStyleClass().add(carrier.isActive() ? "btn-danger" : "btn-primary"); // Use btn-danger
        toggleBtn.setOnAction(e -> {
            if (carrier.isActive())
                userDAO.deactivateCarrier(carrier.getId());
            else
                userDAO.activateCarrier(carrier.getId());
            loadCarriers(); // Reload
            User updated = userDAO.getUserById(carrier.getId());
            if (updated != null)
                showCarrierDetail(updated);
        });

        HBox actions = new HBox(10);
        actions.setPadding(new javafx.geometry.Insets(16, 0, 0, 0));
        actions.getChildren().add(toggleBtn);

        card.getChildren().add(actions);

        carrierDetailContainer.getChildren().add(card);
    }

    // ==================== ORDER MANAGEMENT ====================

    private void loadOrders() {
        ordersListContainer.getChildren().clear();
        orderDetailContainer.getChildren().clear();

        List<Order> orders = orderDAO.getAllOrders();
        if (orders.isEmpty()) {
            ordersListContainer.getChildren().add(createPlaceholder("No orders found."));
            if (ordersCountLabel != null)
                ordersCountLabel.setText("All Orders (0)");
            return;
        }

        if (ordersCountLabel != null)
            ordersCountLabel.setText("All Orders (" + orders.size() + ")");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd HH:mm");

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
                default -> "badge-neutral";
            };
            status.getStyleClass().addAll("badge", badgeClass);
            status.setPrefWidth(100);

            Label date = new Label(o.getOrderTime().format(fmt));
            date.getStyleClass().add("muted");
            date.setPrefWidth(120);

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Label total = new Label(formatPrice(o.getTotalAfterTax()));
            total.getStyleClass().add("detail-value");
            total.setStyle("-fx-font-weight: bold;");

            item.getChildren().addAll(id, status, date, spacer, total);
            item.setOnMouseClicked(e -> showOrderDetail(o));
            ordersListContainer.getChildren().add(item);
        }
    }

    private void showOrderDetail(Order order) {
        orderDetailContainer.getChildren().clear();
        VBox card = new VBox(16);
        card.getStyleClass().addAll("detail-card", "card");
        card.setStyle("-fx-padding: 24;");

        Label title = new Label("Order #" + order.getId());
        title.getStyleClass().add("detail-header");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: white; -fx-padding: 0 0 8 0;");

        Separator separator = new Separator();
        separator.setStyle("-fx-opacity: 0.1; -fx-padding: 8 0;");

        VBox meta = new VBox(10);
        String statusBadge = switch (order.getStatus()) {
            case CREATED -> "🔵 Created";
            case ASSIGNED -> "🟡 Assigned";
            case DELIVERED -> "✅ Delivered";
            case CANCELLED -> "❌ Cancelled";
            default -> order.getStatus().name();
        };
        meta.getChildren().addAll(
                createDetailRow("Status", statusBadge),
                createDetailRow("Total", formatPrice(order.getTotalAfterTax())),
                createDetailRow("Date", order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Separator itemsSeparator = new Separator();
            itemsSeparator.setStyle("-fx-opacity: 0.1; -fx-padding: 12 0;");

            Label itemsHeader = new Label("Order Items");
            itemsHeader.getStyleClass().add("h3");
            itemsHeader
                    .setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: white; -fx-padding: 8 0 8 0;");

            VBox itemsBox = new VBox(8);
            itemsBox.setStyle("-fx-padding: 8 0;");
            for (CartItem ci : order.getItems()) {
                String pName = ci.getProduct() != null ? ci.getProduct().getName() : "Product";
                HBox itemRow = new HBox(12);
                itemRow.setAlignment(Pos.CENTER_LEFT);

                Label itemLabel = new Label(String.format("• %s", pName));
                itemLabel.getStyleClass().add("detail-value");
                itemLabel.setStyle("-fx-font-weight: 500;");

                Label qtyLabel = new Label(String.format("%.1f kg", ci.getQuantityKg()));
                qtyLabel.getStyleClass().add("muted");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label priceLabel = new Label(formatPrice(ci.getLineTotal()));
                priceLabel.getStyleClass().add("detail-value");
                priceLabel.setStyle("-fx-font-weight: 600;");

                itemRow.getChildren().addAll(itemLabel, qtyLabel, spacer, priceLabel);
                itemsBox.getChildren().add(itemRow);
            }
            meta.getChildren().addAll(itemsSeparator, itemsHeader, itemsBox);
        }

        card.getChildren().addAll(title, separator, meta);
        orderDetailContainer.getChildren().add(card);
    }

    // Helper method defining list item style
    private HBox createListItemBase() {
        HBox item = new HBox(12);
        item.getStyleClass().addAll("list-item-base", "card");
        item.setStyle(
                "-fx-padding: 14 18; -fx-cursor: hand; -fx-alignment: center-left; -fx-background-radius: 10; -fx-border-radius: 10;");
        item.setPrefWidth(Double.MAX_VALUE);

        item.setOnMouseEntered(e -> {
            if (item.getProperties().get("selected") == null) {
                item.setStyle(
                        "-fx-background-color: rgba(99, 102, 241, 0.2); -fx-padding: 14 18; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #6366f1; -fx-alignment: center-left; -fx-translate-x: 4;");
            }
        });
        item.setOnMouseExited(e -> {
            if (item.getProperties().get("selected") == null) {
                item.setStyle(
                        "-fx-padding: 14 18; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-radius: 10; -fx-alignment: center-left; -fx-translate-x: 0;");
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
        header.setStyle(
                "-fx-padding: 16; -fx-background-color: rgba(30, 41, 59, 0.8); -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setStyle(
                "-fx-background-color: #6366f1; -fx-background-radius: 50%; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
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

            // If incoming message and not read, mark it!
            if (!isOwnerMsg && !m.isRead()) {
                messageDAO.markAsRead(m.getId());
            }

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
        inputArea.setStyle(
                "-fx-padding: 16; -fx-background-color: rgba(30, 41, 59, 0.8); -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1 0 0 0;");
        inputArea.setAlignment(Pos.CENTER_LEFT);

        TextField replyField = new TextField();
        replyField.setPromptText("Type a message...");
        replyField.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.8); -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-padding: 10 16;");
        HBox.setHgrow(replyField, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setStyle(
                "-fx-background-color: #6366f1; -fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: 600; -fx-padding: 10 24; -fx-cursor: hand;");
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(
                "-fx-background-color: #4f46e5; -fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: 600; -fx-padding: 10 24; -fx-cursor: hand;"));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle(
                "-fx-background-color: #6366f1; -fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: 600; -fx-padding: 10 24; -fx-cursor: hand;"));

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

    private Node createChatBubble(String sender, String text, LocalDateTime time, boolean isOwner) {
        VBox bubble = new VBox(6);
        bubble.setMaxWidth(400);

        if (isOwner) {
            // Sent message (Owner) - Blue
            bubble.setStyle("-fx-padding: 12 16; -fx-background-radius: 18 18 2 18; -fx-background-color: #6366f1;");
        } else {
            // Received message (Customer) - Dark gray
            bubble.setStyle(
                    "-fx-padding: 12 16; -fx-background-radius: 18 18 18 2; -fx-background-color: rgba(30, 41, 59, 0.8);");
        }

        Label txt = new Label(text);
        txt.setWrapText(true);
        txt.setStyle(
                "-fx-text-fill: " + (isOwner ? "white" : "#f8fafc") + "; -fx-font-size: 14px; -fx-line-spacing: 2px;");
        txt.setMaxWidth(350);

        HBox footer = new HBox(6);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        Label timeLbl = new Label(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLbl.setStyle(
                "-fx-text-fill: " + (isOwner ? "rgba(255,255,255,0.7)" : "#94a3b8") + "; -fx-font-size: 10px;");

        if (isOwner) {
            // Add checkmark icon for sent messages
            Label checkmark = new Label("✓");
            checkmark.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 9px; -fx-font-weight: bold;");
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
            kind.getStyleClass().addAll("badge",
                    c.getKind() == Coupon.CouponKind.PERCENT ? "badge-info" : "badge-warning");

            Label value = new Label(c.getKind() == Coupon.CouponKind.PERCENT ? String.format("%.0f%%", c.getValue())
                    : String.format("%.2f ₺", c.getValue()));
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
                createDetailRow("Value",
                        coupon.getKind() == Coupon.CouponKind.PERCENT ? String.format("%.0f%%", coupon.getValue())
                                : String.format("%.2f ₺", coupon.getValue())),
                createDetailRow("Min Cart", String.format("%.2f ₺", coupon.getMinCart())),
                createDetailRow("Status", coupon.isActive() ? "✅ Active" : "❌ Inactive"),
                createDetailRow("Expires",
                        coupon.getExpiresAt() != null
                                ? coupon.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                : "Never"));

        HBox actions = new HBox(12);
        actions.getStyleClass().add("actions-container");
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setStyle("-fx-padding: 10 20;");
        editBtn.setOnAction(e -> showCouponDialog(coupon));

        Button toggleBtn = new Button(coupon.isActive() ? "Deactivate" : "Activate");
        toggleBtn.getStyleClass().add(coupon.isActive() ? "btn-outline" : "btn-primary");
        toggleBtn.setStyle(coupon.isActive() ? "-fx-border-color: #ef4444; -fx-text-fill: #f87171; -fx-padding: 10 20;"
                : "-fx-padding: 10 20;");
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
                    LocalDateTime expires = expiresPicker.getValue() != null ? expiresPicker.getValue().atTime(23, 59)
                            : null;
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

    // ==================== RATINGS ====================

    private void loadCarrierRatings() {
        ratingsContainer.getChildren().clear();

        List<Rating> ratings = ratingDAO.getAllRatings();
        if (ratings.isEmpty()) {
            ratingsContainer.getChildren().add(createPlaceholder("No ratings submitted yet."));
            return;
        }

        // We specifically want to show Carrier Name, Score, Comment, Date

        for (Rating r : ratings) {
            HBox item = createListItemBase();
            item.setUserData(r.getId());

            User carrier = userDAO.getUserById(r.getCarrierId()); // Assuming getting user by ID is possible
            String carrierName = (carrier != null) ? carrier.getUsername() : "Unknown Carrier";

            Label name = new Label(carrierName);
            name.getStyleClass().add("detail-value");
            name.setStyle("-fx-font-weight: bold;");
            name.setPrefWidth(150);

            HBox stars = new HBox(2);
            for (int i = 0; i < 5; i++) {
                Label star = new Label("★");
                if (i < r.getRating()) {
                    star.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 14px;");
                } else {
                    star.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
                }
                stars.getChildren().add(star);
            }
            stars.setPrefWidth(100);

            Label comment = new Label(r.getComment());
            comment.getStyleClass().add("muted");
            comment.setWrapText(true);
            comment.setMaxWidth(400); // Limit width

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label date = new Label(r.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd")));
            date.getStyleClass().add("muted");
            date.setStyle("-fx-font-size: 11px;");

            item.getChildren().addAll(name, stars, comment, spacer, date);
            ratingsContainer.getChildren().add(item);
        }
    }

    @FXML
    private void handleRefreshRatings() {
        loadCarrierRatings();
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
        // If the chart container is undefined in FXML (e.g. if I missed adding it to
        // the class), we skip
        // But assuming chartContainer is defined as @FXML private StackPane
        // chartContainer;
        // Wait, I need to check if chartContainer is defined. It was in my previous
        // edits.

        // Actually, let's check if chartContainer is defined in the class fields.
        // It's just a StackPane.

        if (chartContainer == null) {
            System.err.println("chartContainer is null");
            return;
        }

        chartContainer.getChildren().clear();

        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setLabel("Product");
        yAxis.setLabel("Stock (kg)");

        javafx.scene.chart.BarChart<String, Number> barChart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        barChart.setTitle("Product Stock Levels");

        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Stock");

        List<Product> products = productDAO.findAll();
        for (Product p : products) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(p.getName(), p.getStockKg()));
        }

        barChart.getData().add(series);
        chartContainer.getChildren().add(barChart);
    }

    private void loadDashboard() {
        if (dashboardContainer == null)
            return;
        dashboardContainer.getChildren().clear();

        // Get statistics
        List<Product> products = productDAO.findAll();
        List<Order> orders = orderDAO.getAllOrders();
        List<User> carriers = userDAO.getAllCarriers();
        List<Coupon> coupons = couponDAO.getAllCoupons();

        long activeProducts = products.stream().filter(p -> p.getStockKg() > 0).count();
        long lowStockProducts = products.stream().filter(Product::isLowStock).count();
        long pendingOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CREATED || o.getStatus() == OrderStatus.ASSIGNED).count();
        long activeCoupons = coupons.stream().filter(Coupon::isActive).count();

        // Create dashboard cards
        dashboardContainer.getChildren().addAll(
                createDashboardCard("Total Products", String.valueOf(products.size()), "#6366f1", "📦"),
                createDashboardCard("Active Products", String.valueOf(activeProducts), "#10b981", "✅"),
                createDashboardCard("Low Stock", String.valueOf(lowStockProducts), "#ef4444", "⚠️"),
                createDashboardCard("Total Orders", String.valueOf(orders.size()), "#3b82f6", "📋"),
                createDashboardCard("Pending Orders", String.valueOf(pendingOrders), "#f59e0b", "⏳"),
                createDashboardCard("Carriers", String.valueOf(carriers.size()), "#8b5cf6", "🚚"),
                createDashboardCard("Active Coupons", String.valueOf(activeCoupons), "#ec4899", "🎫"));
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
        showProductEditMode(product);
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
        TextField discountThresholdField = new TextField();
        discountThresholdField.setPromptText("Discount Threshold (kg)");

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
            discountThresholdField.setText(String.valueOf(existing.getDiscountThreshold()));
            if (existing.getImageBlob() != null) {
                imageLabel.setText("Current Image Loaded (" + existing.getImageBlob().length + " bytes)");
                imageBlobHolder[0] = existing.getImageBlob();
            }
        } else {
            discountThresholdField.setText("5.0"); // Default
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
        grid.add(new Label("Low Stock Threshold:"), 0, 4);
        grid.add(thresholdField, 1, 4);
        grid.add(new Label("Discount Threshold:"), 0, 5);
        grid.add(discountThresholdField, 1, 5);
        grid.add(new Label("Image:"), 0, 6);
        grid.add(new HBox(10, uploadBtn, imageLabel), 1, 6);

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
                    double discThreshold = Double.parseDouble(discountThresholdField.getText());

                    if (existing == null) {
                        return new Product(0, name, typeCombo.getValue(), price, stock, threshold, discThreshold,
                                imageBlobHolder[0]);
                    } else {
                        return new Product(existing.getId(), name, typeCombo.getValue(), price, stock, threshold,
                                discThreshold,
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
