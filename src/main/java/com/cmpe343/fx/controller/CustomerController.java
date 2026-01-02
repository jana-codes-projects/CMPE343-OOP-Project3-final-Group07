package com.cmpe343.fx.controller;

import com.cmpe343.dao.CartDao;
import com.cmpe343.dao.ProductDao;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.fx.Session;
import com.cmpe343.model.Product;
import com.cmpe343.model.Message;
import com.cmpe343.dao.MessageDao;
import com.cmpe343.dao.UserDao;
import javafx.application.Platform;
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
import javafx.stage.Stage;
import javafx.scene.layout.Priority; // Added

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
    private VBox chatContainer;
    @FXML
    private VBox chatMessagesBox;
    @FXML
    private TextField chatInput;
    @FXML
    private ScrollPane chatScroll;
    @FXML
    private javafx.scene.shape.SVGPath vegChevron;
    @FXML
    private javafx.scene.shape.SVGPath fruitChevron;
    @FXML
    private VBox vegContentBox;
    @FXML
    private VBox fruitContentBox;
    @FXML
    private Label vegCountLabel;
    @FXML
    private Label fruitCountLabel;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label avatarLetter;
    @FXML
    private Label resultInfoLabel;

    private final ProductDao productDao = new ProductDao();
    private final CartDao cartDao = new CartDao();
    private final MessageDao messageDao = new MessageDao();
    private final UserDao userDao = new UserDao();

    private FilteredList<Product> filteredProducts;
    private int currentCustomerId;

    private boolean vegetablesExpanded = true;
    private boolean fruitsExpanded = true;

    @FXML
    public void initialize() {
        if (Session.isLoggedIn()) {
            this.currentCustomerId = Session.getUser().getId();
            String username = Session.getUser().getUsername();
            usernameLabel.setText(username);
            if (welcomeLabel != null) {
                welcomeLabel.setText("Welcome back, " + username);
            }
            if (avatarLetter != null && !username.isEmpty()) {
                avatarLetter.setText(username.substring(0, 1).toUpperCase());
            }
        }

        // Load Data - Filter out zero stock items
        java.util.List<Product> products = productDao.findAll();
        products.removeIf(p -> p.getStockKg() <= 0);
        products.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

        ObservableList<Product> allProducts = FXCollections.observableArrayList(products);
        filteredProducts = new FilteredList<>(allProducts, p -> true);

        // Update category counts
        long vegCount = products.stream().filter(p -> p.getType() == Product.ProductType.VEG).count();
        long fruitCount = products.stream().filter(p -> p.getType() == Product.ProductType.FRUIT).count();
        if (vegCountLabel != null) {
            vegCountLabel.setText(vegCount + " products");
        }
        if (fruitCountLabel != null) {
            fruitCountLabel.setText(fruitCount + " products");
        }

        // Search Listener
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredProducts.setPredicate(p -> {
                if (newV == null || newV.isBlank())
                    return true;
                return p.getName().toLowerCase().contains(newV.toLowerCase());
            });
            renderGrids();
            updateSearchResultInfo();
        });

        renderGrids();
        updateBadge();
        updateSearchResultInfo();

        // Ensure CSS
        Platform.runLater(() -> {
            if (searchField.getScene() != null) {
                searchField.getScene().getStylesheets().clear();
                searchField.getScene().getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
                searchField.getScene().getStylesheets()
                        .add(getClass().getResource("/css/customer.css").toExternalForm());
            }
        });
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
    }

    private void renderGrids() {
        vegetablesGrid.getChildren().clear();
        fruitsGrid.getChildren().clear();

        for (Product p : filteredProducts) {
            Node card = createProductCard(p);
            if (p.getType() == Product.ProductType.VEG) {
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
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        // Ensure card has consistent size and alignment
        card.setAlignment(Pos.TOP_CENTER);

        // --- Image Section ---
        Node imageNode;
        if (p.getImageBlob() != null && p.getImageBlob().length > 0) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(new java.io.ByteArrayInputStream(p.getImageBlob())));
                iv.setFitWidth(100);
                iv.setFitHeight(100);
                iv.setPreserveRatio(false);

                // Rounded corners
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(100, 100);
                clip.setArcWidth(15);
                clip.setArcHeight(15);
                iv.setClip(clip);

                imageNode = iv;
            } catch (Exception e) {
                Label img = new Label(p.getName().substring(0, 1).toUpperCase());
                img.getStyleClass().add("product-image-placeholder");
                imageNode = img;
            }
        } else {
            Label img = new Label(p.getName().substring(0, 1).toUpperCase());
            img.getStyleClass().add("product-image-placeholder");
            imageNode = img;
        }

        VBox imgContainer = new VBox(imageNode);
        imgContainer.setAlignment(Pos.CENTER);
        imgContainer.getStyleClass().add("product-image-container");
        imgContainer.setMinHeight(110);

        // --- Info Section ---
        Label nameLbl = new Label(p.getName());
        nameLbl.getStyleClass().add("product-title");
        nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Price & Deal
        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER);

        Label priceLbl = new Label(String.format("%.2f ₺ / kg", p.getPrice()));
        priceLbl.getStyleClass().add("product-price");
        priceLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        Label dealBadge = new Label(
                String.format("Buy >%.0fkg, Get %.0f%% OFF", p.getDiscountThreshold(), p.getDiscountPercentage()));
        dealBadge.getStyleClass().addAll("badge", "badge-info");
        dealBadge.setStyle(
                "-fx-font-size: 9px; -fx-padding: 2 6; -fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #60a5fa;");

        priceBox.getChildren().addAll(priceLbl, dealBadge);

        // Stock Status
        HBox stockBox = new HBox(4);
        stockBox.setAlignment(Pos.CENTER);

        Label stockLbl = new Label(String.format("Stock: %.1f kg", p.getStockKg()));
        stockLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        if (p.isLowStock()) {
            stockLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            stockLbl.setText("Low Stock: " + p.getStockKg() + " kg");
        }

        stockBox.getChildren().add(stockLbl);

        // --- Controls Section ---
        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new javafx.geometry.Insets(8, 0, 0, 0));

        TextField kgInput = new TextField();
        kgInput.setPromptText("kg");
        kgInput.setPrefWidth(50);
        kgInput.getStyleClass().add("field");
        kgInput.setStyle("-fx-alignment: center; -fx-padding: 4 8; -fx-font-size: 12px;");

        // Auto-select text on focus for easier editing
        kgInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal)
                Platform.runLater(kgInput::selectAll);
        });

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 16;");

        addBtn.setOnAction(e -> handleAddToCart(p, kgInput));

        controls.getChildren().addAll(kgInput, addBtn);

        card.getChildren().addAll(imgContainer, nameLbl, priceBox, stockBox, controls);
        return card;
    }

    private void handleAddToCart(Product p, TextField kgInput) {
        try {
            double kg = Double.parseDouble(kgInput.getText().trim());
            if (kg <= 0) {
                toast("Invalid amount", ToastService.Type.ERROR);
                return;
            }
            if (kg > p.getStockKg()) {
                toast("Insufficient stock!", ToastService.Type.ERROR);
                return;
            }

            cartDao.addToCart(currentCustomerId, p.getId(), kg);
            toast("Added to cart", ToastService.Type.SUCCESS);
            kgInput.clear();
            updateBadge();
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

    @FXML
    private void handleOpenCart() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cart.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            // Carry Styles
            scene.getStylesheets().addAll(stage.getScene().getStylesheets());

            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            toast("Failed to open cart", ToastService.Type.ERROR);
        }
    }

    @FXML
    private void handleOpenOrders() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/orders.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            if (stage.getScene() != null) {
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            stage.setScene(new Scene(loader.load(), 640, 480));
            stage.setTitle("Gr7Project3 - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Kept handleViewMessages for legacy support if needed, or simply for full view
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
        card.setStyle("-fx-background-color: " + (msg.isRead() ? "#1e293b" : "#2563eb")
                + "; -fx-background-radius: 8; -fx-padding: 16;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label sender = new Label("To: Owner");
        sender.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(
                msg.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
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

    private void toast(String msg, ToastService.Type type) {
        ToastService.show(searchField.getScene(), msg, type);
    }

    @FXML
    private void toggleChat() {
        boolean visible = !chatContainer.isVisible();
        chatContainer.setVisible(visible);
        chatContainer.setManaged(visible);
        chatContainer.setMouseTransparent(!visible);
        if (visible) {
            // Ensure chat container is on top of all other elements
            Platform.runLater(() -> {
                if (chatContainer.getParent() != null
                        && chatContainer.getParent() instanceof javafx.scene.layout.StackPane) {
                    javafx.scene.layout.StackPane parent = (javafx.scene.layout.StackPane) chatContainer.getParent();
                    // Remove and re-add to ensure it's on top
                    parent.getChildren().remove(chatContainer);
                    parent.getChildren().add(chatContainer);
                    // Also use toFront() to ensure z-order
                    chatContainer.toFront();
                }
                loadChatWidgetMessages();
                Platform.runLater(() -> {
                    if (chatInput != null) {
                        chatInput.requestFocus();
                    }
                    // Double check z-order after layout
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
        // Use getMessagesBetween to get full conversation including Owner replies as
        // separate rows
        int ownerId = userDao.getOwnerId();
        java.util.List<Message> messages = messageDao.getMessagesBetween(ownerId, currentCustomerId);

        for (Message msg : messages) {
            chatMessagesBox.getChildren().add(createWidgetMessage(msg));
        }
        // Scroll to bottom logic:
        // We must wait for layout to be computed.
        Platform.runLater(() -> {
            chatMessagesBox.applyCss();
            chatMessagesBox.layout();
            chatScroll.applyCss();
            chatScroll.layout();
            chatScroll.setVvalue(1.0);

            // Double check to ensure scroll
            Platform.runLater(() -> {
                chatScroll.setVvalue(1.0);
                // Ensure chat container is still on top
                if (chatContainer.getParent() != null) {
                    chatContainer.toFront();
                }
            });
        });
    }

    private Node createWidgetMessage(Message msg) {
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(260);

        // Logic: specific bubble for "Me" (Customer) vs "Owner"
        // Use robust senderId check
        boolean isMe = (msg.getSenderId() == currentCustomerId);
        boolean isOwnerMsg = !isMe;

        String bg = isMe ? "#3b82f6" : "#475569"; // Blue for me, Gray for owner
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

        // Legacy: check if there is an OLD reply_text attached to this message row
        // If so, we must show it as a separate bubble AFTER this one to preserve
        // history
        // But createWidgetMessage returns 1 Node.
        // We might need to return VBox of nodes? Or just handle it in the loop?
        // Let's handle it in the loop? No, loop calls `createWidgetMessage`.
        // Let's return VBox containing both?

        String replyText = messageDao.getReplyText(msg.getId());
        if (replyText != null && !replyText.isEmpty()) {
            // Append reply bubble
            VBox replyBubble = new VBox(4);
            replyBubble.setMaxWidth(260);
            replyBubble.setStyle("-fx-background-color: #475569; -fx-background-radius: 12; -fx-padding: 8 12;"); // Owner
                                                                                                                  // Color

            Label rContent = new Label(replyText);
            rContent.setWrapText(true);
            rContent.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

            // Approximate time
            Label rDate = new Label(
                    msg.getTimestamp().plusMinutes(1).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            rDate.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 10px; opacity: 0.8;");

            replyBubble.getChildren().addAll(rContent, rDate);

            HBox replyRow = new HBox(replyBubble);
            replyRow.setAlignment(Pos.CENTER_LEFT); // Owner is Left

            VBox container = new VBox(5);
            container.getChildren().addAll(row, replyRow);
            return container;
        }

        return row;
    }

    @FXML
    private void handleToggleVegetables() {
        vegetablesExpanded = !vegetablesExpanded;
        vegContentBox.setVisible(vegetablesExpanded);
        vegContentBox.setManaged(vegetablesExpanded);

        // Rotate chevron
        if (vegChevron != null) {
            if (vegetablesExpanded) {
                vegChevron.setContent("M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z"); // Down
            } else {
                vegChevron.setContent("M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"); // Up
            }
        }
    }

    @FXML
    private void handleToggleFruits() {
        fruitsExpanded = !fruitsExpanded;
        fruitContentBox.setVisible(fruitsExpanded);
        fruitContentBox.setManaged(fruitsExpanded);

        // Rotate chevron
        if (fruitChevron != null) {
            if (fruitsExpanded) {
                fruitChevron.setContent("M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z"); // Down
            } else {
                fruitChevron.setContent("M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"); // Up
            }
        }
    }

    private void updateSearchResultInfo() {
        if (resultInfoLabel == null)
            return;

        long totalCount = filteredProducts.size();
        long vegCount = filteredProducts.stream().filter(p -> p.getType() == Product.ProductType.VEG).count();
        long fruitCount = filteredProducts.stream().filter(p -> p.getType() == Product.ProductType.FRUIT).count();

        if (searchField.getText() == null || searchField.getText().isBlank()) {
            resultInfoLabel.setText("");
        } else {
            resultInfoLabel.setText(
                    String.format("%d results found (%d vegetables, %d fruits)", totalCount, vegCount, fruitCount));
        }
    }

}
