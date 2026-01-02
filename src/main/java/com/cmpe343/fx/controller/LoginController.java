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
import javafx.scene.Node;
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
        COUNTRY_CITIES.put("Turkey", Arrays.asList("Istanbul", "Ankara", "Izmir", "Bursa", "Antalya", "Adana", "Gaziantep", "Konya", "Kayseri", "Mersin"));
        COUNTRY_CITIES.put("USA", Arrays.asList("New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"));
        COUNTRY_CITIES.put("UK", Arrays.asList("London", "Manchester", "Birmingham", "Leeds", "Glasgow", "Liverpool", "Newcastle", "Sheffield", "Bristol", "Edinburgh"));
        COUNTRY_CITIES.put("Germany", Arrays.asList("Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Stuttgart", "Düsseldorf", "Dortmund", "Essen", "Leipzig"));
        COUNTRY_CITIES.put("France", Arrays.asList("Paris", "Marseille", "Lyon", "Toulouse", "Nice", "Nantes", "Strasbourg", "Montpellier", "Bordeaux", "Lille"));
        COUNTRY_CITIES.put("Italy", Arrays.asList("Rome", "Milan", "Naples", "Turin", "Palermo", "Genoa", "Bologna", "Florence", "Bari", "Catania"));
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
            stage.setTitle("Gr7Project3 - " + user.getRole() + " (" + user.getUsername() + ")");
            
            // If login was fullscreen, keep it fullscreen; otherwise make customer/carrier/owner UIs maximized (fullscreen)
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
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Register New Customer");
        dialog.setHeaderText("Create your account");
        dialog.setResizable(true);

        // === Username ===
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #64748b;");

        // === Password ===
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #64748b;");

        Label passwordStrengthLabel = new Label();
        passwordStrengthLabel.setStyle("-fx-font-size: 11px; -fx-padding: 4 0 0 0;");
        passwordStrengthLabel.setTextFill(Color.WHITE);

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            String strength = calculatePasswordStrength(newVal);
            passwordStrengthLabel.setText("Password strength: " + strength);
            switch (strength.toLowerCase()) {
                case "weak" -> passwordStrengthLabel.setTextFill(Color.web("#ef4444"));
                case "good" -> passwordStrengthLabel.setTextFill(Color.web("#f59e0b"));
                case "strong" -> passwordStrengthLabel.setTextFill(Color.web("#10b981"));
            }
        });

        // === Country / City ===
        ComboBox<String> countryCombo = new ComboBox<>();
        ComboBox<String> cityCombo = new ComboBox<>();
        countryCombo.getItems().addAll(COUNTRY_CITIES.keySet());
        countryCombo.setPromptText("Select Country");
        countryCombo.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #64748b;");

        countryCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setTextFill(Color.WHITE);
            }
        });

        countryCombo.setOnAction(e -> {
            String selectedCountry = countryCombo.getValue();
            if (selectedCountry != null) {
                cityCombo.getItems().clear();
                cityCombo.getItems().addAll(COUNTRY_CITIES.get(selectedCountry));
        }
    });

    // === Address ===
    TextField addressField = new TextField();
    addressField.setPromptText("Maslak Mah. Büyükdere Cd. No:12 Şişli/Istanbul");
    addressField.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #64748b;");

    Label addressExample = new Label("Example: Maslak Mah. Büyükdere Cd. No:12 Şişli/Istanbul");
    addressExample.setStyle("-fx-font-size: 11px;");
    addressExample.setTextFill(Color.web("#64748b"));

    // === Phone ===
    ComboBox<String> phoneCodeCombo = new ComboBox<>();
    phoneCodeCombo.getItems().addAll(COUNTRY_CODES.keySet());
    phoneCodeCombo.setPromptText("Country Code");
    phoneCodeCombo.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #64748b;");

    phoneCodeCombo.setButtonCell(new ListCell<>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(item);
            setTextFill(Color.WHITE);
        }
    });

    TextField phoneField = new TextField();
    phoneField.setPromptText("532 101 10 01");
    phoneField.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #64748b;");

    // === Layout ===
    VBox form = new VBox(10);
    form.setStyle("-fx-padding: 20; -fx-background-color: #0f172a;");

    Label usernameLabel = new Label("Username:");
    Label passwordLabel = new Label("Password:");
    Label countryLabel = new Label("Country:");
    Label cityLabel = new Label("City:");
    Label addressLabel = new Label("Address:");
    Label phoneLabel = new Label("Phone Number:");

    for (Label l : List.of(usernameLabel, passwordLabel, countryLabel, cityLabel, addressLabel, phoneLabel)) {
        l.setTextFill(Color.WHITE);
        l.getStyleClass().add("field-label");
    }

        HBox phoneBox = new HBox(8, phoneCodeCombo, phoneField);

    form.getChildren().addAll(
        usernameLabel, usernameField,
        passwordLabel, passwordField, passwordStrengthLabel,
        countryLabel, countryCombo,
        cityLabel, cityCombo,
        addressLabel, addressField, addressExample,
        phoneLabel, phoneBox
    );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(600, 700);

        // === Button Styling ===
        Platform.runLater(() -> {
            Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (okBtn != null) okBtn.getStyleClass().add("btn-primary");
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Map<String, String> result = new HashMap<>();
                result.put("username", usernameField.getText().trim());
                result.put("password", passwordField.getText());
                result.put("country", countryCombo.getValue());
                result.put("city", cityCombo.getValue());
                result.put("address", addressField.getText().trim());
                result.put("phoneCode", phoneCodeCombo.getValue() != null
                        ? COUNTRY_CODES.get(phoneCodeCombo.getValue()) : "");
                result.put("phone", phoneField.getText().trim());
                return result;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> {
            if (data.values().stream().anyMatch(v -> v == null || v.isEmpty())) {
                toastError("Please fill in all fields.");
                return;
            }

            if (userDao.usernameExists(data.get("username"))) {
                toastError("Username already exists. Please choose another.");
                return;
            }
            
            // Validate phone number
            String phoneNumber = data.get("phone");
            if (phoneNumber != null && !phoneNumber.isEmpty() && !isValidPhoneNumber(phoneNumber)) {
                toastError("Invalid phone number format. Please enter a valid phone number (e.g., 532 123 45 67).");
                return;
            }

            try {
                int userId = userDao.createCustomer(
                    data.get("username"),
                    data.get("password"),
                    data.get("phoneCode") + " " + data.get("phone"),
                    data.get("address") + ", " + data.get("city") + ", " + data.get("country")
                );

                if (userId > 0) toastSuccess("Registration successful! You can now login.");
                else toastError("Registration failed. Please try again.");

            } catch (Exception e) {
                toastError("Registration failed: " + e.getMessage());
            }
        });
    }

    
    private String calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "weak";
        }
        
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;
        
        if (score <= 2) return "weak";
        if (score <= 4) return "good";
        return "strong";
    }
    
    /**
     * Validates phone number format.
     * Accepts formats like: 5321234567, 532 123 45 67, +90 532 123 45 67, etc.
     * Phone number should contain 10-15 digits after removing formatting characters.
     * 
     * @param phoneNumber The phone number to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove common formatting characters (spaces, dashes, parentheses, plus signs, dots)
        String digitsOnly = phoneNumber.replaceAll("[\\s\\-\\(\\)\\+\\.]", "");
        
        // Check if remaining string contains only digits
        if (!digitsOnly.matches("\\d+")) {
            return false;
        }
        
        // Check length: should be between 10-15 digits (allows for country codes)
        int length = digitsOnly.length();
        return length >= 10 && length <= 15;
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
