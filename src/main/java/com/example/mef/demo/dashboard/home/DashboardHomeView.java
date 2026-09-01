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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.kordamp.ikonli.javafx.FontIcon;

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
        pageTitleLabel.setText(I18n.t("nav.dashboard", "تسجيل الحضور"));
        Label loading = new Label(I18n.t("table.loading", "تسجيل الحضور"));
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
                statCard("fth-users",       String.valueOf(d.students),  I18n.t("dashboard.students", "تسجيل الحضور"),  "#2563EB", "#DBEAFE"),
                statCard("fth-briefcase",   String.valueOf(d.teachers),  I18n.t("dashboard.teachers", "تسجيل الحضور"),  "#7C3AED", "#EDE9FE"),
                statCard("fth-layout",      String.valueOf(d.classes),   I18n.t("dashboard.classes", "تسجيل الحضور"),   "#0D9488", "#CCFBF1"),
                statCard("fth-credit-card", String.valueOf(d.payments),  I18n.t("dashboard.payments", "تسجيل الحضور"),  "#059669", "#D1FAE5")
        );
        for (Node n : statsRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // ── Attendance pie chart ─────────────────────────────────
        int present = d.attendance.getOrDefault("PRESENT", 0);
        int absent  = d.attendance.getOrDefault("ABSENT", 0);
        int late    = d.attendance.getOrDefault("LATE",   0);

        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data(I18n.t("dashboard.present", "تسجيل الحضور") + " (" + present + ")", Math.max(present, 0.01)),
                new PieChart.Data(I18n.t("dashboard.absent", "تسجيل الحضور")  + " (" + absent  + ")", Math.max(absent, 0.01)),
                new PieChart.Data(I18n.t("dashboard.late", "تسجيل الحضور")    + " (" + late    + ")", Math.max(late, 0.01))
        ));
        chart.setTitle(I18n.t("dashboard.attendance", "تسجيل الحضور"));
        chart.setLegendVisible(true);
        chart.setPrefHeight(240);

        VBox chartCard = new VBox(8, chart);
        chartCard.getStyleClass().add("monthly-card");
        chartCard.setPrefWidth(320);

        // ── Total revenue card ───────────────────────────────────
        NumberAxis xAxis = new NumberAxis();
        xAxis.setTickLabelsVisible(false); xAxis.setMinorTickVisible(false); xAxis.setTickMarkVisible(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(false); yAxis.setMinorTickVisible(false); yAxis.setTickMarkVisible(false);
        LineChart<Number,Number> revenueChart = new LineChart<>(xAxis,yAxis);
        revenueChart.setLegendVisible(false);
        revenueChart.setPrefHeight(160);
        revenueChart.setHorizontalGridLinesVisible(false);
        revenueChart.setVerticalGridLinesVisible(false);
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>(1, 10));
        series.getData().add(new XYChart.Data<>(2, 20));
        series.getData().add(new XYChart.Data<>(3, 15));
        series.getData().add(new XYChart.Data<>(4, 30));
        series.getData().add(new XYChart.Data<>(5, 25));
        series.getData().add(new XYChart.Data<>(6, 45));
        series.getData().add(new XYChart.Data<>(7, 35));
        series.getData().add(new XYChart.Data<>(8, 60));
        revenueChart.getData().add(series);

        VBox revenueCard = new VBox(8,
                new Label(I18n.t("dashboard.monthly_income", "تسجيل الحضور")),
                labelWith(String.format("%.2f DA", d.totalPayments), "stat-number"),
                revenueChart
        );
        revenueCard.getStyleClass().add("monthly-card");
        revenueCard.setPadding(new Insets(20));
        ((Label) revenueCard.getChildren().get(0)).getStyleClass().add("section-title");
        HBox.setHgrow(revenueCard, Priority.ALWAYS);

        HBox middleRow = new HBox(14, chartCard, revenueCard);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        HBox.setHgrow(revenueCard, Priority.ALWAYS);

        // ── Recent payments ──────────────────────────────────────
        Label recentTitle = new Label(I18n.t("dashboard.recent_payments", "تسجيل الحضور"));
        recentTitle.getStyleClass().add("section-title");

        VBox recentList = new VBox(8);
        if (d.recentPayments.isEmpty()) {
            recentList.getChildren().add(new Label(I18n.t("dashboard.no_payments", "تسجيل الحضور")));
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

    private HBox statCard(String icon, String value, String label, String accentColor, String iconBg) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(22);
        fontIcon.setStyle("-fx-icon-color: " + accentColor + ";");

        StackPane iconWrap = new StackPane(fontIcon);
        iconWrap.getStyleClass().add("stat-icon-wrap");
        iconWrap.setStyle("-fx-background-color: " + iconBg + ";");

        Label valLbl  = new Label(value);
        valLbl.getStyleClass().add("stat-number");
        Label captLbl = new Label(label);
        captLbl.getStyleClass().add("stat-caption");

        VBox text = new VBox(2, valLbl, captLbl);
        HBox card = new HBox(14, iconWrap, text);
        card.setAlignment(Pos.CENTER_LEFT);
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