package com.example.mef.demo.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One continuous availability period for one teacher on one day. */
@Getter
@Setter
@Entity
@Table(name = "teacher_availability_slot")
@NoArgsConstructor
public class TeacherAvailabilitySlot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee teacher;

    @Column(nullable = false)
    private String dayOfWeek;

    @Column(nullable = false)
    private String startTime;

    @Column(nullable = false)
    private String endTime;

    public TeacherAvailabilitySlot(String dayOfWeek, String startTime, String endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
