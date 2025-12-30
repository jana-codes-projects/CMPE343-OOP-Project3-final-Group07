package com.cmpe343.fx.controller;

import com.cmpe343.dao.*;
import com.cmpe343.model.*;
import com.cmpe343.model.Order.OrderStatus;
import com.cmpe343.model.Product.ProductType;
import com.cmpe343.fx.Session;

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
import java.util.stream.Collectors;

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

    private void showCarrierDetail(User carrier) {
        carrierDetailContainer.getChildren().clear();
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-card");

        Label header = new Label(carrier.getUsername());
        header.getStyleClass().add("detail-header");

        card.getChildren().add(header);
        card.getChildren().add(createDetailRow("Phone", carrier.getPhone() != null ? carrier.getPhone() : "-"));
        card.getChildren().add(createDetailRow("Address", carrier.getAddress() != null ? carrier.getAddress() : "-"));

        Button toggleBtn = new Button(carrier.isActive() ? "Deactivate" : "Activate");
        toggleBtn.getStyleClass().add("btn-outline");
        toggleBtn.setOnAction(e -> {
            if (carrier.isActive())
                userDAO.deactivateCarrier(carrier.getId());
            else
                userDAO.activateCarrier(carrier.getId());
            loadCarriers(); // Reload
        });

        card.getChildren().add(toggleBtn);

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
            item.setOnMouseClicked(e -> showOrderDetail(o));
            ordersListContainer.getChildren().add(item);
        }
    }

    private void showOrderDetail(Order order) {
        orderDetailContainer.getChildren().clear();
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-card");

        Label title = new Label("Order #" + order.getId());
        title.getStyleClass().add("detail-header");

        card.getChildren().add(title);
        card.getChildren().add(createDetailRow("Status", order.getStatus().name()));
        card.getChildren().add(createDetailRow("Total", formatPrice(order.getTotalAfterTax())));
        card.getChildren().add(
                createDetailRow("Date", order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        // Items placeholder
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsHeader = new Label("Items");
            itemsHeader.getStyleClass().add("field-label");
            card.getChildren().add(itemsHeader);
            // .. items loop if needed
        }

        orderDetailContainer.getChildren().add(card);
    }

    // ==================== MESSAGE MANAGEMENT ====================

    private void loadMessages() {
        messagesListContainer.getChildren().clear();
        messageDetailContainer.getChildren().clear();

        List<Message> msgs = messageDAO.getAllMessages();
        if (msgs.isEmpty()) {
            messagesListContainer.getChildren().add(createPlaceholder("No messages."));
            return;
        }

        for (Message m : msgs) {
            HBox item = createListItemBase();
            Label sender = new Label(m.getSender());
            item.getChildren().add(sender);
            messagesListContainer.getChildren().add(item);
        }
    }

    @FXML
    private void handleRefreshMessages() {
        loadMessages();
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
            Label code = new Label(c.getCode());
            item.getChildren().add(code);
            couponsListContainer.getChildren().add(item);
        }
    }

    @FXML
    private void handleAddCoupon() {
        /* TODO */ }

    @FXML
    private void handleRefreshCoupons() {
        loadCoupons();
    }

    // ==================== RATINGS ====================

    private void loadCarrierRatings() {
        ratingsContainer.getChildren().clear();
        // Placeholder implementation
        ratingsContainer.getChildren().add(createPlaceholder("Ratings feature active."));
    }

    @FXML
    private void handleRefreshRatings() {
        loadCarrierRatings();
    }

    @FXML
    private void handleRefreshLoyalty() {
        /* TODO */ }

    // ==================== COMMON ====================

    @FXML
    private void handleAddProduct() {
        /* TODO */ }

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
        /* TODO */ }

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

    private void handleEditProduct(Product product) {
        // Placeholder for edit logic
        System.out.println("Edit product: " + product.getName());
        // In a real app, show a dialog to edit fields
        // For now, just show an alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Edit Product");
        alert.setHeaderText("Edit Feature");
        alert.setContentText("Editing product " + product.getName() + " is not yet implemented.");
        alert.showAndWait();
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
}
