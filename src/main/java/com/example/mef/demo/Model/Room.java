package com.example.mef.demo.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A physical room / salle that can be assigned to one or more sections
 * (Classroom). A room is shared infrastructure: several sections may be
 * linked to it, but they may not occupy it at overlapping times — that
 * conflict is checked in ClassroomService against each room's occupants'
 * weekly schedules.
 */
@Getter
@Setter
@Entity
@Table(name = "Room")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    /** Optional building / floor / wing description. */
    @Column
    private String location;

    /** Optional max number of students/people the room can host. */
    @Column
    private Integer capacity;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Sections currently linked to this room. */
    @ManyToMany(mappedBy = "rooms")
    @Builder.Default
    private List<Classroom> classrooms = new ArrayList<>();
}