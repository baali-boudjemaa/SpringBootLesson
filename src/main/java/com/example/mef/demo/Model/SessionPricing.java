package com.example.mef.demo.Model;

import com.example.mef.demo.enums.SessionName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(
        name = "\"SessionPricing\"",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "session")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SessionPricing  {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private SessionName session;

    @Column(nullable = false)
    private Double monthlyFee;
}