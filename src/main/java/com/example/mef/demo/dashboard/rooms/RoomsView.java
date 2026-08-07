package com.example.mef.demo.dashboard.rooms;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Room;
import com.example.mef.demo.Services.RoomService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.geometry.Insets;
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
import javafx.scene.layout.VBox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The rooms (salles) management screen: a card grid of available rooms
 * with a create/edit form. Rooms created here can then be linked to one
 * or more sections from the "Aqsam" (classes) page, where their weekly
 * occupancy schedule is checked for conflicts against every other section
 * sharing that room.
 */
@Component
public class RoomsView {

    @Autowired
    private RoomService roomService;

    public void render(BorderPane contentPane) {
        FlowPane cardGrid = new FlowPane(16, 16);
        cardGrid.setPadding(new Insets(4));

        TextField nameField = FormFactory.textField(I18n.t("room.name"));
        TextField locationField = FormFactory.textField(I18n.t("room.location"));
        TextField capacityField = FormFactory.textField(I18n.t("room.capacity"));
        CheckBox activeField = new CheckBox(I18n.t("room.active"));
        activeField.setSelected(true);
        TextArea notesField = new TextArea();
        notesField.setPromptText(I18n.t("room.notes"));
        notesField.setPrefRowCount(3);
        notesField.setWrapText(true);

        GridPane form = FormFactory.sectionGrid();
        FormFactory.addRow(form, 0, I18n.t("room.name"), nameField);
        FormFactory.addRow(form, 1, I18n.t("room.location"), locationField);
        FormFactory.addRow(form, 2, I18n.t("room.capacity"), capacityField);
        FormFactory.addRow(form, 3, "", activeField);
        FormFactory.addRow(form, 4, I18n.t("room.notes"));
        FormFactory.addRow(form, 5, notesField);

        Button save   = new Button(I18n.t("action.save"));   save.getStyleClass().add("primary-button");
        Button clear  = new Button(I18n.t("action.clear"));  clear.getStyleClass().add("secondary-button");
        Button delete = new Button(I18n.t("action.delete")); delete.getStyleClass().add("danger-button");
        HBox actions = new HBox(10, save, clear, delete);

        Room[] selected = new Room[]{null};
        Runnable[] reload = new Runnable[1];

        Runnable clearForm = () -> {
            selected[0] = null;
            nameField.setText("");
            locationField.setText("");
            capacityField.setText("");
            activeField.setSelected(true);
            notesField.setText("");
            cardGrid.getChildren().forEach(n -> n.getStyleClass().remove("class-card-selected"));
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
                        });
                        cardGrid.getChildren().add(card);
                    }
                },
                err -> DialogUtil.error(I18n.t("action.save"), err.getMessage())
        );

        clear.setOnAction(e -> clearForm.run());

        save.setOnAction(e -> {
            try {
                if (nameField.getText().isBlank()) throw new IllegalArgumentException(I18n.t("room.name_required"));
                Integer capacity = null;
                if (!capacityField.getText().isBlank()) {
                    try {
                        capacity = Integer.parseInt(capacityField.getText().trim());
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(I18n.t("room.capacity_invalid"));
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
                        err -> { save.setDisable(false); DialogUtil.error(I18n.t("action.save"), err.getMessage()); }
                );
            } catch (RuntimeException ex) {
                DialogUtil.error(I18n.t("action.save"), ex.getMessage());
            }
        });

        delete.setOnAction(e -> {
            if (selected[0] == null) {
                DialogUtil.info(I18n.t("action.delete"), I18n.t("room.select_before_delete"));
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
                                ? I18n.t("room.confirm_delete")
                                : I18n.t("room.confirm_delete_in_use") + " ("
                                + usedBy.stream().map(Classroom::getName).collect(Collectors.joining(", ")) + ")";
                        if (DialogUtil.confirm(I18n.t("action.delete"), name + " — " + confirmMessage)) {
                            delete.setDisable(true);
                            AsyncTasks.run(
                                    () -> roomService.delete(id),
                                    () -> { delete.setDisable(false); reload[0].run(); clearForm.run(); },
                                    err -> { delete.setDisable(false); DialogUtil.error(I18n.t("action.delete"), err.getMessage()); }
                            );
                        }
                    },
                    err -> { delete.setDisable(false); DialogUtil.error(I18n.t("action.delete"), err.getMessage()); }
            );
        });

        Button addNew = new Button("➕  " + I18n.t("room.new"));
        addNew.getStyleClass().add("primary-button");
        addNew.setOnAction(e -> {
            clearForm.run();
            nameField.requestFocus();
        });

        reload[0].run();

        VBox formPanel = new VBox(14, new Label(I18n.t("table.details")), form, actions);
        formPanel.getStyleClass().add("side-panel");

        ScrollPane cardScroll = new ScrollPane(cardGrid);
        cardScroll.setFitToWidth(true);
        cardScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox cardPanel = new VBox(10, addNew, cardScroll);
        VBox.setVgrow(cardScroll, Priority.ALWAYS);

        HBox workspace = new HBox(18, cardPanel, formPanel);
        HBox.setHgrow(cardPanel, Priority.ALWAYS);
        workspace.setPadding(new Insets(24));
        contentPane.setCenter(workspace);
    }

    private VBox buildRoomCard(Room r) {
        Label name = new Label(r.getName());
        name.getStyleClass().add("section-title");

        String locationText = r.getLocation() == null || r.getLocation().isBlank() ? "" : r.getLocation();
        Label meta = new Label(locationText);
        meta.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        String capacityText = r.getCapacity() == null
                ? I18n.t("room.capacity_unset")
                : r.getCapacity() + " " + I18n.t("room.capacity_unit");
        Label capacity = new Label(capacityText);
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #15803D; -fx-font-weight: bold;");

        Label status = new Label(r.isActive() ? I18n.t("room.status_active") : I18n.t("room.status_inactive"));
        status.setStyle(r.isActive()
                ? "-fx-font-size: 10px; -fx-text-fill: #15803D;"
                : "-fx-font-size: 10px; -fx-text-fill: #DC2626;");

        VBox card = new VBox(6, name, meta, capacity, status);
        card.getStyleClass().add("class-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(220);
        return card;
    }
}