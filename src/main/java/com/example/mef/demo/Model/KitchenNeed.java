package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Ingredient;
import com.example.mef.demo.enums.NeedStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "KitchenNeed")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class KitchenNeed  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private Double quantityNeeded;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NeedStatus status = NeedStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredientId", nullable = false)
    private Ingredient ingredient;
}