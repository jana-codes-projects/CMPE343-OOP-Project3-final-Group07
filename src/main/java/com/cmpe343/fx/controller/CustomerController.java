package com.cmpe343.fx.controller;

import com.cmpe343.dao.CartDao;
import com.cmpe343.dao.OrderDao;
import com.cmpe343.dao.ProductDao;
import com.cmpe343.dao.UserDao;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.fx.Session;
import com.cmpe343.model.Product;
import com.cmpe343.model.CartItem;
import com.cmpe343.model.Order;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class CustomerController {

    @FXML
    private TextField searchField;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label cartCountBadge;

    @FXML
    private FlowPane vegetablesGrid;
    @FXML
    private FlowPane fruitsGrid;
    @FXML
    private Label vegetablesToggle;
    @FXML
    private Label fruitsToggle;
    
    @FXML
    private TabPane mainTabPane;
    
    // Cart tab fields
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
    
    // Orders tab fields
    @FXML
    private VBox ordersContainer;
    
    private boolean vegetablesVisible = true;
    private boolean fruitsVisible = true;

    private final ProductDao productDao = new ProductDao();
    private final CartDao cartDao = new CartDao();
    private final OrderDao orderDao = new OrderDao();
    private final com.cmpe343.dao.CouponDao couponDao = new com.cmpe343.dao.CouponDao();
    private final com.cmpe343.dao.InvoiceDAO invoiceDAO = new com.cmpe343.dao.InvoiceDAO();
    private final com.cmpe343.dao.MessageDao messageDao = new com.cmpe343.dao.MessageDao();
    private final com.cmpe343.dao.UserDao userDao = new com.cmpe343.dao.UserDao();

    private static boolean imagesPopulated = false; // Flag to ensure images are only populated once
    private FilteredList<Product> filteredProducts;
    private int currentCustomerId;
    
    // Cart state
    private List<CartItem> currentCartItems = new ArrayList<>();
    private Integer selectedCouponId = null;
    
    // Orders state
    private List<Order> userOrders = new ArrayList<>();
    
    // Chat widget fields
    @FXML
    private VBox chatContainer;
    @FXML
    private VBox chatMessagesBox;
    @FXML
    private TextField chatInput;
    @FXML
    private ScrollPane chatScroll;

    @FXML
    public void initialize() {
        if (Session.isLoggedIn()) {
            this.currentCustomerId = Session.getUser().getId();
            usernameLabel.setText(Session.getUser().getUsername());
        }

        // Populate product images on first initialization (only if needed)
        // This can be optimized to check if images already exist in the database
        populateProductImagesIfNeeded();

        // Load Data
        ObservableList<Product> allProducts = FXCollections.observableArrayList(productDao.findAll());
        filteredProducts = new FilteredList<>(allProducts, p -> true);

        // Search Listener
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredProducts.setPredicate(p -> {
                if (newV == null || newV.isBlank())
                    return true;
                return p.getName().toLowerCase().contains(newV.toLowerCase());
            });
            renderGrids();
        });

        renderGrids();
        updateBadge();

        // Ensure CSS
        Platform.runLater(() -> {
            if (searchField.getScene() != null) {
                searchField.getScene().getStylesheets().clear();
                searchField.getScene().getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
                searchField.getScene().getStylesheets()
                        .add(getClass().getResource("/css/customer.css").toExternalForm());
                searchField.getScene().getStylesheets()
                        .add(getClass().getResource("/css/cart.css").toExternalForm());
            }
        });
        
        // Set up tab selection listeners
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null) {
                    String tabText = newTab.getText();
                    if ("Cart".equals(tabText)) {
                        loadCoupons();
                        loadCart();
                    } else if ("Orders".equals(tabText)) {
                        loadOrders();
                    }
                }
            });
        }
    }

    private void renderGrids() {
        vegetablesGrid.getChildren().clear();
        fruitsGrid.getChildren().clear();

        for (Product p : filteredProducts) {
            Node card = createProductCard(p);
            if (p.getType() == Product.ProductType.VEGETABLE) {
                vegetablesGrid.getChildren().add(card);
            } else if (p.getType() == Product.ProductType.FRUIT) {
                fruitsGrid.getChildren().add(card);
            } else {
                // Default fallback
                vegetablesGrid.getChildren().add(card);
            }
        }
    }

    private Node createProductCard(Product p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");

        // Image - fetch from BLOB using product ID
        Node imageNode;
        try {
            byte[] imageBytes = productDao.getProductImageBlob(p.getId());
            if (imageBytes != null && imageBytes.length > 0) {
                javafx.scene.image.Image image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(image);
                iv.setFitWidth(120);
                iv.setFitHeight(120);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                imageNode = iv;
            } else {
                throw new Exception("No image available");
            }
        } catch (Exception e) {
            // Fallback to placeholder
            Label img = new Label(p.getName().substring(0, 1).toUpperCase());
            img.getStyleClass().add("product-image-placeholder");
            imageNode = img;
        }

        // Wrapper for image to ensure centering and styling
        VBox imgContainer = new VBox(imageNode);
        imgContainer.setAlignment(Pos.CENTER);
        imgContainer.getStyleClass().add("product-image-container");

        // Info
        Label nameLbl = new Label(p.getName());
        nameLbl.getStyleClass().add("product-title");

        // Calculate price with threshold pricing (double if stock <= threshold)
        double displayPrice = p.getPrice();
        if (p.getStockKg() <= p.getThresholdKg()) {
            displayPrice = p.getPrice() * 2.0;
        }
        Label priceLbl = new Label(String.format("%.2f ₺ / kg", displayPrice));
        priceLbl.getStyleClass().add("product-price");

        // Calculate available stock (current stock - items in cart)
        double cartQuantity = cartDao.getCartQuantity(currentCustomerId, p.getId());
        double availableStock = p.getStockKg() - cartQuantity;
        
        Label stockLbl;
        if (availableStock <= 0) {
            stockLbl = new Label("Out of Stock");
            stockLbl.getStyleClass().addAll("stock-tag", "stock-low");
            stockLbl.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        } else {
            stockLbl = new Label(String.format("%.2f kg", availableStock));
            stockLbl.getStyleClass().addAll("stock-tag", availableStock <= p.getThresholdKg() ? "stock-low" : "stock-ok");
        }

        // Controls
        TextField kgInput = new TextField();
        kgInput.setPromptText("kg");
        kgInput.setPrefWidth(60);
        kgInput.getStyleClass().add("field");
        kgInput.setStyle("-fx-alignment: center; -fx-padding: 6;");

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        addBtn.setOnAction(e -> handleAddToCart(p, kgInput));

        HBox actions = new HBox(8, kgInput, addBtn);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(imgContainer, nameLbl, priceLbl, stockLbl, actions);
        return card;
    }

    private void handleAddToCart(Product p, TextField kgInput) {
        try {
            double kg = Double.parseDouble(kgInput.getText().trim());
            if (kg <= 0) {
                toast("Invalid amount", ToastService.Type.ERROR);
                return;
            }
            
            // Calculate available stock (current stock - items already in cart)
            double cartQuantity = cartDao.getCartQuantity(currentCustomerId, p.getId());
            double availableStock = p.getStockKg() - cartQuantity;
            
            if (availableStock <= 0) {
                toast("Out of stock!", ToastService.Type.ERROR);
                return;
            }
            
            if (kg > availableStock) {
                toast("Insufficient stock! Available: " + String.format("%.2f", availableStock) + " kg", ToastService.Type.ERROR);
                return;
            }

            cartDao.addToCart(currentCustomerId, p.getId(), kg);
            toast("Added to cart", ToastService.Type.SUCCESS);
            kgInput.clear();
            updateBadge();
            // Refresh product display to show updated stock
            refreshProductDisplay();
        } catch (NumberFormatException e) {
            toast("Enter valid number", ToastService.Type.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            toast("Error: " + e.getMessage(), ToastService.Type.ERROR);
        }
    }

    private void updateBadge() {
        int count = cartDao.getCartItemCount(currentCustomerId);
        if (count > 0) {
            cartCountBadge.setText(String.valueOf(count));
            cartCountBadge.setVisible(true);
        } else {
            cartCountBadge.setVisible(false);
        }
    }
    
    private void refreshProductDisplay() {
        // Reload products from database to get updated stock
        ObservableList<Product> allProducts = FXCollections.observableArrayList(productDao.findAll());
        filteredProducts = new FilteredList<>(allProducts, p -> {
            // Apply current search filter
            String searchText = searchField.getText();
            if (searchText == null || searchText.isBlank()) {
                return true;
            }
            return p.getName().toLowerCase().contains(searchText.toLowerCase());
        });
        renderGrids();
    }

    @FXML
    private void handleOpenCart() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(1); // Select Cart tab (index 1)
            loadCoupons();
            loadCart();
        }
    }
    
    @FXML
    private void handleViewOrders() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(2); // Select Orders tab (index 2)
            loadOrders();
        }
    }
    
    // ========== CART METHODS ==========
    
    private void loadCoupons() {
        if (couponComboBox == null) return;
        couponComboBox.getItems().clear();
        couponComboBox.getItems().add("No Coupon");
        couponComboBox.setValue("No Coupon");
        
        List<com.cmpe343.model.Coupon> coupons = couponDao.getActiveCouponsForCustomer(Session.getUser().getId());
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
        if (cartItemsContainer == null) return;

        CartDao.CartLoadResult res = cartDao.getCartItemsWithStockCheck(Session.getUser().getId());

        if (!res.warnings.isEmpty()) {
            StringBuilder sb = new StringBuilder("⚠️ Stock Warning:\n");
            for (String w : res.warnings)
                sb.append("- ").append(w).append("\n");
            toast(sb.toString(), ToastService.Type.INFO);
        }

        currentCartItems = res.items;
        renderCartItems();
    }
    
    private void renderCartItems() {
        if (cartItemsContainer == null) return;
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
        try {
            javafx.scene.image.Image image = null;
            byte[] imageBytes = productDao.getProductImageBlob(item.getProduct().getId());
            if (imageBytes != null && imageBytes.length > 0) {
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

        StackPane imgContainer = new StackPane(imageNode);
        imgContainer.getStyleClass().add("cart-item-image-container");

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
        SVGPath trashIcon = new SVGPath();
        trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        trashIcon.setFill(javafx.scene.paint.Color.web("#f87171"));
        removeBtn.setGraphic(trashIcon);

        removeBtn.getStyleClass().add("btn-remove");
        removeBtn.setOnAction(e -> {
            cartDao.remove(Session.getUser().getId(), item.getProduct().getId());
            currentCartItems.remove(item);
            renderCartItems(); // Re-render
            updateBadge();
            toast("Item removed", ToastService.Type.INFO);
        });

        row.getChildren().addAll(imgContainer, info, spacer, priceBox, removeBtn);
        return row;
    }
    
    private void updateTotal() {
        if (totalLabel == null) return;
        // Round each line total before summing to match order_items precision
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
    private void handlePlaceOrder() {
        if (cartItemsContainer == null || currentCartItems.isEmpty()) {
            toast("Cart is empty.", ToastService.Type.ERROR);
            return;
        }

        // Validate Date
        if (deliveryDatePicker.getValue() == null || deliveryTimeField.getText().isBlank()) {
            toast("Delivery time must be entered.", ToastService.Type.ERROR);
            return;
        }

        try {
            LocalDate d = deliveryDatePicker.getValue();
            LocalTime t = LocalTime.parse(deliveryTimeField.getText().trim());
            LocalDateTime requested = LocalDateTime.of(d, t);

            // Basic check
            if (requested.isBefore(LocalDateTime.now())) {
                toast("Cannot select past time.", ToastService.Type.ERROR);
                return;
            }

            // Validate coupon one more time before placing order
            // This validation must match OrderDao.getCouponDiscount() logic:
            // 1. Check if coupon exists and is active/not expired
            // 2. Calculate subtotal after loyalty discount (same as OrderDao.createOrder does)
            // 3. Validate that subtotalAfterLoyalty meets the coupon's min_cart requirement
            if (selectedCouponId != null) {
                com.cmpe343.model.Coupon coupon = couponDao.getCouponById(selectedCouponId);
                if (coupon == null) {
                    toast("The selected coupon is no longer valid. Please remove it and try again.", ToastService.Type.ERROR);
                    selectedCouponId = null;
                    couponComboBox.setValue("No Coupon");
                    couponDiscountLabel.setText("");
                    updateTotal();
                    return;
                }
                
                // Calculate subtotal (same logic as updateTotal and OrderDao.createOrder)
                double subtotal = currentCartItems.stream()
                    .mapToDouble(item -> Math.round(item.getLineTotal() * 100.0) / 100.0)
                    .sum();
                
                // Calculate loyalty discount first (same as OrderDao.createOrder)
                double loyaltyDiscount = orderDao.calculateLoyaltyDiscount(Session.getUser().getId(), subtotal);
                double subtotalAfterLoyalty = Math.max(0, subtotal - loyaltyDiscount);
                
                // Validate that subtotalAfterLoyalty meets the coupon's min_cart requirement
                // This matches the validation in OrderDao.getCouponDiscount()
                if (subtotalAfterLoyalty < coupon.getMinCart()) {
                    toast(String.format("Cart total (after loyalty discount) %.2f ₺ does not meet coupon minimum requirement of %.2f ₺. Please add more items or remove the coupon.", 
                        subtotalAfterLoyalty, coupon.getMinCart()), ToastService.Type.ERROR);
                    return;
                }
            }
            
            int orderId = orderDao.createOrder(Session.getUser().getId(), currentCartItems, requested, selectedCouponId);

            // Clear cart from DB after order
            cartDao.clear(Session.getUser().getId());
            currentCartItems.clear();
            renderCartItems();
            updateBadge();

            toast("Order placed! #" + orderId, ToastService.Type.SUCCESS);
            
            // Switch to Orders tab to show the new order
            if (mainTabPane != null) {
                mainTabPane.getSelectionModel().select(2);
                loadOrders();
            }

        } catch (Exception e) {
            e.printStackTrace();
            toast("Error: " + e.getMessage(), ToastService.Type.ERROR);
        }
    }
    
    // ========== ORDERS METHODS ==========
    
    private void loadOrders() {
        if (!Session.isLoggedIn())
            return;
        if (ordersContainer == null) return;
        userOrders = orderDao.getOrdersForCustomer(Session.getUser().getId());
        renderOrders();
    }
    
    private void renderOrders() {
        if (ordersContainer == null) return;
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

            emptyBox.getChildren().addAll(boxIcon, emptyTitle, emptySub);
            ordersContainer.getChildren().add(emptyBox);
        } else {
            for (Order order : userOrders) {
                ordersContainer.getChildren().add(createOrderRow(order));
            }
        }
    }
    
    private VBox createOrderRow(Order order) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "cart-item");
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16;");

        // Main row (clickable to view details)
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(e -> handleViewDetails(order));

        // 1. Icon / Status Indicator
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

        // 2. Order Info
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

        // 3. Status Badge
        Label statusBadge = new Label(order.getStatus().name());
        statusBadge.getStyleClass().add("badge");
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            statusBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399;");
        } else if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            statusBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #f87171;");
        } else {
            statusBadge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #60a5fa;");
        }

        // 4. Total Price
        Label total = new Label(String.format("%.2f ₺", order.getTotalAfterTax()));
        total.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px; -fx-min-width: 100; -fx-alignment: center-right;");

        row.getChildren().addAll(iconContainer, info, spacer, statusBadge, total);
        card.getChildren().add(row);
        
        // Add product images section
        List<CartItem> orderItems = orderDao.getOrderItems(order.getId());
        if (orderItems != null && !orderItems.isEmpty()) {
            Label itemsLabel = new Label("Items:");
            itemsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 0 4 0;");
            
            FlowPane imagesContainer = new FlowPane();
            imagesContainer.setHgap(8);
            imagesContainer.setVgap(8);
            imagesContainer.setStyle("-fx-padding: 4 0;");
            
            for (CartItem item : orderItems) {
                if (item.getProduct() != null) {
                    try {
                        byte[] imageBytes = productDao.getProductImageBlob(item.getProduct().getId());
                        if (imageBytes != null && imageBytes.length > 0) {
                            javafx.scene.image.Image image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(image);
                            iv.setFitWidth(50);
                            iv.setFitHeight(50);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(true);
                            
                            // Add tooltip with product name and quantity
                            Tooltip tooltip = new Tooltip(
                                item.getProduct().getName() + "\n" + String.format("%.2f kg", item.getQuantityKg())
                            );
                            Tooltip.install(iv, tooltip);
                            
                            imagesContainer.getChildren().add(iv);
                        }
                    } catch (Exception e) {
                        // Skip images that fail to load
                    }
                }
            }
            
            if (!imagesContainer.getChildren().isEmpty()) {
                card.getChildren().add(itemsLabel);
                card.getChildren().add(imagesContainer);
            }
        }
        
        // Add action buttons
        HBox actions = new HBox(8);
        
        // Add cancel button for CREATED and ASSIGNED orders
        if (order.getStatus() == Order.OrderStatus.CREATED || 
            order.getStatus() == Order.OrderStatus.ASSIGNED) {
            Button cancelBtn = new Button("Cancel Order");
            cancelBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 6 12;");
            cancelBtn.setOnAction(e -> handleCancelOrder(order));
            actions.getChildren().add(cancelBtn);
        }
        
        // Add track order button for ASSIGNED orders
        if (order.getStatus() == Order.OrderStatus.ASSIGNED) {
            Button trackBtn = new Button("Track Order");
            trackBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-padding: 6 12;");
            trackBtn.setOnAction(e -> openTrackingScreen(order));
            actions.getChildren().add(trackBtn);
        }
        
        // Add download PDF button if delivered
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            Button downloadBtn = new Button("Download Invoice");
            downloadBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 12;");
            downloadBtn.setOnAction(e -> downloadInvoice(order));
            
            // Check if rating exists
            com.cmpe343.dao.RatingDao ratingDao = new com.cmpe343.dao.RatingDao();
            if (!ratingDao.hasRatingForOrder(order.getId(), currentCustomerId)) {
                Button rateBtn = new Button("Rate Order");
                rateBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 6 12;");
                rateBtn.setOnAction(e -> showRatingDialog(order));
                actions.getChildren().add(rateBtn);
            }
            
            actions.getChildren().add(downloadBtn);
        }
        
        if (!actions.getChildren().isEmpty()) {
            card.getChildren().add(actions);
        }
        
        return card;
    }
    
    private void handleViewDetails(Order order) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Order Details #" + order.getId());
        dialog.setHeaderText("Order Items");

        // CSS
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().addAll(ordersContainer.getScene().getStylesheets());
        pane.getStyleClass().add("dialog-pane");
        pane.setMinWidth(500);
        pane.setMinHeight(400);

        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 20; -fx-background-color: #0f172a;");

        // Items list
        List<CartItem> items = orderDao.getOrderItems(order.getId());

        ScrollPane scroll = new ScrollPane();
        VBox itemsBox = new VBox(8);
        itemsBox.setStyle("-fx-padding: 0 10 0 0;");

        for (CartItem item : items) {
            HBox itemRow = new HBox(12);
            itemRow.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-padding: 12; -fx-background-radius: 8;");
            itemRow.setAlignment(Pos.CENTER_LEFT);

            VBox itemInfo = new VBox(2);
            // Product Name
            Label name = new Label(item.getProduct().getName());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

            // Qty and Unit Price
            Label qty = new Label(String.format("%.2f kg x %.2f ₺", item.getQuantityKg(), item.getUnitPrice()));
            qty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

            itemInfo.getChildren().addAll(name, qty);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            // Subtotal
            Label subtotal = new Label(String.format("%.2f ₺", item.getLineTotal()));
            subtotal.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

            itemRow.getChildren().addAll(itemInfo, sp, subtotal);
            itemsBox.getChildren().add(itemRow);
        }

        scroll.setContent(itemsBox);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-outline");
        closeBtn.setOnAction(e -> dialog.setResult(null));
        closeBtn.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(scroll, closeBtn);

        pane.setContent(content);
        // Remove default buttons
        pane.getButtonTypes().add(ButtonType.CLOSE);
        Node closeNode = pane.lookupButton(ButtonType.CLOSE);
        if (closeNode != null)
            closeNode.setVisible(false);

        dialog.showAndWait();
    }
    
    private void handleCancelOrder(Order order) {
        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Cancel Order");
        confirmDialog.setHeaderText("Cancel Order #" + order.getId() + "?");
        confirmDialog.setContentText("Are you sure you want to cancel this order? The products will be restocked and you will receive a refund.");
        
        java.util.Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean cancelled = orderDao.cancelOrder(order.getId());
                
                if (cancelled) {
                    toast("Order cancelled successfully. Products have been restocked.", ToastService.Type.SUCCESS);
                    // Refresh the orders list
                    loadOrders();
                } else {
                    toast("Failed to cancel order", ToastService.Type.ERROR);
                }
            } catch (RuntimeException e) {
                toast("Error: " + e.getMessage(), ToastService.Type.ERROR);
            } catch (Exception e) {
                e.printStackTrace();
                toast("Failed to cancel order: " + e.getMessage(), ToastService.Type.ERROR);
            }
        }
    }
    
    private void downloadInvoice(com.cmpe343.model.Order order) {
        try {
            byte[] invoicePDF = invoiceDAO.getInvoice(order.getId());
            if (invoicePDF == null) {
                toast("Invoice not available for this order", ToastService.Type.WARNING);
                return;
            }
            
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Save Invoice");
            fileChooser.setInitialFileName("Invoice_Order_" + order.getId() + ".pdf");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            java.io.File saveFile = fileChooser.showSaveDialog(stage);
            
            if (saveFile != null) {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(saveFile)) {
                    fos.write(invoicePDF);
                    toast("Invoice downloaded successfully", ToastService.Type.SUCCESS);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("Failed to download invoice: " + e.getMessage(), ToastService.Type.ERROR);
        }
    }
    
    private void showRatingDialog(com.cmpe343.model.Order order) {
        javafx.scene.control.Dialog<java.util.Map<String, Object>> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Rate Your Order");
        dialog.setHeaderText("How was your delivery experience?");
        dialog.setResizable(true);

        javafx.scene.control.ButtonType submitButtonType = new javafx.scene.control.ButtonType("Submit", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, javafx.scene.control.ButtonType.CANCEL);

        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 20; -fx-background-color: #0f172a;");

        Label ratingLabel = new Label("Rating (1-5):");
        ratingLabel.setStyle("-fx-text-fill: white;");
        javafx.scene.control.Spinner<Integer> ratingSpinner = new javafx.scene.control.Spinner<>(1, 5, 5);
        ratingSpinner.setEditable(true);

        Label commentLabel = new Label("Comment (optional):");
        commentLabel.setStyle("-fx-text-fill: white;");
        javafx.scene.control.TextArea commentArea = new javafx.scene.control.TextArea();
        commentArea.setPrefRowCount(10);
        commentArea.setWrapText(true);

        content.getChildren().addAll(ratingLabel, ratingSpinner, commentLabel, commentArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(600, 500);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("rating", ratingSpinner.getValue());
                result.put("comment", commentArea.getText());
                return result;
            }
            return null;
        });
        
        java.util.Optional<java.util.Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(r -> {
            try {
                com.cmpe343.dao.RatingDao ratingDao = new com.cmpe343.dao.RatingDao();
                if (order.getCarrierId() != null) {
                    ratingDao.createRating(order.getCarrierId(), currentCustomerId, (Integer) r.get("rating"), (String) r.get("comment"));
                    toast("Thank you for your rating!", ToastService.Type.SUCCESS);
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("Failed to submit rating", ToastService.Type.ERROR);
            }
        });
    }
    
    @FXML
    private void handleViewMessages() {
        try {
            Stage stage = new Stage();
            stage.setTitle("Messages");
            VBox root = new VBox(16);
            root.setStyle("-fx-padding: 24; -fx-background-color: #0f172a;");
            
            Label title = new Label("Messages");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
            
            ScrollPane scrollPane = new ScrollPane();
            VBox messagesContainer = new VBox(12);
            messagesContainer.setStyle("-fx-padding: 12;");
            messagesContainer.setId("messagesContainer"); // Add ID for lookup
            
            com.cmpe343.dao.MessageDao messageDao = new com.cmpe343.dao.MessageDao();
            refreshMessagesList(messagesContainer, messageDao);
            
            Button sendMessageBtn = new Button("Send New Message");
            sendMessageBtn.getStyleClass().add("btn-primary");
            sendMessageBtn.setOnAction(e -> {
                handleSendMessage(stage, messagesContainer, messageDao);
            });
            
            HBox titleBar = new HBox(12);
            titleBar.setAlignment(Pos.CENTER_LEFT);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            titleBar.getChildren().addAll(title, spacer, sendMessageBtn);
            
            scrollPane.setContent(messagesContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            
            root.getChildren().addAll(titleBar, scrollPane);
            
            Scene scene = new Scene(root, 700, 500);
            scene.getStylesheets().addAll(searchField.getScene().getStylesheets());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            toast("Failed to load messages", ToastService.Type.ERROR);
        }
    }
    
    private VBox createMessageCard(com.cmpe343.model.Message msg, com.cmpe343.dao.MessageDao messageDao) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: " + (msg.isRead() ? "#1e293b" : "#2563eb") + "; -fx-background-radius: 8; -fx-padding: 16;");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label sender = new Label("To: Owner");
        sender.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label date = new Label(msg.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        date.setStyle("-fx-text-fill: #94a3b8;");
        
        header.getChildren().addAll(sender, spacer, date);
        
        Label content = new Label(msg.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: white;");
        
        card.getChildren().addAll(header, content);
        
        // Show reply if exists
        String replyText = messageDao.getReplyText(msg.getId());
        if (replyText != null && !replyText.trim().isEmpty()) {
            Separator replySep = new Separator();
            replySep.setStyle("-fx-opacity: 0.3; -fx-padding: 8 0;");
            
            Label replyHeader = new Label("Owner's Reply:");
            replyHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981; -fx-font-size: 12px;");
            
            Label replyContent = new Label(replyText);
            replyContent.setWrapText(true);
            replyContent.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            
            card.getChildren().addAll(replySep, replyHeader, replyContent);
        }
        
        if (!msg.isRead()) {
            card.setOnMouseClicked(e -> {
                messageDao.markAsRead(msg.getId());
                card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16;");
            });
        }
        
        return card;
    }
    
    private void handleSendMessage(Stage parentStage, VBox messagesContainer, com.cmpe343.dao.MessageDao messageDao) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Send Message to Owner");
        dialog.setHeaderText("Write your message");
        dialog.setResizable(true);
        
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Enter your message...");
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(15);
        messageArea.getStyleClass().add("field");
        
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20; -fx-background-color: #0f172a;");
        Label label = new Label("Message:");
        label.getStyleClass().add("field-label");
        content.getChildren().addAll(label, messageArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(800, 600);
        
        ButtonType sendButton = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButton, ButtonType.CANCEL);
        
        // Style buttons
        Platform.runLater(() -> {
            javafx.scene.Node sendBtn = dialog.getDialogPane().lookupButton(sendButton);
            if (sendBtn != null) {
                sendBtn.getStyleClass().add("btn-primary");
            }
            javafx.scene.Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelBtn != null) {
                cancelBtn.getStyleClass().add("btn-outline");
            }
        });
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == sendButton) {
                return messageArea.getText().trim();
            }
            return null;
        });
        
        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(messageText -> {
            if (messageText.isEmpty()) {
                toast("Message cannot be empty", ToastService.Type.ERROR);
                return;
            }
            
            try {
                com.cmpe343.dao.UserDao userDao = new com.cmpe343.dao.UserDao();
                int ownerId = userDao.getOwnerId();
                
                if (ownerId == -1) {
                    toast("Owner not found", ToastService.Type.ERROR);
                    return;
                }
                
                int messageId = messageDao.createMessage(currentCustomerId, ownerId, currentCustomerId, messageText);
                if (messageId > 0) {
                    toast("Message sent successfully!", ToastService.Type.SUCCESS);
                    // Refresh the messages list
                    refreshMessagesList(messagesContainer, messageDao);
                } else {
                    toast("Failed to send message", ToastService.Type.ERROR);
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("Error: " + e.getMessage(), ToastService.Type.ERROR);
            }
        });
    }
    
    private void refreshMessagesList(VBox messagesContainer, com.cmpe343.dao.MessageDao messageDao) {
        messagesContainer.getChildren().clear();
        java.util.List<com.cmpe343.model.Message> messages = messageDao.getMessagesForCustomer(currentCustomerId);
        
        if (messages.isEmpty()) {
            Label empty = new Label("No messages");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20;");
            messagesContainer.getChildren().add(empty);
        } else {
            for (com.cmpe343.model.Message msg : messages) {
                VBox msgCard = createMessageCard(msg, messageDao);
                messagesContainer.getChildren().add(msgCard);
            }
        }
    }

    @FXML
    private void handleToggleVegetables() {
        vegetablesVisible = !vegetablesVisible;
        vegetablesGrid.setVisible(vegetablesVisible);
        vegetablesGrid.setManaged(vegetablesVisible);
        vegetablesToggle.setText(vegetablesVisible ? "▼" : "▶");
    }
    
    @FXML
    private void handleToggleFruits() {
        fruitsVisible = !fruitsVisible;
        fruitsGrid.setVisible(fruitsVisible);
        fruitsGrid.setManaged(fruitsVisible);
        fruitsToggle.setText(fruitsVisible ? "▼" : "▶");
    }

    @FXML
    private void toggleChat() {
        boolean visible = !chatContainer.isVisible();
        chatContainer.setVisible(visible);
        chatContainer.setManaged(visible);
        chatContainer.setMouseTransparent(!visible);
        if (visible) {
            Platform.runLater(() -> {
                if (chatContainer.getParent() != null && chatContainer.getParent() instanceof javafx.scene.layout.StackPane) {
                    javafx.scene.layout.StackPane parent = (javafx.scene.layout.StackPane) chatContainer.getParent();
                    parent.getChildren().remove(chatContainer);
                    parent.getChildren().add(chatContainer);
                    chatContainer.toFront();
                }
                loadChatWidgetMessages();
                Platform.runLater(() -> {
                    if (chatInput != null) {
                        chatInput.requestFocus();
                    }
                    if (chatContainer.getParent() != null) {
                        chatContainer.toFront();
                    }
                });
            });
        }
    }

    @FXML
    private void handleSendChatMessage() {
        String text = chatInput.getText().trim();
        if (text.isEmpty())
            return;

        try {
            int ownerId = userDao.getOwnerId();
            if (ownerId == -1) {
                toast("Owner not found", ToastService.Type.ERROR);
                return;
            }

            int msgId = messageDao.createMessage(currentCustomerId, ownerId, currentCustomerId, text);
            if (msgId > 0) {
                chatInput.clear();
                loadChatWidgetMessages();
            } else {
                toast("Failed to send", ToastService.Type.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("Error sending message", ToastService.Type.ERROR);
        }
    }

    private void loadChatWidgetMessages() {
        chatMessagesBox.getChildren().clear();
        int ownerId = userDao.getOwnerId();
        java.util.List<com.cmpe343.model.Message> messages = messageDao.getMessagesBetween(ownerId, currentCustomerId);

        for (com.cmpe343.model.Message msg : messages) {
            chatMessagesBox.getChildren().add(createWidgetMessage(msg));
        }
        Platform.runLater(() -> {
            chatMessagesBox.applyCss();
            chatMessagesBox.layout();
            chatScroll.applyCss();
            chatScroll.layout();
            chatScroll.setVvalue(1.0);

            Platform.runLater(() -> {
                chatScroll.setVvalue(1.0);
                if (chatContainer.getParent() != null) {
                    chatContainer.toFront();
                }
            });
        });
    }

    private Node createWidgetMessage(com.cmpe343.model.Message msg) {
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(260);

        boolean isMe = (msg.getSenderId() == currentCustomerId);
        boolean isOwnerMsg = !isMe;

        String bg = isMe ? "#3b82f6" : "#475569";
        bubble.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12; -fx-padding: 8 12;");

        Label content = new Label(msg.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        Label date = new Label(msg.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        date.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 10px; opacity: 0.8;");
        date.setAlignment(Pos.BOTTOM_RIGHT);

        bubble.getChildren().addAll(content, date);

        HBox row = new HBox(bubble);
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        String replyText = messageDao.getReplyText(msg.getId());
        if (replyText != null && !replyText.isEmpty() && msg.getId() > 0) {
            VBox replyBubble = new VBox(4);
            replyBubble.setMaxWidth(260);
            replyBubble.setStyle("-fx-background-color: #475569; -fx-background-radius: 12; -fx-padding: 8 12;");

            Label rContent = new Label(replyText);
            rContent.setWrapText(true);
            rContent.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

            Label rDate = new Label(
                    msg.getTimestamp().plusMinutes(1).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            rDate.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 10px; opacity: 0.8;");

            replyBubble.getChildren().addAll(rContent, rDate);

            HBox replyRow = new HBox(replyBubble);
            replyRow.setAlignment(Pos.CENTER_LEFT);

            VBox container = new VBox(5);
            container.getChildren().addAll(row, replyRow);
            return container;
        }

        return row;
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleClearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
    }
    
    @FXML
    private void handleAddFunds() {
        javafx.scene.control.Dialog<com.cmpe343.model.CreditCard> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Credit Card Payment");
        dialog.setHeaderText("Secure Wallet Top-up");
        
        // Create the custom UI
        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 20; -fx-min-width: 300;");
        
        TextField holderName = new TextField();
        holderName.setPromptText("Card Holder Name");
        TextField cardNum = new TextField();
        cardNum.setPromptText("Card Number (8 Digits)");
        TextField expiry = new TextField();
        expiry.setPromptText("Expiry Date (MM/YY)");
        TextField cvc = new TextField();
        cvc.setPromptText("CVC (3 Digits)");
        TextField amount = new TextField();
        amount.setPromptText("Amount to Add (₺)");
        
        // Input restrictions
        // 1. Card Number: Only digits, max 8
        cardNum.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*") || newValue.length() > 8) {
                cardNum.setText(old);
            }
        });
        
        // 2. CVC: Only digits, max 3
        cvc.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*") || newValue.length() > 3) {
                cvc.setText(old);
            }
        });
        
        // 3. Expiry: Auto-format MM/YY
        expiry.setPromptText("MM/YY (e.g. 12/28)");
        
        content.getChildren().addAll(
                new Label("Card Information"), holderName, cardNum,
                new HBox(10, expiry, cvc),
                new Separator(),
                new Label("Top-up Amount"), amount
        );
        
        dialog.getDialogPane().setContent(content);
        javafx.scene.control.ButtonType payButtonType = new javafx.scene.control.ButtonType("Pay Now", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(payButtonType, javafx.scene.control.ButtonType.CANCEL);
        
        // Apply CSS
        dialog.getDialogPane().getStylesheets().addAll(searchField.getScene().getStylesheets());
        
        dialog.setResultConverter(btn -> {
            if (btn == payButtonType) {
                return new com.cmpe343.model.CreditCard(holderName.getText(), cardNum.getText(), expiry.getText(), cvc.getText());
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(card -> {
            try {
                // 1. Validate Card
                if (!card.isValid()) {
                    toast("Invalid Card Details! Check digits and date.", ToastService.Type.ERROR);
                    return;
                }
                
                // 2. Validate Amount
                double topUpAmount = Double.parseDouble(amount.getText());
                if (topUpAmount <= 0) {
                    throw new Exception("Amount must be positive");
                }
                
                // 3. Database Update
                boolean success = com.cmpe343.db.Db.updateUserBalance(currentCustomerId, topUpAmount);
                
                if (success) {
                    // Update Session & UI
                    double newBalance = Session.getUser().getWalletBalance() + topUpAmount;
                    Session.getUser().setWalletBalance(newBalance);
                    usernameLabel.setText(Session.getUser().getUsername() + " | Wallet: " + String.format("%.2f", newBalance) + " ₺");
                    
                    toast("Successfully added " + String.format("%.2f", topUpAmount) + " ₺!", ToastService.Type.SUCCESS);
                } else {
                    toast("Failed to update wallet balance", ToastService.Type.ERROR);
                }
                
            } catch (NumberFormatException e) {
                toast("Please enter a valid amount", ToastService.Type.ERROR);
            } catch (Exception e) {
                toast("Error: " + e.getMessage(), ToastService.Type.ERROR);
            }
        });
    }
    
    private void openTrackingScreen(com.cmpe343.model.Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TrackingView.fxml"));
            javafx.scene.Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Live Tracking - Order #" + order.getId());
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
            
            System.out.println("Tracking screen opened for Order ID: " + order.getId());
        } catch (java.io.IOException e) {
            e.printStackTrace();
            toast("Could not load tracking view", ToastService.Type.ERROR);
        }
    }

    private void toast(String msg, ToastService.Type type) {
        ToastService.show(searchField.getScene(), msg, type);
    }
    
    /**
     * Populates product images from resources if they haven't been populated yet.
     * This checks if any products have images, and if not, populates them from the resources folder.
     */
    private void populateProductImagesIfNeeded() {
        if (imagesPopulated) {
            return; // Already populated in this session
        }
        
        try {
            // Check if any products have images
            boolean hasImages = false;
            java.util.List<Product> products = productDao.findAll();
            for (Product p : products) {
                byte[] imageBytes = productDao.getProductImageBlob(p.getId());
                if (imageBytes != null && imageBytes.length > 0) {
                    hasImages = true;
                    break;
                }
            }
            
            // If no images found, populate them
            if (!hasImages) {
                System.out.println("No product images found in database. Populating from resources...");
                int count = com.cmpe343.service.ImagePopulationService.populateProductImages();
                if (count > 0) {
                    System.out.println("Successfully populated " + count + " product images.");
                }
            }
            
            imagesPopulated = true;
        } catch (Exception e) {
            System.err.println("Error populating product images: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
