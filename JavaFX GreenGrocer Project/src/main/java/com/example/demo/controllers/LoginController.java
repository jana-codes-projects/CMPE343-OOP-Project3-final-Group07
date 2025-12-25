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
 * Controller for the Login UI.
 * Handles user authentication and navigation to role-based interfaces.
 * 
 * @author Group07
 * @version 1.0
 */
public class LoginController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Button registerButton;
    
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
     * Handles the login button action.
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }
        
        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                openRoleBasedInterface(user);
            } else {
                showError("Invalid username or password");
            }
        } catch (Exception e) {
            showError("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the register button action.
     */
    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/register.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 500, 400));
            stage.setTitle("Register - Group07 GreenGrocer");
            stage.show();
            
            // Close login window
            ((Stage) loginButton.getScene().getWindow()).close();
        } catch (IOException e) {
            showError("Failed to open registration window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the appropriate interface based on user role.
     * 
     * @param user the authenticated user
     */
    private void openRoleBasedInterface(User user) {
        try {
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            FXMLLoader loader;
            String fxmlFile;
            String title;
            
            switch (user.getRole()) {
                case CUSTOMER:
                    fxmlFile = "/com/example/demo/customer.fxml";
                    title = "Group07 GreenGrocer - Customer";
                    break;
                case CARRIER:
                    fxmlFile = "/com/example/demo/carrier.fxml";
                    title = "Group07 GreenGrocer - Carrier";
                    break;
                case OWNER:
                    fxmlFile = "/com/example/demo/owner.fxml";
                    title = "Group07 GreenGrocer - Owner";
                    break;
                default:
                    showError("Unknown user role");
                    return;
            }
            
            loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load(), 960, 540);
            
            // Pass user to the controller
            Object controller = loader.getController();
            if (controller instanceof BaseController) {
                ((BaseController) controller).setCurrentUser(user);
            }
            
            currentStage.setScene(scene);
            currentStage.setTitle(title);
            currentStage.centerOnScreen();
        } catch (IOException e) {
            showError("Failed to open interface: " + e.getMessage());
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
        
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Login Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

