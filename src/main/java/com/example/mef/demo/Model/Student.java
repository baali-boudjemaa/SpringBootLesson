package com.example.mef.demo.Model;

import com.example.mef.demo.enums.Sexe;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String studentNumber;

    private String firstName;

    private String lastName;

    private LocalDateTime dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Sexe gender;

    @Column(columnDefinition = "TEXT")
    private String medicalInfo;

    private LocalDateTime enrollmentDate;

    /** Blood type, e.g. "A+", "O-". Shown as the "Groupage" badge in the students table. */
    private String bloodType;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    void prePersist() {
        if (studentNumber == null || studentNumber.isBlank()) {
            studentNumber = "STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (enrollmentDate == null) {
            enrollmentDate = LocalDateTime.now();
        }
    }
}
