package com.cmpe343.fx.controller;

import com.cmpe343.fx.Session;

import com.cmpe343.fx.util.ToastService;
import com.cmpe343.model.User;
import com.cmpe343.service.AuthService;
import com.cmpe343.dao.UserDao;
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

    // Register fields (for register.fxml)
    @FXML
    private TextField registerUsernameField;
    @FXML
    private PasswordField registerPasswordField;
    @FXML
    private PasswordField registerConfirmPasswordField;
    @FXML
    private Button registerButton;

    private final AuthService authService = new AuthService();
    private final UserDao userDao = new UserDao();

    @FXML
    public void initialize() {
        // Login mode initialization
        if (usernameField != null) {
            usernameField.setOnAction(e -> handleLogin());
            Platform.runLater(() -> usernameField.requestFocus());
        }
        if (passwordField != null) {
            passwordField.setOnAction(e -> handleLogin());
        }

        // Register mode initialization
        if (registerUsernameField != null) {
            Platform.runLater(() -> registerUsernameField.requestFocus());
            // Optional: Add Enter key support for registration
            // registerConfirmPasswordField.setOnAction(e -> handleRegisterSubmit());
        }
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
            Scene newScene = new Scene(loader.load(), 1200, 800);

            // Eski scene CSS'lerini yeni scene'e taşı (login tasarımın bozulmasın diye)
            Scene oldScene = stage.getScene();
            if (oldScene != null) {
                newScene.getStylesheets().addAll(oldScene.getStylesheets());
            }

            stage.setScene(newScene);
            stage.setTitle("Gr7Project3 - " + user.getRole() + " (" + user.getUsername() + ")");
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.centerOnScreen();

        } catch (Exception ex) {
            ex.printStackTrace();
            toastError("Unexpected error: " + safeMsg(ex));
        } finally {
            setBusy(false);
        }
    }

    @FXML
    private void handleRegister() {
        try {
            Stage stage = getStage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Scene scene = new Scene(loader.load(), 640, 480);
            if (stage.getScene() != null) {
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.setTitle("Gr7Project3 - Create Account");
        } catch (Exception e) {
            e.printStackTrace();
            toastError("Failed to load registration screen");
        }
    }

    @FXML
    private void handleRegisterSubmit() {
        if (registerUsernameField == null || registerPasswordField == null || registerConfirmPasswordField == null) {
            toastError("Form fields not found");
            return;
        }

        String username = registerUsernameField.getText() == null ? "" : registerUsernameField.getText().trim();
        String password = registerPasswordField.getText() == null ? "" : registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText() == null ? ""
                : registerConfirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            toastError("Please fill in all fields");
            return;
        }

        if (username.length() < 3) {
            toastError("Username must be at least 3 characters");
            return;
        }

        if (password.length() < 4) {
            toastError("Password must be at least 4 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            toastError("Passwords do not match");
            return;
        }

        try {
            // Register with default address and phone (can be enhanced later)
            boolean success = userDao.registerCustomer(username, password, "", "");

            if (success) {
                toastSuccess("Registration successful! You can now sign in.");
                handleBackToLogin();
            } else {
                toastError("This username is already taken");
            }
        } catch (Exception e) {
            e.printStackTrace();
            toastError("Registration error: " + safeMsg(e));
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Stage stage = getStage();
            if (stage == null && registerUsernameField != null && registerUsernameField.getScene() != null) {
                stage = (Stage) registerUsernameField.getScene().getWindow();
            } else if (stage == null && usernameField != null && usernameField.getScene() != null) {
                stage = (Stage) usernameField.getScene().getWindow();
            }

            if (stage == null) {
                toastError("Stage not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 640, 480);
            if (stage.getScene() != null) {
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.setTitle("Gr7Project3 - Login");
        } catch (Exception e) {
            e.printStackTrace();
            toastError("Failed to return to login screen");
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
        if (usernameField != null && usernameField.getScene() != null) {
            return (Stage) usernameField.getScene().getWindow();
        }
        if (registerUsernameField != null && registerUsernameField.getScene() != null) {
            return (Stage) registerUsernameField.getScene().getWindow();
        }
        return null;
    }

    private void setBusy(boolean busy) {
        if (loginButton != null)
            loginButton.setDisable(busy);
        if (usernameField != null)
            usernameField.setDisable(busy);
        if (passwordField != null)
            passwordField.setDisable(busy);
        if (registerButton != null)
            registerButton.setDisable(busy);
        if (registerUsernameField != null)
            registerUsernameField.setDisable(busy);
        if (registerPasswordField != null)
            registerPasswordField.setDisable(busy);
        if (registerConfirmPasswordField != null)
            registerConfirmPasswordField.setDisable(busy);
    }

    // Toast wrappers (Toastify-style, ekrana etki etmez)
    private void toastInfo(String msg) {
        javafx.scene.Scene scene = usernameField != null ? usernameField.getScene()
                : (registerUsernameField != null ? registerUsernameField.getScene() : null);
        if (scene != null) {
            ToastService.show(scene, msg, ToastService.Type.INFO,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2.2));
        }
    }

    private void toastError(String msg) {
        javafx.scene.Scene scene = usernameField != null ? usernameField.getScene()
                : (registerUsernameField != null ? registerUsernameField.getScene() : null);
        if (scene != null) {
            ToastService.show(scene, msg, ToastService.Type.ERROR,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(2.8));
        }
    }

    private void toastSuccess(String msg) {
        javafx.scene.Scene scene = usernameField != null ? usernameField.getScene()
                : (registerUsernameField != null ? registerUsernameField.getScene() : null);
        if (scene != null) {
            ToastService.show(scene, msg, ToastService.Type.SUCCESS,
                    ToastService.Position.BOTTOM_CENTER, Duration.seconds(1.7));
        }
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null || m.isBlank())
            return t.getClass().getSimpleName();
        return m.length() > 160 ? m.substring(0, 160) + "..." : m;
    }
}
