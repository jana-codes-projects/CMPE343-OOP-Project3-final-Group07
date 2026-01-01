package com.cmpe343.fx.controller;

import com.cmpe343.dao.CartDao;
import com.cmpe343.dao.OrderDao;
import com.cmpe343.fx.Session;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.model.CartItem;
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
    private ComboBox<String> couponComboBox;
    @FXML
    private Label couponDiscountLabel;

    private final CartDao cartDao = new CartDao();
    private final OrderDao orderDao = new OrderDao();
    private final com.cmpe343.dao.CouponDao couponDao = new com.cmpe343.dao.CouponDao();
    private final com.cmpe343.dao.ProductDao productDao = new com.cmpe343.dao.ProductDao();
    private java.util.List<CartItem> currentCartItems = new java.util.ArrayList<>();
    private Integer selectedCouponId = null;

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

        loadCoupons();
        loadCart();
    }

    private void loadCoupons() {
        if (couponComboBox == null)
            return;
        couponComboBox.getItems().clear();
        couponComboBox.getItems().add("No Coupon");
        couponComboBox.setValue("No Coupon");

        java.util.List<com.cmpe343.model.Coupon> coupons = couponDao
                .getActiveCouponsForCustomer(Session.getUser().getId());
        for (com.cmpe343.model.Coupon coupon : coupons) {
            String display;
            if (coupon.getKind() == com.cmpe343.model.Coupon.CouponKind.AMOUNT) {
                display = coupon.getCode() + " (-" + coupon.getValue() + " TL)";
            } else {
                display = coupon.getCode() + " (-" + coupon.getValue() + "%)";
            }
            couponComboBox.getItems().add(display);
        }

        couponComboBox.setOnAction(e -> {
            String selected = couponComboBox.getValue();
            if (selected == null || selected.equals("No Coupon")) {
                selectedCouponId = null;
                couponDiscountLabel.setText("");
            } else {
                // Extract coupon code from display string
                String code = selected.split(" ")[0];
                com.cmpe343.model.Coupon coupon = couponDao.getCouponByCode(code);
                if (coupon != null) {
                    selectedCouponId = coupon.getId();
                    // Calculate actual discount based on current cart total
                    double cartTotal = currentCartItems.stream()
                            .mapToDouble(item -> Math.round(item.getLineTotal() * 100.0) / 100.0)
                            .sum();
                    double discount = coupon.calculateDiscount(cartTotal);
                    couponDiscountLabel.setText("Discount: -" + String.format("%.2f", discount) + " TL");
                }
            }
            updateTotal();
        });
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
        for (CartItem item : currentCartItems) {
            cartItemsContainer.getChildren().add(createCartItemRow(item));
        }
        updateTotal();
    }

    private HBox createCartItemRow(CartItem item) {
        HBox row = new HBox(16);
        row.getStyleClass().add("cart-item");
        row.setAlignment(Pos.CENTER_LEFT);

        // 1. Image - fetch from BLOB using product ID
        Node imageNode;
        // Reuse the ProductDao instance instead of creating a new one for each cart
        // item
        try {
            javafx.scene.image.Image image = null;
            // byte[] imageBytes =
            // productDao.getProductImageBlob(item.getProduct().getId());
            // if (imageBytes != null) {
            // image = new javafx.scene.image.Image(new
            // java.io.ByteArrayInputStream(imageBytes));
            // }

            if (image != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(image);
                iv.setFitWidth(50);
                iv.setFitHeight(50);
                iv.setPreserveRatio(true);
                imageNode = iv;
            } else {
                throw new Exception("No image available");
            }
        } catch (Exception e) {
            // Fallback to placeholder
            Label img = new Label(item.getProduct().getName().substring(0, 1).toUpperCase());
            img.getStyleClass().add("cart-item-image");
            imageNode = img;
        }

        // Wrap in VBox for alignment if needed, or just add directly
        // The original was just a Label. Let's wrap in a StackPane or VBox if we want
        // to keep style class on wrapper
        StackPane imgContainer = new StackPane(imageNode);
        imgContainer.getStyleClass().add("cart-item-image-container"); // New class if needed, or just let it be

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

        row.getChildren().addAll(imgContainer, info, spacer, priceBox, removeBtn);
        return row;
    }

    private void updateTotal() {
        // Round each line total before summing to match order_items precision
        // This ensures cart display matches order totals
        double subtotal = currentCartItems.stream()
                .mapToDouble(item -> Math.round(item.getLineTotal() * 100.0) / 100.0)
                .sum();
        double discount = 0.0;
        if (selectedCouponId != null) {
            com.cmpe343.model.Coupon coupon = couponDao.getCouponById(selectedCouponId);
            if (coupon != null) {
                discount = coupon.calculateDiscount(subtotal);
            } else {
                // Coupon became invalid - clear selection
                selectedCouponId = null;
                couponComboBox.setValue("No Coupon");
                couponDiscountLabel.setText("");
            }
        }
        double totalAfterDiscount = Math.max(0, subtotal - discount);
        double vat = Math.round((totalAfterDiscount * 0.20) * 100.0) / 100.0;
        double finalTotal = totalAfterDiscount + vat;

        // Display total with breakdown if discount is applied
        if (discount > 0) {
            totalLabel.setText(String.format("Subtotal: %.2f ₺ | Discount: -%.2f ₺ | VAT: %.2f ₺ | Total: %.2f ₺",
                    subtotal, discount, vat, finalTotal));
        } else {
            totalLabel.setText(String.format("Subtotal: %.2f ₺ | VAT: %.2f ₺ | Total: %.2f ₺",
                    subtotal, vat, finalTotal));
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
            ToastService.show(cartItemsContainer.getScene(), "Delivery time must be entered.", ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }

        try {
            LocalDate d = deliveryDatePicker.getValue();

            // Validate time format before parsing
            String timeText = deliveryTimeField.getText().trim();
            LocalTime t;
            try {
                t = LocalTime.parse(timeText);
            } catch (java.time.format.DateTimeParseException parseEx) {
                ToastService.show(cartItemsContainer.getScene(),
                        "Invalid time format! Please enter time as HH:mm (e.g., 14:30)",
                        ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
                return;
            }

            LocalDateTime requested = LocalDateTime.of(d, t);

            // Basic check - cannot select past time
            if (requested.isBefore(LocalDateTime.now())) {
                ToastService.show(cartItemsContainer.getScene(), "Cannot select past time.", ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
                return;
            }

            // 48 HOUR MAXIMUM DELIVERY TIME CHECK
            LocalDateTime maxDeliveryTime = LocalDateTime.now().plusHours(48);
            if (requested.isAfter(maxDeliveryTime)) {
                ToastService.show(cartItemsContainer.getScene(),
                        "Delivery must be within 48 hours! Maximum: " + maxDeliveryTime
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
                return;
            }

            // MINIMUM CART VALUE CHECK (50 TL)
            double subtotal = currentCartItems.stream().mapToDouble(i -> i.getLineTotal()).sum();
            double minCartValue = 50.0;
            if (subtotal < minCartValue) {
                ToastService.show(cartItemsContainer.getScene(),
                        String.format("Minimum cart value is %.2f ₺. Current: %.2f ₺", minCartValue, subtotal),
                        ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
                return;
            }

            // Create Order
            // Validate coupon one more time before placing order to catch race conditions
            if (selectedCouponId != null) {
                com.cmpe343.model.Coupon coupon = couponDao.getCouponById(selectedCouponId);
                if (coupon == null) {
                    ToastService.show(cartItemsContainer.getScene(),
                            "The selected coupon is no longer valid. Please remove it and try again.",
                            ToastService.Type.ERROR,
                            ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
                    selectedCouponId = null;
                    couponComboBox.setValue("No Coupon");
                    couponDiscountLabel.setText("");
                    updateTotal();
                    return;
                }
            }

            int orderId = orderDao.createOrder(Session.getUser().getId(), currentCartItems, requested,
                    selectedCouponId);

            // Clear cart from DB after order
            cartDao.clear(Session.getUser().getId());
            currentCartItems.clear();
            renderCartItems();

            ToastService.show(cartItemsContainer.getScene(), "Order placed! #" + orderId, ToastService.Type.SUCCESS,
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
            boolean wasMaximized = stage.isMaximized();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);

            // CSS Transfer
            if (stage.getScene() != null) {
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            }

            stage.setScene(scene);
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
