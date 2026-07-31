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

    /**
     * Employees assigned to this classroom.
     */
}