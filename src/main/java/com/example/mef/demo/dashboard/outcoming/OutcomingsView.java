package com.example.mef.demo.dashboard.outcoming;

import com.example.mef.demo.Model.Outcoming;
import com.example.mef.demo.Services.OutcomingService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.enums.OutcomingCategory;
import com.example.mef.demo.enums.OutcomingFrequency;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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

/** Typed CRUD screen for the "outcoming" module (Outcoming entity) — expenses / money going out, with recurring support. */
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

    private final TextField searchField = FormFactory.textField("Rechercher par libellé ou bénéficiaire...");
    private final ComboBox<String> statusFilter = new ComboBox<>(
            FXCollections.observableArrayList("Tous", "PAYÉ", "EN ATTENTE", "EN RETARD"));
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final DatePicker dateFrom = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 1, 1));
    private final DatePicker dateTo = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 12, 31));

    private final TextField labelField = FormFactory.textField("Libellé (ex: Salaire Août, Facture Sonelgaz)");
    private final TextField amountField = FormFactory.textField("Montant");
    private final TextField beneficiaryField = FormFactory.textField("Bénéficiaire (fournisseur, employé, ...)");
    private final ComboBox<OutcomingCategory> categoryField = new ComboBox<>(FXCollections.observableArrayList(OutcomingCategory.values()));
    private final ComboBox<PaymentType> methodField = new ComboBox<>(FXCollections.observableArrayList(PaymentType.values()));
    private final ComboBox<PaymentStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
    private final DatePicker outcomingDateField = new DatePicker();

    private final CheckBox recurringCheck = new CheckBox("Dépense récurrente");
    private final ComboBox<OutcomingFrequency> frequencyField = new ComboBox<>(FXCollections.observableArrayList(OutcomingFrequency.values()));
    private final Label startDateLabel = new Label("Date de début");

    private final Label footerCountLabel = new Label();
    private final Label footerTotalLabel = new Label();
    private final HBox summaryCards = new HBox(14);

    private List<Outcoming> allOutcomings = List.of();
    private Outcoming selected;
    private BorderPane layout;
    private VBox form;

    public OutcomingsView(OutcomingService outcomingService) {
        this.outcomingService = outcomingService;
        statusFilter.setValue("Tous");
        categoryFilter.setItems(FXCollections.observableArrayList(
                "Toutes", "Salaires", "Loyer", "Fournitures", "Électricité", "Eau", "Maintenance", "Nourriture", "Transport", "Autre"));
        categoryFilter.setValue("Toutes");
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
        pageTitleLabel.setText("Sorties");

        buildColumns();
        buildRecurringColumns();

        Label subtitle = new Label("Gérer les dépenses et sorties d'argent");
        subtitle.getStyleClass().add("page-subtitle");

        searchField.getStyleClass().add("filter-field");
        statusFilter.getStyleClass().add("filter-field");
        statusFilter.setPrefWidth(140);
        categoryFilter.getStyleClass().add("filter-field");
        categoryFilter.setPrefWidth(150);

        Button addBtn = new Button("+  Nouvelle Sortie");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> startCreate());

        HBox filters = new HBox(10,
                labeledFilter("Du", dateFrom),
                labeledFilter("Au", dateTo),
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

        Label recurringTitle = new Label("Dépenses récurrentes");
        recurringTitle.getStyleClass().add("section-title");
        VBox recurringBlock = new VBox(8, recurringTitle, recurringTable);

        VBox center = new VBox(16, subtitle, toolbar, tableBlock, summaryCards, recurringBlock);
        center.setPadding(new Insets(24));
        VBox.setVgrow(tableBlock, Priority.ALWAYS);

        form = buildForm();

        layout = new BorderPane();
        layout.setCenter(center);

        contentPane.setCenter(layout);
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
    }

    private void buildColumns() {
        table.getColumns().clear();

        TableColumn<Outcoming, String> date = new TableColumn<>("DATE");
        date.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateOutcome() == null ? "—" : d.getValue().getDateOutcome().format(DATE_FORMAT)));

        TableColumn<Outcoming, String> label = new TableColumn<>("LIBELLÉ");
        label.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getLabel()));
        label.setPrefWidth(170);
        label.setCellFactory(col -> recurringAwareLabelCell());

        TableColumn<Outcoming, String> beneficiary = new TableColumn<>("BÉNÉFICIAIRE");
        beneficiary.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getBeneficiary() == null || d.getValue().getBeneficiary().isBlank()
                        ? "—" : d.getValue().getBeneficiary()));
        beneficiary.setPrefWidth(150);

        TableColumn<Outcoming, String> category = new TableColumn<>("CATÉGORIE");
        category.setCellValueFactory(d -> new ReadOnlyStringWrapper(categoryLabel(d.getValue().getCategory())));
        category.setCellFactory(col -> categoryPillCell());

        TableColumn<Outcoming, String> amount = new TableColumn<>("MONTANT");
        amount.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatAmount(d.getValue().getAmount())));

        TableColumn<Outcoming, String> method = new TableColumn<>("MÉTHODE");
        method.setCellValueFactory(d -> new ReadOnlyStringWrapper(methodLabel(d.getValue().getPaymentMethod())));

        TableColumn<Outcoming, PaymentStatus> status = new TableColumn<>("STATUT");
        status.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getStatus()));
        status.setCellFactory(col -> statusCell());

        TableColumn<Outcoming, Outcoming> actions = new TableColumn<>("ACTION");
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

        TableColumn<Outcoming, String> label = new TableColumn<>("LIBELLÉ");
        label.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getLabel()));
        label.setPrefWidth(180);

        TableColumn<Outcoming, String> amount = new TableColumn<>("MONTANT");
        amount.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatAmount(d.getValue().getAmount())));

        TableColumn<Outcoming, String> frequency = new TableColumn<>("FRÉQUENCE");
        frequency.setCellValueFactory(d -> new ReadOnlyStringWrapper(frequencyLabel(d.getValue().getFrequency())));
        frequency.setCellFactory(col -> categoryPillCell());

        TableColumn<Outcoming, String> next = new TableColumn<>("PROCHAINE ÉCHÉANCE");
        next.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getNextOccurrenceDate() == null ? "—" : d.getValue().getNextOccurrenceDate().format(DATE_FORMAT)));

        TableColumn<Outcoming, Outcoming> actions = new TableColumn<>("ACTION");
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
                Button del = iconBtn("fth-trash-2", "Arrêter cette récurrence");
                del.getStyleClass().add("icon-action-danger");
                del.setOnAction(e -> {
                    if (!DialogUtil.confirm("Confirmer",
                            "Arrêter cette dépense récurrente ? Les sorties déjà générées ne seront pas supprimées.")) {
                        return;
                    }
                    String id = item.getId();
                    AsyncTasks.run(
                            () -> outcomingService.delete(id),
                            () -> reload(),
                            err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
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
            case SALAIRES -> "Salaires";
            case LOYER -> "Loyer";
            case FOURNITURES -> "Fournitures";
            case ELECTRICITE -> "Électricité";
            case EAU -> "Eau";
            case MAINTENANCE -> "Maintenance";
            case NOURRITURE -> "Nourriture";
            case TRANSPORT -> "Transport";
            case AUTRE -> "Autre";
        };
    }

    private static String frequencyLabel(OutcomingFrequency frequency) {
        if (frequency == null) return "—";
        return switch (frequency) {
            case DAILY -> "Chaque jour";
            case WEEKLY -> "Chaque semaine";
            case MONTHLY -> "Chaque mois";
            case QUARTERLY -> "Tous les 3 mois";
        };
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
        FormFactory.addRow(grid, 0, "Libellé *", labelField);
        FormFactory.addRow(grid, 1, "Montant *", amountField);
        FormFactory.addRow(grid, 2, "Catégorie", categoryField);
        FormFactory.addRow(grid, 3, "Bénéficiaire", beneficiaryField);
        FormFactory.addRow(grid, 4, "Méthode", methodField);
        FormFactory.addRow(grid, 5, "Statut", statusField);
        FormFactory.addRow(grid, 6, startDateLabel.getText(), outcomingDateField);
        FormFactory.addRow(grid, 7, recurringCheck);
        FormFactory.addRow(grid, 8, "Fréquence", frequencyField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("+ Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> startCreate());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        Label title = new Label("Détails de la sortie");
        title.getStyleClass().add("section-title");

        VBox panel = new VBox(12, title, grid, new HBox(8, save, clear, delete));
        panel.getStyleClass().add("side-panel");
        panel.setPrefWidth(340);
        return panel;
    }

    private void startCreate() {
        clearForm();
        showFormPanel();
    }

    private void showFormPanel() {
        layout.setRight(form);
        BorderPane.setMargin(form, new Insets(0, 24, 24, 0));
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
            DialogUtil.error("Champs requis", "Le libellé et le montant sont obligatoires.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            DialogUtil.error("Valeur invalide", "Le montant doit être un nombre.");
            return;
        }
        if (recurringCheck.isSelected() && frequencyField.getValue() == null) {
            DialogUtil.error("Champs requis", "Choisissez une fréquence pour une dépense récurrente.");
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
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer cette sortie ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> outcomingService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                () -> outcomingService.generateDueOccurrences(),
                this::loadLists,
                err -> DialogUtil.error("Erreur", "Échec de la génération des sorties récurrentes : " + err.getMessage())
        );
    }

    private void loadLists() {
        AsyncTasks.run(
                () -> outcomingService.findAll(),
                list -> {
                    allOutcomings = list;
                    applyFilters();
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
        AsyncTasks.run(
                () -> outcomingService.findRecurringTemplates(),
                list -> recurringRows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement des récurrences : " + err.getMessage())
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
                    if (statusVal != null && !"Tous".equals(statusVal)) {
                        if (!statusLabel(o.getStatus()).equals(statusVal)) return false;
                    }
                    if (categoryVal != null && !"Toutes".equals(categoryVal)) {
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
        footerCountLabel.setText("Total des sorties : " + data.size());
        footerTotalLabel.setText("Total Montant : " + formatAmount(total));
    }

    private void updateSummaryCards(List<Outcoming> data) {
        summaryCards.getChildren().clear();
        double totalAmount = data.stream().mapToDouble(o -> o.getAmount() == null ? 0 : o.getAmount()).sum();

        List<Outcoming> pending = data.stream().filter(o -> o.getStatus() == PaymentStatus.PENDING).toList();
        double pendingAmount = pending.stream().mapToDouble(o -> o.getAmount() == null ? 0 : o.getAmount()).sum();

        List<Outcoming> overdue = data.stream().filter(o -> o.getStatus() == PaymentStatus.OVERDUE).toList();
        double overdueAmount = overdue.stream().mapToDouble(o -> o.getAmount() == null ? 0 : o.getAmount()).sum();

        summaryCards.getChildren().addAll(
                summaryCard("fth-trending-down", String.valueOf(data.size()), "Total Sorties", "#DC2626", "#FEE2E2"),
                summaryCard("fth-dollar-sign", formatAmount(totalAmount), "Montant Total", "#DC2626", "#FEE2E2"),
                summaryCard("fth-clock", pending.size() + " · " + formatAmount(pendingAmount), "En Attente", "#D97706", "#FEF3C7"),
                summaryCard("fth-alert-circle", overdue.size() + " · " + formatAmount(overdueAmount), "En Retard", "#B91C1C", "#FECACA")
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