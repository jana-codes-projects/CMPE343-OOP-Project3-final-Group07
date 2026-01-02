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

        // Validate all required fields
        if (username.isEmpty() || password.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            ToastService.show(usernameField.getScene(), 
                "Please fill in all fields (username, password, address, and phone).", 
                ToastService.Type.ERROR,
                com.cmpe343.fx.util.ToastService.Position.TOP_RIGHT,
                javafx.util.Duration.seconds(3));
            return;
        }

        // Validate phone number format
        if (!isValidPhoneNumber(phone)) {
            ToastService.show(usernameField.getScene(),
                    "Invalid phone number format. Please enter a valid phone number (e.g., 532 123 45 67).",
                    ToastService.Type.ERROR,
                    com.cmpe343.fx.util.ToastService.Position.TOP_RIGHT,
                    javafx.util.Duration.seconds(3));
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
                ToastService.show(usernameField.getScene(), 
                    "Registration successful! Please login.",
                    ToastService.Type.SUCCESS,
                    com.cmpe343.fx.util.ToastService.Position.TOP_RIGHT,
                    javafx.util.Duration.seconds(3));
                handleBackToLogin();
            } else {
                ToastService.show(usernameField.getScene(), 
                    "Username already exists. Please choose another.",
                    ToastService.Type.ERROR,
                    com.cmpe343.fx.util.ToastService.Position.TOP_RIGHT,
                    javafx.util.Duration.seconds(3));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ToastService.show(usernameField.getScene(), 
                "Registration failed: " + e.getMessage(),
                ToastService.Type.ERROR,
                com.cmpe343.fx.util.ToastService.Position.TOP_RIGHT,
                javafx.util.Duration.seconds(3));
        }
    }

    // Removed handleRegister2() - features integrated into handleRegister()
    
    // This method was removed - features have been integrated into handleRegister()
    /*
    @FXML 
    private void handleRegister2() {
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
    */

    /**
     * Calculates password strength based on various criteria.
     * 
     * @param password the password to evaluate
     * @return "weak", "good", or "strong"
     */
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
