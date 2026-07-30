package com.example.mef.demo.Model;




import com.example.mef.demo.enums.PaymentType;
import jakarta.persistence.*;
        import lombok.*;
        import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "\"Payment\"")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

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

    @Column(nullable = false)
    private String label;
}