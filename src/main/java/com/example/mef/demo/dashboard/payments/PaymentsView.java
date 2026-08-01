package com.example.mef.demo.dashboard.payments;

import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Services.EnrollmentService;
import com.example.mef.demo.Services.PaymentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.PaymentStatus;
import com.example.mef.demo.enums.PaymentType;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Typed CRUD screen for the "payments" module (Payment entity). */
@Component
public class PaymentsView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PaymentService paymentService;
    private final EnrollmentService enrollmentService;

    private final ObservableList<Payment> rows = FXCollections.observableArrayList();
    private final TableView<Payment> table = new TableView<>(rows);
    { com.example.mef.demo.dashboard.common.TableStyleKit.applyTheme(table, "payments"); }

    private final ComboBox<Inscription> inscriptionField = new ComboBox<>();
    private final TextField amountField = FormFactory.textField("Montant");
    private final TextField categoryField = FormFactory.textField("Catégorie (Scolarité, Transport, ...)");
    private final ComboBox<PaymentType> methodField = new ComboBox<>(FXCollections.observableArrayList(PaymentType.values()));
    private final ComboBox<PaymentStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));

    private Payment selected;

    public PaymentsView(PaymentService paymentService, EnrollmentService enrollmentService) {
        this.paymentService = paymentService;
        this.enrollmentService = enrollmentService;
        inscriptionField.setMaxWidth(Double.MAX_VALUE);
        methodField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);
        inscriptionField.setCellFactory(cb -> inscriptionCell());
        inscriptionField.setButtonCell(inscriptionCell());
    }

    private ListCell<Inscription> inscriptionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Inscription item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                String student = item.getStudent() == null ? "?" : item.getStudent().getFirstName() + " " + item.getStudent().getLastName();
                setText(student + " (" + (item.getClassroom() == null ? "?" : item.getClassroom().getName()) + ")");
            }
        };
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Paiements");

        table.getColumns().clear();
        TableColumn<Payment, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDatePay() == null ? "" : d.getValue().getDatePay().format(DATE_FORMAT)));
        TableColumn<Payment, String> student = new TableColumn<>("Élève");
        student.setCellValueFactory(d -> new ReadOnlyStringWrapper(studentLabel(d.getValue())));
        student.setPrefWidth(160);
        TableColumn<Payment, String> amount = new TableColumn<>("Montant");
        amount.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getAmount() == null ? "" : String.format("%.2f", d.getValue().getAmount())));
        TableColumn<Payment, String> status = new TableColumn<>("Statut");
        status.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStatus() == null ? "" : d.getValue().getStatus().name()));
        table.getColumns().addAll(List.of(date, student, amount, status));

        VBox listPane = new VBox(10, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> selectRow(val));

        VBox form = buildForm();
        BorderPane layout = new BorderPane();
        layout.setCenter(listPane);
        layout.setRight(form);
        BorderPane.setMargin(form, new Insets(0, 0, 0, 16));
        form.setPrefWidth(320);

        contentPane.setCenter(layout);
        loadPickers();
        reload();
    }

    private String studentLabel(Payment payment) {
        if (payment.getInscription() == null || payment.getInscription().getStudent() == null) return "—";
        return payment.getInscription().getStudent().getFirstName() + " " + payment.getInscription().getStudent().getLastName();
    }

    private void loadPickers() {
        AsyncTasks.run(enrollmentService::findAll,
                list -> inscriptionField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des inscriptions : " + err.getMessage()));
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Inscription", inscriptionField);
        FormFactory.addRow(grid, 1, "Montant", amountField);
        FormFactory.addRow(grid, 2, "Catégorie", categoryField);
        FormFactory.addRow(grid, 3, "Méthode", methodField);
        FormFactory.addRow(grid, 4, "Statut", statusField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        return new VBox(12, new Label("Détails du paiement"), grid, new HBox(8, save, clear, delete));
    }

    private void selectRow(Payment payment) {
        selected = payment;
        if (payment == null) { clearForm(); return; }
        inscriptionField.setValue(payment.getInscription());
        amountField.setText(payment.getAmount() == null ? "" : String.valueOf(payment.getAmount()));
        categoryField.setText(payment.getLabel());
        methodField.setValue(payment.getPaymentMethod());
        statusField.setValue(payment.getStatus());
    }

    private void clearForm() {
        selected = null;
        inscriptionField.setValue(null);
        amountField.clear();
        categoryField.clear();
        methodField.setValue(null);
        statusField.setValue(null);
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
        String inscriptionId = inscriptionField.getValue().getId();

        AsyncTasks.run(
                () -> paymentService.save(payment, inscriptionId),
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer ce paiement ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> paymentService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                paymentService::findAll,
                list -> rows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}