package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Student;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "Absence")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Absence  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studentId", nullable = false)
    private Student student;

    @Column(name = "dateAbsc", nullable = false)
    private LocalDateTime dateAbsc;

    @Column(columnDefinition = "TEXT")
    private String motif;
}