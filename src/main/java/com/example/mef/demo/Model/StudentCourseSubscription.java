package com.example.mef.demo.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Historical membership of a student in a course.  Unlike Inscription.courses,
 * this entity keeps the dates required for monthly billing.
 */
@Entity
@Table(name = "student_course_subscription",
       uniqueConstraints = @UniqueConstraint(columnNames = {"inscription_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCourseSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDate startDate;

    /** Null means the student is still following this course. */
    private LocalDate endDate;

    public boolean isActiveOn(LocalDate date) {
        // The stopping month remains payable in full, including when the stop
        // happens on the student's monthly due date.
        return !startDate.isAfter(date) && (endDate == null || !endDate.isBefore(date));
    }
}
