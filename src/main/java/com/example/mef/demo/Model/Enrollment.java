package com.example.mef.demo.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "Enrollment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    // Links to Student with database-level cascade configuration
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;

    // Tracks which classroom division the enrollment belongs to
    private String classSection; // e.g., "Section A", "Maternelle-1"

    private String academicYear; // e.g., "2026-2027"

    private double registrationFee;

    @Column(nullable = false)
    private String status; // e.g., "ACTIVE", "PENDING", "COMPLETED"

    private LocalDateTime enrollmentDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    void prePersist() {
        if (enrollmentDate == null) {
            enrollmentDate = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
