package com.example.mef.demo.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "classroom_room_slot")
public class ClassroomRoomSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String dayOfWeek;   // "Lundi", "Mardi", ...

    @Column(nullable = false)
    private String startTime;   // "07:00"

    @Column(nullable = false)
    private String endTime;     // "09:00"
}