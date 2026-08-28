package com.example.mef.demo.dashboard.outcoming;

import com.example.mef.demo.Model.Outcoming;
import com.example.mef.demo.Services.OutcomingService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FloatingPanel;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.enums.OutcomingCategory;
import com.example.mef.demo.enums.OutcomingFrequency;
import com.example.mef.demo.enums.PaymentStatus;
import com.example.mef.demo.enums.PaymentType;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Typed CRUD screen for the "outcoming" module (Outcoming entity) — expenses / money going out,
 * with recurring-expense support and a floating (draggable + resizable) details panel.
 */
@Component
public class OutcomingsView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OutcomingService outcomingService;

    private final ObservableList<Outcoming> rows = FXCollections.observableArrayList();
    private final TableView<Outcoming> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "outcoming");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final ObservableList<Outcoming> recurringRows = FXCollections.observableArrayList();
    private final TableView<Outcoming> recurringTable = new TableView<>(recurringRows);
    {
        TableStyleKit.applyTheme(recurringTable, "outcoming-recurring");
        recurringTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recurringTable.setPrefHeight(180);
    }

    private final TextField searchField = FormFactory.textField(I18n.t("outcoming.search"));
    private final ComboBox<String> statusFilter = new ComboBox<>(
            FXCollections.observableArrayList(I18n.t("outcoming.all"), I18n.t("status.paid"), I18n.t("status.pending"), I18n.t("status.overdue")));
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final DatePicker dateFrom = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 1, 1));
    private final DatePicker dateTo = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 12, 31));

    private final TextField labelField = FormFactory.textField(I18n.t("outcoming.label_hint"));
    private final TextField amountField = FormFactory.textField(I18n.t("field.amount"));
    private final TextField beneficiaryField = FormFactory.textField(I18n.t("outcoming.beneficiary_hint"));
    private final ComboBox<OutcomingCategory> categoryField = new ComboBox<>(FXCollections.observableArrayList(OutcomingCategory.values()));
    private final ComboBox<PaymentType> methodField = new ComboBox<>(FXCollections.observableArrayList(PaymentType.values()));
    private final ComboBox<PaymentStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
    private final DatePicker outcomingDateField = new DatePicker();

    private final CheckBox recurringCheck = new CheckBox(I18n.t("outcoming.recurring_expense"));
    private final ComboBox<OutcomingFrequency> frequencyField = new ComboBox<>(FXCollections.observableArrayList(OutcomingFrequency.values()));
    private final Label startDateLabel = new Label(I18n.t("outcoming.start_date"));

    private final Label footerCountLabel = new Label();
    private final Label footerTotalLabel = new Label();
    private final HBox summaryCards = new HBox(14);

    private List<Outcoming> allOutcomings = List.of();
    private Outcoming selected;
    private VBox form;

    /** Overlay Pane that floating panels live in, stacked on top of the normal screen content. */
    private Pane overlay;
    private FloatingPanel floatingForm;

    public OutcomingsView(OutcomingService outcomingService) {
        this.outcomingService = outcomingService;
        statusFilter.setValue(I18n.t("outcoming.all"));
        categoryFilter.setItems(FXCollections.observableArrayList(
                I18n.t("outcoming.all"), I18n.t("outcoming.category.salaries"), I18n.t("outcoming.category.rent"), I18n.t("outcoming.category.supplies"), I18n.t("outcoming.category.electricity"), I18n.t("outcoming.category.water"), I18n.t("outcoming.category.maintenance"), I18n.t("outcoming.category.food"), I18n.t("outcoming.category.transport"), I18n.t("outcoming.category.other")));
        categoryFilter.setValue(I18n.t("outcoming.all"));
        categoryField.setMaxWidth(Double.MAX_VALUE);
        methodField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);
        outcomingDateField.setMaxWidth(Double.MAX_VALUE);
        frequencyField.setMaxWidth(Double.MAX_VALUE);
        categoryField.setCellFactory(cb -> categoryCell());
        categoryField.setButtonCell(categoryCell());
        frequencyField.setCellFactory(cb -> frequencyCell());
        frequencyField.setButtonCell(frequencyCell());

        recurringCheck.selectedProperty().addListener((obs, was, isNow) -> {
            frequencyField.setDisable(!isNow);
            frequencyField.setVisible(isNow);
            frequencyField.setManaged(isNow);
            startDateLabel.setVisible(isNow);
            startDateLabel.setManaged(isNow);
            if (!isNow) frequencyField.setValue(null);
        });
        frequencyField.setVisible(false);
        frequencyField.setManaged(false);
        startDateLabel.setVisible(false);
        startDateLabel.setManaged(false);
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText(I18n.t("outcoming.title"));

        buildColumns();
        buildRecurringColumns();
        wireRowDoubleClick();
        Label subtitle = new Label(I18n.t("outcoming.subtitle"));
        subtitle.getStyleClass().add("page-subtitle");

        searchField.getStyleClass().add("filter-field");
        statusFilter.getStyleClass().add("filter-field");
        statusFilter.setPrefWidth(140);
        categoryFilter.getStyleClass().add("filter-field");
        categoryFilter.setPrefWidth(150);

        Button addBtn = new Button("+  " + I18n.t("outcoming.new"));
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> startCreate());

        HBox filters = new HBox(10,
                labeledFilter(I18n.t("filter.from"), dateFrom),
                labeledFilter(I18n.t("filter.to"), dateTo),
                categoryFilter,
                statusFilter,
                searchField
        );
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox toolbar = new HBox(12, filters, addBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("module-toolbar");

        footerCountLabel.getStyleClass().add("footer-stat");
        footerTotalLabel.getStyleClass().add("footer-stat-bold");
        HBox footer = new HBox(20, footerCountLabel, new Region(), footerTotalLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.getStyleClass().add("table-footer");

        for (Node n : summaryCards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        VBox tableBlock = new VBox(0, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);

        Label recurringTitle = new Label(I18n.t("outcoming.recurring"));
        recurringTitle.getStyleClass().add("section-title");
        VBox recurringBlock = new VBox(8, recurringTitle, recurringTable);
        recurringBlock.setMinHeight(250);
        VBox center = new VBox(16, subtitle, toolbar, tableBlock, summaryCards, recurringBlock);
        center.setPadding(new Insets(24));
        VBox.setVgrow(tableBlock, Priority.ALWAYS);


        if (form == null) {
            form = buildForm();
        }
        // Overlay hosts the floating panel; pickOnBounds(false) lets clicks pass through
        // to the table/buttons underneath wherever the overlay itself has no floating panel.
        overlay = new Pane();
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(center, overlay);
        ScrollPane scrollPane=new ScrollPane(root);
        scrollPane.getStyleClass().add("details-scroll");
        contentPane.setCenter(scrollPane);
        wireFilters();
        reload();
    }

    private HBox labeledFilter(String label, DatePicker picker) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("filter-label");
        picker.getStyleClass().add("filter-field");
        picker.setPrefWidth(120);
        return new HBox(6, lbl, picker);
    }

    private void wireFilters() {
        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        categoryFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        dateFrom.valueProperty().addListener((o, a, b) -> applyFilters());
        dateTo.valueProperty().addListener((o, a, b) -> applyFilters());
        dateFrom.setStyle("-fx-show-week-numbers: false;");
        dateTo.setStyle("-fx-show-week-numbers: false;");
        outcomingDateField.setStyle("-fx-show-week-numbers: false;");

    }

    private void buildColumns() {
        table.getColumns().clear();

        TableColumn<Outcoming, String> date = new TableColumn<>(I18n.t("field.date").toUpperCase());
        date.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateOutcome() == null ? "—" : d.getValue().getDateOutcome().format(DATE_FORMAT)));

        TableColumn<Outcoming, String> label = new TableColumn<>(I18n.t("outcoming.label").toUpperCase());
        label.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getLabel()));
        label.setPrefWidth(170);
        label.setCellFactory(col -> recurringAwareLabelCell());

        TableColumn<Outcoming, String> beneficiary = new TableColumn<>(I18n.t("outcoming.beneficiary").toUpperCase());
        beneficiary.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getBeneficiary() == null || d.getValue().getBeneficiary().isBlank()
                        ? "—" : d.getValue().getBeneficiary()));
        beneficiary.setPrefWidth(150);

        TableColumn<Outcoming, String> category = new TableColumn<>(I18n.t("field.category").toUpperCase());
        category.setCellValueFactory(d -> new ReadOnlyStringWrapper(categoryLabel(d.getValue().getCategory())));
        category.setCellFactory(col -> categoryPillCell());

        TableColumn<Outcoming, String> amount = new TableColumn<>(I18n.t("field.amount").toUpperCase());
        amount.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatAmount(d.getValue().getAmount())));

        TableColumn<Outcoming, String> method = new TableColumn<>(I18n.t("field.method").toUpperCase());
        method.setCellValueFactory(d -> new ReadOnlyStringWrapper(methodLabel(d.getValue().getPaymentMethod())));

        TableColumn<Outcoming, PaymentStatus> status = new TableColumn<>(I18n.t("field.status").toUpperCase());
        status.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getStatus()));
        status.setCellFactory(col -> statusCell());

        TableColumn<Outcoming, Outcoming> actions = new TableColumn<>(I18n.t("students.table.actions").toUpperCase());
        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> actionCell());
        actions.setPrefWidth(110);
        actions.setMaxWidth(120);

        table.getColumns().addAll(List.of(date, label, beneficiary, category, amount, method, status, actions));
    }

    /** Marks rows generated from a recurring template with a small badge next to the label. */
    private TableCell<Outcoming, String> recurringAwareLabelCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                Outcoming row = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || item == null || row == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                if (row.getParentRecurringId() != null) {
                    Label badge = new Label("↻");
                    badge.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-padding: 0 4 0 0;");
                    Label text = new Label(item);
                    HBox box = new HBox(4, badge, text);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(item);
                }
            }
        };
    }

    private void buildRecurringColumns() {
        recurringTable.getColumns().clear();

        TableColumn<Outcoming, String> label = new TableColumn<>(I18n.t("outcoming.label").toUpperCase());
        label.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getLabel()));
        label.setPrefWidth(180);

        TableColumn<Outcoming, String> amount = new TableColumn<>(I18n.t("field.amount").toUpperCase());
        amount.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatAmount(d.getValue().getAmount())));

        TableColumn<Outcoming, String> frequency = new TableColumn<>(I18n.t("outcoming.frequency").toUpperCase());
        frequency.setCellValueFactory(d -> new ReadOnlyStringWrapper(frequencyLabel(d.getValue().getFrequency())));
        frequency.setCellFactory(col -> categoryPillCell());

        TableColumn<Outcoming, String> next = new TableColumn<>(I18n.t("outcoming.next_due").toUpperCase());
        next.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getNextOccurrenceDate() == null ? "—" : d.getValue().getNextOccurrenceDate().format(DATE_FORMAT)));

        TableColumn<Outcoming, Outcoming> actions = new TableColumn<>(I18n.t("students.table.actions").toUpperCase());
        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> recurringActionCell());
        actions.setPrefWidth(90);
        actions.setMaxWidth(100);

        recurringTable.getColumns().addAll(List.of(label, amount, frequency, next, actions));
    }

    private TableCell<Outcoming, Outcoming> recurringActionCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Outcoming item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Button del = iconBtn("fth-trash-2", I18n.t("outcoming.stop_recurring"));
                del.getStyleClass().add("icon-action-danger");
                del.setOnAction(e -> {
                    if (!DialogUtil.confirm(I18n.t("dialog.confirm"), I18n.t("outcoming.stop_recurring_confirm"))) {
                        return;
                    }
                    String id = item.getId();
                    AsyncTasks.run(
                            () -> outcomingService.delete(id),
                            () -> reload(),
                            err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("dialog.delete_failed").replace("{0}", err.getMessage()))
                    );
                });
                HBox box = new HBox(4, del);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        };
    }

    private TableCell<Outcoming, String> categoryPillCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                } else {
                    setText(null);
                    setGraphic(TableStyleKit.pill(item, "#EEF2FF", "#4338CA"));
                }
            }
        };
    }

    private TableCell<Outcoming, PaymentStatus> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(PaymentStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(statusLabel(item));
                badge.getStyleClass().add("status-badge");
                badge.setStyle(statusBadgeStyle(item));
                setGraphic(badge);
            }
        };
    }

    private TableCell<Outcoming, Outcoming> actionCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Outcoming item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Button view = iconBtn("fth-eye", I18n.t("action.view"));
                Button edit = iconBtn("fth-edit-2", I18n.t("action.edit"));
                Button del  = iconBtn("fth-trash-2", I18n.t("action.delete"));
                del.getStyleClass().add("icon-action-danger");

                view.setOnAction(e -> { table.getSelectionModel().select(item); selectRow(item); });
                edit.setOnAction(e -> { table.getSelectionModel().select(item); selectRow(item); });
                del.setOnAction(e -> { selected = item; delete(); });

                HBox box = new HBox(4, view, edit, del);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        };
    }

    private Button iconBtn(String icon, String tooltip) {
        Button btn = new Button();
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        btn.setGraphic(fi);
        btn.getStyleClass().add("icon-action-btn");
        btn.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return btn;
    }

    private javafx.scene.control.ListCell<OutcomingCategory> categoryCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(OutcomingCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : categoryLabel(item));
            }
        };
    }

    private javafx.scene.control.ListCell<OutcomingFrequency> frequencyCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(OutcomingFrequency item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : frequencyLabel(item));
            }
        };
    }

    private static String categoryLabel(OutcomingCategory category) {
        if (category == null) return "—";
        return switch (category) {
            case SALAIRES -> I18n.t("outcoming.category.salaries");
            case LOYER -> I18n.t("outcoming.category.rent");
            case FOURNITURES -> I18n.t("outcoming.category.supplies");
            case ELECTRICITE -> I18n.t("outcoming.category.electricity");
            case EAU -> I18n.t("outcoming.category.water");
            case MAINTENANCE -> I18n.t("outcoming.category.maintenance");
            case NOURRITURE -> I18n.t("outcoming.category.food");
            case TRANSPORT -> I18n.t("outcoming.category.transport");
            case AUTRE -> I18n.t("outcoming.category.other");
        };
    }

    private static String frequencyLabel(OutcomingFrequency frequency) {
        if (frequency == null) return "—";
        return switch (frequency) {
            case DAILY -> I18n.t("outcoming.frequency.daily");
            case WEEKLY -> I18n.t("outcoming.frequency.weekly");
            case MONTHLY -> I18n.t("outcoming.frequency.monthly");
            case QUARTERLY -> I18n.t("outcoming.frequency.quarterly");
        };
    }

    private static String methodLabel(PaymentType type) {
        if (type == null) return "—";
        return switch (type) {
            case CASH -> I18n.t("payment_method.cash");
            case CARD -> I18n.t("payment_method.card");
            case TRANSFER -> I18n.t("payment_method.transfer");
        };
    }

    private static String statusLabel(PaymentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PAID -> I18n.t("status.paid");
            case PENDING -> I18n.t("status.pending");
            case OVERDUE -> I18n.t("status.overdue");
        };
    }

    private static String statusBadgeStyle(PaymentStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PAID -> "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";
            case PENDING -> "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;";
            case OVERDUE -> "-fx-background-color: #FDE68A; -fx-text-fill: #78350F;";
        };
    }

    private static String formatAmount(Double amount) {
        if (amount == null) return "—";
        return String.format(Locale.FRENCH, "%,.2f DA", amount);
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, I18n.t("outcoming.label") + " *", labelField);
        FormFactory.addRow(grid, 1, I18n.t("field.amount") + " *", amountField);
        FormFactory.addRow(grid, 2, I18n.t("field.category"), categoryField);
        FormFactory.addRow(grid, 3, I18n.t("outcoming.beneficiary"), beneficiaryField);
        FormFactory.addRow(grid, 4, I18n.t("field.method"), methodField);
        FormFactory.addRow(grid, 5, I18n.t("field.status"), statusField);
        FormFactory.addRow(grid, 6, startDateLabel.getText(), outcomingDateField);
        grid.add(recurringCheck, 0, 7, 2, 1);
        FormFactory.addRow(grid, 8, I18n.t("outcoming.frequency"), frequencyField);

        Button save = new Button(I18n.t("action.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("+ " + I18n.t("action.new"));
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> startCreate());
        Button delete = new Button(I18n.t("action.delete"));
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        // No title label here — the FloatingPanel header already shows "Détails de la sortie".
        // NOTE: intentionally no setPrefWidth() here anymore — the panel sits inside a
        // ScrollPane with fitToWidth(true) (see FloatingPanel), and forcing a fixed
        // prefWidth on this VBox fought that constraint on the very first layout pass,
        // which could resolve the GridPane's input column to 0 width and make every
        // TextField/ComboBox/DatePicker inside it render invisible.
        VBox panel = new VBox(12, grid, new HBox(8, save, clear, delete));
        return panel;
    }

    private void startCreate() {
        clearForm();
        showFormPanel();
    }

    private void showFormPanel() {
        if (floatingForm == null) {
            floatingForm = new FloatingPanel(I18n.t("outcoming.details"), form, this::closeForm);
        }
        boolean wasAdded = !overlay.getChildren().contains(floatingForm);
        if (wasAdded) {
            overlay.getChildren().add(floatingForm);
        }
        double x = Math.max(24, overlay.getWidth() - floatingForm.getPrefWidth() - 24);
        floatingForm.positionAt(x, 24);
        floatingForm.toFront();

        if (wasAdded) {
            // Force an immediate CSS + layout pass now, before the panel is ever painted.
            // Without this, the GridPane's column widths can resolve on a stale/zero-width
            // parent chain the first time the panel is added to the overlay, leaving the
            // form's editors invisible until some later event (e.g. a manual resize)
            // triggers a fresh layout pass.
            floatingForm.applyCss();
            floatingForm.layout();
        }
    }

    private void closeForm() {
        if (floatingForm != null) {
            overlay.getChildren().remove(floatingForm);
        }
        clearForm();
    }

    private void selectRow(Outcoming outcoming) {
        selected = outcoming;
        if (outcoming == null) return;
        labelField.setText(outcoming.getLabel());
        amountField.setText(outcoming.getAmount() == null ? "" : String.valueOf(outcoming.getAmount()));
        beneficiaryField.setText(outcoming.getBeneficiary());
        categoryField.setValue(outcoming.getCategory());
        methodField.setValue(outcoming.getPaymentMethod());
        statusField.setValue(outcoming.getStatus());
        outcomingDateField.setValue(outcoming.getDateOutcome() == null ? null : outcoming.getDateOutcome().toLocalDate());
        // A generated occurrence is a plain expense row in the UI — editing it never turns it into a template.
        recurringCheck.setSelected(false);
        frequencyField.setValue(null);
        showFormPanel();
    }

    private void clearForm() {
        selected = null;
        labelField.clear();
        amountField.clear();
        beneficiaryField.clear();
        categoryField.setValue(null);
        methodField.setValue(null);
        statusField.setValue(null);
        outcomingDateField.setValue(LocalDate.now());
        recurringCheck.setSelected(false);
        frequencyField.setValue(null);
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (labelField.getText().isBlank() || amountField.getText().isBlank()) {
            DialogUtil.error(I18n.t("dialog.required_fields"), I18n.t("outcoming.label_amount_required"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            DialogUtil.error(I18n.t("dialog.error"), I18n.t("outcoming.invalid_amount"));
            return;
        }
        if (recurringCheck.isSelected() && frequencyField.getValue() == null) {
            DialogUtil.error(I18n.t("dialog.required_fields"), I18n.t("outcoming.frequency_required"));
            return;
        }

        Outcoming outcoming = selected != null ? selected : new Outcoming();
        outcoming.setLabel(labelField.getText().trim());
        outcoming.setAmount(amount);
        outcoming.setBeneficiary(beneficiaryField.getText() == null ? null : beneficiaryField.getText().trim());
        outcoming.setCategory(categoryField.getValue() == null ? OutcomingCategory.AUTRE : categoryField.getValue());
        outcoming.setPaymentMethod(methodField.getValue() == null ? PaymentType.CASH : methodField.getValue());
        outcoming.setStatus(statusField.getValue() == null ? PaymentStatus.PAID : statusField.getValue());
        outcoming.setDateOutcome(outcomingDateField.getValue() == null
                ? LocalDateTime.now()
                : outcomingDateField.getValue().atStartOfDay());
        outcoming.setRecurring(recurringCheck.isSelected());
        outcoming.setFrequency(recurringCheck.isSelected() ? frequencyField.getValue() : null);
        if (recurringCheck.isSelected()) {
            outcoming.setNextOccurrenceDate(outcoming.getDateOutcome());
        }

        AsyncTasks.run(
                () -> outcomingService.save(outcoming),
                saved -> { clearForm(); closeForm(); reload(); },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("dialog.save_failed").replace("{0}", err.getMessage()))
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm(I18n.t("dialog.confirm"), I18n.t("outcoming.delete_confirm"))) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> outcomingService.delete(id),
                () -> { clearForm(); closeForm(); reload(); },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("dialog.delete_failed").replace("{0}", err.getMessage()))
        );
    }

    private void reload() {
        AsyncTasks.run(
                () -> outcomingService.generateDueOccurrences(),
                this::loadLists,
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("outcoming.generate_failed").replace("{0}", err.getMessage()))
        );
    }

    private void loadLists() {
        AsyncTasks.run(
                () -> outcomingService.findAll(),
                list -> {
                    allOutcomings = list;
                    applyFilters();
                },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("dialog.load_failed").replace("{0}", err.getMessage()))
        );
        AsyncTasks.run(
                () -> outcomingService.findRecurringTemplates(),
                list -> recurringRows.setAll(list),
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("outcoming.load_recurring_failed").replace("{0}", err.getMessage()))
        );
    }

    private void applyFilters() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statusVal = statusFilter.getValue();
        String categoryVal = categoryFilter.getValue();
        LocalDate from = dateFrom.getValue();
        LocalDate to = dateTo.getValue();

        List<Outcoming> filtered = allOutcomings.stream()
                .filter(o -> {
                    if (!needle.isBlank()) {
                        String label = o.getLabel() == null ? "" : o.getLabel().toLowerCase();
                        String beneficiary = o.getBeneficiary() == null ? "" : o.getBeneficiary().toLowerCase();
                        if (!label.contains(needle) && !beneficiary.contains(needle)) return false;
                    }
                    if (statusVal != null && !I18n.t("outcoming.all").equals(statusVal)) {
                        if (!statusLabel(o.getStatus()).equals(statusVal)) return false;
                    }
                    if (categoryVal != null && !I18n.t("outcoming.all").equals(categoryVal)) {
                        if (!categoryLabel(o.getCategory()).equals(categoryVal)) return false;
                    }
                    if (from != null && o.getDateOutcome() != null && o.getDateOutcome().toLocalDate().isBefore(from)) {
                        return false;
                    }
                    if (to != null && o.getDateOutcome() != null && o.getDateOutcome().toLocalDate().isAfter(to)) {
                        return false;
                    }
                    return true;
                })
                .toList();

        rows.setAll(filtered);
        updateFooter(filtered);
        updateSummaryCards(allOutcomings);
    }

    private void updateFooter(List<Outcoming> data) {
        double total = data.stream().mapToDouble(o -> o.getAmount() == null ? 0 : o.getAmount()).sum();
        footerCountLabel.setText(I18n.t("outcoming.total_count").replace("{0}", String.valueOf(data.size())));
        footerTotalLabel.setText(I18n.t("outcoming.total_amount").replace("{0}", formatAmount(total)));
    }

    private void updateSummaryCards(List<Outcoming> data) {
        summaryCards.getChildren().clear();
        double totalAmount = data.stream().mapToDouble(o -> o.getAmount() == null ? 0 : o.getAmount()).sum();

        List<Outcoming> pending = data.stream().filter(o -> o.getStatus() == PaymentStatus.PENDING).toList();
        double pendingAmount = pending.stream().mapToDouble(o -> o.getAmount() == null ? 0 : o.getAmount()).sum();

        List<Outcoming> overdue = data.stream().filter(o -> o.getStatus() == PaymentStatus.OVERDUE).toList();
        double overdueAmount = overdue.stream().mapToDouble(o -> o.getAmount() == null ? 0 : o.getAmount()).sum();

        summaryCards.getChildren().addAll(
                summaryCard("fth-trending-down", String.valueOf(data.size()), I18n.t("outcoming.total"), "#DC2626", "#FEE2E2"),
                summaryCard("fth-dollar-sign", formatAmount(totalAmount), I18n.t("field.amount"), "#DC2626", "#FEE2E2"),
                summaryCard("fth-clock", pending.size() + " · " + formatAmount(pendingAmount), I18n.t("outcoming.pending"), "#D97706", "#FEF3C7"),
                summaryCard("fth-alert-circle", overdue.size() + " · " + formatAmount(overdueAmount), I18n.t("status.overdue"), "#B91C1C", "#FECACA")
        );
        for (var n : summaryCards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
    }

    private HBox summaryCard(String icon, String value, String label, String accent, String bg) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(20);
        fi.setStyle("-fx-icon-color: " + accent + ";");
        StackPane iconWrap = new StackPane(fi);
        iconWrap.getStyleClass().add("stat-icon-wrap");
        iconWrap.setStyle("-fx-background-color: " + bg + ";");

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("stat-number");
        valLbl.setStyle("-fx-font-size: 20px;");
        Label capLbl = new Label(label);
        capLbl.getStyleClass().add("stat-caption");

        VBox text = new VBox(2, valLbl, capLbl);
        HBox card = new HBox(12, iconWrap, text);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("stat-box");
        card.setPadding(new Insets(14));
        return card;
    }
    private void wireRowDoubleClick() {
        table.setRowFactory(tv -> {
            TableRow<Outcoming> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    selectRow(row.getItem());
                }
            });
            return row;
        });
    }
}
