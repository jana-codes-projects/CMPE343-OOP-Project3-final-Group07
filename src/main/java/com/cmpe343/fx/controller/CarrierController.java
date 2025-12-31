package com.cmpe343.fx.controller;

import com.cmpe343.dao.OrderDao;
import com.cmpe343.dao.UserDao;
import com.cmpe343.fx.Session;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.model.Order;
import com.cmpe343.model.CartItem;
import com.cmpe343.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CarrierController {
    
    @FXML
    private Label usernameLabel;
    @FXML
    private VBox availableOrdersListContainer;
    @FXML
    private VBox availableOrderDetailContainer;
    @FXML
    private VBox myOrdersListContainer;
    @FXML
    private VBox myOrderDetailContainer;
    @FXML
    private VBox completedOrdersListContainer;
    @FXML
    private VBox completedOrderDetailContainer;
    @FXML
    private Button logoutButton;
    
    private OrderDao orderDao;
    private UserDao userDao;
    private Order selectedAvailableOrder;
    private Order selectedMyOrder;
    private Order selectedCompletedOrder;
    private int currentCarrierId;
    
    @FXML
    public void initialize() {
        orderDao = new OrderDao();
        userDao = new UserDao();
        
        if (Session.isLoggedIn()) {
            currentCarrierId = Session.getUser().getId();
            usernameLabel.setText("Carrier: " + Session.getUser().getUsername());
        }
        
        loadOrders();
    }
    
    private void loadOrders() {
        loadAvailableOrders();
        loadMyOrders();
        loadCompletedOrders();
    }
    
    @FXML
    private void handleRefresh() {
        loadOrders();
    }
    
    private void loadAvailableOrders() {
        // Preserve the currently selected order ID before clearing
        Integer selectedOrderId = selectedAvailableOrder != null ? selectedAvailableOrder.getId() : null;
        
        availableOrdersListContainer.getChildren().clear();
        availableOrderDetailContainer.getChildren().clear();
        selectedAvailableOrder = null;
        
        List<Order> orders = orderDao.getAvailableOrders();
        
        if (orders.isEmpty()) {
            Label noOrdersLabel = new Label("No available orders at this time.");
            noOrdersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 20;");
            availableOrdersListContainer.getChildren().add(noOrdersLabel);
            return;
        }
        
        orders.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        // Restore selection if the order still exists
        Order orderToSelect = null;
        for (Order order : orders) {
            HBox listItem = createOrderListItem(order, formatter, "#3b82f6", availableOrdersListContainer);
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                availableOrdersListContainer.getChildren().add(listItem);
                
                // Check if this is the previously selected order
                if (selectedOrderId != null && order.getId() == selectedOrderId) {
                    orderToSelect = order;
                }
            }
        }
        
        // Restore the detail view if the order still exists
        if (orderToSelect != null) {
            showAvailableOrderDetail(orderToSelect, formatter);
        } else {
            showAvailableOrderDetailPlaceholder();
        }
    }
    
    private void loadMyOrders() {
        // Preserve the currently selected order ID before clearing
        Integer selectedOrderId = selectedMyOrder != null ? selectedMyOrder.getId() : null;
        
        myOrdersListContainer.getChildren().clear();
        myOrderDetailContainer.getChildren().clear();
        selectedMyOrder = null;
        
        if (currentCarrierId == 0) {
            Label noUserLabel = new Label("Carrier not logged in.");
            noUserLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 20;");
            myOrdersListContainer.getChildren().add(noUserLabel);
            return;
        }
        
        List<Order> orders = orderDao.getOrdersByCarrier(currentCarrierId, Order.OrderStatus.ASSIGNED);
        
        if (orders.isEmpty()) {
            Label noOrdersLabel = new Label("No assigned orders. Select orders from the 'Active Orders' tab.");
            noOrdersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 20;");
            myOrdersListContainer.getChildren().add(noOrdersLabel);
            return;
        }
        
        orders.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        // Restore selection if the order still exists
        Order orderToSelect = null;
        for (Order order : orders) {
            HBox listItem = createOrderListItem(order, formatter, "#f59e0b", myOrdersListContainer);
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                myOrdersListContainer.getChildren().add(listItem);
                
                // Check if this is the previously selected order
                if (selectedOrderId != null && order.getId() == selectedOrderId) {
                    orderToSelect = order;
                }
            }
        }
        
        // Restore the detail view if the order still exists
        if (orderToSelect != null) {
            showMyOrderDetail(orderToSelect, formatter);
        } else {
            showMyOrderDetailPlaceholder();
        }
    }
    
    private void loadCompletedOrders() {
        // Preserve the currently selected order ID before clearing
        Integer selectedOrderId = selectedCompletedOrder != null ? selectedCompletedOrder.getId() : null;
        
        completedOrdersListContainer.getChildren().clear();
        completedOrderDetailContainer.getChildren().clear();
        selectedCompletedOrder = null;
        
        if (currentCarrierId == 0) {
            Label noUserLabel = new Label("Carrier not logged in.");
            noUserLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 20;");
            completedOrdersListContainer.getChildren().add(noUserLabel);
            return;
        }
        
        List<Order> orders = orderDao.getOrdersByCarrier(currentCarrierId, Order.OrderStatus.DELIVERED);
        
        if (orders.isEmpty()) {
            Label noOrdersLabel = new Label("No completed orders yet.");
            noOrdersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 20;");
            completedOrdersListContainer.getChildren().add(noOrdersLabel);
            return;
        }
        
        orders.sort((o1, o2) -> Integer.compare(o2.getId(), o1.getId()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        // Restore selection if the order still exists
        Order orderToSelect = null;
        for (Order order : orders) {
            HBox listItem = createOrderListItem(order, formatter, "#10b981", completedOrdersListContainer);
            if (listItem != null) {
                listItem.setPrefWidth(Double.MAX_VALUE);
                listItem.setMaxWidth(Double.MAX_VALUE);
                completedOrdersListContainer.getChildren().add(listItem);
                
                // Check if this is the previously selected order
                if (selectedOrderId != null && order.getId() == selectedOrderId) {
                    orderToSelect = order;
                }
            }
        }
        
        // Restore the detail view if the order still exists
        if (orderToSelect != null) {
            showCompletedOrderDetail(orderToSelect, formatter);
        } else {
            showCompletedOrderDetailPlaceholder();
        }
    }
    
    private HBox createOrderListItem(Order order, DateTimeFormatter formatter, String statusColor, VBox parentContainer) {
        HBox item = new HBox(10);
        item.setStyle("-fx-padding: 10 12; -fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0; " +
                      "-fx-cursor: hand;");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefWidth(Double.MAX_VALUE);
        item.setUserData(order);
        
        item.setOnMouseEntered(e -> {
            if (!item.getStyle().contains("#2563eb")) {
                item.setStyle(item.getStyle().replace("#1e293b", "#2563eb"));
            }
        });
        item.setOnMouseExited(e -> {
            if (!item.getStyle().contains("selected")) {
                item.setStyle(item.getStyle().replace("#2563eb", "#1e293b"));
            }
        });
        
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: white;");
        idLabel.setPrefWidth(100);
        
        Label statusLabel = new Label(order.getStatus().name());
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + "; " +
                            "-fx-padding: 4 8; -fx-background-color: " + statusColor + "20; -fx-background-radius: 4;");
        
        Label dateLabel = new Label(order.getOrderTime().format(formatter));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        dateLabel.setPrefWidth(150);
        
        User customer = null;
        try {
            customer = userDao.getUserById(order.getCustomerId());
        } catch (Exception e) {
            // Continue without customer info
        }
        String customerName = customer != null ? customer.getUsername() : "Customer #" + order.getCustomerId();
        Label customerLabel = new Label(customerName);
        customerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");
        
        Label totalLabel = new Label(String.format("%.2f ₺", order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        totalLabel.setPrefWidth(100);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        item.getChildren().addAll(idLabel, statusLabel, dateLabel, customerLabel, spacer, totalLabel);
        
        item.setOnMouseClicked(e -> {
            if (parentContainer == availableOrdersListContainer) {
                showAvailableOrderDetail(order, formatter);
            } else if (parentContainer == myOrdersListContainer) {
                showMyOrderDetail(order, formatter);
            } else if (parentContainer == completedOrdersListContainer) {
                showCompletedOrderDetail(order, formatter);
            }
        });
        
        return item;
    }
    
    private void showAvailableOrderDetail(Order order, DateTimeFormatter formatter) {
        selectedAvailableOrder = order;
        availableOrderDetailContainer.getChildren().clear();
        
        VBox detailCard = createAvailableOrderDetailCard(order, formatter);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            availableOrderDetailContainer.getChildren().add(detailCard);
        }
        
        updateOrderListSelection(order, availableOrdersListContainer);
    }
    
    private void showMyOrderDetail(Order order, DateTimeFormatter formatter) {
        selectedMyOrder = order;
        myOrderDetailContainer.getChildren().clear();
        
        VBox detailCard = createMyOrderDetailCard(order, formatter);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            myOrderDetailContainer.getChildren().add(detailCard);
        }
        
        updateOrderListSelection(order, myOrdersListContainer);
    }
    
    private void showCompletedOrderDetail(Order order, DateTimeFormatter formatter) {
        selectedCompletedOrder = order;
        completedOrderDetailContainer.getChildren().clear();
        
        VBox detailCard = createCompletedOrderDetailCard(order, formatter);
        if (detailCard != null) {
            detailCard.setPrefWidth(Double.MAX_VALUE);
            detailCard.setMaxWidth(Double.MAX_VALUE);
            completedOrderDetailContainer.getChildren().add(detailCard);
        }
        
        updateOrderListSelection(order, completedOrdersListContainer);
    }
    
    private void updateOrderListSelection(Order selected, VBox listContainer) {
        for (javafx.scene.Node node : listContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                if (item.getUserData() instanceof Order) {
                    Order order = (Order) item.getUserData();
                    if (order.getId() == selected.getId()) {
                        item.setStyle(item.getStyle().replace("#1e293b", "#2563eb").replace("#2563eb", "#2563eb") + " selected");
                    } else {
                        item.setStyle(item.getStyle().replace(" selected", "").replace("#2563eb", "#1e293b"));
                    }
                }
            }
        }
    }
    
    private void showAvailableOrderDetailPlaceholder() {
        availableOrderDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select an order from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 20;");
        availableOrderDetailContainer.getChildren().add(placeholder);
    }
    
    private void showMyOrderDetailPlaceholder() {
        myOrderDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select an order from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 20;");
        myOrderDetailContainer.getChildren().add(placeholder);
    }
    
    private void showCompletedOrderDetailPlaceholder() {
        completedOrderDetailContainer.getChildren().clear();
        Label placeholder = new Label("Select an order from the list to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 20;");
        completedOrderDetailContainer.getChildren().add(placeholder);
    }
    
    private VBox createAvailableOrderDetailCard(Order order, DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 8; " +
                      "-fx-background-color: #1e3a8a; -fx-background-radius: 8;");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: white;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusLabel = new Label("AVAILABLE");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #93c5fd; " +
                            "-fx-padding: 6 12; -fx-background-color: #3b82f620; -fx-background-radius: 4;");
        
        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        VBox detailsBox = new VBox(8);
        
        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        orderDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
        
        if (order.getRequestedDeliveryTime() != null) {
            Label requestedDeliveryLabel = new Label("Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
            requestedDeliveryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
            detailsBox.getChildren().add(requestedDeliveryLabel);
        }
        detailsBox.getChildren().add(orderDateLabel);
        
        User customer = null;
        try {
            customer = userDao.getUserById(order.getCustomerId());
        } catch (Exception e) {
            // Continue without customer info
        }
        
        if (customer != null) {
            Label customerLabel = new Label("Customer: " + customer.getUsername());
            customerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
            detailsBox.getChildren().add(customerLabel);
            
            if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                Label addressLabel = new Label("Address: " + customer.getAddress());
                addressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-wrap-text: true;");
                detailsBox.getChildren().add(addressLabel);
            }
            
            if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
                Label phoneLabel = new Label("Phone: " + customer.getPhone());
                phoneLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
                detailsBox.getChildren().add(phoneLabel);
            }
        }
        
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsHeader = new Label("Items:");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
            
            VBox itemsBox = new VBox(3);
            itemsBox.setStyle("-fx-padding: 0 0 0 20;");
            for (CartItem item : order.getItems()) {
                String itemName = item.getProduct() != null ? item.getProduct().getName() : "Product";
                Label itemLabel = new Label(String.format("• %s: %.2f kg @ %.2f ₺ = %.2f ₺",
                        itemName,
                        item.getQuantityKg(),
                        item.getUnitPrice(),
                        item.getLineTotal()));
                itemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");
                itemsBox.getChildren().add(itemLabel);
            }
            
            detailsBox.getChildren().addAll(itemsHeader, itemsBox);
        }
        
        Label totalLabel = new Label("Total: " + String.format("%.2f ₺", order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        detailsBox.getChildren().add(totalLabel);
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button selectButton = new Button("Select Order");
        selectButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
                             "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
        selectButton.setOnAction(e -> {
            handleSelectOrder(order);
            loadOrders();
        });
        
        buttonBox.getChildren().add(selectButton);
        
        card.getChildren().addAll(header, detailsBox, buttonBox);
        return card;
    }
    
    private VBox createMyOrderDetailCard(Order order, DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #f59e0b; -fx-border-width: 2; -fx-border-radius: 8; " +
                      "-fx-background-color: #78350f; -fx-background-radius: 8;");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: white;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusLabel = new Label("ASSIGNED");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #fbbf24; " +
                            "-fx-padding: 6 12; -fx-background-color: #f59e0b20; -fx-background-radius: 4;");
        
        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        VBox detailsBox = new VBox(8);
        
        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        orderDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
        
        if (order.getRequestedDeliveryTime() != null) {
            Label requestedDeliveryLabel = new Label("Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
            requestedDeliveryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
            detailsBox.getChildren().add(requestedDeliveryLabel);
        }
        detailsBox.getChildren().add(orderDateLabel);
        
        User customer = null;
        try {
            customer = userDao.getUserById(order.getCustomerId());
        } catch (Exception e) {
            // Continue without customer info
        }
        
        if (customer != null) {
            Label customerLabel = new Label("Customer: " + customer.getUsername());
            customerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
            detailsBox.getChildren().add(customerLabel);
            
            if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                Label addressLabel = new Label("Address: " + customer.getAddress());
                addressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-wrap-text: true;");
                detailsBox.getChildren().add(addressLabel);
            }
            
            if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
                Label phoneLabel = new Label("Phone: " + customer.getPhone());
                phoneLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
                detailsBox.getChildren().add(phoneLabel);
            }
        }
        
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsHeader = new Label("Items:");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
            
            VBox itemsBox = new VBox(3);
            itemsBox.setStyle("-fx-padding: 0 0 0 20;");
            for (CartItem item : order.getItems()) {
                String itemName = item.getProduct() != null ? item.getProduct().getName() : "Product";
                Label itemLabel = new Label(String.format("• %s: %.2f kg @ %.2f ₺ = %.2f ₺",
                        itemName,
                        item.getQuantityKg(),
                        item.getUnitPrice(),
                        item.getLineTotal()));
                itemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");
                itemsBox.getChildren().add(itemLabel);
            }
            
            detailsBox.getChildren().addAll(itemsHeader, itemsBox);
        }
        
        Label totalLabel = new Label("Total: " + String.format("%.2f ₺", order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        detailsBox.getChildren().add(totalLabel);
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button completeButton = new Button("Mark as Delivered");
        completeButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
                               "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6;");
        completeButton.setOnAction(e -> {
            handleCompleteOrder(order);
            loadOrders();
        });
        
        buttonBox.getChildren().add(completeButton);
        
        card.getChildren().addAll(header, detailsBox, buttonBox);
        return card;
    }
    
    private VBox createCompletedOrderDetailCard(Order order, DateTimeFormatter formatter) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 18 20; -fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8; " +
                      "-fx-background-color: #064e3b; -fx-background-radius: 8;");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(12);
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: white;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusLabel = new Label("✓ DELIVERED");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34d399; " +
                            "-fx-padding: 6 12; -fx-background-color: #10b98120; -fx-background-radius: 4;");
        
        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        VBox detailsBox = new VBox(8);
        
        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        orderDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
        detailsBox.getChildren().add(orderDateLabel);
        
        if (order.getRequestedDeliveryTime() != null) {
            Label requestedDeliveryLabel = new Label("Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
            requestedDeliveryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
            detailsBox.getChildren().add(requestedDeliveryLabel);
        }
        
        if (order.getDeliveredTime() != null) {
            Label deliveredLabel = new Label("Delivered: " + order.getDeliveredTime().format(formatter));
            deliveredLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #34d399;");
            detailsBox.getChildren().add(deliveredLabel);
        }
        
        User customer = null;
        try {
            customer = userDao.getUserById(order.getCustomerId());
        } catch (Exception e) {
            // Continue without customer info
        }
        
        if (customer != null) {
            Label customerLabel = new Label("Customer: " + customer.getUsername());
            customerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
            detailsBox.getChildren().add(customerLabel);
        }
        
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsHeader = new Label("Items:");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
            
            VBox itemsBox = new VBox(3);
            itemsBox.setStyle("-fx-padding: 0 0 0 20;");
            for (CartItem item : order.getItems()) {
                String itemName = item.getProduct() != null ? item.getProduct().getName() : "Product";
                Label itemLabel = new Label(String.format("• %s: %.2f kg @ %.2f ₺ = %.2f ₺",
                        itemName,
                        item.getQuantityKg(),
                        item.getUnitPrice(),
                        item.getLineTotal()));
                itemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");
                itemsBox.getChildren().add(itemLabel);
            }
            
            detailsBox.getChildren().addAll(itemsHeader, itemsBox);
        }
        
        Label totalLabel = new Label("Total: " + String.format("%.2f ₺", order.getTotalAfterTax()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        detailsBox.getChildren().add(totalLabel);
        
        card.getChildren().addAll(header, detailsBox);
        return card;
    }
    
    @FXML
    private void handleSelectOrder(Order order) {
        if (currentCarrierId == 0) return;
        
        // Use the selected order if order parameter is null (fallback)
        Order orderToProcess = order != null ? order : selectedAvailableOrder;
        if (orderToProcess == null) {
            ToastService.show(logoutButton.getScene(), "Please select an order first.", 
                    ToastService.Type.ERROR, ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }
        
        boolean success = orderDao.assignOrderToCarrier(orderToProcess.getId(), currentCarrierId);
        if (success) {
            ToastService.show(logoutButton.getScene(), "Order " + orderToProcess.getId() + " has been assigned to you.", 
                    ToastService.Type.SUCCESS, ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
            // Clear selection after successful assignment
            selectedAvailableOrder = null;
        } else {
            ToastService.show(logoutButton.getScene(), "Failed to select order. It may have been selected by another carrier.", 
                    ToastService.Type.ERROR, ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
        }
    }
    
    @FXML
    private void handleCompleteOrder(Order order) {
        // Use the selected order if order parameter is null (fallback)
        Order orderToProcess = order != null ? order : selectedMyOrder;
        if (orderToProcess == null) {
            ToastService.show(logoutButton.getScene(), "Please select an order first.", 
                    ToastService.Type.ERROR, ToastService.Position.BOTTOM_CENTER, Duration.seconds(2));
            return;
        }
        
        boolean success = orderDao.markOrderDelivered(orderToProcess.getId(), LocalDateTime.now());
        if (success) {
            ToastService.show(logoutButton.getScene(), "Order " + orderToProcess.getId() + " has been marked as delivered.", 
                    ToastService.Type.SUCCESS, ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
            // Clear selection after successful completion
            selectedMyOrder = null;
        } else {
            ToastService.show(logoutButton.getScene(), "Failed to mark order as delivered.", 
                    ToastService.Type.ERROR, ToastService.Position.BOTTOM_CENTER, Duration.seconds(3));
        }
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
}
