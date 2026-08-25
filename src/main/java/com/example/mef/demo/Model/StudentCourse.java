package com.example.mef.demo.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

/**
 * Join entity linking Student to Course with enrollment metadata.
 * Allows flexible multi-course enrollment per student.
 *
 * This model replaces the old system where students were linked only to classrooms.
 * Now students can be enrolled in multiple courses at different times/schedules.
 */
@Entity
@Table(name = "student_course", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "course_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Course course;

    /**
     * Enrollment status for this specific course
     * Possible values: ACTIVE, PENDING, COMPLETED, DROPPED
     */
    @Column(nullable = false)
    private String enrollmentStatus = "ACTIVE";

    /** When student enrolled in this course */
    private LocalDateTime enrollmentDate;

    /**
     * Semester or period designation
     * e.g., "FULL_YEAR", "FIRST_SEMESTER", "SECOND_SEMESTER"
     */
    private String semester;

    /** Course-specific notes for this student */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Whether this student is currently active in this course */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    void prePersist() {
        if (enrollmentDate == null) {
            enrollmentDate = LocalDateTime.now();
        }
        if (enrollmentStatus == null || enrollmentStatus.isBlank()) {
            enrollmentStatus = "ACTIVE";
        }
    }

    @PreUpdate
    void preUpdate() {
        // Keep enrollment date unchanged on updates
    }
}