package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Outcoming;
import com.example.mef.demo.Repository.OutcomingRepository;
import com.example.mef.demo.enums.OutcomingCategory;
import com.example.mef.demo.enums.OutcomingFrequency;
import com.example.mef.demo.enums.PaymentStatus;
import com.example.mef.demo.enums.PaymentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Typed service backing the "outcoming" (expenses) module, including recurring templates. */
@Service
@Transactional
public class OutcomingService {

    private final OutcomingRepository outcomingRepository;

    public OutcomingService(OutcomingRepository outcomingRepository) {
        this.outcomingRepository = outcomingRepository;
    }

    /** Real expense rows only — what shows in the main table and counts toward totals. */
    @Transactional(readOnly = true)
    public List<Outcoming> findAll() {
        return outcomingRepository.findAllByRecurringFalseOrderByDateOutcomeDesc();
    }

    /** Recurring templates only. */
    @Transactional(readOnly = true)
    public List<Outcoming> findRecurringTemplates() {
        return outcomingRepository.findAllByRecurringTrueOrderByLabelAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Outcoming> findById(String id) {
        return outcomingRepository.findById(id);
    }

    /** Saves either a one-off expense or a recurring template, depending on outcoming.getRecurring(). */
    public Outcoming save(Outcoming outcoming) {
        if (outcoming.getPaymentMethod() == null) {
            outcoming.setPaymentMethod(PaymentType.CASH);
        }
        if (outcoming.getStatus() == null) {
            outcoming.setStatus(PaymentStatus.PAID);
        }
        if (outcoming.getCategory() == null) {
            outcoming.setCategory(OutcomingCategory.AUTRE);
        }
        if (Boolean.TRUE.equals(outcoming.getRecurring())) {
            if (outcoming.getFrequency() == null) {
                throw new IllegalArgumentException("Une fréquence est requise pour une dépense récurrente.");
            }
            if (outcoming.getNextOccurrenceDate() == null) {
                outcoming.setNextOccurrenceDate(
                        outcoming.getDateOutcome() != null ? outcoming.getDateOutcome() : LocalDateTime.now());
            }
            // Templates aren't spending events themselves; dateOutcome just anchors the start date.
            if (outcoming.getDateOutcome() == null) {
                outcoming.setDateOutcome(outcoming.getNextOccurrenceDate());
            }
        } else {
            if (outcoming.getDateOutcome() == null) {
                outcoming.setDateOutcome(LocalDateTime.now());
            }
            outcoming.setFrequency(null);
            outcoming.setNextOccurrenceDate(null);
        }
        return outcomingRepository.save(outcoming);
    }

    public void delete(String id) {
        outcomingRepository.deleteById(id);
    }

    /**
     * Generates every occurrence due (dateOutcome/nextOccurrenceDate in the past or now)
     * for every active recurring template, then advances each template past "now".
     * Safe to call repeatedly (e.g. every time the module is opened) — catches up on
     * any periods missed while the app was closed.
     *
     * @return how many new expense rows were generated.
     */
    public int generateDueOccurrences() {
        LocalDateTime now = LocalDateTime.now();
        List<Outcoming> templates = outcomingRepository.findAllByRecurringTrueOrderByLabelAsc();
        int created = 0;

        for (Outcoming template : templates) {
            if (template.getFrequency() == null || template.getNextOccurrenceDate() == null) {
                continue;
            }
            // Safety cap so a template that's been dormant for years doesn't flood the table.
            int guard = 0;
            while (!template.getNextOccurrenceDate().isAfter(now) && guard < 500) {
                Outcoming instance = Outcoming.builder()
                        .label(template.getLabel())
                        .amount(template.getAmount())
                        .category(template.getCategory())
                        .paymentMethod(template.getPaymentMethod())
                        .beneficiary(template.getBeneficiary())
                        .status(PaymentStatus.PENDING)
                        .dateOutcome(template.getNextOccurrenceDate())
                        .recurring(false)
                        .parentRecurringId(template.getId())
                        .build();
                outcomingRepository.save(instance);
                created++;
                guard++;

                template.setNextOccurrenceDate(advance(template.getNextOccurrenceDate(), template.getFrequency()));
            }
            outcomingRepository.save(template);
        }
        return created;
    }

    private LocalDateTime advance(LocalDateTime date, OutcomingFrequency frequency) {
        return switch (frequency) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
        };
    }

    @Transactional(readOnly = true)
    public double totalExpenseBetween(LocalDateTime start, LocalDateTime end) {
        return outcomingRepository.findAllByRecurringFalseOrderByDateOutcomeDesc().stream()
                .filter(o -> o.getDateOutcome() != null
                        && !o.getDateOutcome().isBefore(start)
                        && !o.getDateOutcome().isAfter(end))
                .mapToDouble(o -> o.getAmount() == null ? 0.0 : o.getAmount())
                .sum();
    }
}