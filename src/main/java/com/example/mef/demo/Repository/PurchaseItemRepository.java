package com.example.mef.demo.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, String> {

    List<PurchaseItem> findByPurchaseId(String purchaseId);

    List<PurchaseItem> findByIngredientId(String ingredientId);

}