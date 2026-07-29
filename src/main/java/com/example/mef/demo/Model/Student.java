package com.example.mef.demo.Model;

import com.example.mef.demo.enums.Sexe;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"Student\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student  {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String studentNumber;

    private String firstName;

    private String lastName;

    private LocalDateTime dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Sexe gender;

    @Column(columnDefinition = "TEXT")
    private String medicalInfo;

    private LocalDateTime enrollmentDate;
}