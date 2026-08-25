package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.enums.BloodType;
import com.example.mef.demo.enums.Sexe;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student {
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

    @Enumerated(EnumType.STRING)
    private BloodType bloodType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ===== UPDATED RELATIONSHIPS =====

    /** Student's enrollment in specific courses */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentCourse> courseEnrollments = new ArrayList<>();

    /** Attendance records (now tracks course-specific attendance) */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Attendance> attendanceRecords = new ArrayList<>();

    /** Administrative enrollments (for billing, registration, etc.) */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    // ===== HELPER METHODS =====

    /**
     * Get all active courses for this student
     */
    public List<Course> getActiveCourses() {
        return courseEnrollments.stream()
                .filter(sc -> sc.isActive() && "ACTIVE".equals(sc.getEnrollmentStatus()))
                .map(StudentCourse::getCourse)
                .toList();
    }

    /**
     * Get all courses for this student (including inactive)
     */
    public List<Course> getAllCourses() {
        return courseEnrollments.stream()
                .map(StudentCourse::getCourse)
                .toList();
    }

    /**
     * Check if student is enrolled in a specific course
     */
    public boolean isEnrolledInCourse(Course course) {
        return courseEnrollments.stream()
                .anyMatch(sc -> sc.getCourse().getId().equals(course.getId()) && sc.isActive());
    }

    /**
     * Enroll student in a course
     */
    public void enrollInCourse(Course course, String semester) {
        if (isEnrolledInCourse(course)) {
            return; // Already enrolled
        }

        StudentCourse enrollment = StudentCourse.builder()
                .student(this)
                .course(course)
                .semester(semester)
                .enrollmentStatus("ACTIVE")
                .active(true)
                .build();

        courseEnrollments.add(enrollment);
    }

    /**
     * Drop student from a course
     */
    public void dropFromCourse(Course course) {
        courseEnrollments.stream()
                .filter(sc -> sc.getCourse().getId().equals(course.getId()))
                .findFirst()
                .ifPresent(sc -> {
                    sc.setActive(false);
                    sc.setEnrollmentStatus("DROPPED");
                });
    }

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