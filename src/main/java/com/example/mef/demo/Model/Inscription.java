package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.enums.EnrollmentStatus;
import com.example.mef.demo.enums.SessionName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Inscription")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Inscription  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studentId", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anneeScolaireId", nullable = false)
    private AnneeScolaire anneeScolaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classId", nullable = false)
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'JOURNEE_COMPLETE'")
    @Builder.Default
    private SessionName session = SessionName.JOURNEE_COMPLETE;

    @Column(nullable = false, columnDefinition = "timestamp default now()")
    @Builder.Default
    private LocalDateTime dateInscription = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ACTIVE'")
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @OneToMany(
            mappedBy = "inscription",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}