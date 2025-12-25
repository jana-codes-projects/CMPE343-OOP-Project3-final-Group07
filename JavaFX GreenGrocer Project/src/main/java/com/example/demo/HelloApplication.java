package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main application class for Group07 GreenGrocer.
 * Initializes and starts the JavaFX application with the login screen.
 * 
 * @author Group07
 * @version 1.0
 */
public class HelloApplication extends Application {
    /**
     * Starts the JavaFX application.
     * 
     * @param stage the primary stage
     * @throws IOException if FXML loading fails
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/com/example/demo/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 400);
        stage.setTitle("Login - Group07 GreenGrocer");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Main method to launch the application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch();
    }
}
