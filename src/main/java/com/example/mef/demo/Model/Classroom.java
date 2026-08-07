package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.enums.Category;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Classroom")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Classroom  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column
    private String ageGroup;

    /** Whether this section is a creche (nursery) or preparatoire (pre-school) group. */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255) default 'CRECHE'")
    @Builder.Default
    private Category category = Category.CRECHE;

    @Column(nullable = false)
    private Integer capacity;

    @Column
    private String room;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    /** Comma-separated day names this class receives students (e.g. "Lundi,Mardi,Mercredi"). Null/blank = no restriction. */
    @Column(columnDefinition = "TEXT")
    private String attendanceDays;

    /** Start of this class's daily period, e.g. "08:00". Null/blank = no restriction. */
    @Column
    private String periodStartTime;

    /** End of this class's daily period, e.g. "17:00". Null/blank = no restriction. */
    @Column
    private String periodEndTime;

    /** Individual occupied cells, e.g. "Lundi 07:00-09:00; Mardi 09:00-11:00". */
    @Column(columnDefinition = "TEXT")
    private String occupancySchedule;

    /**
     * Lead teacher assigned to this classroom (optional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacherId")
    private Employee teacher;

    /**
     * Physical room(s) (salles) this section is allowed to use. A section
     * can be linked to several rooms; the actual times it occupies each
     * room are still described by {@link #occupancySchedule}. Two sections
     * sharing a room must not have overlapping occupancy — enforced in
     * ClassroomService before save.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "classroom_rooms",
            joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();

    /**
     * Students enrolled in this classroom.
     */
    @ManyToMany(
            mappedBy = "classroom",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Inscription> inscriptions = new ArrayList<>();
}