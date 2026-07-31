package com.example.mef.demo.Model;


import com.example.mef.demo.enums.AttendanceStatus;
import jakarta.persistence.*;
        import lombok.*;
        import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "Attendance")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Attendance  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studentId")
    private Student student;

    @Column(name = "date")
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column
    private LocalDateTime checkInTime;

    @Column
    private LocalDateTime checkOutTime;
}