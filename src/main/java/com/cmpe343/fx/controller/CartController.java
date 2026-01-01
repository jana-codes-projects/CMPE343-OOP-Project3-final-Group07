package com.cmpe343.fx.controller;

import com.cmpe343.dao.CartDao;
import com.cmpe343.dao.OrderDao;
import com.cmpe343.fx.Session;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.model.CartItem;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CartController {

    @FXML
    private VBox cartItemsContainer;

    @FXML
    private DatePicker deliveryDatePicker;
    @FXML
    private TextField deliveryTimeField;
    @FXML
    private Label totalLabel;
    @FXML
    private Button placeOrderButton;

    private final CartDao cartDao = new CartDao();
    private final OrderDao orderDao = new OrderDao();
    private java.util.List<CartItem> currentCartItems = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn())
            return;

        // Load CSS manually to avoid FXML warnings
        javafx.application.Platform.runLater(() -> {
            if (cartItemsContainer.getScene() != null) {
                cartItemsContainer.getScene().getStylesheets().clear();
                cartItemsContainer.getScene().getStylesheets()
                        .add(getClass().getResource("/css/base.css").toExternalForm());
                cartItemsContainer.getScene().getStylesheets()
                        .add(getClass().getResource("/css/cart.css").toExternalForm());
            }
        });

        // Time Formatter (HH:mm)
        deliveryTimeField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([01]?[0-9]|2[0-3])?:?[0-5]?[0-9]?")) {
                // Auto-insert colon
                if (change.isAdded() && newText.length() == 2 && !newText.contains(":")) {
                    change.setText(change.getText() + ":");
                    change.setCaretPosition(change.getCaretPosition() + 1);
                    change.setAnchor(change.getAnchor() + 1);
                }
                return change;
            }
            return null;
        }));

        loadCart();
    }

    private void loadCart() {
        if (!Session.isLoggedIn())
            return;

        CartDao.CartLoadResult res = cartDao.getCartItemsWithStockCheck(Session.getUser().getId());

        if (!res.warnings.isEmpty()) {
            StringBuilder sb = new StringBuilder("⚠️ Stock Warning:\n");
            for (String w : res.warnings)
                sb.append("- ").append(w).append("\n");
            ToastService.show(cartItemsContainer.getScene(), sb.toString(), ToastService.Type.INFO,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(5));
        }

        currentCartItems = res.items;
        renderCartItems();
    }

    private void renderCartItems() {
        cartItemsContainer.getChildren().clear();
        
        if (currentCartItems.isEmpty()) {
            // Empty cart message
            VBox emptyBox = new VBox(16);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 60 20;");
            
            javafx.scene.shape.SVGPath cartIcon = new javafx.scene.shape.SVGPath();
            cartIcon.setContent("M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.54-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z");
            cartIcon.setFill(javafx.scene.paint.Color.web("#64748b"));
            cartIcon.setScaleX(2.0);
            cartIcon.setScaleY(2.0);
            
            Label emptyTitle = new Label("Your Cart is Empty");
            emptyTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
            
            Label emptySub = new Label("Add products to start shopping");
            emptySub.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            
            Button shopButton = new Button("Start Shopping");
            shopButton.getStyleClass().add("btn-primary");
            shopButton.setOnAction(e -> handleBack());
            
            emptyBox.getChildren().addAll(cartIcon, emptyTitle, emptySub, shopButton);
            cartItemsContainer.getChildren().add(emptyBox);
        } else {
            for (CartItem item : currentCartItems) {
                cartItemsContainer.getChildren().add(createCartItemRow(item));
            }
        }
        updateTotal();
        updatePlaceOrderButton();
    }

    private void updatePlaceOrderButton() {
        if (placeOrderButton != null) {
            placeOrderButton.setDisable(currentCartItems.isEmpty());
        }
    }

    private HBox createCartItemRow(CartItem item) {
        HBox row = new HBox(16);
        row.getStyleClass().addAll("card", "cart-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 16; -fx-cursor: default;");

        // 1. Image
        Node imageNode;
        if (item.getProduct().getImageBlob() != null && item.getProduct().getImageBlob().length > 0) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(
                                new java.io.ByteArrayInputStream(item.getProduct().getImageBlob())));
                iv.setFitWidth(50);
                iv.setFitHeight(50);
                iv.setPreserveRatio(true);
                imageNode = iv;
            } catch (Exception e) {
                Label img = new Label(item.getProduct().getName().substring(0, 1).toUpperCase());
                img.getStyleClass().add("cart-item-image");
                imageNode = img;
            }
        } else {
            Label img = new Label(item.getProduct().getName().substring(0, 1).toUpperCase());
            img.getStyleClass().add("cart-item-image");
            imageNode = img;
        }

        StackPane imgContainer = new StackPane(imageNode);
        imgContainer.getStyleClass().add("cart-item-image-container");

        // 2. Info
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(item.getProduct().getName());
        name.getStyleClass().add("detail-value");
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        Label meta = new Label(item.getProduct().getType() + " • " + item.getUnitPrice() + " ₺/kg");
        meta.getStyleClass().add("muted");
        info.getChildren().addAll(name, meta);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Price & Quantity
        VBox priceBox = new VBox(8);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        Label total = new Label(String.format("%.2f ₺", item.getLineTotal()));
        total.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");

        if (item.hasDiscount()) {
            Label discountBadge = new Label("10% OFF");
            discountBadge.getStyleClass().addAll("badge", "badge-success");
            discountBadge.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            priceBox.getChildren().add(discountBadge);
        }

        // Quantity Controls
        HBox qtyControls = new HBox(4);
        qtyControls.setAlignment(Pos.CENTER);
        qtyControls.getStyleClass().add("qty-controls");
        qtyControls.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 4; -fx-padding: 2;");

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().addAll("btn-icon", "btn-sm"); // Assuming we might add btn-sm or just style inline
        minusBtn.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 6; -fx-min-width: 24;");

        Label qtyLabel = new Label(String.format("%.1f", item.getQuantityKg()));
        qtyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 0 4;");

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().addAll("btn-icon", "btn-sm");
        plusBtn.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 6; -fx-min-width: 24;");

        minusBtn.setOnAction(e -> handleUpdateQuantity(item, -0.5));
        plusBtn.setOnAction(e -> handleUpdateQuantity(item, 0.5));

        qtyControls.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        priceBox.getChildren().addAll(total, qtyControls);

        // 4. Remove Button
        Button removeBtn = new Button();
        javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
        trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        trashIcon.setFill(javafx.scene.paint.Color.web("#f87171"));
        removeBtn.setGraphic(trashIcon);

        removeBtn.getStyleClass().add("btn-outline");
        removeBtn.setStyle("-fx-border-color: #ef4444; -fx-padding: 6 10;");

        removeBtn.setOnAction(e -> {
            cartDao.remove(Session.getUser().getId(), item.getProduct().getId());
            currentCartItems.remove(item);
            renderCartItems();
            ToastService.show(cartItemsContainer.getScene(), "Item removed", ToastService.Type.INFO,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(1));
        });

        row.getChildren().addAll(imgContainer, info, spacer, priceBox, removeBtn);
        return row;
    }

    private void handleUpdateQuantity(CartItem item, double change) {
        double newQty = item.getQuantityKg() + change;

        // Check for negative/zero
        if (newQty <= 0) {
            // Confirm removal? For now just remove
            cartDao.remove(Session.getUser().getId(), item.getProduct().getId());
            currentCartItems.remove(item);
            renderCartItems();
            return;
        }

        // Check Stock
        if (newQty > item.getProduct().getStockKg()) {
            ToastService.show(cartItemsContainer.getScene(), "Maximum stock amount: " + item.getProduct().getStockKg() + " kg",
                    ToastService.Type.WARNING,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }

        // Update DB
        cartDao.updateQuantity(Session.getUser().getId(), item.getProduct().getId(), newQty);

        // Update Local Model
        item.setQuantityKg(newQty);

        // Refresh UI (Full refresh to update totals)
        renderCartItems();
    }

    private void updateTotal() {
        double total = currentCartItems.stream().mapToDouble(CartItem::getLineTotal).sum();
        if (totalLabel != null) {
            totalLabel.setText(String.format("%.2f ₺", total));
        }
    }

    @FXML
    private void handleClear() {
        if (currentCartItems.isEmpty())
            return;
        cartDao.clear(Session.getUser().getId());
        currentCartItems.clear();
        renderCartItems();
    }

    @FXML
    private void handlePlaceOrder() {
        if (currentCartItems.isEmpty()) {
            ToastService.show(cartItemsContainer.getScene(), "Cart is empty.", ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }

        // Validate Date (Reused logic, simplified)
        if (deliveryDatePicker.getValue() == null || deliveryTimeField.getText().isBlank()) {
            ToastService.show(cartItemsContainer.getScene(), "Please enter delivery date and time.", ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }

        try {
            LocalDate d = deliveryDatePicker.getValue();
            LocalTime t = LocalTime.parse(deliveryTimeField.getText().trim());
            LocalDateTime requested = LocalDateTime.of(d, t);

            // Basic check
            if (requested.isBefore(LocalDateTime.now())) {
                ToastService.show(cartItemsContainer.getScene(), "Cannot select past time.", ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
                return;
            }

            // Create Order
            int orderId = orderDao.createOrder(Session.getUser().getId(), currentCartItems, requested);

            // Generate Invoice
            orderDao.createInvoice(orderId, currentCartItems);

            // Clear cart from DB after order
            cartDao.clear(Session.getUser().getId());
            currentCartItems.clear();
            renderCartItems();

            ToastService.show(cartItemsContainer.getScene(), "Order placed successfully! #" + orderId, ToastService.Type.SUCCESS,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));

        } catch (Exception e) {
            e.printStackTrace();
            ToastService.show(cartItemsContainer.getScene(), "Error: " + e.getMessage(), ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
        }
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) cartItemsContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            // CSS Transfer
            if (stage.getScene() != null) {
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            }

            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
