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
@Table(name = "AnneeScolaire")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AnneeScolaire  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;
    @Column(name = "libelleAnneesc", nullable = false)
    private String libelleAnneesc;

    @OneToMany(
            mappedBy = "anneeScolaire",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Inscription> inscriptions = new ArrayList<>();
}