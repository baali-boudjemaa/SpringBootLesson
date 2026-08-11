package com.example.mef.demo.dashboard.payments;

import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Services.EnrollmentService;
import com.example.mef.demo.Services.PaymentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FloatingPanel;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.enums.PaymentStatus;
import com.example.mef.demo.enums.PaymentType;
import com.example.mef.demo.util.DialogUtil;
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

    private final ObservableList<Payment> rows = FXCollections.observableArrayList();
    private final TableView<Payment> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "payments");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField("Rechercher par élève...");
    private final ComboBox<String> statusFilter = new ComboBox<>(
            FXCollections.observableArrayList("Tous", "PAYÉ", "EN ATTENTE", "EN RETARD"));
    private final DatePicker dateFrom = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 1, 1));
    private final DatePicker dateTo = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 12, 31));

    private final ComboBox<Inscription> inscriptionField = new ComboBox<>();
    private final TextField amountField = FormFactory.textField("Montant");
    private final TextField categoryField = FormFactory.textField("Catégorie (Scolarité, Transport, ...)");
    private final ComboBox<PaymentType> methodField = new ComboBox<>(FXCollections.observableArrayList(PaymentType.values()));
    private final ComboBox<PaymentStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
    private final DatePicker paymentDateField = new DatePicker();

    private final Label footerCountLabel = new Label();
    private final Label footerTotalLabel = new Label();
    private final HBox summaryCards = new HBox(14);

    private List<Payment> allPayments = List.of();
    private Payment selected;
    private VBox form;

    /** Overlay Pane that the floating panel lives in, stacked on top of the normal screen content. */
    private Pane overlay;
    private FloatingPanel floatingForm;

    public PaymentsView(PaymentService paymentService, EnrollmentService enrollmentService) {
        this.paymentService = paymentService;
        this.enrollmentService = enrollmentService;
        statusFilter.setValue("Tous");
        inscriptionField.setMaxWidth(Double.MAX_VALUE);
        methodField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);
        paymentDateField.setMaxWidth(Double.MAX_VALUE);
        inscriptionField.setCellFactory(cb -> inscriptionCell());
        inscriptionField.setButtonCell(inscriptionCell());
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Paiements");

        buildColumns();
        wireRowDoubleClick();

        Label subtitle = new Label("Gérer les paiements des élèves");
        subtitle.getStyleClass().add("page-subtitle");

        searchField.getStyleClass().add("filter-field");
        statusFilter.getStyleClass().add("filter-field");
        statusFilter.setPrefWidth(140);

        Button addBtn = new Button("+  Nouveau Paiement");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> startCreate());

        HBox filters = new HBox(10,
                labeledFilter("Du", dateFrom),
                labeledFilter("Au", dateTo),
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

        VBox center = new VBox(16, subtitle, toolbar, tableBlock, summaryCards);
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
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.getStyleClass().add("details-scroll");
        contentPane.setCenter(scrollPane);

        wireFilters();
        loadPickers();
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

        TableColumn<Payment, String> date = new TableColumn<>("DATE");
        date.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDatePay() == null ? "—" : d.getValue().getDatePay().format(DATE_FORMAT)));
        date.setPrefWidth(100);

        TableColumn<Payment, String> student = new TableColumn<>("ÉLÈVE");
        student.setCellValueFactory(d -> new ReadOnlyStringWrapper(studentLabel(d.getValue())));
        student.setPrefWidth(150);

        TableColumn<Payment, String> inscription = new TableColumn<>("INSCRIPTION");
        inscription.setCellValueFactory(d -> new ReadOnlyStringWrapper(inscriptionLabel(d.getValue())));

        TableColumn<Payment, String> amount = new TableColumn<>("MONTANT");
        amount.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatAmount(d.getValue().getAmount())));

        TableColumn<Payment, String> method = new TableColumn<>("MÉTHODE");
        method.setCellValueFactory(d -> new ReadOnlyStringWrapper(methodLabel(d.getValue().getPaymentMethod())));

        TableColumn<Payment, PaymentStatus> status = new TableColumn<>("STATUT");
        status.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getStatus()));
        status.setCellFactory(col -> statusCell());

        TableColumn<Payment, Payment> actions = new TableColumn<>("ACTION");
        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> actionCell());
        actions.setPrefWidth(110);
        actions.setMaxWidth(120);

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
                Button view = iconBtn("fth-eye", "Voir");
                Button edit = iconBtn("fth-edit-2", "Modifier");
                Button del  = iconBtn("fth-trash-2", "Supprimer");
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
            case CASH -> "Espèces";
            case CARD -> "Carte";
            case TRANSFER -> "Virement";
        };
    }

    private static String statusLabel(PaymentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PAID -> "PAYÉ";
            case PENDING -> "EN ATTENTE";
            case OVERDUE -> "EN RETARD";
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
        FormFactory.addRow(grid, 0, "Inscription *", inscriptionField);
        FormFactory.addRow(grid, 1, "Montant *", amountField);
        FormFactory.addRow(grid, 2, "Catégorie", categoryField);
        FormFactory.addRow(grid, 3, "Méthode", methodField);
        FormFactory.addRow(grid, 4, "Statut", statusField);
        FormFactory.addRow(grid, 5, "Date de paiement", paymentDateField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());

        Button clear = new Button("+ Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> startCreate());

        Button delete = new Button("Supprimer");
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
            floatingForm = new FloatingPanel("Détails du paiement", form, this::closeForm);
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
        inscriptionField.setValue(payment.getInscription());
        amountField.setText(payment.getAmount() == null ? "" : String.valueOf(payment.getAmount()));
        categoryField.setText(payment.getLabel());
        methodField.setValue(payment.getPaymentMethod());
        statusField.setValue(payment.getStatus());
        paymentDateField.setValue(payment.getDatePay() == null ? null : payment.getDatePay().toLocalDate());
        showFormPanel();
    }

    private void clearForm() {
        selected = null;
        inscriptionField.setValue(null);
        amountField.clear();
        categoryField.clear();
        methodField.setValue(null);
        statusField.setValue(null);
        paymentDateField.setValue(LocalDate.now());
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (inscriptionField.getValue() == null || amountField.getText().isBlank()) {
            DialogUtil.error("Champs requis", "L'inscription et le montant sont obligatoires.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            DialogUtil.error("Valeur invalide", "Le montant doit être un nombre.");
            return;
        }

        Payment payment = selected != null ? selected : new Payment();
        payment.setAmount(amount);
        payment.setLabel(categoryField.getText().isBlank() ? "Scolarité" : categoryField.getText().trim());
        payment.setPaymentMethod(methodField.getValue() == null ? PaymentType.CASH : methodField.getValue());
        payment.setStatus(statusField.getValue() == null ? PaymentStatus.PAID : statusField.getValue());
        payment.setDatePay(paymentDateField.getValue() == null
                ? LocalDateTime.now()
                : paymentDateField.getValue().atStartOfDay());
        String inscriptionId = inscriptionField.getValue().getId();

        AsyncTasks.run(
                () -> paymentService.save(payment, inscriptionId),
                saved -> { clearForm(); closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer ce paiement ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> paymentService.delete(id),
                () -> { clearForm(); closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                paymentService::findAll,
                list -> {
                    allPayments = list;
                    applyFilters();
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }

    private void applyFilters() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statusVal = statusFilter.getValue();
        LocalDate from = dateFrom.getValue();
        LocalDate to = dateTo.getValue();

        List<Payment> filtered = allPayments.stream()
                .filter(p -> {
                    if (!needle.isBlank()) {
                        String name = studentLabel(p).toLowerCase();
                        if (!name.contains(needle)) return false;
                    }
                    if (statusVal != null && !"Tous".equals(statusVal)) {
                        if (!statusLabel(p.getStatus()).equals(statusVal)) return false;
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

    private void updateFooter(List<Payment> data) {
        double total = data.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();
        footerCountLabel.setText("Total des paiements : " + data.size());
        footerTotalLabel.setText("Total Montant : " + formatAmount(total));
    }

    private void updateSummaryCards(List<Payment> data) {
        summaryCards.getChildren().clear();
        double totalAmount = data.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();

        List<Payment> pending = data.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).toList();
        double pendingAmount = pending.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();

        List<Payment> overdue = data.stream().filter(p -> p.getStatus() == PaymentStatus.OVERDUE).toList();
        double overdueAmount = overdue.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();

        summaryCards.getChildren().addAll(
                summaryCard("fth-credit-card", String.valueOf(data.size()), "Total Paiements", "#059669", "#D1FAE5"),
                summaryCard("fth-dollar-sign", formatAmount(totalAmount), "Montant Total", "#2563EB", "#DBEAFE"),
                summaryCard("fth-clock", pending.size() + " · " + formatAmount(pendingAmount), "En Attente", "#D97706", "#FEF3C7"),
                summaryCard("fth-alert-circle", overdue.size() + " · " + formatAmount(overdueAmount), "En Retard", "#DC2626", "#FEE2E2")
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
}