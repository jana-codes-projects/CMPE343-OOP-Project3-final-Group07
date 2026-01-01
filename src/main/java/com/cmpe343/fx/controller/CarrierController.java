package com.cmpe343.fx.controller;

import com.cmpe343.dao.MessageDao;
import com.cmpe343.dao.OrderDao;
import com.cmpe343.dao.UserDao;
import com.cmpe343.model.Message;
import com.cmpe343.fx.Session;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.model.Order;
import com.cmpe343.model.CartItem;
import com.cmpe343.model.User;
import javafx.application.Platform;
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
    private VBox messagesListContainer;
    @FXML
    private VBox messageDetailContainer;
    @FXML
    private TextField messageInput;
    @FXML
    private Button logoutButton;

    private OrderDao orderDao;
    private UserDao userDao;
    private MessageDao messageDao;
    private Order selectedAvailableOrder;
    private Order selectedMyOrder;
    private Order selectedCompletedOrder;
    private int currentCarrierId;

    @FXML
    public void initialize() {
        orderDao = new OrderDao();
        userDao = new UserDao();
        messageDao = new MessageDao();

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
        loadMessages();
    }

    @FXML
    private void handleRefreshMessages() {
        loadMessages();
    }

    private void loadMessages() {
        if (messagesListContainer == null)
            return; // In case tab not selected/loaded

        messagesListContainer.getChildren().clear();

        if (currentCarrierId == 0)
            return;

        // Carrier chats with Owner. Owner ID is retrieved via UserDao
        int ownerId = userDao.getOwnerId();

        // We reuse getMessagesBetween to fetch chat with owner
        // Since carrier is "customer" in this context (messaging initiator),
        // we use currentCarrierId as "customer_id" in the query logic of MessageDao.
        // Wait, MessageDao.getMessagesBetween(ownerId, otherUserId) works.

        List<Message> messages = messageDao.getMessagesBetween(ownerId, currentCarrierId);

        if (messages.isEmpty()) {
            Label placeholder = new Label("No messages yet. Start a conversation!");
            placeholder.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20;");
            messagesListContainer.getChildren().add(placeholder);
        }

        for (Message m : messages) {
            // Message Logic:
            // If senderId == currentCarrierId, then it's ME.
            boolean isMe = (m.getSenderId() == currentCarrierId);

            messagesListContainer.getChildren()
                    .add(createChatBubble(m.getSender(), m.getContent(), m.getTimestamp(), isMe));

            // Owner replies
            String reply = messageDao.getReplyText(m.getId());
            if (reply != null && !reply.isEmpty()) {
                messagesListContainer.getChildren()
                        .add(createChatBubble("Owner", reply, m.getTimestamp().plusMinutes(1), true));
            }
        }

        // Auto scroll
        Platform.runLater(() -> {
            if (messagesListContainer.getParent() instanceof ScrollPane) {
                ((ScrollPane) messagesListContainer.getParent()).setVvalue(1.0);
            }
        });
    }

    @FXML
    private void handleSendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty())
            return;

        int ownerId = userDao.getOwnerId();
        if (ownerId == -1) {
            ToastService.show(logoutButton.getScene(), "Owner not found", ToastService.Type.ERROR);
            return;
        }

        int msgId = messageDao.createMessage(currentCarrierId, ownerId, currentCarrierId, text);
        if (msgId > 0) {
            messageInput.clear();
            loadMessages();
        } else {
            ToastService.show(logoutButton.getScene(), "Failed to send message", ToastService.Type.ERROR);
        }
    }

    private javafx.scene.Node createChatBubble(String sender, String text, LocalDateTime time, boolean isIncoming) {
        VBox bubble = new VBox(6);
        bubble.setMaxWidth(320);

        String bg = isIncoming ? "#475569" : "#3b82f6";
        bubble.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 16; -fx-padding: 12 16;");

        Label txt = new Label(text);
        txt.setWrapText(true);
        txt.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-line-spacing: 2px;");

        Label timeLbl = new Label(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLbl.setStyle("-fx-text-fill: rgba(203, 213, 225, 0.7); -fx-font-size: 10px;");
        timeLbl.setAlignment(Pos.BOTTOM_RIGHT);

        bubble.getChildren().addAll(txt, timeLbl);

        HBox row = new HBox(bubble);
        row.setAlignment(isIncoming ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        row.setPadding(new javafx.geometry.Insets(4, 0, 4, 0));
        return row;
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

    private HBox createOrderListItem(Order order, DateTimeFormatter formatter, String statusColorClass,
            VBox parentContainer) {
        HBox item = new HBox(12);
        item.getStyleClass().addAll("list-item-base", "card");
        item.setStyle("-fx-padding: 14 18; -fx-cursor: hand; -fx-alignment: center-left; -fx-background-radius: 10; -fx-border-radius: 10;");

        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefWidth(Double.MAX_VALUE);
        item.setUserData(order);

        item.setOnMouseEntered(e -> {
            if (item.getProperties().get("selected") == null) {
                item.setStyle("-fx-background-color: rgba(99, 102, 241, 0.2); -fx-padding: 14 18; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #6366f1; -fx-alignment: center-left; -fx-translate-x: 4;");
            }
        });
        item.setOnMouseExited(e -> {
            if (item.getProperties().get("selected") == null) {
                item.setStyle("-fx-padding: 14 18; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-radius: 10; -fx-alignment: center-left; -fx-translate-x: 0;");
            }
        });

        Label idLabel = new Label("Order #" + order.getId());
        idLabel.getStyleClass().add("detail-value");
        idLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: white;");
        idLabel.setPrefWidth(100);

        Label statusLabel = new Label(order.getStatus().name());
        String badgeClass = switch (order.getStatus()) {
            case CREATED -> "badge-info";
            case ASSIGNED -> "badge-warning";
            case DELIVERED -> "badge-success";
            case CANCELLED -> "badge-danger";
            default -> "badge-neutral";
        };
        statusLabel.getStyleClass().addAll("badge", badgeClass);

        Label dateLabel = new Label(order.getOrderTime().format(formatter));
        dateLabel.getStyleClass().add("muted");
        dateLabel.setPrefWidth(150);

        User customer = null;
        try {
            customer = userDao.getUserById(order.getCustomerId());
        } catch (Exception e) {
        }
        String customerName = customer != null ? customer.getUsername() : "Customer #" + order.getCustomerId();
        Label customerLabel = new Label(customerName);
        customerLabel.getStyleClass().add("detail-value");
        customerLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");

        Label totalLabel = new Label(String.format("%.2f ₺", order.getTotalAfterTax()));
        totalLabel.getStyleClass().add("detail-value");
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: white;");
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
                        // Mark as selected
                        if (item.getStyle().contains("#1e293b")) {
                            item.setStyle(item.getStyle().replace("#1e293b", "#2563eb"));
                        }
                        item.getProperties().put("selected", true);
                    } else {
                        // Uncheck
                        item.getProperties().remove("selected");
                        if (item.getStyle().contains("#2563eb")) {
                            item.setStyle(item.getStyle().replace("#2563eb", "#1e293b"));
                        }
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
        VBox card = new VBox(16);
        card.getStyleClass().addAll("detail-card", "card");
        card.setStyle("-fx-padding: 24; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-border-color: #3b82f6; -fx-border-width: 2;");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 20px; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label("AVAILABLE");
        statusLabel.getStyleClass().addAll("badge", "badge-info");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6 12;");

        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        Separator separator = new Separator();
        separator.setStyle("-fx-opacity: 0.1; -fx-padding: 12 0;");

        VBox detailsBox = new VBox(10);

        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        orderDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");

        if (order.getRequestedDeliveryTime() != null) {
            Label requestedDeliveryLabel = new Label(
                    "Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
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
        selectButton.getStyleClass().add("btn-primary");
        selectButton.setStyle("-fx-padding: 12 24; -fx-font-size: 14px; -fx-font-weight: 700;");
        selectButton.setOnAction(e -> {
            handleSelectOrder(order);
            loadOrders();
        });

        buttonBox.getChildren().add(selectButton);

        card.getChildren().addAll(header, separator, detailsBox, buttonBox);
        return card;
    }

    private VBox createMyOrderDetailCard(Order order, DateTimeFormatter formatter) {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("detail-card", "card");
        card.setStyle("-fx-padding: 24; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-border-color: #f59e0b; -fx-border-width: 2;");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 20px; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label("ASSIGNED");
        statusLabel.getStyleClass().addAll("badge", "badge-warning");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6 12;");

        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        Separator separator = new Separator();
        separator.setStyle("-fx-opacity: 0.1; -fx-padding: 12 0;");

        VBox detailsBox = new VBox(10);

        Label orderDateLabel = new Label("Order Date: " + order.getOrderTime().format(formatter));
        orderDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");

        if (order.getRequestedDeliveryTime() != null) {
            Label requestedDeliveryLabel = new Label(
                    "Requested Delivery: " + order.getRequestedDeliveryTime().format(formatter));
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
        completeButton.getStyleClass().add("btn-primary");
        completeButton.setStyle("-fx-padding: 12 24; -fx-font-size: 14px; -fx-font-weight: 700;");
        completeButton.setOnAction(e -> {
            handleCompleteOrder(order);
            loadOrders();
        });

        buttonBox.getChildren().add(completeButton);

        card.getChildren().addAll(header, separator, detailsBox, buttonBox);
        return card;
    }

    private VBox createCompletedOrderDetailCard(Order order, DateTimeFormatter formatter) {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("detail-card", "card");
        card.setStyle("-fx-padding: 24; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-border-color: #10b981; -fx-border-width: 2;");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 20px; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label("✓ DELIVERED");
        statusLabel.getStyleClass().addAll("badge", "badge-success");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6 12;");

        header.getChildren().addAll(idLabel, spacer, statusLabel);
        
        Separator separator = new Separator();
        separator.setStyle("-fx-opacity: 0.1; -fx-padding: 12 0;");

        VBox detailsBox = new VBox(10);
        detailsBox.getChildren().add(createDetailRow("Order Date", order.getOrderTime().format(formatter)));

        if (order.getRequestedDeliveryTime() != null) {
            detailsBox.getChildren().add(createDetailRow("Requested Delivery", order.getRequestedDeliveryTime().format(formatter)));
        }

        if (order.getDeliveredTime() != null) {
            HBox deliveredRow = createDetailRow("Delivered", order.getDeliveredTime().format(formatter));
            Label deliveredValue = (Label) deliveredRow.getChildren().get(1);
            deliveredValue.setStyle("-fx-font-weight: 600; -fx-text-fill: #34d399;");
            detailsBox.getChildren().add(deliveredRow);
        }

        User customer = null;
        try {
            customer = userDao.getUserById(order.getCustomerId());
        } catch (Exception e) {
            // Continue without customer info
        }

        if (customer != null) {
            detailsBox.getChildren().add(createDetailRow("Customer", customer.getUsername()));
        }

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Separator itemsSeparator = new Separator();
            itemsSeparator.setStyle("-fx-opacity: 0.1; -fx-padding: 12 0;");
            
            Label itemsHeader = new Label("Order Items");
            itemsHeader.getStyleClass().add("h3");
            itemsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: white; -fx-padding: 8 0 8 0;");

            VBox itemsBox = new VBox(8);
            itemsBox.setStyle("-fx-padding: 8 0;");
            for (CartItem item : order.getItems()) {
                String itemName = item.getProduct() != null ? item.getProduct().getName() : "Product";
                HBox itemRow = new HBox(12);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                
                Label itemLabel = new Label(String.format("• %s", itemName));
                itemLabel.getStyleClass().add("detail-value");
                itemLabel.setStyle("-fx-font-weight: 500;");
                
                Label qtyLabel = new Label(String.format("%.1f kg", item.getQuantityKg()));
                qtyLabel.getStyleClass().add("muted");
                
                Label priceLabel = new Label(String.format("@ %.2f ₺", item.getUnitPrice()));
                priceLabel.getStyleClass().add("muted");
                
                Region spacer2 = new Region();
                HBox.setHgrow(spacer2, Priority.ALWAYS);
                
                Label totalLabel = new Label(String.format("%.2f ₺", item.getLineTotal()));
                totalLabel.getStyleClass().add("detail-value");
                totalLabel.setStyle("-fx-font-weight: 600;");
                
                itemRow.getChildren().addAll(itemLabel, qtyLabel, priceLabel, spacer2, totalLabel);
                itemsBox.getChildren().add(itemRow);
            }

            detailsBox.getChildren().addAll(itemsSeparator, itemsHeader, itemsBox);
        }

        Separator totalSeparator = new Separator();
        totalSeparator.setStyle("-fx-opacity: 0.1; -fx-padding: 12 0;");
        
        HBox totalRow = new HBox(12);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalLabel = new Label("Total:");
        totalLabel.getStyleClass().add("detail-label");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");
        Label totalValue = new Label(String.format("%.2f ₺", order.getTotalAfterTax()));
        totalValue.getStyleClass().add("detail-value");
        totalValue.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #10b981;");
        totalRow.getChildren().addAll(totalLabel, totalValue);
        detailsBox.getChildren().addAll(totalSeparator, totalRow);

        card.getChildren().addAll(header, separator, detailsBox);
        return card;
    }
    
    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label + ":");
        l.getStyleClass().add("detail-label");
        l.setPrefWidth(140);
        l.setStyle("-fx-font-weight: 600;");

        Label v = new Label(value);
        v.getStyleClass().add("detail-value");
        v.setStyle("-fx-font-weight: 500;");

        row.getChildren().addAll(l, v);
        return row;
    }

    @FXML
    private void handleSelectOrder(Order order) {
        if (currentCarrierId == 0)
            return;

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
            ToastService.show(logoutButton.getScene(),
                    "Failed to select order. It may have been selected by another carrier.",
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
            ToastService.show(logoutButton.getScene(),
                    "Order " + orderToProcess.getId() + " has been marked as delivered.",
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
            stage.setScene(new Scene(loader.load(), 640, 480));
            stage.setTitle("Gr7Project3 - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
