package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Inscription;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class AnneeScolaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @NotNull(message = "Le libellé de l'année scolaire est obligatoire")
    @Pattern(
            regexp = "^(19|20)\\d{2}-(19|20)\\d{2}$",
            message = "Format d'année scolaire invalide. Utilisez le format YYYY-YYYY (Ex: 2025-2026)"
    )
    @Column(name = "libelleAnneesc", nullable = false, unique = true)
    private String libelleAnneesc;

    @OneToMany(
            mappedBy = "anneeScolaire",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Inscription> inscriptions = new ArrayList<>();

    /**
     * Clean readable string mapping for JavaFX ComboBoxes
     */
    @Override
    public String toString() {
        return this.libelleAnneesc != null ? this.libelleAnneesc : "Nouvelle Année";
    }
}
