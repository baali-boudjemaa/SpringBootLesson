package com.example.mef.demo.Model;


import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenPurchaseRepository
        extends JpaRepository<KitchenPurchase, String> {
}