package com.example.mef.demo.Model;

import com.example.mef.demo.enums.EmployeeRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "Employee")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String employeeNumber;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    @Pattern(
            regexp = "^(05|06|07)\\d{8}$",
            message = "Numéro de téléphone invalide. Doit commencer par 05, 06 ou 07 et contenir 10 chiffres."
    )
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private EmployeeRole role;

    @Column(columnDefinition = "TEXT")
    private String certifications;

    /** Comma-separated day names this teacher works (e.g. "Lundi,Mardi,Mercredi"). Null/blank = no restriction. */
    @Column(columnDefinition = "TEXT")
    private String workingDays;

    /** Start of this teacher's daily working window, e.g. "08:00". Null/blank = no restriction. */
    @Column
    private String workStartTime;

    /** End of this teacher's daily working window, e.g. "17:00". Null/blank = no restriction. */
    @Column
    private String workEndTime;

    /** Per-day availability, e.g. "Lundi 08:00-10:00; Mardi 14:00-15:00". */
    @Column(columnDefinition = "TEXT")
    private String availabilitySchedule;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<TeacherAvailabilitySlot> availabilitySlots = new ArrayList<>();

    public void replaceAvailabilitySlots(List<TeacherAvailabilitySlot> slots) {
        availabilitySlots.clear();
        slots.forEach(slot -> {
            slot.setTeacher(this);
            availabilitySlots.add(slot);
        });
    }

    @PrePersist
    void prePersist() {
        if (employeeNumber == null || employeeNumber.isBlank()) {
            employeeNumber = "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (role == null) {
            role = EmployeeRole.TEACHER;
        }
    }

}
