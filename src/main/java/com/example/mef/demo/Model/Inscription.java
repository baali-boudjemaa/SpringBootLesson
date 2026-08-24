package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.enums.AttendancePlan;
import com.example.mef.demo.enums.EnrollmentStatus;
import com.example.mef.demo.enums.SessionName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
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

    /** Optional now: a student's real attendance is driven by their courses (each course
     *  already has its own classroom), so an inscription no longer requires a single fixed
     *  classroom — a student can follow courses held in different classrooms. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classId")
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

    /** Day the child starts attending (from the enrollment wizard). */
    @Column
    private LocalDate startDate;

    /** Attendance plan chosen in the enrollment wizard. */
    @Enumerated(EnumType.STRING)
    @Column
    private AttendancePlan attendancePlan;

    /** Comma-separated {@link java.time.DayOfWeek} names the child attends, e.g. "MONDAY,WEDNESDAY,FRIDAY". */
    @Column
    private String attendanceDays;

    @OneToMany(
            mappedBy = "inscription",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    /** Support subjects (Course entities) the student follows, when enrolled in a SOUTIEN classroom. */
    @ManyToMany
    @JoinTable(
            name = "inscription_course",
            joinColumns = @JoinColumn(name = "inscriptionId"),
            inverseJoinColumns = @JoinColumn(name = "courseId")
    )
    @Builder.Default
    private List<Course> courses = new ArrayList<>();
}