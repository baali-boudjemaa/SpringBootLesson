package com.example.mef.demo.Model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Ingredient")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Ingredient  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private Double currentStock = 0.0;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    @Builder.Default
    private Double alertThreshold = 5.0;

    @OneToMany(
            mappedBy = "ingredient",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<KitchenNeed> kitchenNeeds = new ArrayList<>();

    @OneToMany(
            mappedBy = "ingredient",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PurchaseItem> purchaseItems = new ArrayList<>();
}