package com.cmpe343.fx.controller;

import com.cmpe343.fx.Session;
import com.cmpe343.fx.util.ToastService;
import com.cmpe343.model.User;
import com.cmpe343.service.AuthService;
import com.cmpe343.dao.UserDao;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton; // login.fxml'de fx:id="loginButton" olmalı

    private final AuthService authService = new AuthService();
    private final UserDao userDao = new UserDao();

    // Country-City mapping
    private static final Map<String, List<String>> COUNTRY_CITIES = new LinkedHashMap<>();
    static {
        COUNTRY_CITIES.put("Turkey", Arrays.asList("Istanbul", "Ankara", "Izmir", "Bursa", "Antalya", "Adana",
                "Gaziantep", "Konya", "Kayseri", "Mersin"));
        COUNTRY_CITIES.put("USA", Arrays.asList("New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
                "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"));
        COUNTRY_CITIES.put("UK", Arrays.asList("London", "Manchester", "Birmingham", "Leeds", "Glasgow", "Liverpool",
                "Newcastle", "Sheffield", "Bristol", "Edinburgh"));
        COUNTRY_CITIES.put("Germany", Arrays.asList("Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Stuttgart",
                "Düsseldorf", "Dortmund", "Essen", "Leipzig"));
        COUNTRY_CITIES.put("France", Arrays.asList("Paris", "Marseille", "Lyon", "Toulouse", "Nice", "Nantes",
                "Strasbourg", "Montpellier", "Bordeaux", "Lille"));
        COUNTRY_CITIES.put("Italy", Arrays.asList("Rome", "Milan", "Naples", "Turin", "Palermo", "Genoa", "Bologna",
                "Florence", "Bari", "Catania"));
    }

    // Country codes for phone numbers
    private static final Map<String, String> COUNTRY_CODES = new LinkedHashMap<>();
    static {
        COUNTRY_CODES.put("Turkey (+90)", "+90");
        COUNTRY_CODES.put("USA (+1)", "+1");
        COUNTRY_CODES.put("UK (+44)", "+44");
        COUNTRY_CODES.put("Germany (+49)", "+49");
        COUNTRY_CODES.put("France (+33)", "+33");
        COUNTRY_CODES.put("Italy (+39)", "+39");
        COUNTRY_CODES.put("Spain (+34)", "+34");
        COUNTRY_CODES.put("Netherlands (+31)", "+31");
        COUNTRY_CODES.put("Belgium (+32)", "+32");
        COUNTRY_CODES.put("Switzerland (+41)", "+41");
    }

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

            // Check if login window was fullscreen (to preserve that state)
            boolean wasFullScreen = stage.isFullScreen();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene newScene = new Scene(loader.load(), 900, 600);

            // Eski scene CSS'lerini yeni scene'e taşı (login tasarımın bozulmasın diye)
            Scene oldScene = stage.getScene();
            if (oldScene != null) {
                newScene.getStylesheets().addAll(oldScene.getStylesheets());
            }

            stage.setScene(newScene);
            stage.setTitle("Group07 GreenGrocer - " + user.getRole().toUpperCase() + " (" + user.getUsername() + ")");

            // If login was fullscreen, keep it fullscreen; otherwise make
            // customer/carrier/owner UIs maximized (fullscreen)
            if (wasFullScreen) {
                stage.setFullScreen(true);
            } else {
                // Make customer/carrier/owner UIs open in maximized (fullscreen) mode
                stage.setMaximized(true);
            }

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

    @FXML
    private void handleRegister() {
        try {
            Stage stage = getStage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setTitle("Gr7Project3 - Register");
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            toastError("Failed to load registration page.");
        }
    }

    private String calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "weak";
        }

        int score = 0;
        if (password.length() >= 8)
            score++;
        if (password.length() >= 12)
            score++;
        if (password.matches(".*[a-z].*"))
            score++;
        if (password.matches(".*[A-Z].*"))
            score++;
        if (password.matches(".*[0-9].*"))
            score++;
        if (password.matches(".*[^a-zA-Z0-9].*"))
            score++;

        if (score <= 2)
            return "weak";
        if (score <= 4)
            return "good";
        return "strong";
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
