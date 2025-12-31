package com.cmpe343.fx.controller;

import com.cmpe343.fx.Session;

import com.cmpe343.fx.util.ToastService;
import com.cmpe343.model.User;
import com.cmpe343.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton; // login.fxml'de fx:id="loginButton" olmalı

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        // Enter ile login
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());

        Platform.runLater(() -> usernameField.requestFocus());

        // İstersen ilk açılış test (sonra silebilirsin)
        // Platform.runLater(() -> toastInfo("Toast hazır ✅"));
    }

    @FXML
    private void handleLogin() {
        setBusy(true);

        String u = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String p = passwordField.getText() == null ? "" : passwordField.getText();

        if (u.isEmpty() || p.isEmpty()) {
            toastInfo("Please enter username and password.");
            setBusy(false);
            return;
        }

        try {
            User user = authService.login(u, p);

            if (user == null) {
                toastError("Invalid username or password.");
                setBusy(false);
                return;
            }

            toastSuccess("Login successful ✅");
            Session.setUser(user);

            String fxml = switch (user.getRole()) {
                case "customer" -> "/fxml/customer.fxml";
                case "carrier" -> "/fxml/carrier.fxml";
                case "owner" -> "/fxml/owner.fxml";
                default -> "/fxml/login.fxml";
            };

            Stage stage = getStage();
            if (stage == null) {
                toastError("Stage not found (UI error).");
                setBusy(false);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene newScene = new Scene(loader.load(), 900, 600);

            // Eski scene CSS'lerini yeni scene'e taşı (login tasarımın bozulmasın diye)
            Scene oldScene = stage.getScene();
            if (oldScene != null) {
                newScene.getStylesheets().addAll(oldScene.getStylesheets());
            }

            stage.setScene(newScene);
            stage.setTitle("Gr7Project3 - " + user.getRole() + " (" + user.getUsername() + ")");
            stage.centerOnScreen();

        } catch (Exception ex) {
            ex.printStackTrace();
            toastError("Unexpected error: " + safeMsg(ex));
        } finally {
            setBusy(false);
        }
    }

    @FXML
    private void handleExit() {
        Stage stage = getStage();
        if (stage != null)
            stage.close();
    }

    // ---------------- Helpers ----------------

    private Stage getStage() {
        if (usernameField == null || usernameField.getScene() == null)
            return null;
        return (Stage) usernameField.getScene().getWindow();
    }

    private void setBusy(boolean busy) {
        if (loginButton != null)
            loginButton.setDisable(busy);
        if (usernameField != null)
            usernameField.setDisable(busy);
        if (passwordField != null)
            passwordField.setDisable(busy);
    }

    // Toast wrappers (Toastify-style, ekrana etki etmez)
    private void toastInfo(String msg) {
        ToastService.show(
                usernameField.getScene(),
                msg,
                ToastService.Type.INFO,
                ToastService.Position.BOTTOM_CENTER,
                Duration.seconds(2.2));
    }

    private void toastError(String msg) {
        ToastService.show(
                usernameField.getScene(),
                msg,
                ToastService.Type.ERROR,
                ToastService.Position.BOTTOM_CENTER,
                Duration.seconds(2.8));
    }

    private void toastSuccess(String msg) {
        ToastService.show(
                usernameField.getScene(),
                msg,
                ToastService.Type.SUCCESS,
                ToastService.Position.BOTTOM_CENTER,
                Duration.seconds(1.7));
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null || m.isBlank())
            return t.getClass().getSimpleName();
        return m.length() > 160 ? m.substring(0, 160) + "..." : m;
    }
}
