package com.example.mef.demo.Model;

import com.example.mef.demo.enums.TeacherAttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** One daily attendance record for a teacher.  Hours are used for partial absences. */
@Getter
@Setter
@Entity
@Table(name = "teacher_attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "attendance_date"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Employee teacher;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TeacherAttendanceStatus status = TeacherAttendanceStatus.PRESENT;

    @Column(nullable = false)
    @Builder.Default
    private Double absentHours = 0d;
}
