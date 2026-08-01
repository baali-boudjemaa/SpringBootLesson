package com.example.mef.demo.dashboard.reports;

import com.example.mef.demo.Model.Report;
import com.example.mef.demo.Services.ReportService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.ReportType;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Typed CRUD screen for the "reports" module (Report entity). Follows the
 * same table+form pattern as PaymentsView/CoursesView/etc: a TableView<Report>
 * bound directly to entities, with a detail form on the right that calls
 * ReportService. Replaces the generic ModuleTableView fallback that was
 * previously used for this module.
 */
@Component
public class ReportsView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReportService reportService;

    private final ObservableList<Report> rows = FXCollections.observableArrayList();
    private final TableView<Report> table = new TableView<>(rows);

    private final TextField titleField = FormFactory.textField("Titre");
    private final ComboBox<ReportType> typeField = new ComboBox<>(FXCollections.observableArrayList(ReportType.values()));
    private final TextArea summaryField = new TextArea();

    private Report selected;

    public ReportsView(ReportService reportService) {
        this.reportService = reportService;
        typeField.setMaxWidth(Double.MAX_VALUE);
        summaryField.setPrefRowCount(6);
        summaryField.setWrapText(true);
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Rapports");

        table.getColumns().clear();
        TableColumn<Report, String> title = new TableColumn<>("Titre");
        title.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTitle()));
        title.setPrefWidth(220);
        TableColumn<Report, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getReportType() == null ? "" : d.getValue().getReportType().name()));
        TableColumn<Report, String> created = new TableColumn<>("Créé le");
        created.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getCreatedAt() == null ? "" : d.getValue().getCreatedAt().format(DATE_FORMAT)));
        table.getColumns().addAll(List.of(title, type, created));

        VBox listPane = new VBox(10, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> selectRow(val));

        VBox form = buildForm();
        BorderPane layout = new BorderPane();
        layout.setCenter(listPane);
        layout.setRight(form);
        BorderPane.setMargin(form, new Insets(0, 0, 0, 16));
        form.setPrefWidth(340);

        contentPane.setCenter(layout);
        reload();
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Titre", titleField);
        FormFactory.addRow(grid, 1, "Type", typeField);
        FormFactory.addRow(grid, 2, "Résumé", summaryField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        return new VBox(12, new Label("Détails du rapport"), grid, new HBox(8, save, clear, delete));
    }

    private void selectRow(Report report) {
        selected = report;
        if (report == null) { clearForm(); return; }
        titleField.setText(report.getTitle());
        typeField.setValue(report.getReportType());
        summaryField.setText(report.getSummary() == null ? "" : report.getSummary());
    }

    private void clearForm() {
        selected = null;
        titleField.clear();
        typeField.setValue(null);
        summaryField.clear();
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (titleField.getText().isBlank()) {
            DialogUtil.error("Champ requis", "Le titre est obligatoire.");
            return;
        }

        Report report = selected != null ? selected : new Report();
        report.setTitle(titleField.getText().trim());
        report.setReportType(typeField.getValue() == null ? ReportType.GENERAL : typeField.getValue());
        report.setSummary(summaryField.getText());

        AsyncTasks.run(
                () -> reportService.save(report),
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer ce rapport ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> reportService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                reportService::findAll,
                list -> rows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}
