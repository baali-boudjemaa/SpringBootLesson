package com.example.mef.demo.Model;

import com.example.mef.demo.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "Course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacherId")
    private Employee teacher;

    /** Classroom where this course is held (can be NULL for flexible scheduling) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroomId")
    private Classroom classroom;

    /** All students enrolled in this course */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentCourse> studentEnrollments = new ArrayList<>();

    /** Schedule slots (days/times this course runs) */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<CourseScheduleSlot> scheduleSlots = new ArrayList<>();

    private String schedule;

    @Column(nullable = false)
    @Builder.Default
    private Double monthlyFee = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseStatus status = CourseStatus.ACTIVE;

    // ===== HELPER METHODS =====

    public void replaceScheduleSlots(List<CourseScheduleSlot> slots) {
        scheduleSlots.clear();
        slots.forEach(slot -> {
            slot.setCourse(this);
            scheduleSlots.add(slot);
        });
    }

    /**
     * Get all active student enrollments
     */
    public List<Student> getActiveStudents() {
        return studentEnrollments.stream()
                .filter(sc -> sc.isActive() && "ACTIVE".equals(sc.getEnrollmentStatus()))
                .map(StudentCourse::getStudent)
                .toList();
    }

    /**
     * Get enrollment count
     */
    public int getStudentCount() {
        return (int) studentEnrollments.stream()
                .filter(sc -> sc.isActive())
                .count();
    }

    /**
     * Check if course has capacity
     */
    public boolean hasCapacity() {
        if (classroom == null) return true;
        return getStudentCount() < classroom.getCapacity();
    }
}