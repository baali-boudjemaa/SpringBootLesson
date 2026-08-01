package com.example.mef.demo.Model;




import com.example.mef.demo.enums.PaymentStatus;
import com.example.mef.demo.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "Payment")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscriptionId", nullable = false)
    private Inscription inscription;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentType paymentMethod = PaymentType.CASH;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime datePay = LocalDateTime.now();

    /** Also used as the payment "category" (Scolarité, Cours, Transport, ...) shown in the UI. */
    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PAID;
}