package com.example.mef.demo.Model;

import com.example.mef.demo.Model.Ingredient;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "PurchaseItem")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PurchaseItem  {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchaseId", nullable = false)
    private KitchenPurchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredientId", nullable = false)
    private Ingredient ingredient;

    @Transient
    public Double getTotalPrice() {
        return quantity * unitPrice;
    }
}