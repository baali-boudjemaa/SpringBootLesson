package com.example.mef.demo.dashboard.rooms;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Room;
import com.example.mef.demo.Services.RoomService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The rooms (salles) management screen: a card grid of available rooms
 * with a create/edit form. Selecting a room also shows which sections
 * currently occupy it, so you can see at a glance whether a room is free
 * before deleting it or assigning a new class to it.
 */
@Component
public class RoomsView {

    @Autowired
    private RoomService roomService;

    public void render(BorderPane contentPane) {
        FlowPane cardGrid = new FlowPane(16, 16);
        cardGrid.setPadding(new Insets(4));

        TextField nameField = FormFactory.textField(I18n.t("room.name", "تسجيل الحضور"));
        TextField locationField = FormFactory.textField(I18n.t("room.location", "تسجيل الحضور"));
        TextField capacityField = FormFactory.textField(I18n.t("room.capacity", "تسجيل الحضور"));
        CheckBox activeField = new CheckBox(I18n.t("room.active", "تسجيل الحضور"));
        activeField.setSelected(true);
        TextArea notesField = new TextArea();
        notesField.setPromptText(I18n.t("room.notes", "تسجيل الحضور"));
        notesField.setPrefRowCount(3);
        notesField.setWrapText(true);

        GridPane form = FormFactory.sectionGrid();
        FormFactory.addRow(form, 0, I18n.t("room.name", "تسجيل الحضور"), nameField);
        FormFactory.addRow(form, 1, I18n.t("room.location", "تسجيل الحضور"), locationField);
        FormFactory.addRow(form, 2, I18n.t("room.capacity", "تسجيل الحضور"), capacityField);
        FormFactory.addRow(form, 3, "", activeField);
        FormFactory.addRow(form, 4, I18n.t("room.notes", "تسجيل الحضور"));
        FormFactory.addRow(form, 5, notesField);

        Button save   = new Button(I18n.t("action.save", "تسجيل الحضور"));   save.getStyleClass().add("primary-button");
        Button clear  = new Button(I18n.t("action.clear", "تسجيل الحضور"));  clear.getStyleClass().add("secondary-button");
        Button delete = new Button(I18n.t("action.delete", "تسجيل الحضور")); delete.getStyleClass().add("danger-button");
        HBox actions = new HBox(10, save, clear, delete);

        // --- "Sections occupying this room" block -------------------------
        Label occupantsTitle = new Label(I18n.t("room.occupied_by", "تسجيل الحضور"));
        occupantsTitle.getStyleClass().add("section-title");
        Label occupantsHint = new Label(I18n.t("room.select_to_see_occupants", "تسجيل الحضور"));
        occupantsHint.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        FlowPane occupantsBox = new FlowPane(8, 8);
        occupantsBox.setPadding(new Insets(2, 0, 2, 0));
        VBox occupantsPanel = new VBox(8, occupantsTitle, occupantsHint, occupantsBox);

        Room[] selected = new Room[]{null};
        Runnable[] reload = new Runnable[1];

        // Loads the sections that use the given room into the chip list.
        java.util.function.Consumer<Room> loadOccupants = room -> {
            occupantsBox.getChildren().clear();
            if (room == null || room.getId() == null) {
                occupantsHint.setText(I18n.t("room.select_to_see_occupants", "تسجيل الحضور"));
                occupantsHint.setVisible(true);
                occupantsHint.setManaged(true);
                return;
            }
            occupantsHint.setText(I18n.t("action.loading", "تسجيل الحضور"));
            occupantsHint.setVisible(true);
            occupantsHint.setManaged(true);
            AsyncTasks.run(
                    () -> roomService.findSectionsUsingRoom(room.getId()),
                    (List<Classroom> usedBy) -> {
                        occupantsBox.getChildren().clear();
                        if (usedBy.isEmpty()) {
                            occupantsHint.setText(I18n.t("room.no_occupants", "تسجيل الحضور"));
                            occupantsHint.setVisible(true);
                            occupantsHint.setManaged(true);
                            return;
                        }
                        occupantsHint.setVisible(false);
                        occupantsHint.setManaged(false);
                        for (Classroom c : usedBy) {
                            String label = c.getName()
                                    + (c.getAgeGroup() == null || c.getAgeGroup().isBlank()
                                    ? "" : " · " + c.getAgeGroup());
                            Label chip = new Label(label);
                            chip.setStyle(
                                    "-fx-background-color: #EFF6FF;"
                                            + " -fx-border-color: #BFDBFE;"
                                            + " -fx-background-radius: 999; -fx-border-radius: 999;"
                                            + " -fx-padding: 5 12 5 12;"
                                            + " -fx-font-size: 12px; -fx-text-fill: #1D4ED8;");
                            occupantsBox.getChildren().add(chip);
                        }
                    },
                    err -> {
                        occupantsHint.setText(err.getMessage());
                        occupantsHint.setVisible(true);
                        occupantsHint.setManaged(true);
                    }
            );
        };

        Runnable clearForm = () -> {
            selected[0] = null;
            nameField.setText("");
            locationField.setText("");
            capacityField.setText("");
            activeField.setSelected(true);
            notesField.setText("");
            cardGrid.getChildren().forEach(n -> n.getStyleClass().remove("class-card-selected"));
            loadOccupants.accept(null);
        };

        reload[0] = () -> AsyncTasks.run(
                () -> roomService.findAll(),
                rooms -> {
                    cardGrid.getChildren().clear();
                    for (Room r : rooms) {
                        VBox card = buildRoomCard(r);
                        card.setOnMouseClicked(ev -> {
                            selected[0] = r;
                            cardGrid.getChildren().forEach(n -> n.getStyleClass().remove("class-card-selected"));
                            card.getStyleClass().add("class-card-selected");
                            nameField.setText(r.getName());
                            locationField.setText(r.getLocation() == null ? "" : r.getLocation());
                            capacityField.setText(r.getCapacity() == null ? "" : String.valueOf(r.getCapacity()));
                            activeField.setSelected(r.isActive());
                            notesField.setText(r.getNotes() == null ? "" : r.getNotes());
                            loadOccupants.accept(r);
                        });
                        cardGrid.getChildren().add(card);
                    }
                },
                err -> DialogUtil.error(I18n.t("action.save", "تسجيل الحضور"), err.getMessage())
        );

        clear.setOnAction(e -> clearForm.run());

        save.setOnAction(e -> {
            try {
                if (nameField.getText().isBlank()) throw new IllegalArgumentException(I18n.t("room.name_required", "تسجيل الحضور"));
                Integer capacity = null;
                if (!capacityField.getText().isBlank()) {
                    try {
                        capacity = Integer.parseInt(capacityField.getText().trim());
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(I18n.t("room.capacity_invalid", "تسجيل الحضور"));
                    }
                }

                Room r = selected[0] != null ? selected[0] : Room.builder().build();
                r.setName(nameField.getText().trim());
                r.setLocation(locationField.getText().trim());
                r.setCapacity(capacity);
                r.setActive(activeField.isSelected());
                r.setNotes(notesField.getText());

                save.setDisable(true);
                AsyncTasks.run(
                        () -> roomService.save(r),
                        () -> { save.setDisable(false); reload[0].run(); clearForm.run(); },
                        err -> { save.setDisable(false); DialogUtil.error(I18n.t("action.save", "تسجيل الحضور"), err.getMessage()); }
                );
            } catch (RuntimeException ex) {
                DialogUtil.error(I18n.t("action.save", "تسجيل الحضور"), ex.getMessage());
            }
        });

        delete.setOnAction(e -> {
            if (selected[0] == null) {
                DialogUtil.info(I18n.t("action.delete", "تسجيل الحضور"), I18n.t("room.select_before_delete", "تسجيل الحضور"));
                return;
            }
            String id = selected[0].getId();
            String name = selected[0].getName();
            delete.setDisable(true);
            AsyncTasks.run(
                    () -> roomService.findSectionsUsingRoom(id),
                    (List<Classroom> usedBy) -> {
                        delete.setDisable(false);
                        String confirmMessage = usedBy.isEmpty()
                                ? I18n.t("room.confirm_delete", "تسجيل الحضور")
                                : I18n.t("room.confirm_delete_in_use", "تسجيل الحضور") + " ("
                                + usedBy.stream().map(Classroom::getName).collect(Collectors.joining(", ")) + ")";
                        if (DialogUtil.confirm(I18n.t("action.delete", "تسجيل الحضور"), name + " — " + confirmMessage)) {
                            delete.setDisable(true);
                            AsyncTasks.run(
                                    () -> roomService.delete(id),
                                    () -> { delete.setDisable(false); reload[0].run(); clearForm.run(); },
                                    err -> { delete.setDisable(false); DialogUtil.error(I18n.t("action.delete", "تسجيل الحضور"), err.getMessage()); }
                            );
                        }
                    },
                    err -> { delete.setDisable(false); DialogUtil.error(I18n.t("action.delete", "تسجيل الحضور"), err.getMessage()); }
            );
        });

        Button addNew = new Button("➕  " + I18n.t("room.new", "تسجيل الحضور"));
        addNew.getStyleClass().add("primary-button");
        addNew.setOnAction(e -> {
            clearForm.run();
            nameField.requestFocus();
        });

        reload[0].run();
        loadOccupants.accept(null);

        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setStyle("-fx-background-color: #E2E8F0;");

        VBox formPanel = new VBox(14,
                new Label(I18n.t("table.details", "تسجيل الحضور")), form, actions,
                divider, occupantsPanel);
        formPanel.getStyleClass().add("side-panel");

        // Details panel scrolls vertically so long forms never get cut off.
        ScrollPane formScroll = new ScrollPane(formPanel);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setPrefWidth(380);
        formScroll.setMinWidth(340);
        formScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        ScrollPane cardScroll = new ScrollPane(cardGrid);
        cardScroll.setFitToWidth(true);
        cardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        formScroll.setPrefWidth(400);
        formScroll.setMinWidth(320);
        VBox cardPanel = new VBox(10, addNew, cardScroll);
        VBox.setVgrow(cardScroll, Priority.ALWAYS);

        HBox workspace = new HBox(18, cardPanel, formScroll);
        HBox.setHgrow(cardPanel, Priority.ALWAYS);
        workspace.setPadding(new Insets(24));
        contentPane.setCenter(workspace);
    }

    private VBox buildRoomCard(Room r) {
        Label name = new Label(r.getName());
        name.getStyleClass().add("section-title");

        Label status = new Label(r.isActive() ? I18n.t("room.status_active", "تسجيل الحضور") : I18n.t("room.status_inactive", "تسجيل الحضور"));
        status.setStyle(r.isActive()
                ? "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 10px;"
                + " -fx-background-radius: 999; -fx-padding: 2 8 2 8;"
                : "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 10px;"
                + " -fx-background-radius: 999; -fx-padding: 2 8 2 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, name, spacer, status);
        header.setAlignment(Pos.CENTER_LEFT);

        String locationText = r.getLocation() == null || r.getLocation().isBlank()
                ? "—" : "📍  " + r.getLocation();
        Label meta = new Label(locationText);
        meta.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        String capacityText = r.getCapacity() == null
                ? I18n.t("room.capacity_unset", "تسجيل الحضور")
                : "👥  " + r.getCapacity() + " " + I18n.t("room.capacity_unit", "تسجيل الحضور");
        Label capacity = new Label(capacityText);
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #15803D; -fx-font-weight: bold;");

        VBox card = new VBox(8, header, meta, capacity);
        card.getStyleClass().add("class-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(220);
        return card;
    }
}
