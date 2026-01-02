package com.cmpe343.fx.controller;

import com.cmpe343.fx.util.ToastService;
import com.cmpe343.dao.CartDao;
import com.cmpe343.dao.OrderDao;
import com.cmpe343.dao.CouponDao;
import com.cmpe343.dao.ProductDao;
import com.cmpe343.fx.Session;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.model.CartItem;
import com.cmpe343.model.Product;
import javafx.fxml.FXML;
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
import javafx.fxml.FXMLLoader;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
            byte[] imageBytes = productDao.getProductImageBlob(item.getProduct().getId());
            if (imageBytes != null) {
                image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
            }

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

        // Calculate LOYALTY discount first (based on customer's order history)
        double loyaltyDiscount = orderDao.calculateLoyaltyDiscount(Session.getUser().getId(), subtotal);
        double subtotalAfterLoyalty = Math.max(0, subtotal - loyaltyDiscount);

        // Calculate coupon discount (applied after loyalty)
        double couponDiscount = 0.0;
        if (selectedCouponId != null) {
            com.cmpe343.model.Coupon coupon = couponDao.getCouponById(selectedCouponId);
            if (coupon != null) {
                couponDiscount = coupon.calculateDiscount(subtotalAfterLoyalty);
            } else {
                // Coupon became invalid - clear selection
                selectedCouponId = null;
                couponComboBox.setValue("No Coupon");
                couponDiscountLabel.setText("");
            }
        }
        double totalAfterDiscount = Math.max(0, subtotalAfterLoyalty - couponDiscount);
        double vat = Math.round((totalAfterDiscount * 0.20) * 100.0) / 100.0;
        double finalTotal = totalAfterDiscount + vat;

        // Display total with breakdown
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Subtotal: %.2f ₺", subtotal));
        if (loyaltyDiscount > 0) {
            double percent = orderDao.getLoyaltyDiscountPercent(Session.getUser().getId()) * 100;
            sb.append(String.format(" | Loyalty (%.0f%%): -%.2f ₺", percent, loyaltyDiscount));
        }
        if (couponDiscount > 0) {
            sb.append(String.format(" | Coupon: -%.2f ₺", couponDiscount));
        }
        sb.append(String.format(" | VAT: %.2f ₺ | Total: %.2f ₺", vat, finalTotal));
        totalLabel.setText(sb.toString());
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

        if (deliveryDatePicker.getValue() == null || deliveryTimeField.getText().isBlank()) {
            ToastService.show(cartItemsContainer.getScene(), "Delivery time must be entered.", ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }

        try {
            LocalDate d = deliveryDatePicker.getValue();
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
            if (requested.isBefore(LocalDateTime.now())) {
                ToastService.show(cartItemsContainer.getScene(), "Cannot select past time.", ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
                return;
            }

            LocalDateTime maxDeliveryTime = LocalDateTime.now().plusHours(48);
            if (requested.isAfter(maxDeliveryTime)) {
                ToastService.show(cartItemsContainer.getScene(),
                        "Delivery must be within 48 hours!", ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
                return;
            }

            double subtotal = currentCartItems.stream().mapToDouble(i -> i.getLineTotal()).sum();
            if (subtotal < 50.0) {
                ToastService.show(cartItemsContainer.getScene(),
                        "Minimum cart value is 50.00 ₺.", ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
                return;
            }

            if (selectedCouponId != null) {
                if (couponDao.getCouponById(selectedCouponId) == null) {
                    ToastService.show(cartItemsContainer.getScene(), "Coupon invalid.", ToastService.Type.ERROR,
                            ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
                    return;
                }
            }

            double discount = 0.0;
            if (selectedCouponId != null) {
                com.cmpe343.model.Coupon c = couponDao.getCouponById(selectedCouponId);
                if (c != null)
                    discount = c.calculateDiscount(subtotal);
            }
            double totalAfterDiscount = Math.max(0, subtotal - discount);
            double vat = Math.round((totalAfterDiscount * 0.20) * 100.0) / 100.0;
            double finalTotal = totalAfterDiscount + vat;

            com.cmpe343.model.User user = Session.getUser();
            if (user.getBalance() < finalTotal) {
                ToastService.show(cartItemsContainer.getScene(), "Insufficient balance!", ToastService.Type.ERROR,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
                return;
            }

            // Prepare Order Object
            com.cmpe343.model.Order orderObj = new com.cmpe343.model.Order();
            orderObj.setCustomerId(user.getId());
            orderObj.setOrderTime(LocalDateTime.now());
            orderObj.setRequestedDeliveryTime(requested);
            orderObj.setTotalBeforeTax(totalAfterDiscount);
            orderObj.setVat(vat);
            orderObj.setTotalAfterTax(finalTotal);
            orderObj.setItems(new ArrayList<>(currentCartItems));
            orderObj.setStatus(com.cmpe343.model.Order.OrderStatus.CREATED);

            int orderId = orderDao.createOrder(user.getId(), currentCartItems, requested, selectedCouponId);
            if (orderId > 0) {
                orderObj.setId(orderId);

                // Invoice Persistence (BLOB/CLOB)
                try {
                    com.cmpe343.service.PdfService pdfService = new com.cmpe343.service.PdfService();
                    File pdfFile = pdfService.generateInvoice(orderObj);
                    String invoiceText = pdfService.generateInvoiceText(orderObj);
                    new com.cmpe343.dao.InvoiceDao().saveInvoice(orderId, pdfFile, invoiceText);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Balance & Loyalty
                com.cmpe343.dao.UserDao uDao = new com.cmpe343.dao.UserDao();
                uDao.updateWalletBalance(user.getId(), -finalTotal);
                double pts = finalTotal * 0.01;
                uDao.updateWalletBalance(user.getId(), pts);
                user.setBalance(user.getBalance() - finalTotal + pts);

                cartDao.clear(user.getId());
                currentCartItems.clear();
                renderCartItems();
                ToastService.show(cartItemsContainer.getScene(), "Order placed! #" + orderId, ToastService.Type.SUCCESS,
                        ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
            }
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
