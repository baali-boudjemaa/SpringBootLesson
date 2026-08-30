package com.example.mef.demo.Services;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Daily in-app reminder for unpaid and soon-due monthly course payments. */
@Service
public class DailyBillingReminderService {
    private final MonthlyBillingService billing;
    private final SettingService settings;

    public DailyBillingReminderService(MonthlyBillingService billing, SettingService settings) {
        this.billing = billing;
        this.settings = settings;
    }

    /** Also checks at startup, so a centre that opens the app after 08:00 is not missed. */
    @EventListener(ApplicationReadyEvent.class)
    public void checkAtStartup() {
        checkAndNotify();
    }

    /** Runs once every day at 08:00 while the desktop application is running. */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkAndNotify() {
        LocalDate today = LocalDate.now();
        if (today.toString().equals(settings.get(AppSettingsKeys.BILLING_REMINDER_LAST_DATE, ""))) return;

        int overdue = billing.findOpenDues().size();
        int upcoming = billing.findDueWithinDays(7).size();
        if (overdue == 0 && upcoming == 0) return;
        settings.set(AppSettingsKeys.BILLING_REMINDER_LAST_DATE, today.toString(),
                "Dernière notification quotidienne des échéances mensuelles");

        Platform.runLater(() -> showReminder(overdue, upcoming));
    }

    private void showReminder(int overdue, int upcoming) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("تنبيه الاستحقاقات الشهرية");
        alert.setHeaderText("يوجد استحقاقات تحتاج إلى متابعة");
        alert.setContentText("المتأخرون عن الدفع: " + overdue
                + "\nالمستحقون خلال 7 أيام: " + upcoming
                + "\n\nافتح شاشة المدفوعات ثم «Échéances» لعرض القائمة والمبالغ.");
        alert.show();
    }
}
