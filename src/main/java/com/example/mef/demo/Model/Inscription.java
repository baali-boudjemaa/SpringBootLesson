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

    /**
     * The courses this student follows under this enrollment. This is now the primary
     * link driving which classroom(s) the student attends: each {@link Course} carries
     * its own {@link Classroom}, its own schedule ({@link CourseScheduleSlot}s), and its
     * own teacher — so a student can be "present" in several different classrooms/classes
     * at once simply by following courses taught in each of them, according to whatever
     * timetable suits them. There is deliberately no single fixed classroom on the
     * enrollment anymore.
     */
    @ManyToMany
    @JoinTable(
            name = "inscription_course",
            joinColumns = @JoinColumn(name = "inscriptionId"),
            inverseJoinColumns = @JoinColumn(name = "courseId")
    )
    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    /** Distinct classrooms attended, derived from the classroom of each followed course. */
    @Transient
    public List<Classroom> getClassrooms() {
        if (courses == null) {
            return new ArrayList<>();
        }
        List<Classroom> result = new ArrayList<>();
        for (Course course : courses) {
            Classroom classroom = course.getClassroom();
            if (classroom != null && result.stream().noneMatch(c -> c.getId().equals(classroom.getId()))) {
                result.add(classroom);
            }
        }
        return result;
    }

    /** Comma-separated names of the classrooms attended (derived from courses), or "—" if none. */
    @Transient
    public String classroomsLabel() {
        List<Classroom> classrooms = getClassrooms();
        if (classrooms.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < classrooms.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(classrooms.get(i).getName());
        }
        return sb.toString();
    }

    /** Comma-separated names of the followed courses, or "—" if none. */
    @Transient
    public String coursesLabel() {
        if (courses == null || courses.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < courses.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(courses.get(i).getName());
        }
        return sb.toString();
    }
}