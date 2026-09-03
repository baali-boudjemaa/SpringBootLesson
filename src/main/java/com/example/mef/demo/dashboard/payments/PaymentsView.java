package com.example.mef.demo.dashboard.payments;

import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Services.EnrollmentService;
import com.example.mef.demo.Services.PaymentService;
import com.example.mef.demo.Services.MonthlyBillingService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FloatingPanel;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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
 * Typed CRUD screen for the "payments" module (Payment entity), restyled to match the
 * Students/Courses modules: filter toolbar, summary cards, and a floating (draggable)
 * details panel instead of a fixed side form.
 */
@Component
public class PaymentsView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PaymentService paymentService;
    private final EnrollmentService enrollmentService;
    private final MonthlyBillingService monthlyBillingService;

    private final ObservableList<Payment> rows = FXCollections.observableArrayList();
    private final TableView<Payment> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "payments");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField("");
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final DatePicker dateFrom = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 1, 1));
    private final DatePicker dateTo = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 12, 31));

    private final ComboBox<Inscription> inscriptionField = new ComboBox<>();
    private final TextField amountField = FormFactory.textField("");
    private final TextField categoryField = FormFactory.textField("");
    private final ComboBox<PaymentType> methodField = new ComboBox<>(FXCollections.observableArrayList(PaymentType.values()));
    private final ComboBox<PaymentStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
    private final DatePicker paymentDateField = new DatePicker();
    private final DatePicker billingDueDateField = new DatePicker();

    private final Label footerCountLabel = new Label();
    private final Label footerTotalLabel = new Label();
    private final HBox summaryCards = new HBox(14);
    private final Label billingAlertLabel = new Label();

    private List<Payment> allPayments = List.of();
    private Payment selected;
    /** Avoids replacing a payment's saved values while its row is loaded into the form. */
    private boolean loadingPayment;
    private VBox form;

    /** Overlay Pane that the floating panel lives in, stacked on top of the normal screen content. */
    private Pane overlay;
    private FloatingPanel floatingForm;

    public PaymentsView(PaymentService paymentService, EnrollmentService enrollmentService,
                        MonthlyBillingService monthlyBillingService) {
        this.paymentService = paymentService;
        this.enrollmentService = enrollmentService;
        this.monthlyBillingService = monthlyBillingService;
        inscriptionField.setMaxWidth(Double.MAX_VALUE);
        methodField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);
        paymentDateField.setMaxWidth(Double.MAX_VALUE);
        billingDueDateField.setMaxWidth(Double.MAX_VALUE);
        paymentDateField.getStyleClass().add("filter-field");
        inscriptionField.setCellFactory(cb -> inscriptionCell());
        inscriptionField.setButtonCell(inscriptionCell());
        // Explicit converter: without this, the button cell can fall back to
        // Inscription#toString() (e.g. "com.example...@1a2b3c") instead of the
        // label, particularly when the selected value isn't reference-equal
        // to an item already loaded into the combo's items list.
        inscriptionField.setConverter(new javafx.util.StringConverter<Inscription>() {
            @Override
            public String toString(Inscription i) {
                return i == null ? "" : inscriptionLabel(i);
            }

            @Override
            public Inscription fromString(String s) {
                return inscriptionField.getValue();
            }
        });
        inscriptionField.valueProperty().addListener((obs, oldInscription, newInscription) -> {
            if (!loadingPayment && newInscription != null) {
                populateForInscription(newInscription);
            }
        });
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        searchField.setPromptText(I18n.t("payment.search", "تسجيل الحضور"));
        amountField.setPromptText(I18n.t("field.amount", "تسجيل الحضور"));
        categoryField.setPromptText(I18n.t("field.category", "تسجيل الحضور"));
        pageTitleLabel.setText(I18n.t("payment.title", "تسجيل الحضور"));

        buildColumns();
        wireRowDoubleClick();

        Label subtitle = new Label(I18n.t("payment.subtitle", "تسجيل الحضور"));
        subtitle.getStyleClass().add("page-subtitle");

        searchField.getStyleClass().add("filter-field");
        statusFilter.getStyleClass().add("filter-field");
        statusFilter.setPrefWidth(140);

        Button addBtn = new Button("+  " + I18n.t("payment.add", "تسجيل الحضور"));
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> startCreate());

        HBox filters = new HBox(10,
                labeledFilter(I18n.t("payment.from", "تسجيل الحضور"), dateFrom),
                labeledFilter(I18n.t("payment.to", "تسجيل الحضور"), dateTo),
                statusFilter,
                searchField
        );
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button duesBtn = new Button(I18n.t("payment.dues_btn", "Échéances"));
        duesBtn.getStyleClass().add("secondary-button");
        duesBtn.setOnAction(e -> showMonthlyDues());

        HBox toolbar = new HBox(12, filters, duesBtn, addBtn);
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

        billingAlertLabel.getStyleClass().add("page-subtitle");
        billingAlertLabel.setStyle("-fx-text-fill: #B45309;");
        VBox center = new VBox(16, subtitle, billingAlertLabel, toolbar, tableBlock, summaryCards);
        center.setPadding(new Insets(24));
        VBox.setVgrow(tableBlock, Priority.ALWAYS);

        form = buildForm();
        floatingForm = null;

        // Overlay hosts the floating panel; pickOnBounds(false) lets clicks pass through
        // to the table/buttons underneath wherever the overlay itself has no floating panel.
        overlay = new Pane();
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(center, overlay);
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.getStyleClass().add("details-scroll");
        scrollPane.setFitToWidth(true);
        contentPane.setCenter(scrollPane);

        TableStyleKit.applyEmptyPlaceholder(table);
        refreshStatusFilter();
        wireFilters();
        loadPickers();
        reload();
    }

    /** Rebuilds status combo labels for the current locale (bean is created before locale is applied). */
    private void refreshStatusFilter() {
        String previous = statusFilter.getValue();
        String all = I18n.t("payment.filter.all", "Tous");
        List<String> items = List.of(
                all,
                I18n.t("status.paid", "PAYÉ"),
                I18n.t("status.pending", "EN ATTENTE"),
                I18n.t("status.overdue", "EN RETARD")
        );
        statusFilter.getItems().setAll(items);
        if (previous != null && items.contains(previous)) {
            statusFilter.setValue(previous);
        } else {
            statusFilter.setValue(all);
        }
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
        dateFrom.valueProperty().addListener((o, a, b) -> applyFilters());
        dateTo.valueProperty().addListener((o, a, b) -> applyFilters());
    }

    /** Opens the floating details panel for a row when the user double-clicks it. */
    private void wireRowDoubleClick() {
        table.setRowFactory(tv -> {
            TableRow<Payment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    selectRow(row.getItem());
                }
            });
            return row;
        });
    }

    private void buildColumns() {
        table.getColumns().clear();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Payment, String> date = new TableColumn<>(I18n.t("payment.col.date", "DATE"));
        date.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDatePay() == null ? "—" : d.getValue().getDatePay().format(DATE_FORMAT)));

        TableColumn<Payment, String> student = new TableColumn<>(I18n.t("payment.col.student", "ÉLÈVE"));
        student.setCellValueFactory(d -> new ReadOnlyStringWrapper(studentLabel(d.getValue())));

        TableColumn<Payment, String> inscription = new TableColumn<>(I18n.t("payment.col.inscription", "INSCRIPTION"));
        inscription.setCellValueFactory(d -> new ReadOnlyStringWrapper(inscriptionLabel(d.getValue())));

        TableColumn<Payment, String> amount = new TableColumn<>(I18n.t("payment.col.amount", "MONTANT"));
        amount.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatAmount(d.getValue().getAmount())));

        TableColumn<Payment, String> method = new TableColumn<>(I18n.t("payment.col.method", "MÉTHODE"));
        method.setCellValueFactory(d -> new ReadOnlyStringWrapper(methodLabel(d.getValue().getPaymentMethod())));

        TableColumn<Payment, PaymentStatus> status = new TableColumn<>(I18n.t("payment.col.status", "STATUT"));
        status.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getStatus()));
        status.setCellFactory(col -> statusCell());

        TableColumn<Payment, Payment> actions = new TableColumn<>(I18n.t("payment.col.actions", "ACTION"));
        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> actionCell());
        actions.setMaxWidth(120);
        actions.setMinWidth(100);

        table.getColumns().addAll(List.of(date, student, inscription, amount, method, status, actions));
    }

    private TableCell<Payment, PaymentStatus> statusCell() {
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

    private TableCell<Payment, Payment> actionCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Payment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Button view = iconBtn("fth-eye", I18n.t("action.view", "Voir"));
                Button edit = iconBtn("fth-edit-2", I18n.t("action.edit", "Modifier"));
                Button del  = iconBtn("fth-trash-2", I18n.t("action.delete", "Supprimer"));
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
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private ListCell<Inscription> inscriptionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Inscription item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                setText(inscriptionLabel(item));
            }
        };
    }

    private String studentLabel(Payment payment) {
        if (payment.getInscription() == null || payment.getInscription().getStudent() == null) return "—";
        var s = payment.getInscription().getStudent();
        return s.getFirstName() + " " + s.getLastName();
    }

    private String inscriptionLabel(Payment payment) {
        if (payment.getInscription() == null) return "—";
        return inscriptionLabel(payment.getInscription());
    }

    private String inscriptionLabel(Inscription item) {
        String student = item.getStudent() == null ? "?"
                : item.getStudent().getFirstName() + " " + item.getStudent().getLastName();
        String cls = item.getClassroom() == null ? "?" : item.getClassroom().getName();
        return student + " · " + cls;
    }

    private static String methodLabel(PaymentType type) {
        if (type == null) return "—";
        return switch (type) {
            case CASH -> I18n.t("payment_method.cash", "Espèces");
            case CARD -> I18n.t("payment_method.card", "Carte");
            case TRANSFER -> I18n.t("payment_method.transfer", "Virement");
        };
    }

    private static String statusLabel(PaymentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PAID -> I18n.t("status.paid", "PAYÉ");
            case PENDING -> I18n.t("status.pending", "EN ATTENTE");
            case OVERDUE -> I18n.t("status.overdue", "EN RETARD");
        };
    }

    private static String statusBadgeStyle(PaymentStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PAID -> "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;";
            case PENDING -> "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;";
            case OVERDUE -> "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";
        };
    }

    private static String formatAmount(Double amount) {
        if (amount == null) return "—";
        return String.format(Locale.FRENCH, "%,.2f DA", amount);
    }

    private void loadPickers() {
        AsyncTasks.run(enrollmentService::findAll,
                list -> inscriptionField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des inscriptions : " + err.getMessage()));
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, I18n.t("nav.enrollments", "تسجيل الحضور") + " *", inscriptionField);
        FormFactory.addRow(grid, 1, I18n.t("field.amount", "تسجيل الحضور") + " *", amountField);
        FormFactory.addRow(grid, 2, I18n.t("field.category", "تسجيل الحضور"), categoryField);
        FormFactory.addRow(grid, 3, I18n.t("field.method", "تسجيل الحضور"), methodField);
        FormFactory.addRow(grid, 4, I18n.t("field.status", "تسجيل الحضور"), statusField);
        FormFactory.addRow(grid, 5, I18n.t("payment.date", "تسجيل الحضور"), paymentDateField);
        FormFactory.addRow(grid, 6, I18n.t("payment.billing_due_date", "Échéance mensuelle couverte"), billingDueDateField);

        Button save = new Button(I18n.t("action.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());

        Button clear = new Button("+ " + I18n.t("action.new", "تسجيل الحضور"));
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> startCreate());

        Button delete = new Button(I18n.t("action.delete", "تسجيل الحضور"));
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        // No title label here — the FloatingPanel header already shows "Détails du paiement".
        // NOTE: no setPrefWidth() on this VBox — same reasoning as StudentsView: the panel
        // sits inside a ScrollPane with fitToWidth(true) (see FloatingPanel), and forcing a
        // fixed prefWidth here fought that constraint on the very first layout pass.
        return new VBox(12, grid, new HBox(8, save, clear, delete));
    }

    private void startCreate() {
        clearForm();
        showFormPanel();
    }

    private void showFormPanel() {
        if (floatingForm == null) {
            floatingForm = new FloatingPanel(I18n.t("payment.details", "تسجيل الحضور"), form, this::closeForm, 480);
        }
        boolean wasAdded = !overlay.getChildren().contains(floatingForm);
        if (wasAdded) {
            overlay.getChildren().add(floatingForm);
        }
        double x = Math.max(24, overlay.getWidth() - floatingForm.getPrefWidth() - 24);
        floatingForm.positionAt(x, 24);
        floatingForm.toFront();

        if (wasAdded) {
            // Force an immediate CSS + layout pass now, before the panel is ever painted —
            // see StudentsView.showFormPanel() for why this is needed.
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

    private void selectRow(Payment payment) {
        selected = payment;
        if (payment == null) return;
        loadingPayment = true;
        try {
            inscriptionField.setValue(payment.getInscription());
            amountField.setText(payment.getAmount() == null ? "" : String.valueOf(payment.getAmount()));
            categoryField.setText(payment.getLabel());
            methodField.setValue(payment.getPaymentMethod());
            statusField.setValue(payment.getStatus());
            paymentDateField.setValue(payment.getDatePay() == null ? null : payment.getDatePay().toLocalDate());
            billingDueDateField.setValue(payment.getBillingDueDate());
        } finally {
            loadingPayment = false;
        }
        showFormPanel();
    }

    /** Fills the form from the selected enrollment instead of leaving values from the previous one. */
    private void populateForInscription(Inscription inscription) {
        AsyncTasks.run(
                () -> monthlyBillingService.currentCycleDue(inscription),
                due -> {
                    // Ignore a result that belongs to a previous quick selection.
                    if (due == null || inscriptionField.getValue() == null
                            || !inscription.getId().equals(inscriptionField.getValue().getId())) return;
                    amountField.setText(String.valueOf(due.remainingAmount()));
                    categoryField.setText(I18n.t("payment.default_category", "Scolarité"));
                    statusField.setValue(due.isPaid() ? PaymentStatus.PAID : PaymentStatus.PENDING);
                    billingDueDateField.setValue(due.dueDate());
                },
                err -> DialogUtil.error(I18n.t("payment.dues.title", "Échéances mensuelles"), I18n.t("payment.error.load_due", "Impossible de charger l'échéance") + " : " + err.getMessage()));
    }

    private void clearForm() {
        selected = null;
        inscriptionField.setValue(null);
        amountField.clear();
        categoryField.clear();
        methodField.setValue(null);
        statusField.setValue(null);
        paymentDateField.setValue(LocalDate.now());
        billingDueDateField.setValue(null);
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (inscriptionField.getValue() == null || amountField.getText().isBlank()) {
            DialogUtil.error(I18n.t("dialog.required_fields", "Champs requis"), I18n.t("payment.error.required_fields", "L'inscription et le montant sont obligatoires."));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            DialogUtil.error(I18n.t("dialog.error", "Erreur"), I18n.t("payment.error.invalid_amount", "Le montant doit être un nombre."));
            return;
        }

        Payment payment = selected != null ? selected : new Payment();
        payment.setAmount(amount);
        payment.setLabel(categoryField.getText().isBlank() ? I18n.t("payment.default_category", "Scolarité") : categoryField.getText().trim());
        payment.setPaymentMethod(methodField.getValue() == null ? PaymentType.CASH : methodField.getValue());
        payment.setStatus(statusField.getValue() == null ? PaymentStatus.PAID : statusField.getValue());
        payment.setDatePay(paymentDateField.getValue() == null
                ? LocalDateTime.now()
                : paymentDateField.getValue().atStartOfDay());
        payment.setBillingDueDate(billingDueDateField.getValue());
        String inscriptionId = inscriptionField.getValue().getId();

        AsyncTasks.run(
                () -> paymentService.save(payment, inscriptionId),
                saved -> { 
                    clearForm(); 
                    closeForm(); 
                    reload(); 
                    if (DialogUtil.confirm(I18n.t("payment.confirm.print_title", "Paiement enregistré"), I18n.t("payment.confirm.print", "Le paiement a été enregistré avec succès.\nVoulez-vous imprimer un reçu ?"))) {
                        printPaymentReceipt(saved);
                    }
                },
                err -> DialogUtil.error(I18n.t("dialog.error", "Erreur"), I18n.t("payment.error.save", "Échec de l'enregistrement") + " : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm(I18n.t("dialog.confirm", "Confirmer"), I18n.t("payment.confirm.delete", "Supprimer ce paiement ?"))) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> paymentService.delete(id),
                () -> { clearForm(); closeForm(); reload(); },
                err -> DialogUtil.error(I18n.t("dialog.error", "Erreur"), I18n.t("payment.error.delete", "Échec de la suppression") + " : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                paymentService::findAll,
                list -> {
                    allPayments = list;
                    applyFilters();
                    refreshBillingAlert();
                },
                err -> DialogUtil.error(I18n.t("dialog.error", "Erreur"), I18n.t("payment.error.load", "Échec du chargement") + " : " + err.getMessage())
        );
    }

    private void applyFilters() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        PaymentStatus wantedStatus = statusFromFilter();
        LocalDate from = dateFrom.getValue();
        LocalDate to = dateTo.getValue();

        List<Payment> filtered = allPayments.stream()
                .filter(p -> {
                    if (!needle.isBlank()) {
                        String name = studentLabel(p).toLowerCase();
                        if (!name.contains(needle)) return false;
                    }
                    if (wantedStatus != null && p.getStatus() != wantedStatus) {
                        return false;
                    }
                    if (from != null && p.getDatePay() != null && p.getDatePay().toLocalDate().isBefore(from)) {
                        return false;
                    }
                    if (to != null && p.getDatePay() != null && p.getDatePay().toLocalDate().isAfter(to)) {
                        return false;
                    }
                    return true;
                })
                .toList();

        rows.setAll(filtered);
        updateFooter(filtered);
        updateSummaryCards(allPayments);
    }

    /** Null means "all". Stale labels left over from another locale are treated as all. */
    private PaymentStatus statusFromFilter() {
        String statusVal = statusFilter.getValue();
        if (statusVal == null || statusVal.equals(I18n.t("payment.filter.all", "Tous"))) {
            return null;
        }
        for (PaymentStatus status : PaymentStatus.values()) {
            if (statusLabel(status).equals(statusVal)) {
                return status;
            }
        }
        return null;
    }

    private void updateFooter(List<Payment> data) {
        double total = data.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();
        footerCountLabel.setText(java.text.MessageFormat.format(I18n.t("payment.footer.count", "Total des paiements : {0}"), data.size()));
        footerTotalLabel.setText(java.text.MessageFormat.format(I18n.t("payment.footer.total", "Total Montant : {0}"), formatAmount(total)));
    }

    private void updateSummaryCards(List<Payment> data) {
        summaryCards.getChildren().clear();
        double totalAmount = data.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();

        List<Payment> pending = data.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).toList();
        double pendingAmount = pending.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();

        List<Payment> overdue = data.stream().filter(p -> p.getStatus() == PaymentStatus.OVERDUE).toList();
        double overdueAmount = overdue.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();

        summaryCards.getChildren().addAll(
                summaryCard("fth-credit-card", String.valueOf(data.size()), I18n.t("payment.card.total", "Total Paiements"), "#059669", "#D1FAE5"),
                summaryCard("fth-dollar-sign", formatAmount(totalAmount), I18n.t("payment.card.amount", "Montant Total"), "#2563EB", "#DBEAFE"),
                summaryCard("fth-clock", pending.size() + " · " + formatAmount(pendingAmount), I18n.t("payment.card.pending", "En Attente"), "#D97706", "#FEF3C7"),
                summaryCard("fth-alert-circle", overdue.size() + " · " + formatAmount(overdueAmount), I18n.t("payment.card.overdue", "En Retard"), "#DC2626", "#FEE2E2")
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

    private void refreshBillingAlert() {
        AsyncTasks.run(
                () -> new BillingOverview(monthlyBillingService.findOpenDues(), monthlyBillingService.findDueWithinDays(7)),
                overview -> billingAlertLabel.setText(java.text.MessageFormat.format(I18n.t("payment.billing_alert", "{0} en retard · {1} à échéance dans les 7 prochains jours"), overview.overdue().size(), overview.upcoming().size())),
                err -> billingAlertLabel.setText(I18n.t("payment.billing_alert_error", "Échéances mensuelles indisponibles") + " : " + err.getMessage()));
    }

    private void showMonthlyDues() {
        AsyncTasks.run(
                () -> new BillingOverview(monthlyBillingService.findOpenDues(), monthlyBillingService.findDueWithinDays(7)),
                this::showMonthlyDuesTable,
                err -> DialogUtil.error(I18n.t("payment.dues.title", "Échéances mensuelles"), err.getMessage()));
    }

    private void showMonthlyDuesTable(BillingOverview overview) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(I18n.t("payment.dues.title", "Échéances mensuelles"));
        dialog.setHeaderText(I18n.t("payment.dues.header", "Liste des étudiants avec des paiements en retard ou à échéance."));

        TableView<MonthlyBillingService.Due> duesTable = new TableView<>();
        TableStyleKit.applyTheme(duesTable, "payments-dues");
        duesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<MonthlyBillingService.Due, String> studentCol = new TableColumn<>(I18n.t("payment.col.student", "ÉLÈVE"));
        studentCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(MonthlyBillingService.studentName(d.getValue().inscription())));
        studentCol.setPrefWidth(220);

        TableColumn<MonthlyBillingService.Due, String> dateCol = new TableColumn<>(I18n.t("payment.dues.col.due_date", "DATE D'ÉCHÉANCE"));
        dateCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().dueDate().format(DATE_FORMAT)));
        dateCol.setPrefWidth(140);

        TableColumn<MonthlyBillingService.Due, String> amountCol = new TableColumn<>(I18n.t("payment.col.amount", "MONTANT"));
        amountCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatAmount(d.getValue().remainingAmount())));
        amountCol.setPrefWidth(120);

        TableColumn<MonthlyBillingService.Due, String> statusCol = new TableColumn<>(I18n.t("payment.col.status", "STATUT"));
        statusCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().isOverdue(LocalDate.now()) ? I18n.t("payment.dues.status.overdue", "En retard") : I18n.t("payment.dues.status.upcoming", "À échéance")
        ));
        statusCol.setPrefWidth(120);

        TableColumn<MonthlyBillingService.Due, MonthlyBillingService.Due> actionsCol = new TableColumn<>(I18n.t("payment.col.actions", "ACTIONS"));
        actionsCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actionsCol.setPrefWidth(200);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button payBtn = new Button(I18n.t("payment.dues.pay_btn", "Payer"));
            private final Button suspendBtn = new Button(I18n.t("payment.dues.suspend_btn", "Suspendre"));
            private final HBox pane = new HBox(8, payBtn, suspendBtn);

            {
                payBtn.getStyleClass().add("primary-button");
                suspendBtn.getStyleClass().add("danger-button");
                pane.setAlignment(Pos.CENTER_LEFT);

                payBtn.setOnAction(e -> {
                    MonthlyBillingService.Due due = getItem();
                    if (due == null) return;
                    
                    Payment payment = new Payment();
                    payment.setInscription(due.inscription());
                    payment.setAmount(due.remainingAmount());
                    payment.setLabel(I18n.t("payment.default_category", "Scolarité"));
                    payment.setPaymentMethod(PaymentType.CASH);
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setDatePay(LocalDateTime.now());
                    payment.setBillingDueDate(due.dueDate());
                    
                    AsyncTasks.run(
                        () -> paymentService.save(payment, due.inscription().getId()),
                        savedPayment -> {
                            getTableView().getItems().remove(due);
                            refreshBillingAlert();
                            reload(); // reload main table
                            
                            if (DialogUtil.confirm(I18n.t("payment.confirm.print_title", "Paiement enregistré"), I18n.t("payment.confirm.print", "Le paiement a été enregistré avec succès.\nVoulez-vous imprimer un reçu ?"))) {
                                printPaymentReceipt(savedPayment);
                            }
                            if (getTableView().getItems().isEmpty()) {
                                dialog.close();
                            }
                        },
                        err -> DialogUtil.error(I18n.t("dialog.error", "Erreur"), I18n.t("payment.error.save", "Échec de l'enregistrement") + " : " + err.getMessage())
                    );
                });

                suspendBtn.setOnAction(e -> {
                    MonthlyBillingService.Due due = getItem();
                    if (due == null) return;
                    boolean confirm = DialogUtil.confirm(I18n.t("payment.suspend.confirm_title", "Confirmer la suspension"),
                            java.text.MessageFormat.format(I18n.t("payment.suspend.confirm", "Êtes-vous sûr de vouloir suspendre l'élève {0} ?"), MonthlyBillingService.studentName(due.inscription())));
                    if (confirm) {
                        Inscription inscription = due.inscription();
                        inscription.setStatus(com.example.mef.demo.enums.EnrollmentStatus.DROPPED);
                        java.util.List<String> courseIds = inscription.getCourses() != null ?
                                inscription.getCourses().stream().map(c -> c.getId()).toList() : null;
                        AsyncTasks.run(() -> enrollmentService.save(
                                inscription,
                                inscription.getStudent() != null ? inscription.getStudent().getId() : null,
                                inscription.getClassroom() != null ? inscription.getClassroom().getId() : null,
                                inscription.getAnneeScolaire() != null ? inscription.getAnneeScolaire().getId() : null,
                                courseIds),
                                saved -> {
                                    getTableView().getItems().remove(due);
                                    refreshBillingAlert();
                                },
                                err -> DialogUtil.error(I18n.t("dialog.error", "Erreur"), I18n.t("payment.error.suspend", "Impossible de suspendre l'élève") + " : " + err.getMessage()));
                    }
                });
            }

            @Override
            protected void updateItem(MonthlyBillingService.Due item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : pane);
            }
        });

        duesTable.getColumns().addAll(List.of(studentCol, dateCol, amountCol, statusCol, actionsCol));

        List<MonthlyBillingService.Due> allDues = new java.util.ArrayList<>();
        allDues.addAll(overview.overdue());
        allDues.addAll(overview.upcoming());
        duesTable.setItems(FXCollections.observableArrayList(allDues));
        duesTable.setPrefSize(950, 450);

        dialog.getDialogPane().setContent(duesTable);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        if (table.getScene() != null && !table.getScene().getStylesheets().isEmpty()) {
            dialog.getDialogPane().getStylesheets().addAll(table.getScene().getStylesheets());
            dialog.getDialogPane().getStyleClass().add("app-dialog");
        }

        dialog.showAndWait();
    }

    private record BillingOverview(List<MonthlyBillingService.Due> overdue,
                                   List<MonthlyBillingService.Due> upcoming) { }

    private void printPaymentReceipt(Payment payment) {
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job == null) {
            DialogUtil.error(I18n.t("payment.print.title", "Impression"), I18n.t("payment.print.no_printer", "Aucune imprimante trouvée."));
            return;
        }

        boolean accepted = job.showPrintDialog(table.getScene().getWindow());
        if (!accepted) return;

        VBox receipt = new VBox(15);
        receipt.setPadding(new Insets(30));
        receipt.setStyle("-fx-background-color: white; -fx-font-family: 'Arial';");
        if (I18n.isRTL()) {
            receipt.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
            receipt.setStyle(receipt.getStyle() + " -fx-font-family: 'NotoNaskhArabic';");
        }

        Label title = new Label(I18n.t("payment.receipt.title", "REÇU DE PAIEMENT"));
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Inscription inscription = payment.getInscription();
        com.example.mef.demo.Model.Student s = inscription.getStudent();
        String studentName = s != null ? s.getFirstName() + " " + s.getLastName() : "—";
        
        VBox detailsBox = new VBox(8);
        detailsBox.setStyle("-fx-font-size: 14px;");
        detailsBox.getChildren().addAll(
            new Label(I18n.t("payment.receipt.student", "Élève") + " : " + studentName),
            new Label(I18n.t("payment.receipt.date", "Date") + " : " + (payment.getDatePay() != null ? payment.getDatePay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—")),
            new Label(I18n.t("payment.receipt.amount", "Montant") + " : " + formatAmount(payment.getAmount())),
            new Label(I18n.t("payment.receipt.method", "Méthode") + " : " + methodLabel(payment.getPaymentMethod()))
        );
        
        VBox coursesBox = new VBox(8);
        coursesBox.setStyle("-fx-font-size: 14px;");
        coursesBox.getChildren().add(new Label(I18n.t("payment.receipt.details", "Détails d'inscription") + " :"));
        if (inscription.getClassroom() != null) {
            coursesBox.getChildren().add(new Label(" - " + I18n.t("payment.receipt.class", "Classe") + " : " + inscription.getClassroom().getName()));
        }
        if (inscription.getSession() != null) {
            coursesBox.getChildren().add(new Label(" - " + I18n.t("payment.receipt.session", "Session") + " : " + inscription.getSession().name()));
        }
        if (inscription.getCourses() != null && !inscription.getCourses().isEmpty()) {
            coursesBox.getChildren().add(new Label(" - " + I18n.t("payment.receipt.courses", "Cours") + " :"));
            for (com.example.mef.demo.Model.Course c : inscription.getCourses()) {
                coursesBox.getChildren().add(new Label("   * " + c.getName()));
            }
        }
        
        receipt.getChildren().addAll(title, detailsBox, coursesBox);

        javafx.print.Printer printer = job.getPrinter();
        javafx.print.PageLayout pageLayout = printer.createPageLayout(
                javafx.print.Paper.A4, 
                javafx.print.PageOrientation.PORTRAIT, 
                javafx.print.Printer.MarginType.DEFAULT
        );

        // Scale receipt to fit printable area width
        double scale = Math.min(pageLayout.getPrintableWidth() / receipt.prefWidth(-1), 1.0);
        if (scale < 1.0) {
            receipt.getTransforms().add(new javafx.scene.transform.Scale(scale, scale));
        }

        boolean success = job.printPage(pageLayout, receipt);
        if (success) {
            job.endJob();
            DialogUtil.info(I18n.t("payment.print.title", "Impression"), I18n.t("payment.print.success", "Le reçu a été imprimé avec succès."));
        } else {
            DialogUtil.error(I18n.t("payment.print.title", "Impression"), I18n.t("payment.print.failed", "L'impression a échoué."));
        }
    }
}
