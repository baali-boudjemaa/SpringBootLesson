package com.example.mef.demo.dashboard.reports;

import com.example.mef.demo.Model.Report;
import com.example.mef.demo.Services.ReportService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.ReportType;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
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
    { com.example.mef.demo.dashboard.common.TableStyleKit.applyTheme(table, "reports"); }

    private final TextField titleField = FormFactory.textField(I18n.t("field.title"));
    private final ComboBox<ReportType> typeField = new ComboBox<>(FXCollections.observableArrayList(ReportType.values()));
    private final TextArea summaryField = new TextArea();
    private final Label reportCount = new Label();

    private Report selected;

    public ReportsView(ReportService reportService) {
        this.reportService = reportService;
        typeField.setMaxWidth(Double.MAX_VALUE);
        summaryField.setPrefRowCount(6);
        summaryField.setWrapText(true);
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText(I18n.t("nav.reports"));

        table.setPlaceholder(new Label(I18n.t("report.empty")));

        table.getColumns().clear();
        TableColumn<Report, String> title = new TableColumn<>(I18n.t("field.title"));
        title.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTitle()));
        title.setPrefWidth(220);
        TableColumn<Report, String> type = new TableColumn<>(I18n.t("field.type"));
        type.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getReportType() == null ? "" : d.getValue().getReportType().name()));
        TableColumn<Report, String> created = new TableColumn<>(I18n.t("report.created"));
        created.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getCreatedAt() == null ? "" : d.getValue().getCreatedAt().format(DATE_FORMAT)));
        table.getColumns().addAll(List.of(title, type, created));

        Label listTitle = new Label(I18n.t("report.history"));
        listTitle.getStyleClass().add("workflow-title");
        Label listHint = new Label(I18n.t("report.hint"));
        listHint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        reportCount.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-weight: bold; "
                + "-fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 11px;");
        Button newReport = new Button("+ " + I18n.t("report.new"));
        newReport.getStyleClass().add("primary-button");
        newReport.setOnAction(e -> clearForm());
        HBox listHeader = new HBox(10, new VBox(3, listTitle, listHint), reportCount, newReport);
        listHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(listHeader.getChildren().get(0), Priority.ALWAYS);

        VBox listPane = new VBox(14, listHeader, table);
        listPane.setPadding(new Insets(20));
        listPane.getStyleClass().add("workflow-card");
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
        FormFactory.addRow(grid, 0, I18n.t("field.title"), titleField);
        FormFactory.addRow(grid, 1, I18n.t("field.type"), typeField);
        FormFactory.addRow(grid, 2, I18n.t("field.summary"), summaryField);

        Button save = new Button(I18n.t("action.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button(I18n.t("action.new"));
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button(I18n.t("action.delete"));
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        Label title = new Label(I18n.t("report.details"));
        title.getStyleClass().add("workflow-title");
        Label hint = new Label(I18n.t("report.form_hint"));
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        HBox actions = new HBox(8, save, clear, delete);
        VBox form = new VBox(14, title, hint, grid, actions);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("workflow-card");
        return form;
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
            DialogUtil.error(I18n.t("dialog.required_field"), I18n.t("report.title_required"));
            return;
        }

        Report report = selected != null ? selected : new Report();
        report.setTitle(titleField.getText().trim());
        report.setReportType(typeField.getValue() == null ? ReportType.GENERAL : typeField.getValue());
        report.setSummary(summaryField.getText());

        AsyncTasks.run(
                () -> reportService.save(report),
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("dialog.save_failed").replace("{0}", err.getMessage()))
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm(I18n.t("dialog.confirm"), I18n.t("report.delete_confirm"))) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> reportService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("dialog.delete_failed").replace("{0}", err.getMessage()))
        );
    }

    private void reload() {
        AsyncTasks.run(
                reportService::findAll,
                list -> {
                    rows.setAll(list);
                    reportCount.setText(list.size() + " " + I18n.t(list.size() > 1 ? "report.count_plural" : "report.count_singular"));
                },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("dialog.load_failed").replace("{0}", err.getMessage()))
        );
    }
}
