package com.example.demo.controllers;

import com.example.demo.dao.MessageDAO;
import com.example.demo.models.Message;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Messages UI.
 * Allows customers to send messages to the owner and view message history.
 * 
 * @author Group07
 * @version 1.0
 */
public class MessagesController extends BaseController {
    @FXML private VBox messagesContainer;
    @FXML private TextArea messageTextArea;
    @FXML private Button sendButton;
    @FXML private Button refreshButton;
    @FXML private Button closeButton;
    
    private MessageDAO messageDAO;
    private int customerId;
    private int ownerId;
    
    @FXML
    public void initialize() {
        messageDAO = new MessageDAO();
        
        // Find the owner ID (assuming there's only one owner)
        String sql = "SELECT id, username FROM users WHERE role = 'owner' AND is_active = 1 LIMIT 1";
        try (java.sql.Connection conn = com.example.demo.db.DatabaseAdapter.getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                ownerId = rs.getInt("id");
                String ownerUsername = rs.getString("username");
                System.out.println("DEBUG MessagesController: Found owner ID: " + ownerId + " (Username: " + ownerUsername + ")");
            } else {
                System.out.println("ERROR MessagesController: No active owner found in database!");
            }
        } catch (java.sql.SQLException e) {
            System.err.println("ERROR MessagesController: Exception while finding owner: " + e.getMessage());
            e.printStackTrace();
        }
        
        messageTextArea.setWrapText(true);
        messageTextArea.setPromptText("Type your message to the owner here...");
    }
    
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
        loadMessages();
    }
    
    private void loadMessages() {
        messagesContainer.getChildren().clear();
        
        List<Message> messages = messageDAO.getMessagesByCustomer(customerId);
        
        if (messages.isEmpty()) {
            Label noMessagesLabel = new Label("No messages yet. Send a message to start a conversation.");
            noMessagesLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            messagesContainer.getChildren().add(noMessagesLabel);
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        for (Message message : messages) {
            VBox messageCard = createMessageCard(message, formatter);
            messagesContainer.getChildren().add(messageCard);
        }
    }
    
    private VBox createMessageCard(Message message, DateTimeFormatter formatter) {
        VBox card = new VBox(10);
        card.setStyle("-fx-padding: 15; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #f9f9f9; -fx-background-radius: 5;");
        card.setPrefWidth(Double.MAX_VALUE);
        
        // Header with timestamp
        HBox header = new HBox(10);
        Label dateLabel = new Label("Sent: " + message.getCreatedAt().format(formatter));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(dateLabel);
        
        // Message text
        TextArea messageArea = new TextArea(message.getText());
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(3);
        messageArea.setMaxHeight(100);
        messageArea.setStyle("-fx-background-color: white;");
        
        card.getChildren().addAll(header, messageArea);
        
        return card;
    }
    
    @FXML
    private void handleSendMessage() {
        String text = messageTextArea.getText().trim();
        
        if (text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Message", 
                    "Please enter a message before sending.");
            return;
        }
        
        if (ownerId == 0) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Owner not found. Cannot send message.");
            return;
        }
        
        try {
            System.out.println("DEBUG MessagesController: Sending message from customer ID: " + customerId + " to owner ID: " + ownerId);
            Message message = new Message();
            message.setCustomerId(customerId);
            message.setOwnerId(ownerId);
            message.setText(text);
            
            Message createdMessage = messageDAO.createMessage(message);
            if (createdMessage != null) {
                System.out.println("DEBUG MessagesController: Message created successfully with ID: " + createdMessage.getId());
                showAlert(Alert.AlertType.INFORMATION, "Message Sent", 
                        "Your message has been sent successfully!");
                messageTextArea.clear();
                loadMessages(); // Refresh the messages list
            } else {
                System.out.println("ERROR MessagesController: Failed to create message");
                showAlert(Alert.AlertType.ERROR, "Error", 
                        "Failed to send message. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("ERROR MessagesController: Exception while sending message: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "An error occurred: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadMessages();
    }
    
    @FXML
    private void handleClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

