package com.example.mef.demo.Model;





import com.example.mef.demo.enums.PaymentType;
import jakarta.persistence.*;
        import lombok.*;
        import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "\"KitchenPurchase\"")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder

public class KitchenPurchase {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Column
    private String supplier;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime purchaseDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Column(nullable = false)
    @Builder.Default
    private Double totalAmount = 0.0;

    @OneToMany(
            mappedBy = "purchase",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PurchaseItem> purchaseItems = new ArrayList<>();

    /**
     * Calculates the total amount from all purchase items.
     */
    @Transient
    public Double getCalculatedTotal() {
        return purchaseItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
    }

    /**
     * Updates the stored totalAmount before saving/updating.
     */
    @PrePersist
    @PreUpdate
    private void calculateTotal() {
        this.totalAmount = getCalculatedTotal();
    }
}