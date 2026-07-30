package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.KitchenNeed;
import com.example.mef.demo.enums.NeedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenNeedRepository extends JpaRepository<KitchenNeed, Long> {

    List<KitchenNeed> findByIngredientId(Long ingredientId);

    List<KitchenNeed> findByStatus(NeedStatus status);

}
