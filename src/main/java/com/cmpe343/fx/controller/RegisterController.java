package com.cmpe343.fx.controller;

import com.cmpe343.dao.UserDao;
import com.cmpe343.fx.util.ToastService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextArea addressField;

    private final UserDao userDao = new UserDao();

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            ToastService.show(usernameField.getScene(), "Username and password required", ToastService.Type.ERROR);
            return;
        }

        // Strong password check: at least 6 chars, 1 uppercase, 1 lowercase, 1 number
        if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$")) {
            ToastService.show(usernameField.getScene(),
                    "Password must be at least 6 chars, contain a number, uppercase and lowercase letter.",
                    ToastService.Type.ERROR,
                    com.cmpe343.fx.util.ToastService.Position.TOP_RIGHT,
                    javafx.util.Duration.seconds(3));
            return;
        }

        try {
            boolean success = userDao.registerCustomer(username, password, address, phone);
            if (success) {
                ToastService.show(usernameField.getScene(), "Registration successful! Please login.",
                        ToastService.Type.SUCCESS);
                handleBackToLogin();
            } else {
                ToastService.show(usernameField.getScene(), "Username already exists", ToastService.Type.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ToastService.show(usernameField.getScene(), "Registration failed", ToastService.Type.ERROR);
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            stage.setScene(new Scene(loader.load(), 640, 480));
            stage.setTitle("Gr7Project3 - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
