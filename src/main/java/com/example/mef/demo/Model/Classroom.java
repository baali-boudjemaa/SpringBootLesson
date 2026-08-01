package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Inscription;
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

    @Column(nullable = false)
    private Integer capacity;

    @Column
    private String room;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Lead teacher assigned to this classroom (optional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacherId")
    private Employee teacher;

    /**
     * Students enrolled in this classroom.
     */
    @OneToMany(
            mappedBy = "classroom",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Inscription> inscriptions = new ArrayList<>();
}