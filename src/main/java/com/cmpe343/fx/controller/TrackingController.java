package com.cmpe343.fx.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.InputStream;

/**
 * Controller for the live order tracking view.
 * Shows an animated map with courier and customer markers.
 * 
 * @author Group07
 * @version 1.0
 */
public class TrackingController {
    @FXML 
    private Pane drawingPane;
    @FXML 
    private ImageView mapImage;
    @FXML 
    private Label etaLabel;
    
    private Circle courierMarker;
    private Circle customerMarker;
    
    private double currentX = 80;
    private double currentY = 320;
    private final double destinationX = 450;
    private final double destinationY = 120;
    
    @FXML
    public void initialize() {
        try {
            InputStream is = getClass().getResourceAsStream("/images/map/map.png");
            if (is != null) {
                mapImage.setImage(new Image(is));
            } else {
                System.err.println("CRITICAL: Map image not found in resources! Creating placeholder.");
                // Create a simple placeholder - a colored rectangle
                mapImage.setFitWidth(600);
                mapImage.setFitHeight(400);
                mapImage.setStyle("-fx-background-color: #1e293b;");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Customer marker (green - destination)
        customerMarker = new Circle(12, Color.web("#10b981"));
        customerMarker.setCenterX(destinationX);
        customerMarker.setCenterY(destinationY);
        customerMarker.setStroke(Color.WHITE);
        customerMarker.setStrokeWidth(2);
        
        // Courier marker (red - current position)
        courierMarker = new Circle(10, Color.web("#ef4444"));
        courierMarker.setCenterX(currentX);
        courierMarker.setCenterY(currentY);
        courierMarker.setStroke(Color.WHITE);
        courierMarker.setStrokeWidth(2);
        
        drawingPane.getChildren().addAll(customerMarker, courierMarker);
        startTrackingAnimation();
    }
    
    private void startTrackingAnimation() {
        Timeline trackingTimeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            double lerpFactor = 0.003;
            currentX += (destinationX - currentX) * lerpFactor;
            currentY += (destinationY - currentY) * lerpFactor;
            
            courierMarker.setCenterX(currentX);
            courierMarker.setCenterY(currentY);
            
            double distance = Math.sqrt(Math.pow(destinationX - currentX, 2) + Math.pow(destinationY - currentY, 2));
            
            if (distance < 10) {
                etaLabel.setText("Courier has arrived at your location!");
                etaLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                int estimatedMins = (int) (distance / 15) + 1;
                etaLabel.setText("Estimated Arrival: ~" + estimatedMins + " minutes");
            }
        }));
        trackingTimeline.setCycleCount(Animation.INDEFINITE);
        trackingTimeline.play();
    }
}
