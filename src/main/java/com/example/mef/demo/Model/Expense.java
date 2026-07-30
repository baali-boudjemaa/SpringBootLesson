package com.example.mef.demo.Model;


import com.example.mef.demo.enums.ExpenseCategory;
import jakarta.persistence.Column;
import lombok.Builder;
import org.springframework.data.jpa.repository.JpaRepository;



import com.example.mef.demo.enums.ExpenseCategory;
import jakarta.persistence.*;
        import lombok.*;
        import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@Entity
@Table(name = "\"Expense\"")
@NoArgsConstructor
@AllArgsConstructor
public class Expense  {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime expenseDate = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String description;
}