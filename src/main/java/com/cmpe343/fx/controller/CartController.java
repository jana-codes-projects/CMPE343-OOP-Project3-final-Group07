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

        loadCart();
    }

    private void loadCart() {
        if (!Session.isLoggedIn())
            return;

        CartDao.CartLoadResult res = cartDao.getCartItemsWithStockCheck(Session.getUser().getId());

        if (!res.warnings.isEmpty()) {
            StringBuilder sb = new StringBuilder("⚠️ Stok Uyarısı:\n");
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
        for (CartItem item : currentCartItems) {
            cartItemsContainer.getChildren().add(createCartItemRow(item));
        }
        updateTotal();
    }

    private HBox createCartItemRow(CartItem item) {
        HBox row = new HBox(16);
        row.getStyleClass().add("cart-item");
        row.setAlignment(Pos.CENTER_LEFT);

        // 1. Image Placeholder
        Label img = new Label(item.getProduct().getName().substring(0, 1).toUpperCase());
        img.getStyleClass().add("cart-item-image");

        // 2. Info
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(item.getProduct().getName());
        name.getStyleClass().add("cart-item-title");
        Label meta = new Label(item.getProduct().getType() + " • " + item.getUnitPrice() + " ₺/kg");
        meta.getStyleClass().add("cart-item-meta");
        info.getChildren().addAll(name, meta);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Price & Quantity
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label total = new Label(String.format("%.2f ₺", item.getLineTotal()));
        total.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
        Label qty = new Label(item.getQuantityKg() + " kg");
        qty.getStyleClass().add("cart-item-meta");
        priceBox.getChildren().addAll(total, qty);

        // 4. Remove Button
        Button removeBtn = new Button();
        javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
        trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        trashIcon.setFill(javafx.scene.paint.Color.web("#f87171"));
        removeBtn.setGraphic(trashIcon);

        removeBtn.getStyleClass().add("btn-remove");
        removeBtn.setOnAction(e -> {
            cartDao.remove(Session.getUser().getId(), item.getProduct().getId());
            currentCartItems.remove(item);
            renderCartItems(); // Re-render
            ToastService.show(cartItemsContainer.getScene(), "Item removed", ToastService.Type.INFO,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(1));
        });

        row.getChildren().addAll(img, info, spacer, priceBox, removeBtn);
        return row;
    }

    private void updateTotal() {
        double total = currentCartItems.stream().mapToDouble(CartItem::getLineTotal).sum();
        totalLabel.setText(String.format("Total: %.2f ₺", total));
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
            ToastService.show(cartItemsContainer.getScene(), "Sepet boş.", ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }

        // Validate Date (Reused logic, simplified)
        if (deliveryDatePicker.getValue() == null || deliveryTimeField.getText().isBlank()) {
            ToastService.show(cartItemsContainer.getScene(), "Teslimat zamanı girilmeli.", ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }

        try {
            LocalDate d = deliveryDatePicker.getValue();
            LocalTime t = LocalTime.parse(deliveryTimeField.getText().trim());
            LocalDateTime requested = LocalDateTime.of(d, t);

            // Basic check
            if (requested.isBefore(LocalDateTime.now())) {
                ToastService.show(cartItemsContainer.getScene(), "Geçmiş zaman seçilemez.", ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
                return;
            }

            // Create Order
            int orderId = orderDao.createOrder(Session.getUser().getId(), currentCartItems, requested);

            // Clear cart from DB after order
            cartDao.clear(Session.getUser().getId());
            currentCartItems.clear();
            renderCartItems();

            ToastService.show(cartItemsContainer.getScene(), "Sipariş alındı! #" + orderId, ToastService.Type.SUCCESS,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));

        } catch (Exception e) {
            e.printStackTrace();
            ToastService.show(cartItemsContainer.getScene(), "Hata: " + e.getMessage(), ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
        }
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) cartItemsContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);

            // CSS Transfer
            if (stage.getScene() != null) {
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            }

            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
