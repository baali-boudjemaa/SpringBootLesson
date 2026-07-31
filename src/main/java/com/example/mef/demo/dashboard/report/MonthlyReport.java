package com.example.mef.demo.dashboard.report;


import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.util.I18n;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.TextStyle;

/**
 * Builds and drives the "Monthly report" screen: month picker, stat cards
 * (income / payment count / present / absent), the generated text report,
 * and copy-to-clipboard.
 *
 * Extracted from DashboardController.showMonthlyReport() / buildReportText()
 * / monthlyStatCard(), unchanged in behavior.
 */
public final class MonthlyReport {

    private final javafx.scene.layout.BorderPane contentPane;
    private final Label pageTitleLabel;
    private final DynamicDatabaseService dao;

    public MonthlyReport(javafx.scene.layout.BorderPane contentPane,
                         Label pageTitleLabel,
                         DynamicDatabaseService dao) {
        this.contentPane = contentPane;
        this.pageTitleLabel = pageTitleLabel;
        this.dao = dao;
    }

    /** Renders the monthly report screen into contentPane. */
    public void show() {
        pageTitleLabel.setText(I18n.t("monthly.title"));

        DatePicker monthPicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
        monthPicker.setPromptText(I18n.t("monthly.select_month"));
        monthPicker.setEditable(false);

        Button generateBtn = new Button(I18n.t("monthly.generate"));
        generateBtn.getStyleClass().add("primary-button");

        HBox toolbar = new HBox(12, new Label(I18n.t("monthly.select_month") + " :"), monthPicker, generateBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label incomeNum  = new Label("—");  incomeNum.getStyleClass().add("monthly-stat-number");
        Label countNum   = new Label("—");  countNum.getStyleClass().add("monthly-stat-number");
        Label presentNum = new Label("—");  presentNum.getStyleClass().add("monthly-stat-number");
        Label absentNum  = new Label("—");  absentNum.getStyleClass().add("monthly-stat-number");

        VBox incomeCard  = monthlyStatCard("💰", incomeNum,  I18n.t("monthly.income"));
        VBox countCard   = monthlyStatCard("📋", countNum,   I18n.t("monthly.payments_count"));
        VBox presentCard = monthlyStatCard("✅", presentNum, I18n.t("monthly.present"));
        VBox absentCard  = monthlyStatCard("❌", absentNum,  I18n.t("monthly.absent"));

        HBox statsRow = new HBox(14, incomeCard, countCard, presentCard, absentCard);
        for (Node n : statsRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setWrapText(false);
        reportArea.getStyleClass().add("monthly-report-area");
        reportArea.setText(I18n.t("monthly.no_data"));

        Button copyBtn = new Button("📋  " + I18n.t("monthly.copy"));
        copyBtn.getStyleClass().add("secondary-button");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(reportArea.getText());
            cb.setContent(content);
        });

        Label reportTitle = new Label(I18n.t("monthly.report_title"));
        reportTitle.getStyleClass().add("section-title");
        Region reportSpacer = new Region();
        HBox reportHeader = new HBox(12, reportTitle, reportSpacer, copyBtn);
        HBox.setHgrow(reportSpacer, Priority.ALWAYS);
        reportHeader.setAlignment(Pos.CENTER_LEFT);

        VBox reportCard = new VBox(10, reportHeader, reportArea);
        reportCard.getStyleClass().add("monthly-card");

        generateBtn.setOnAction(e -> {
            LocalDate selected = monthPicker.getValue();
            if (selected == null) return;
            LocalDate start = selected.withDayOfMonth(1);
            LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
            String monthName = start.getMonth().getDisplayName(TextStyle.FULL, I18n.getLocale());

            generateBtn.setDisable(true);
            Task<MonthlyData> task = new Task<>() {
                @Override
                protected MonthlyData call() {
                    DynamicDatabaseService.MonthlyReportData d =
                            dao.monthlyReport(start.toString(), end.toString());
                    return new MonthlyData(d.income(), d.paymentCount(),
                            d.present(), d.absent(), d.late());
                }
            };
            task.setOnSucceeded(ev -> {
                generateBtn.setDisable(false);
                MonthlyData data = task.getValue();
                incomeNum.setText(String.format("%.2f DA", data.income()));
                countNum.setText(String.valueOf(data.paymentCount()));
                presentNum.setText(String.valueOf(data.present()));
                absentNum.setText(String.valueOf(data.absent()));

                String report = buildReportText(monthName, start.getYear(), data);
                reportArea.setText(report);
            });
            task.setOnFailed(ev -> {
                generateBtn.setDisable(false);
                reportArea.setText("Erreur : " + task.getException().getMessage());
            });
            startDaemonThread(task);
        });

        VBox root = new VBox(20, toolbar, statsRow, reportCard);
        root.setPadding(new Insets(24));
        contentPane.setCenter(root);
    }

    private String buildReportText(String month, int year, MonthlyData d) {
        String line = "═".repeat(48);
        return """
               %s
               %s %s %d
               %s

               💰 %s : %.2f DA
               📋 %s : %d

               ── %s ──
               ✅ %s : %d
               ❌ %s : %d
               🕐 %s : %d

               %s
               """.formatted(
                line,
                I18n.t("monthly.report_title"), month.toUpperCase(), year,
                line,
                I18n.t("monthly.income"), d.income(),
                I18n.t("monthly.payments_count"), d.paymentCount(),
                I18n.t("dashboard.attendance"),
                I18n.t("monthly.present"), d.present(),
                I18n.t("monthly.absent"), d.absent(),
                I18n.t("dashboard.late"), d.late(),
                line
        );
    }

    private VBox monthlyStatCard(String icon, Label valueLabel, String caption) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");
        Label captLbl = new Label(caption);
        captLbl.getStyleClass().add("monthly-stat-label");
        VBox card = new VBox(6, iconLbl, valueLabel, captLbl);
        card.getStyleClass().add("monthly-card");
        card.setPadding(new Insets(18));
        return card;
    }

    private record MonthlyData(double income, int paymentCount, int present, int absent, int late) {}

    private void startDaemonThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }
}