package com.cmpe343.fx.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ToastService {

    public enum Type {
        SUCCESS, ERROR, INFO, WARNING
    }

    public enum Position {
        BOTTOM_CENTER, BOTTOM_RIGHT, TOP_RIGHT
    }

    private static final String OVERLAY_ID = "toastOverlay";
    private static final String CONTAINER_ID = "toastContainer";

    public static void show(Scene scene, String message, Type type) {
        show(scene, message, type, Position.BOTTOM_RIGHT, Duration.seconds(2.4));
    }

    public static void show(Scene scene, String message, Type type, Position pos, Duration duration) {
        if (scene == null)
            return;

        Platform.runLater(() -> {
            StackPane root = ensureStackRoot(scene);

            StackPane overlay = (StackPane) root.lookup("#" + OVERLAY_ID);
            if (overlay == null) {
                overlay = new StackPane();
                overlay.setId(OVERLAY_ID);
                overlay.setMouseTransparent(true); // ✅ ekranı etkilemesin
                overlay.setPickOnBounds(false);

                VBox container = new VBox(10);
                container.setId(CONTAINER_ID);
                container.setMouseTransparent(true);

                overlay.getChildren().add(container);
                root.getChildren().add(overlay);
            }

            // Container'ı buradan itibaren TEK seferde alıyoruz -> effectively final
            final VBox container = (VBox) overlay.lookup("#" + CONTAINER_ID);
            applyPosition(container, pos);

            Label toast = new Label(message);
            toast.setWrapText(true);
            toast.setMaxWidth(420);
            toast.getStyleClass().addAll("toast", css(type));
            toast.setOpacity(0);

            container.getChildren().add(toast);

            FadeTransition in = new FadeTransition(Duration.millis(160), toast);
            in.setFromValue(0);
            in.setToValue(1);

            PauseTransition stay = new PauseTransition(duration);

            FadeTransition out = new FadeTransition(Duration.millis(220), toast);
            out.setFromValue(1);
            out.setToValue(0);

            SequentialTransition seq = new SequentialTransition(in, stay, out);
            seq.setOnFinished(e -> container.getChildren().remove(toast));
            seq.play();
        });
    }

    private static void applyPosition(VBox container, Position pos) {
        switch (pos) {
            case BOTTOM_CENTER -> {
                StackPane.setAlignment(container, Pos.BOTTOM_CENTER);
                container.setPadding(new Insets(0, 0, 18, 0));
            }
            case BOTTOM_RIGHT -> {
                StackPane.setAlignment(container, Pos.BOTTOM_RIGHT);
                container.setPadding(new Insets(0, 18, 18, 0));
            }
            case TOP_RIGHT -> {
                StackPane.setAlignment(container, Pos.TOP_RIGHT);
                container.setPadding(new Insets(18, 18, 0, 0));
            }
        }
    }

    private static String css(Type type) {
        return switch (type) {
            case SUCCESS -> "toast-success";
            case ERROR -> "toast-error";
            case INFO -> "toast-info";
            case WARNING -> "toast-warning";
        };
    }

    private static StackPane ensureStackRoot(Scene scene) {
        if (scene.getRoot() instanceof StackPane sp)
            return sp;

        Node old = scene.getRoot();
        StackPane sp = new StackPane(old);
        scene.setRoot(sp);
        return sp;
    }
}
