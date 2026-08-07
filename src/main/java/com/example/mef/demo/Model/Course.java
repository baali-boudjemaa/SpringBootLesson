package com.example.mef.demo.Model;

import com.example.mef.demo.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Course")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroomId")
    private Classroom classroom;

    private String schedule;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<CourseScheduleSlot> scheduleSlots = new ArrayList<>();

    public void replaceScheduleSlots(List<CourseScheduleSlot> slots) {
        scheduleSlots.clear();
        slots.forEach(slot -> {
            slot.setCourse(this);
            scheduleSlots.add(slot);
        });
    }

    @Column(nullable = false)
    @Builder.Default
    private Double monthlyFee = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseStatus status = CourseStatus.ACTIVE;
}
