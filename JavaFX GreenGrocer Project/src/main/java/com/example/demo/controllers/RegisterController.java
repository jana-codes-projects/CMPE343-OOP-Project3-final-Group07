package com.example.demo.controllers;

import com.example.demo.dao.UserDAO;
import com.example.demo.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the Registration UI.
 * Handles customer registration with validation.
 * 
 * @author Group07
 * @version 1.0
 */
public class RegisterController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private TextField addressField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private Button registerButton;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Label errorLabel;
    
    private UserDAO userDAO;

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        userDAO = new UserDAO();
        errorLabel.setVisible(false);
    }

    /**
     * Handles the register button action.
     */
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        
        // Validation
        if (username.isEmpty() || password.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            showError("All fields are required");
            return;
        }
        
        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return;
        }
        
        if (password.length() < 4) {
            showError("Password must be at least 4 characters");
            return;
        }
        
        try {
            // Check if username exists
            if (userDAO.usernameExists(username)) {
                showError("Username already exists. Please choose another.");
                return;
            }
            
            // Create customer
            User user = userDAO.createCustomer(username, password, address, phone);
            if (user != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Registration Successful");
                alert.setHeaderText(null);
                alert.setContentText("Registration successful! You can now login.");
                alert.showAndWait();
                
                handleCancel();
            } else {
                showError("Registration failed. Please try again.");
            }
        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the cancel button action.
     */
    @FXML
    private void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/login.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 500, 400));
            stage.setTitle("Login - Group07 GreenGrocer");
            stage.show();
            
            // Close registration window
            ((Stage) cancelButton.getScene().getWindow()).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows an error message.
     * 
     * @param message the error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

