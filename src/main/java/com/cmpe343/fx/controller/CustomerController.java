package com.cmpe343.fx.controller;

import com.cmpe343.dao.CartDao;
import com.cmpe343.dao.ProductDao;
import com.cmpe343.dao.UserDao;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.fx.Session;
import com.cmpe343.model.Product;
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
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

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
    
    private boolean vegetablesVisible = true;
    private boolean fruitsVisible = true;

    private final ProductDao productDao = new ProductDao();
    private final CartDao cartDao = new CartDao();

    private FilteredList<Product> filteredProducts;
    private int currentCustomerId;

    @FXML
    public void initialize() {
        if (Session.isLoggedIn()) {
            this.currentCustomerId = Session.getUser().getId();
            usernameLabel.setText(Session.getUser().getUsername());
        }

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
            }
        });
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
            javafx.scene.image.Image image = null;
//            byte[] imageBytes = productDao.getProductImageBlob(p.getId());
//            if (imageBytes != null) {
//                image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
//            }
            
            if (image != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(image);
                iv.setFitWidth(80);
                iv.setFitHeight(80);
                iv.setPreserveRatio(true);
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

        Label priceLbl = new Label(p.getPrice() + " ₺ / kg");
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
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cart.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);

            // Carry Styles
            scene.getStylesheets().addAll(stage.getScene().getStylesheets());

            stage.setScene(scene);
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("Failed to open cart", ToastService.Type.ERROR);
        }
    }
    
    @FXML
    private void handleViewOrders() {
        try {
            Stage stage = new Stage();
            stage.setTitle("Order History");
            VBox root = new VBox(16);
            root.setStyle("-fx-padding: 24; -fx-background-color: #0f172a;");
            
            Label title = new Label("Order History");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
            
            ScrollPane scrollPane = new ScrollPane();
            VBox ordersContainer = new VBox(12);
            ordersContainer.setStyle("-fx-padding: 12;");
            
            com.cmpe343.dao.OrderDao orderDao = new com.cmpe343.dao.OrderDao();
            java.util.List<com.cmpe343.model.Order> orders = orderDao.getOrdersForCustomer(currentCustomerId);
            
            if (orders.isEmpty()) {
                Label empty = new Label("No orders yet");
                empty.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20;");
                ordersContainer.getChildren().add(empty);
            } else {
                for (com.cmpe343.model.Order order : orders) {
                    VBox orderCard = createOrderCard(order);
                    ordersContainer.getChildren().add(orderCard);
                }
            }
            
            scrollPane.setContent(ordersContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            
            root.getChildren().addAll(title, scrollPane);
            
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().addAll(searchField.getScene().getStylesheets());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            toast("Failed to load orders", ToastService.Type.ERROR);
        }
    }
    
    private VBox createOrderCard(com.cmpe343.model.Order order) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 16;");
        
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label orderId = new Label("Order #" + order.getId());
        orderId.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label status = new Label(order.getStatus().name());
        status.setStyle("-fx-padding: 4 12; -fx-background-color: #10b981; -fx-background-radius: 4; -fx-text-fill: white;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label total = new Label(String.format("%.2f ₺", order.getTotalAfterTax()));
        total.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        header.getChildren().addAll(orderId, status, spacer, total);
        
        Label date = new Label("Ordered: " + order.getOrderTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        date.setStyle("-fx-text-fill: #94a3b8;");
        
        card.getChildren().addAll(header, date);
        
        // Add download PDF button if delivered
        if (order.getStatus() == com.cmpe343.model.Order.OrderStatus.DELIVERED) {
            HBox actions = new HBox(8);
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
            card.getChildren().add(actions);
        }
        
        return card;
    }
    
    private void downloadInvoice(com.cmpe343.model.Order order) {
        try {
            com.cmpe343.service.PdfService pdfService = new com.cmpe343.service.PdfService();
            java.io.File pdfFile = pdfService.generateInvoice(order);
            
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Save Invoice");
            fileChooser.setInitialFileName("invoice_" + order.getId() + ".pdf");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            java.io.File saveFile = fileChooser.showSaveDialog(stage);
            
            if (saveFile != null) {
                java.nio.file.Files.copy(pdfFile.toPath(), saveFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                toast("Invoice downloaded successfully", ToastService.Type.SUCCESS);
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
                
                int messageId = messageDao.createMessage(currentCustomerId, ownerId, messageText);
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
    private void toggleVegetables() {
        vegetablesVisible = !vegetablesVisible;
        vegetablesGrid.setVisible(vegetablesVisible);
        vegetablesGrid.setManaged(vegetablesVisible);
        vegetablesToggle.setText(vegetablesVisible ? "▼" : "▶");
    }
    
    @FXML
    private void toggleFruits() {
        fruitsVisible = !fruitsVisible;
        fruitsGrid.setVisible(fruitsVisible);
        fruitsGrid.setManaged(fruitsVisible);
        fruitsToggle.setText(fruitsVisible ? "▼" : "▶");
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

    private void toast(String msg, ToastService.Type type) {
        ToastService.show(searchField.getScene(), msg, type);
    }
}
