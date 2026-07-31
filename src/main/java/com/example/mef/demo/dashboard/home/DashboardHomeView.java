package com.example.mef.demo.dashboard.home;

import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.Badges;
import com.example.mef.demo.util.I18n;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * The dashboard home screen: stat cards, attendance pie chart, monthly
 * income, and recent payments list. Extracted verbatim (behavior
 * unchanged) from DashboardController.showDashboard / buildDashboardUI /
 * statCard / labelWith.
 */
@Component
public class DashboardHomeView {

    @Autowired
    private DynamicDatabaseService dao;

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText(I18n.t("nav.dashboard"));
        Label loading = new Label(I18n.t("table.loading"));
        contentPane.setCenter(loading);

        AsyncTasks.run(
                () -> loadDashboardData(),
                data -> buildDashboardUI(contentPane, data),
                err -> contentPane.setCenter(new Label("Erreur lors du chargement."))
        );
    }

    private DashboardData loadDashboardData() {
        long students  = safeCount("students");
        long teachers  = safeCount("teachers");
        long classes   = safeCount("classes");
        long payments  = safeCount("payments");
        double total   = dao.sum("payments", "amount");
        Map<String, Integer> attendance = dao.attendanceSummary();
        List<Map<String, String>> recent = safeFind("payments",
                List.of("payment_date", "student_name", "amount", "method", "status"),
                "payment_date DESC", 5);
        return new DashboardData(students, teachers, classes, payments, total, attendance, recent);
    }

    private void buildDashboardUI(BorderPane contentPane, DashboardData d) {
        // ── Top stat cards ──────────────────────────────────────
        HBox statsRow = new HBox(14,
                statCard("👶", String.valueOf(d.students),  I18n.t("dashboard.students"), "#4F46E5"),
                statCard("👨‍🏫", String.valueOf(d.teachers), I18n.t("dashboard.teachers"), "#7C3AED"),
                statCard("🏫", String.valueOf(d.classes),   I18n.t("dashboard.classes"),  "#0F766E"),
                statCard("💳", String.valueOf(d.payments),  I18n.t("dashboard.payments"), "#15803D")
        );
        for (Node n : statsRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // ── Attendance pie chart ─────────────────────────────────
        int present = d.attendance.getOrDefault("PRESENT", 0);
        int absent  = d.attendance.getOrDefault("ABSENT", 0);
        int late    = d.attendance.getOrDefault("LATE",   0);

        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data(I18n.t("dashboard.present") + " (" + present + ")", Math.max(present, 0.01)),
                new PieChart.Data(I18n.t("dashboard.absent")  + " (" + absent  + ")", Math.max(absent, 0.01)),
                new PieChart.Data(I18n.t("dashboard.late")    + " (" + late    + ")", Math.max(late, 0.01))
        ));
        chart.setTitle(I18n.t("dashboard.attendance"));
        chart.setLegendVisible(true);
        chart.setPrefHeight(240);

        VBox chartCard = new VBox(8, chart);
        chartCard.getStyleClass().add("monthly-card");
        chartCard.setPrefWidth(320);

        // ── Total revenue card ───────────────────────────────────
        VBox revenueCard = new VBox(8,
                new Label(I18n.t("dashboard.monthly_income")),
                labelWith(String.format("%.2f DA", d.totalPayments), "stat-number")
        );
        revenueCard.getStyleClass().add("monthly-card");
        revenueCard.setPadding(new Insets(20));
        ((Label) revenueCard.getChildren().get(0)).getStyleClass().add("section-title");
        HBox.setHgrow(revenueCard, Priority.ALWAYS);

        HBox middleRow = new HBox(14, chartCard, revenueCard);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        HBox.setHgrow(revenueCard, Priority.ALWAYS);

        // ── Recent payments ──────────────────────────────────────
        Label recentTitle = new Label(I18n.t("dashboard.recent_payments"));
        recentTitle.getStyleClass().add("section-title");

        VBox recentList = new VBox(8);
        if (d.recentPayments.isEmpty()) {
            recentList.getChildren().add(new Label(I18n.t("dashboard.no_payments")));
        } else {
            for (Map<String, String> row : d.recentPayments) {
                String name   = row.getOrDefault("student_name", "—");
                String amount = row.getOrDefault("amount", "0");
                String date   = row.getOrDefault("payment_date", "");
                String status = row.getOrDefault("status", "");

                Label nameLbl   = new Label(name);
                nameLbl.setStyle("-fx-font-weight: bold;");
                Label amountLbl = new Label(amount + " DA");
                amountLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #15803D;");
                Label dateLbl   = new Label(date);
                dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
                Label statusLbl = new Label(status);
                statusLbl.setStyle(Badges.badgeStyle(status) +
                        "-fx-padding: 1 8 1 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row2 = new HBox(8, nameLbl, dateLbl, spacer, amountLbl, statusLbl);
                row2.setAlignment(Pos.CENTER_LEFT);
                row2.getStyleClass().add("recent-payment-row");
                recentList.getChildren().add(row2);
            }
        }

        VBox root = new VBox(20, statsRow, middleRow, recentTitle, recentList);
        root.setPadding(new Insets(24));
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentPane.setCenter(scroll);
    }

    private VBox statCard(String icon, String value, String label, String accentColor) {
        Label iconLbl  = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 24px;");
        Label valLbl   = new Label(value);
        valLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");
        Label captLbl  = new Label(label);
        captLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        VBox card = new VBox(6, iconLbl, valLbl, captLbl);
        card.getStyleClass().add("stat-box");
        card.setPadding(new Insets(18));
        card.setMinWidth(150);
        return card;
    }

    private Label labelWith(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }

    private long safeCount(String table) {
        try { return dao.count(table); } catch (Exception e) { return 0; }
    }

    private List<Map<String, String>> safeFind(String table, List<String> cols, String order, int limit) {
        try {
            List<Map<String, String>> all = dao.findAll(table, cols, order);
            return all.size() > limit ? all.subList(0, limit) : all;
        } catch (Exception e) { return List.of(); }
    }

    private record DashboardData(
            long students, long teachers, long classes, long payments,
            double totalPayments, Map<String, Integer> attendance,
            List<Map<String, String>> recentPayments) {}
}