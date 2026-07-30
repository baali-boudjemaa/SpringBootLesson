package com.example.mef.demo.dashboard.table;


import com.example.mef.demo.Model.Field;
import com.example.mef.demo.Model.Module;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

/**
 * Builds the generic TableView columns for any CRUD module: the "#" id
 * column, a calculated-age column for date_of_birth, colored badge
 * columns for status/gender, and plain em-dash-on-empty columns for
 * everything else.
 *
 * Extracted from DashboardController.buildColumns(...) / badgeStyle(...).
 */
public final class TableBuilder {

    private TableBuilder() {
    }

    public static void buildColumns(TableView<Map<String, String>> table, Module module) {
        TableColumn<Map<String, String>, String> id = new TableColumn<>("#");
        id.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().get("id")));
        id.setPrefWidth(48);
        id.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(id);

        for (Field field : module.fields()) {
            if ("password_hash".equals(field.column())) continue;

            TableColumn<Map<String, String>, String> column =
                    new TableColumn<>(field.label().toUpperCase());
            column.setCellValueFactory(data ->
                    new ReadOnlyStringWrapper(data.getValue().get(field.column())));
            column.setPrefWidth(140);

            if ("date_of_birth".equals(field.column())) {
                column.setCellFactory(col -> ageCell());
            } else if ("status".equals(field.column()) || "gender".equals(field.column())) {
                column.setCellFactory(col -> badgeCell());
            } else {
                column.setCellFactory(col -> plainCell());
            }
            table.getColumns().add(column);
        }
    }

    private static TableCell<Map<String, String>, String> ageCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText("—");
                } else {
                    try {
                        LocalDate dob = LocalDate.parse(item.substring(0, 10));
                        Period period = Period.between(dob, LocalDate.now());
                        if (period.getYears() > 0) {
                            setText(period.getYears() + " ans");
                        } else if (period.getMonths() > 0) {
                            setText(period.getMonths() + " mois");
                        } else {
                            setText(period.getDays() + " j");
                        }
                    } catch (Exception e) {
                        setText(item);
                    }
                }
            }
        };
    }

    private static TableCell<Map<String, String>, String> badgeCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setStyle("-fx-padding: 2 10 2 10; -fx-background-radius: 12;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER-LEFT;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                } else {
                    badge.setText(item);
                    badge.setStyle(badge.getStyle() + badgeStyle(item));
                    setGraphic(badge);
                }
            }
        };
    }

    private static TableCell<Map<String, String>, String> plainCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank() ? "—" : item);
            }
        };
    }

    /**
     * Returns inline badge color style based on status/gender value.
     * Public because DashboardView reuses it for the "recent payments" list.
     */
    public static String badgeStyle(String value) {
        if (value == null) return "";
        return switch (value.toUpperCase()) {
            case "ACTIVE", "PRESENT", "PAID", "COMPLETED", "FEMALE", "FILLE" ->
                    "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;";
            case "INACTIVE", "ABSENT", "OVERDUE", "DROPPED" ->
                    "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";
            case "LATE", "PENDING" ->
                    "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;";
            case "MALE", "GARÇON" ->
                    "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF;";
            default ->
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;";
        };
    }
}