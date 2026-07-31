package com.example.mef.demo.dashboard.common;
/**
 * Inline badge color styling shared by the dashboard's recent-payments
 * list and the generic module table's status/gender columns. Extracted
 * verbatim from DashboardController.badgeStyle.
 */
public final class Badges {

    private Badges() {
    }

    /** Returns an inline JavaFX CSS color style based on a status/gender value. */
    public static String badgeStyle(String value) {
        if (value == null) return "";
        return switch (value.toUpperCase()) {
            case "ACTIVE", "PRESENT", "PAID", "COMPLETED", "FEMALE", "FILLE" ->
                    "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;";
            case "INACTIVE", "ABSENT", "OVERDUE", "DROPPED" ->
                    "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";
            case "LATE", "PENDING" ->
                    "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;";
            case "MALE", "GARÇON" ->
                    "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF;";
            default ->
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;";
        };
    }
}