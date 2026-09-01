package com.example.mef.demo.Services;

import com.example.mef.demo.Model.*;
import com.example.mef.demo.Repository.InscriptionRepository;
import com.example.mef.demo.Repository.PaymentRepository;
import com.example.mef.demo.Repository.StudentCourseSubscriptionRepository;
import com.example.mef.demo.enums.EnrollmentStatus;
import com.example.mef.demo.enums.Category;
import com.example.mef.demo.enums.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Computes the monthly course dues without creating artificial unpaid payments. */
@Service
@Transactional(readOnly = true)
public class MonthlyBillingService {
    public record Due(Inscription inscription, LocalDate dueDate, double amount, double paidAmount) {
        public double remainingAmount() { return Math.max(0, amount - paidAmount); }
        public boolean isPaid() { return remainingAmount() < 0.005; }
        public boolean isOverdue(LocalDate today) { return !isPaid() && dueDate.isBefore(today); }
    }

    private final InscriptionRepository inscriptions;
    private final StudentCourseSubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final SettingService settings;

    public MonthlyBillingService(InscriptionRepository inscriptions,
                                 StudentCourseSubscriptionRepository subscriptions,
                                 PaymentRepository payments,
                                 SettingService settings) {
        this.inscriptions = inscriptions;
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.settings = settings;
    }

    public List<Due> findOpenDues() {
        LocalDate today = LocalDate.now();
        List<Inscription> all = inscriptions.findAllWithDetails();
        // Bulk-load all payments in a single query, then group by inscription ID.
        Map<String, List<Payment>> paymentsByInscription = bulkPayments(all);
        List<Due> result = new ArrayList<>();
        for (Inscription inscription : all) {
            if (inscription.getStatus() != EnrollmentStatus.ACTIVE) continue;
            LocalDate enrolled = billingStart(inscription);
            if (enrolled == null || enrolled.isAfter(today)) continue;
            LocalDate dueDate = dueInMonth(inscription, YearMonth.from(today));
            if (dueDate.isAfter(today)) dueDate = dueInMonth(inscription, YearMonth.from(today.minusMonths(1)));
            double amount = amountDueOn(inscription, dueDate);
            if (amount <= 0) continue;
            double paid = paidFor(inscription, dueDate, paymentsByInscription);
            Due due = new Due(inscription, dueDate, amount, paid);
            if (!due.isPaid()) result.add(due);
        }
        return result.stream().sorted(Comparator.comparing(Due::dueDate).thenComparing(d -> studentName(d.inscription()))).toList();
    }

    public List<Due> findDueWithinDays(int days) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        List<Inscription> all = inscriptions.findAllWithDetails();
        // Bulk-load all payments in a single query, then group by inscription ID.
        Map<String, List<Payment>> paymentsByInscription = bulkPayments(all);
        List<Due> result = new ArrayList<>();
        for (Inscription inscription : all) {
            if (inscription.getStatus() != EnrollmentStatus.ACTIVE) continue;
            LocalDate enrolled = billingStart(inscription);
            if (enrolled == null) continue;
            LocalDate due = dueInMonth(inscription, YearMonth.from(today));
            if (due.isBefore(today) || due.isAfter(end)) continue;
            double amount = amountDueOn(inscription, due);
            if (amount <= 0) continue;
            Due item = new Due(inscription, due, amount, paidFor(inscription, due, paymentsByInscription));
            if (!item.isPaid()) result.add(item);
        }
        return result.stream().sorted(Comparator.comparing(Due::dueDate).thenComparing(d -> studentName(d.inscription()))).toList();
    }

    /** The payment cycle that is currently payable for one enrollment. */
    public Due currentCycleDue(Inscription inscription) {
        LocalDate today = LocalDate.now();
        LocalDate enrolled = billingStart(inscription);
        if (enrolled == null || enrolled.isAfter(today)) return null;
        LocalDate dueDate = dueInMonth(inscription, YearMonth.from(today));
        if (dueDate.isAfter(today)) dueDate = dueInMonth(inscription, YearMonth.from(today.minusMonths(1)));
        double amount = amountDueOn(inscription, dueDate);
        Map<String, List<Payment>> paymentsByInscription = bulkPayments(List.of(inscription));
        return new Due(inscription, dueDate, amount, paidFor(inscription, dueDate, paymentsByInscription));
    }

    private double amountDueOn(Inscription inscription, LocalDate dueDate) {
        if (inscription.getClassroom() != null && inscription.getClassroom().getCategory() == Category.CRECHE) {
            return crecheMonthlyAmount(inscription, YearMonth.from(dueDate));
        }
        List<StudentCourseSubscription> records = subscriptions.findByInscriptionIdWithCourse(inscription.getId());
        if (records.isEmpty()) { // Existing data created before subscriptions were introduced.
            return inscription.getCourses().stream().mapToDouble(c -> fee(c)).sum();
        }
        return records.stream().filter(s -> s.isActiveOn(dueDate)).mapToDouble(s -> fee(s.getCourse())).sum();
    }

    /**
     * Computes the amount already paid for {@code dueDate} using a pre-fetched
     * payment map to avoid N+1 queries.
     */
    private double paidFor(Inscription inscription, LocalDate dueDate,
                           Map<String, List<Payment>> paymentsByInscription) {
        LocalDate nextDue = dueInMonth(inscription, YearMonth.from(dueDate).plusMonths(1));
        List<Payment> inscriptionPayments = paymentsByInscription
                .getOrDefault(inscription.getId(), List.of());
        return inscriptionPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getBillingDueDate() != null
                        ? dueDate.equals(p.getBillingDueDate())
                        : p.getDatePay() != null && !p.getDatePay().toLocalDate().isBefore(dueDate)
                          && p.getDatePay().toLocalDate().isBefore(nextDue))
                .mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();
    }

    /**
     * Loads all payments for the given inscriptions in a single SQL query and
     * returns them grouped by inscription ID. This is the key to avoiding N+1
     * selects in {@link #findOpenDues()} and {@link #findDueWithinDays(int)}.
     */
    private Map<String, List<Payment>> bulkPayments(List<Inscription> inscriptionList) {
        List<String> ids = inscriptionList.stream().map(Inscription::getId).toList();
        if (ids.isEmpty()) return Map.of();
        return payments.findByInscriptionIdIn(ids).stream()
                .collect(Collectors.groupingBy(p -> p.getInscription().getId()));
    }

    private static LocalDate billingStart(Inscription inscription) {
        if (inscription.getDateInscription() != null) return inscription.getDateInscription().toLocalDate();
        return inscription.getStartDate();
    }

    private LocalDate dueInMonth(Inscription inscription, YearMonth month) {
        if (inscription.getClassroom() != null && inscription.getClassroom().getCategory() == Category.CRECHE) {
            int dueDay = settings.getInt(EnrollmentSettingsKeys.CRECHE_DUE_DAY, EnrollmentSettingsKeys.CRECHE_DUE_DAY_DEFAULT);
            return month.atDay(Math.min(Math.max(1, dueDay), month.lengthOfMonth()));
        }
        LocalDate anchor = billingStart(inscription);
        return month.atDay(Math.min(anchor.getDayOfMonth(), month.lengthOfMonth()));
    }

    private double crecheMonthlyAmount(Inscription inscription, YearMonth month) {
        String feeKey = EnrollmentSettingsKeys.CRECHE_DAY_BY_DAY_FEE;
        if (inscription.getAttendancePlan() != null && inscription.getAttendancePlan() != com.example.mef.demo.enums.AttendancePlan.CUSTOM_DAYS) {
            if (inscription.getSession() == com.example.mef.demo.enums.SessionName.MATINEE) {
                feeKey = EnrollmentSettingsKeys.CRECHE_HALF_DAY_FEE;
            } else if (inscription.getSession() == com.example.mef.demo.enums.SessionName.MATINEE_AVEC_REPAS) {
                feeKey = EnrollmentSettingsKeys.CRECHE_HALF_DAY_LUNCH_FEE;
            } else if (inscription.getSession() == com.example.mef.demo.enums.SessionName.JOURNEE_COMPLETE) {
                feeKey = EnrollmentSettingsKeys.CRECHE_FULL_DAY_FEE;
            }
        }
        
        double dailyFee;
        try { dailyFee = Double.parseDouble(settings.get(feeKey, "0")); }
        catch (NumberFormatException ex) { return 0; }
        if (dailyFee <= 0 || inscription.getAttendanceDays() == null) return 0;
        java.util.Set<java.time.DayOfWeek> days = java.util.Arrays.stream(inscription.getAttendanceDays().split(","))
                .map(String::trim).filter(s -> !s.isBlank()).map(s -> {
                    try { return java.time.DayOfWeek.valueOf(s); } catch (IllegalArgumentException ex) { return null; }
                }).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        long count = month.atDay(1).datesUntil(month.plusMonths(1).atDay(1))
                .filter(day -> days.contains(day.getDayOfWeek())).count();
        return count * dailyFee;
    }

    private static double fee(Course course) { return course.getMonthlyFee() == null ? 0 : course.getMonthlyFee(); }
    public static String studentName(Inscription inscription) {
        Student s = inscription.getStudent();
        return s == null ? "—" : (s.getFirstName() + " " + s.getLastName()).trim();
    }
}
