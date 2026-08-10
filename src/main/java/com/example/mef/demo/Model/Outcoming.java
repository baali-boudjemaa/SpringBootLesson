package com.example.mef.demo.Model;

import com.example.mef.demo.enums.OutcomingCategory;
import com.example.mef.demo.enums.OutcomingFrequency;
import com.example.mef.demo.enums.PaymentStatus;
import com.example.mef.demo.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * A single outgoing expense record (salaries, rent, supplies, utilities, ...).
 *
 * Also doubles as a *recurring template* when {@link #recurring} is true:
 * a template row is never counted toward expense totals itself — instead
 * OutcomingService periodically generates real (recurring = false) rows
 * from it, tagged with {@link #parentRecurringId}, based on
 * {@link #frequency} and {@link #nextOccurrenceDate}.
 */
@Getter
@Setter
@Entity
@Table(name = "Outcoming")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Outcoming {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'AUTRE'")
    @Builder.Default
    private OutcomingCategory category = OutcomingCategory.AUTRE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'CASH'")
    @Builder.Default
    private PaymentType paymentMethod = PaymentType.CASH;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dateOutcome = LocalDateTime.now();

    /** Short description of the expense (e.g. "Salaire Août", "Facture Sonelgaz"). */
    @Column(nullable = false)
    private String label;

    /** Who the money was paid to (supplier, employee, landlord, ...). Optional. */
    @Column
    private String beneficiary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'PAID'")
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PAID;

    /** True = this row is a recurring template, not a real expense on its own. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean recurring = false;

    /** Only set when recurring = true. */
    @Enumerated(EnumType.STRING)
    @Column
    private OutcomingFrequency frequency;

    /**
     * For a template: the next date an occurrence should be generated.
     * Advances automatically each time an occurrence is generated.
     */
    @Column
    private LocalDateTime nextOccurrenceDate;

    /** For a generated occurrence: the id of the template it came from. Null for manual one-off expenses. */
    @Column
    private String parentRecurringId;
}