package com.example.mef.demo.Model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Guardian")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Guardian {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    @Pattern(
            regexp = "^(05|06|07)\\d{8}$",
            message = "Numéro de téléphone invalide. Doit commencer par 05, 06 ou 07 et contenir 10 chiffres."
    )
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String relation;

    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studentId")
    private Student student;

}