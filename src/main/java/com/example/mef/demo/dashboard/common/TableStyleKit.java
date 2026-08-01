package com.example.mef.demo.dashboard.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Shared building blocks for the "modern table" look: rounded avatar
 * chips with initials, small pill/badge labels, and the CSS wiring
 * (theme class + row height) every dashboard table should carry.
 *
 * Centralizing this keeps every module's table visually consistent
 * (same margins, fonts, row height) instead of each view re-inventing
 * its own inline styles.
 */
public final class TableStyleKit {

    private TableStyleKit() {
    }

    /** Standard row height used by every redesigned data table. */
    public static final double ROW_HEIGHT = 44;

    /** Taller row height for tables whose first column carries an avatar chip. */
    public static final double AVATAR_ROW_HEIGHT = 60;

    /**
     * Applies the shared "data-table" base look plus a per-module accent
     * theme (colored header gradient + selection tint), both already
     * defined in style.css.
     *
     * @param theme e.g. "students", "teachers", "guardians"...
     */
    public static void applyTheme(TableView<?> table, String theme) {
        table.getStyleClass().addAll("data-table", theme + "-table");
        table.setFixedCellSize(ROW_HEIGHT);
    }

    public static void applyTheme(TableView<?> table, String theme, double rowHeight) {
        table.getStyleClass().addAll("data-table", theme + "-table");
        table.setFixedCellSize(rowHeight);
    }

    /** A round initials avatar (e.g. "AA") in a given accent color. */
    public static StackPane avatar(String initials, String hexColor) {
        Circle circle = new Circle(18, Color.web(hexColor));
        Label label = new Label(initials);
        label.getStyleClass().add("avatar-initials");
        StackPane pane = new StackPane(circle, label);
        pane.setMinSize(36, 36);
        pane.setMaxSize(36, 36);
        return pane;
    }

    /** Avatar + a two-line name/subtitle block, used for the primary "person" column. */
    public static HBox avatarNameCell(String initials, String avatarColor, String title, String subtitle) {
        StackPane avatar = avatar(initials, avatarColor);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("cell-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("cell-subtitle");
        VBox text = new VBox(2, titleLabel, subtitleLabel);
        HBox box = new HBox(10, avatar, text);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 0, 4, 0));
        return box;
    }

    /** A colored rounded pill, e.g. for age, status, or blood type. */
    public static Label pill(String text, String bgHex, String fgHex) {
        Label label = new Label(text);
        label.getStyleClass().add("table-pill");
        label.setStyle("-fx-background-color: " + bgHex + "; -fx-text-fill: " + fgHex + ";");
        return label;
    }

    /** Deterministic accent color for an avatar, based on a stable key (e.g. gender or id). */
    public static String colorFor(String key) {
        if (key == null) return "#94A3B8";
        String upper = key.toUpperCase();
        if (upper.startsWith("F")) return "#EC4899"; // fille / féminin
        if (upper.startsWith("M") || upper.startsWith("G")) return "#4F46E5"; // garçon / masculin
        String[] palette = {"#4F46E5", "#0D9488", "#EA580C", "#0891B2", "#7C3AED", "#DB2777"};
        return palette[Math.abs(key.hashCode()) % palette.length];
    }

    public static String initialsOf(String first, String last) {
        String a = first == null || first.isBlank() ? "" : first.substring(0, 1);
        String b = last == null || last.isBlank() ? "" : last.substring(0, 1);
        String initials = (a + b).toUpperCase();
        return initials.isBlank() ? "?" : initials;
    }
}
