package com.cmpe343.fx.controller;

import com.cmpe343.dao.OrderDao;
import com.cmpe343.dao.CartDao;
import com.cmpe343.fx.Session;
import com.cmpe343.model.Order;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrdersController {

    @FXML
    private VBox ordersContainer;

    // Header fields
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label avatarLetter;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label resultInfoLabel;
    @FXML
    private Label cartCountBadge;
    // Removed searchField

    private final OrderDao orderDao = new OrderDao();
    private final CartDao cartDao = new CartDao();
    private List<Order> userOrders = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn())
            return;

        // Initialize header
        String username = Session.getUser().getUsername();
        if (usernameLabel != null)
            usernameLabel.setText(username);
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome back, " + username);
        if (avatarLetter != null && !username.isEmpty())
            avatarLetter.setText(username.substring(0, 1).toUpperCase());
        if (resultInfoLabel != null)
            resultInfoLabel.setText("Your Orders");

        updateCartBadge();

        // Load CSS
        javafx.application.Platform.runLater(() -> {
            if (ordersContainer.getScene() != null) {
                ordersContainer.getScene().getStylesheets().clear();
                ordersContainer.getScene().getStylesheets()
                        .add(getClass().getResource("/css/base.css").toExternalForm());
                ordersContainer.getScene().getStylesheets()
                        .add(getClass().getResource("/css/customer.css").toExternalForm());
                ordersContainer.getScene().getStylesheets()
                        .add(getClass().getResource("/css/cart.css").toExternalForm());
            }
        });

        loadOrders();
    }

    private void updateCartBadge() {
        if (cartCountBadge != null) {
            int count = cartDao.getCartItemCount(Session.getUser().getId());
            if (count > 0) {
                cartCountBadge.setText(String.valueOf(count));
                cartCountBadge.setVisible(true);
            } else {
                cartCountBadge.setVisible(false);
            }
        }
    }

    private void loadOrders() {
        if (!Session.isLoggedIn())
            return;
        userOrders = orderDao.getOrdersByUserId(Session.getUser().getId());
        renderOrders();
    }

    private void renderOrders() {
        ordersContainer.getChildren().clear();

        if (userOrders.isEmpty()) {
            VBox emptyBox = new VBox(16);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 60 20;");

            SVGPath boxIcon = new SVGPath();
            boxIcon.setContent(
                    "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zm-7-2h-2v-2h2v2zm0-4h-2V7h2v6z");
            boxIcon.setFill(javafx.scene.paint.Color.web("#64748b"));
            boxIcon.setScaleX(2.0);
            boxIcon.setScaleY(2.0);

            Label emptyTitle = new Label("No Orders Yet");
            emptyTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

            Label emptySub = new Label("You haven't placed any orders yet.");
            emptySub.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

            Button shopButton = new Button("Start Shopping");
            shopButton.getStyleClass().add("btn-primary");
            shopButton.setOnAction(e -> handleBack());

            emptyBox.getChildren().addAll(boxIcon, emptyTitle, emptySub, shopButton);
            ordersContainer.getChildren().add(emptyBox);
        } else {
            for (Order order : userOrders) {
                ordersContainer.getChildren().add(createOrderRow(order));
            }
        }
    }

    private VBox createOrderRow(Order order) {
        // Main container for the row (Header + Details)
        VBox rowContainer = new VBox(0);
        rowContainer.getStyleClass().addAll("card", "cart-item");
        rowContainer.setStyle("-fx-padding: 0; -fx-background-color: rgba(30, 41, 59, 0.5);");

        // 1. Header (Always visible summary)
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 16; -fx-cursor: hand;");

        // Icon / Status Indicator
        StackPane iconContainer = new StackPane();
        iconContainer.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.1); -fx-background-radius: 8; -fx-min-width: 48; -fx-min-height: 48;");
        SVGPath icon = new SVGPath();
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            icon.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
            icon.setFill(javafx.scene.paint.Color.web("#10b981"));
            iconContainer.setStyle(
                    "-fx-background-color: rgba(16, 185, 129, 0.1); -fx-background-radius: 8; -fx-min-width: 48; -fx-min-height: 48;");
        } else {
            icon.setContent(
                    "M20 8h-3V4H3v16h11V8h6zm-2 10h-2v-2h2v2zm0-4h-2v-2h2v2zm-2-5H5V6h11v2zm-4 7h-2v-2h2v2zm0-4h-2v-2h2v2z");
            icon.setFill(javafx.scene.paint.Color.web("#818cf8"));
        }
        iconContainer.getChildren().add(icon);

        // Order Info
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        Label ref = new Label("Order #" + order.getId());
        ref.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        Label date = new Label(order.getOrderTime().format(dtf));
        date.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        info.getChildren().addAll(ref, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badge
        Label statusBadge = new Label(order.getStatus().name());
        statusBadge.getStyleClass().add("badge");
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            statusBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399;");
        } else if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            statusBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #f87171;");
        } else {
            statusBadge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #60a5fa;");
        }

        // Total Price
        Label total = new Label(String.format("%.2f ₺", order.getTotalAfterTax()));
        total.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px; -fx-min-width: 100; -fx-alignment: center-right;");

        // Chevron for expand indication
        SVGPath chevron = new SVGPath();
        chevron.setContent("M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z");
        chevron.setFill(javafx.scene.paint.Color.web("#94a3b8"));

        header.getChildren().addAll(iconContainer, info, spacer, statusBadge, total, chevron);

        // 2. Details (Hidden by default)
        VBox detailsBox = new VBox(0);
        detailsBox.setVisible(false);
        detailsBox.setManaged(false);
        detailsBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.5); -fx-padding: 0;");

        // Toggle logic
        header.setOnMouseClicked(e -> {
            boolean isExpanded = !detailsBox.isVisible();
            detailsBox.setVisible(isExpanded);
            detailsBox.setManaged(isExpanded);

            // Rotate chevron
            chevron.setRotate(isExpanded ? 180 : 0);

            if (isExpanded && detailsBox.getChildren().isEmpty()) {
                // Load details lazily
                loadOrderDetails(order.getId(), detailsBox);
            }
        });

        rowContainer.getChildren().addAll(header, detailsBox);
        return rowContainer;
    }

    private void loadOrderDetails(int orderId, VBox container) {
        // Show loading or just fetch
        List<CartItem> items = orderDao.getOrderItems(orderId);

        VBox content = new VBox(8);
        content.setStyle("-fx-padding: 16;");

        Label itemsTitle = new Label("Items in this order:");
        itemsTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-padding: 0 0 8 0;");
        content.getChildren().add(itemsTitle);

        for (CartItem item : items) {
            HBox itemRow = new HBox(12);
            itemRow.setStyle("-fx-padding: 8; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0;");
            itemRow.setAlignment(Pos.CENTER_LEFT);

            VBox itemInfo = new VBox(2);
            Label name = new Label(item.getProduct().getName());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");

            Label qty = new Label(String.format("%.2f kg x %.2f ₺", item.getQuantityKg(), item.getUnitPrice()));
            qty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

            itemInfo.getChildren().addAll(name, qty);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label subtotal = new Label(String.format("%.2f ₺", item.getLineTotal()));
            subtotal.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");

            itemRow.getChildren().addAll(itemInfo, sp, subtotal);
            content.getChildren().add(itemRow);
        }

        container.getChildren().add(content);
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) ordersContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            if (stage.getScene() != null)
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Removed handleClearSearch

    @FXML
    private void handleOpenCart() {
        try {
            Stage stage = (Stage) ordersContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cart.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            if (stage.getScene() != null)
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenOrders() {
        loadOrders(); // Refresh
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Stage stage = (Stage) ordersContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            stage.setScene(scene);
            stage.setTitle("Gr7Project3 - Login");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
