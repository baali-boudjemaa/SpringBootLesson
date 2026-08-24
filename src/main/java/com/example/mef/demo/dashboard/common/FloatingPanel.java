package com.example.mef.demo.dashboard.common;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A floating, draggable, resizable panel meant to sit inside a plain {@link Pane}
 * overlay on top of a screen's normal content (a plain Pane does no automatic
 * layout of its children, so manual layoutX/layoutY positioning sticks).
 *
 * Drag by the header, resize from the bottom-right grip.
 */
public class FloatingPanel extends VBox {

    private double dragOffsetX;
    private double dragOffsetY;

    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartWidth;
    private double resizeStartHeight;

    private boolean isResizing = false;

    public FloatingPanel(String title, Node content, Runnable onClose) {
        this(title, content, onClose, 380);
    }

    /** @param prefWidth initial panel width; still draggable/resizable afterward within min/max bounds. */
    public FloatingPanel(String title, Node content, Runnable onClose, double prefWidth) {
        getStyleClass().add("floating-panel");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("floating-panel-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("floating-panel-close");
        closeBtn.setOnAction(e -> { if (onClose != null) onClose.run(); });

        HBox header = new HBox(8, titleLabel, closeBtn);
        header.getStyleClass().add("floating-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 10, 10, 16));
        header.setCursor(Cursor.MOVE);

        VBox body = new VBox(content);
        body.getStyleClass().add("floating-panel-body");
        VBox.setVgrow(content, Priority.ALWAYS);

        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(body);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("details-scroll");
        scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // CRITICAL: Allow events to pass through to the content
        scroller.setPickOnBounds(false);
        VBox.setVgrow(scroller, Priority.ALWAYS);

        Region resizeGrip = new Region();
        resizeGrip.getStyleClass().add("floating-panel-resize-grip");
        // Slightly bigger hit area than before, and inset a couple px from the true
        // corner so a rounded panel border (-fx-background-radius) can't clip it out
        // of hit-testing right where it matters most.
        resizeGrip.setPrefSize(20, 20);
        resizeGrip.setMaxSize(20, 20);
        resizeGrip.setMinSize(20, 20);
        resizeGrip.setCursor(Cursor.SE_RESIZE);
        StackPane.setMargin(resizeGrip, new Insets(0, 3, 3, 0));

        StackPane bodyWithGrip = new StackPane(scroller, resizeGrip);
        StackPane.setAlignment(resizeGrip, Pos.BOTTOM_RIGHT);
        VBox.setVgrow(bodyWithGrip, Priority.ALWAYS);

        getChildren().addAll(header, bodyWithGrip);

        setPrefWidth(prefWidth);
        setPrefHeight(600);
        setMinWidth(300);
        setMinHeight(320);
        setMaxWidth(900);
        setMaxHeight(900);

        header.setOnMousePressed(this::onDragStart);
        header.setOnMouseDragged(this::onDragging);
        header.setOnMouseReleased(e -> e.consume());

        resizeGrip.setOnMousePressed(this::onResizeStart);
        resizeGrip.setOnMouseDragged(this::onResizing);
        resizeGrip.setOnMouseReleased(e -> {
            isResizing = false;
            e.consume();
        });

        // CRITICAL FIX: this panel lives inside a plain, non-managing Pane overlay
        // (see class doc), so nothing forces a fresh layout pass once it's actually
        // attached to a live Scene. A caller doing applyCss()/layout() right after
        // overlay.getChildren().add(this) can still run before the Scene itself has
        // resolved this panel's real geometry, which can leave the inner ScrollPane
        // (fitToWidth=true) — and therefore the form GridPane's field column — stuck
        // at 0 width, so every editor renders invisible. Self-heal here instead of
        // relying on every call site to get the timing right: once we're attached to
        // a Scene, defer one more CSS+layout pass to the next pulse, by which point
        // the Scene has had a chance to size us for real.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    applyCss();
                    layout();
                });
            }
        });
    }

    /** Sets the initial position (top-left corner) within the overlay Pane. */
    public void positionAt(double x, double y) {
        setLayoutX(x);
        setLayoutY(y);
    }

    private void onDragStart(MouseEvent e) {
        dragOffsetX = e.getSceneX() - getLayoutX();
        dragOffsetY = e.getSceneY() - getLayoutY();
        e.consume();
    }

    private void onDragging(MouseEvent e) {
        double newX = e.getSceneX() - dragOffsetX;
        double newY = e.getSceneY() - dragOffsetY;

        if (getParent() instanceof Pane parentPane) {
            double maxX = parentPane.getWidth() - 60;
            double maxY = parentPane.getHeight() - 40;
            newX = Math.max(-getWidth() + 80, Math.min(newX, maxX));
            newY = Math.max(0, Math.min(newY, maxY));
        }

        setLayoutX(newX);
        setLayoutY(newY);
        e.consume();
    }

    private void onResizeStart(MouseEvent e) {
        isResizing = true;
        resizeStartX = e.getSceneX();
        resizeStartY = e.getSceneY();
        resizeStartWidth = getWidth();
        resizeStartHeight = getHeight();
        e.consume();
    }

    private void onResizing(MouseEvent e) {
        if (!isResizing) return;

        double dx = e.getSceneX() - resizeStartX;
        double dy = e.getSceneY() - resizeStartY;
        double newWidth = Math.max(getMinWidth(), Math.min(getMaxWidth(), resizeStartWidth + dx));
        double newHeight = Math.max(getMinHeight(), Math.min(getMaxHeight(), resizeStartHeight + dy));
        setPrefWidth(newWidth);
        setPrefHeight(newHeight);
        e.consume();
    }
}